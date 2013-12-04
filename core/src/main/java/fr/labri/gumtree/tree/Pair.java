package fr.labri.gumtree.tree;

public class Pair<T1, T2> {
	
	private T1 first;
	
	private T2 second;
	
	public Pair(T1 a, T2 b) {
		this.first = a;
		this.second = b;
	}
	
	public void setFirst(T1 a) {
		this.first = a;
	}
	
	public void setSecond(T2 b) {
		this.second = b;
	}
	
	public T1 getFirst() {
		return first;
	}
	
	public T2 getSecond() {
		return second;
	}
	
	public int hashCode() {
		return first.hashCode() + second.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair<?, ?> p = (Pair<?, ?>) o;
			return p.getFirst().equals(this.getFirst()) && p.getSecond().equals(this.getSecond());
		} else return false;
	}
	
	@Override
	public String toString() {
		return "(" + getFirst() + "," + getSecond() + ")";
	}
	
}
