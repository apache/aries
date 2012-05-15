package org.apache.aries.subsystem.core.internal;

import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemPermission;

public class SecurityManager {
	public static void checkContextPermission(Subsystem subsystem) {
		checkPermission(new SubsystemPermission(subsystem, SubsystemPermission.CONTEXT));
	}
	
	public static void checkExecutePermission(Subsystem subsystem) {
		checkPermission(new SubsystemPermission(subsystem, SubsystemPermission.EXECUTE));
	}
	
	public static void checkLifecyclePermission(Subsystem subsystem) {
		checkPermission(new SubsystemPermission(subsystem, SubsystemPermission.LIFECYCLE));
	}
	
	public static void checkMetadataPermission(Subsystem subsystem) {
		checkPermission(new SubsystemPermission(subsystem, SubsystemPermission.METADATA));
	}
	
	public static void checkPermission(SubsystemPermission permission) {
		java.lang.SecurityManager sm = System.getSecurityManager();
		if (sm == null)
			return;
		sm.checkPermission(permission);
	}
}
