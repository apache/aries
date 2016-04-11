package org.apache.aries.tx.control.jpa.xa.impl;

public interface LifecycleAware {

	public void start();
	
	public void stop();
}
