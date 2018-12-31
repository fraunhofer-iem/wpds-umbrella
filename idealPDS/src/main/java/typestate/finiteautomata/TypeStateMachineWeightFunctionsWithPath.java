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
package typestate.finiteautomata;

import java.util.Collection;

import com.google.common.collect.Sets;

import boomerang.WeightedForwardQuery;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.weights.DataFlowPathWeight;
import boomerang.weights.TupleWeightFunctions;
import boomerang.weights.WeightTuple;
import soot.SootMethod;
import soot.Unit;
import sync.pds.solver.WeightFunctions;
import typestate.TransitionFunction;

public abstract class TypeStateMachineWeightFunctionsWithPath extends  TupleWeightFunctions<TransitionFunction, DataFlowPathWeight> {
	private TypeStateMachineWeightFunctions delegateA;
	private WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> delegateB;

	public TypeStateMachineWeightFunctionsWithPath(TypeStateMachineWeightFunctions aFunc,
			WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> bFunc) {
		super(aFunc, bFunc);
		delegateA = aFunc;
	}

	public Collection<WeightedForwardQuery<WeightTuple<TransitionFunction,DataFlowPathWeight>>> generateSeed(SootMethod method, Unit stmt,
			Collection<SootMethod> calledMethod) {
		Collection<WeightedForwardQuery<TransitionFunction>> generateSeed = delegateA.generateSeed(method, stmt, calledMethod);
		Collection<WeightedForwardQuery<WeightTuple<TransitionFunction,DataFlowPathWeight>>> res = Sets.newHashSet();
		for(WeightedForwardQuery<TransitionFunction> s : generateSeed) {
			res.add(new WeightedForwardQuery<>(s.stmt(), s.var(), new WeightTuple<>(s.weight(),delegateB.getOne())));
		}
		return res;
	}
}
	
