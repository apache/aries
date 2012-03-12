package org.apache.aries.subsystem.core.internal;

public class ThreadLocalSubsystem {
	private static ThreadLocal<AriesSubsystem> subsystem = new ThreadLocal<AriesSubsystem>();
	
	public static AriesSubsystem get() {
		AriesSubsystem result = (AriesSubsystem)subsystem.get();
		subsystem.remove();
		return result;
	}
	
	public static void set(AriesSubsystem value) {
		subsystem.set(value);
	}
}
