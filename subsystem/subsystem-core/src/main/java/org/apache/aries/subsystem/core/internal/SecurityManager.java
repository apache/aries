/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
