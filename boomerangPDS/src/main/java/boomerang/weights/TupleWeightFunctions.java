package boomerang.weights;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class TupleWeightFunctions<A extends Weight, B extends Weight> implements WeightFunctions<Statement, Val, Statement, WeightTuple<A,B>>{

	protected final WeightFunctions<Statement, Val, Statement, A> aFunc;
	protected final WeightFunctions<Statement, Val, Statement, B> bFunc;

	public TupleWeightFunctions(WeightFunctions<Statement, Val, Statement, A> aFunc, WeightFunctions<Statement, Val, Statement, B> bFunc) {
		this.aFunc = aFunc;
		this.bFunc = bFunc;
	}
	
	@Override
	public WeightTuple<A, B> push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement field) {
		A a = aFunc.push(curr, succ, field);
		B b = bFunc.push(curr, succ, field);
		return new WeightTuple<A, B>(a, b);
	}

	@Override
	public WeightTuple<A, B> normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		A a = aFunc.normal(curr, succ);
		B b = bFunc.normal(curr, succ);
		return new WeightTuple<A, B>(a, b);
	}

	@Override
	public WeightTuple<A, B> pop(Node<Statement, Val> curr, Statement location) {
		A a = aFunc.pop(curr, location);
		B b = bFunc.pop(curr, location);
		return new WeightTuple<A, B>(a, b);
	}

	@Override
	public WeightTuple<A, B> getOne() {
		return new WeightTuple<A,B>(aFunc.getOne(), bFunc.getOne());
	}

	@Override
	public WeightTuple<A, B> getZero() {
		return new WeightTuple<A,B>(aFunc.getZero(), bFunc.getZero());
	}

}
