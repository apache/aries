package org.apache.aries.subsystem.core.internal;

public class SubsystemIdentifier {
	private static long lastId;
	
	synchronized static long getLastId() {
		return lastId;
	}
	
	synchronized static long getNextId() {
		if (Long.MAX_VALUE == lastId)
			throw new IllegalStateException("The next subsystem ID would exceed Long.MAX_VALUE: " + lastId);
		// First ID will be 1.
		return ++lastId;
	}
	
	synchronized static void setLastId(long id) {
		lastId = id;
	}
}
