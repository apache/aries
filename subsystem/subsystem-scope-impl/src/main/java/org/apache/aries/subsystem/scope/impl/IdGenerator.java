package org.apache.aries.subsystem.scope.impl;

public class IdGenerator {
	private long lastId;
	
	public IdGenerator() {
		this(0);
	}
	
	public IdGenerator(long firstId) {
		lastId = firstId;
	}
	
	public synchronized long nextId() {
		return lastId++;
	}
}
