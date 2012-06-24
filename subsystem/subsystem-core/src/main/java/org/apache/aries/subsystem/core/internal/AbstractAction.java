package org.apache.aries.subsystem.core.internal;

import java.security.PrivilegedAction;

import org.osgi.service.subsystem.SubsystemException;

public abstract class AbstractAction implements PrivilegedAction<Object> {
	protected final boolean disableRootCheck;
	protected final AriesSubsystem subsystem;
	
	public AbstractAction(AriesSubsystem subsystem) {
		this(subsystem, false);
	}
	
	public AbstractAction(AriesSubsystem subsystem, boolean disableRootCheck) {
		this.subsystem = subsystem;
		this.disableRootCheck = disableRootCheck;
	}
	
	protected void checkRoot() {
		if (!disableRootCheck && subsystem.isRoot())
			throw new SubsystemException("This operation may not be performed on the root subsystem");
	}
	
	protected void waitForStateChange() {
		synchronized (subsystem) {
			try {
				subsystem.wait();
			}
			catch (InterruptedException e) {
				throw new SubsystemException(e);
			}
		}
	}
}
