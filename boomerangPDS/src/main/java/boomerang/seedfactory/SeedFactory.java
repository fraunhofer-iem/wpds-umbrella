/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package boomerang.seedfactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.PushRule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.Weight.NoWeight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

/**
 * Created by johannesspath on 07.12.17.
 */
public abstract class SeedFactory<W extends Weight> {

    private final Multimap<Query, Transition<Method, INode<Reachable>>> seedToTransition = HashMultimap.create();
    private final Multimap<SootMethod, Query> seedsPerMethod = HashMultimap.create();
    private final Map<Method, INode<Reachable>> reachableMethods = new HashMap<Method, INode<Reachable>>();
    private final WeightedPAutomaton<Method, INode<Reachable>, Weight.NoWeight> automaton = new WeightedPAutomaton<Method, INode<Reachable>, Weight.NoWeight>(
            wrap(Reachable.entry())) {
        @Override
        public INode<Reachable> createState(INode<Reachable> reachable, Method loc) {
            if (!reachableMethods.containsKey(loc)) {
                reachableMethods.put(loc, new GeneratedState<>(reachable, loc));
            }
            return reachableMethods.get(loc);
        }

        @Override
        public boolean isGeneratedState(INode<Reachable> reachable) {
            return reachable instanceof GeneratedState;
        }

        @Override
        public Method epsilon() {
            return Method.epsilon();
        }

        @Override
        public Weight.NoWeight getZero() {
            return Weight.NO_WEIGHT_ZERO;
        }

        @Override
        public Weight.NoWeight getOne() {
            return Weight.NO_WEIGHT_ONE;
        }
    };
    private Collection<SootMethod> processed = Sets.newHashSet();
    private Multimap<Query, SootMethod> queryToScope = HashMultimap.create();

    public Collection<Query> computeSeeds() {
        List<SootMethod> entryPoints = Scene.v().getEntryPoints();
        System.out.print("Computing seeds starting at " + entryPoints.size() + " entry method(s).");
        Stopwatch watch = Stopwatch.createStarted();
        for (SootMethod m : entryPoints) {
            automaton.addTransition(new Transition<>(wrap(Reachable.v()), new Method(m), automaton.getInitialState()));
        }
        automaton.registerListener(new WPAUpdateListener<Method, INode<Reachable>, Weight.NoWeight>() {
            @Override
            public void onWeightAdded(Transition<Method, INode<Reachable>> t, Weight.NoWeight noWeight,
                    WeightedPAutomaton<Method, INode<Reachable>, Weight.NoWeight> aut) {
                process(t);
            }
        });

        if (analyseClassInitializers()) {
            Set<SootClass> sootClasses = Sets.newHashSet();
            for (SootMethod p : Sets.newHashSet(processed)) {
                if (sootClasses.add(p.getDeclaringClass())) {
                    addStaticInitializerFor(p.getDeclaringClass());
                }
            }
        }
        System.out.print("Seed finding took " + watch.elapsed(TimeUnit.SECONDS) + " second(s) and analyzed "
                + processed.size() + " method(s).");
        automaton.clearListener();
        return seedToTransition.keySet();
    }

    private void addStaticInitializerFor(SootClass declaringClass) {
        for (SootMethod m : declaringClass.getMethods()) {
            if (m.isStaticInitializer()) {
                for (SootMethod ep : Scene.v().getEntryPoints()) {
                    addPushRule(new Method(ep), new Method(m));
                }
            }
        }
    }

    protected boolean analyseClassInitializers() {
        return false;
    }

    protected abstract Collection<? extends Query> generate(SootMethod method, Stmt u);

    private void process(Transition<Method, INode<Reachable>> t) {
        Method curr = t.getLabel();
        SootMethod m = curr.getMethod();
        if (!m.hasActiveBody())
            return;
        computeQueriesPerMethod(m);
        for (Query q : seedsPerMethod.get(m)) {
            seedToTransition.put(q, t);
        }
    }

    private void computeQueriesPerMethod(SootMethod m) {
        if (!processed.add(m)) {
            return;
        }
        Set<Query> seeds = Sets.newHashSet();
        for (Unit u : m.getActiveBody().getUnits()) {
            if (icfg().isCallStmt(u)) {
                icfg().addCalleeListener(new CalleeListener<Unit, SootMethod>() {
                    @Override
                    public Unit getObservedCaller() {
                        return u;
                    }

                    @Override
                    public void onCalleeAdded(Unit n, SootMethod callee) {
                        if (!callee.hasActiveBody() || !callee.getDeclaringClass().isApplicationClass())
                            return;
                        addPushRule(new Method(m), new Method(callee));
                    }
                });
            }
            seeds.addAll(generate(m, (Stmt) u));
        }
        seedsPerMethod.putAll(m, seeds);
    }

    private void addPushRule(Method caller, Method callee) {
        automaton.addTransition(
                new Transition<Method, INode<Reachable>>(automaton.createState(wrap(Reachable.v()), caller), callee,
                        automaton.createState(wrap(Reachable.v()), callee)));
    }

    private INode<Reachable> wrap(Reachable r) {
        return new SingleNode<>(r);
    }

    public abstract ObservableICFG<Unit, SootMethod> icfg();

    public Collection<SootMethod> getMethodScope(Query query) {
        if (queryToScope.containsKey(query)) {
            return queryToScope.get(query);
        }
        queryToScope.putAll(query, transitiveClosure(query));
        return queryToScope.get(query);
    }

    private Set<SootMethod> transitiveClosure(Query query) {
        Set<SootMethod> scope = Sets.newHashSet();
    	Set<INode<Reachable>> visited = Sets.newHashSet();
    	LinkedList<INode<Reachable>> worklist = Lists.newLinkedList();
        for (Transition<Method, INode<Reachable>> t : seedToTransition.get(query)) {
        	worklist.add(t.getTarget());
        }
    	while(!worklist.isEmpty()) {
    		INode<Reachable> first = worklist.pop();
    		visited.add(first);
    		for(Transition<Method, INode<Reachable>> t : automaton.getTransitionsOutOf(first)) {
    			if(!visited.contains(t.getTarget())) {
    				worklist.add(t.getTarget());
    			}
    			scope.add(t.getLabel().getMethod());
    		}
    	}
    	return scope;
    }


    public Collection<SootMethod> getAnyMethodScope() {
        Set<SootMethod> out = Sets.newHashSet();
        for (Query q : seedToTransition.keySet()) {
            out.addAll(getMethodScope(q));
        }
        return out;
    }

}
