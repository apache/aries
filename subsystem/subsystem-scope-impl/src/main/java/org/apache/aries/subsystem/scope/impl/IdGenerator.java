package org.apache.aries.subsystem.scope.impl;

public class IdGenerator {
	private long lastId;
	
	public IdGenerator() {
//IC see: https://issues.apache.org/jira/browse/ARIES-644
//IC see: https://issues.apache.org/jira/browse/ARIES-645
		this(0);
	}
	
	public IdGenerator(long firstId) {
		lastId = firstId;
	}
	
	public synchronized long nextId() {
		return lastId++;
	}
}
