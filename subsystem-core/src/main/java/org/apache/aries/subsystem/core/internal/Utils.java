package org.apache.aries.subsystem.core.internal;

import org.osgi.service.coordinator.Coordination;

public class Utils {
	public static Coordination createCoordination(AriesSubsystem subsystem) {
		return Activator.getInstance().getCoordinator().create(subsystem.getSymbolicName() + '-' + subsystem.getSubsystemId(), 0);
	}
	
	public static AriesSubsystem findFirstSubsystemAcceptingDependenciesStartingFrom(AriesSubsystem subsystem) {
		// The following loop is guaranteed to end once the root subsystem has
		// been reached.
		while (!isAcceptDependencies(subsystem))
			subsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
		return subsystem;
	}
	
	public static boolean isAcceptDependencies(AriesSubsystem subsystem) {
		return subsystem.getArchive().getSubsystemManifest().getSubsystemTypeHeader().getProvisionPolicyDirective().isAcceptDependencies();
	}
}
