package org.apache.aries.subsystem.core.internal;

import java.util.EnumSet;

import org.osgi.service.subsystem.Subsystem.State;

public class UninstallAction extends AbstractAction {
	public UninstallAction(AriesSubsystem subsystem, boolean disableRootCheck, boolean explicit) {
		super(subsystem, disableRootCheck, explicit);
	}
	
	@Override
	public Object run() {
		checkValid();
		checkRoot();
		State state = subsystem.getState();
		if (EnumSet.of(State.UNINSTALLED).contains(state))
			return null;
		else if (EnumSet.of(State.INSTALL_FAILED, State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING, State.UNINSTALLING).contains(state)) {
			waitForStateChange();
			subsystem.uninstall();
		}
		else if (state.equals(State.ACTIVE)) {
			new StopAction(subsystem, disableRootCheck, explicit).run();
			subsystem.uninstall();
		}
		else
			ResourceUninstaller.newInstance(subsystem, subsystem).uninstall();
		return null;
	}
}
