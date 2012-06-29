package org.apache.aries.subsystem.core.internal;

import java.util.EnumSet;

import org.osgi.service.subsystem.Subsystem.State;

public class UninstallAction extends AbstractAction {
	public UninstallAction(AriesSubsystem subsystem) {
		super(subsystem);
	}
	
	@Override
	public Object run() {
		checkRoot();
		State state = subsystem.getState();
		if (EnumSet.of(State.UNINSTALLED).contains(state))
			return null;
		else if (EnumSet.of(State.INSTALL_FAILED, State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING, State.UNINSTALLING).contains(state)) {
			waitForStateChange();
			subsystem.uninstall();
		}
		else if (state.equals(State.ACTIVE)) {
			subsystem.stop();
			subsystem.uninstall();
		}
		else
			ResourceUninstaller.newInstance(subsystem, subsystem).uninstall();
		return null;
	}
}
