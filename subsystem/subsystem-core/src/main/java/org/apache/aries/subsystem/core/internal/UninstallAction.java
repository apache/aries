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
		// UNINSTALLING is included here because the transition to UNINSTALLED
		// is guaranteed, so there's no point in waiting.
		if (EnumSet.of(State.UNINSTALLING, State.UNINSTALLED).contains(state))
			return null;
		else if (EnumSet.of(State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING).contains(state)) {
			waitForStateChange();
			subsystem.uninstall();
		}
		else if (state.equals(State.ACTIVE)) {
			subsystem.stop();
			subsystem.uninstall();
		}
		else
			ResourceUninstaller.newInstance(subsystem).uninstall();
		return null;
	}
}
