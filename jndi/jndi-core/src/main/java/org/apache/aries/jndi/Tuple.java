package org.apache.aries.jndi;

public class Tuple<U,V> {
	public final U first;
	public final V second;
	
	public Tuple(U first, V second) {
		this.first = first;
		this.second = second;
	}
}
