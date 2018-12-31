package boomerang.weights;

import wpds.impl.Weight;

public class WeightTuple<A extends Weight, B extends Weight> extends Weight {

	private final A a;
	private final B b;
	
	public WeightTuple(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	@Override
	public Weight extendWith(Weight other) {
		if(other instanceof WeightTuple) {
			WeightTuple o = (WeightTuple) other;
			return new WeightTuple(a.extendWith(o.a), b.extendWith(o.b));
		}
		throw new IllegalStateException("Cannot extend the weights");
	}

	@Override
	public Weight combineWith(Weight other) {
		if(other instanceof WeightTuple) {
			WeightTuple o = (WeightTuple) other;
			return new WeightTuple(a.combineWith(o.a), b.combineWith(o.b));
		}
		throw new IllegalStateException("Cannot combine the weights");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WeightTuple other = (WeightTuple) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		return true;
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
}
