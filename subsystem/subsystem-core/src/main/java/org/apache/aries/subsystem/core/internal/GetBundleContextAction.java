package org.apache.aries.subsystem.core.internal;

import java.security.PrivilegedAction;
import java.util.EnumSet;

import org.osgi.framework.BundleContext;
import org.osgi.service.subsystem.Subsystem.State;

public class GetBundleContextAction implements PrivilegedAction<BundleContext> {
	private final AriesSubsystem subsystem;
	
	public GetBundleContextAction(AriesSubsystem subsystem) {
		this.subsystem = subsystem;
	}
	
	@Override
	public BundleContext run() {
		if (EnumSet.of(State.INSTALL_FAILED, State.UNINSTALLED).contains(
				subsystem.getState()))
			return null;
		AriesSubsystem subsystem = Utils.findScopedSubsystemInRegion(this.subsystem);
		return subsystem.getRegion().getBundle(
				RegionContextBundleHelper.SYMBOLICNAME_PREFIX
						+ subsystem.getSubsystemId(),
				RegionContextBundleHelper.VERSION).getBundleContext();
	}
}
