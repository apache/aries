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

import java.util.Collection;
import java.util.HashSet;

import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;

public class TargetRegion {
	Collection<Subsystem> region = new HashSet<Subsystem>();

	public TargetRegion(BasicSubsystem subsystem) {
		// Find the scoped subsystem that controls the region.
		while (!subsystem.isScoped()) {
			subsystem = (BasicSubsystem) subsystem.getParents().iterator().next();
		}
		// All children of the scoped subsystem controlling the region are
		// part of the target region, even those that are scoped subsystems.
		add(subsystem.getChildren());
	}

	public boolean contains(Subsystem subsystem) {
		return find(subsystem.getSymbolicName(), subsystem.getVersion()) != null;
	}
	
	public Subsystem find(String symbolicName, Version version) {
		for (Subsystem s : region) {
			if (s.getSymbolicName().equals(symbolicName)
					&& s.getVersion().equals(version))
				return s;
		}
		return null;
	}
	
	private void add(Collection<Subsystem> children) {
		for (Subsystem child : children) {
			region.add(child);
			if (((BasicSubsystem) child).isScoped()) {
				// Children of scoped children are not part of the target region.
				continue;
			}
			// Children of unscoped children are part of the target region.
			add(child.getChildren());
		}
	}
}
