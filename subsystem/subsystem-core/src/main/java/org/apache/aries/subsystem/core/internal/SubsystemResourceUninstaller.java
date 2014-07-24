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

import org.apache.aries.util.io.IOUtils;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemResourceUninstaller extends ResourceUninstaller {
	private static final Logger logger = LoggerFactory.getLogger(BasicSubsystem.class);
	
	private static void removeChild(BasicSubsystem parent, BasicSubsystem child) {
		Activator.getInstance().getSubsystems().removeChild(parent, child);
	}
	
	public SubsystemResourceUninstaller(Resource resource, BasicSubsystem subsystem) {
		super(resource, subsystem);
	}
	
	public void uninstall() {
		removeReferences();
		try {
			if (isResourceUninstallable())
				uninstallSubsystem();
		}
		finally {
			removeConstituents();
			removeChildren();
			removeSubsystem();
		}
	}
	
	private void removeChildren() {
		if (!isExplicit()) {
			removeChild((BasicSubsystem)subsystem, (BasicSubsystem)resource);
			return;
		}
		for (Subsystem subsystem : ((BasicSubsystem)resource).getParents())
			removeChild((BasicSubsystem)subsystem, (BasicSubsystem)resource);
	}
	
	private void removeConstituents() {
		if (!isExplicit()) {
			removeConstituent();
			return;
		}
		for (Subsystem subsystem : ((BasicSubsystem)resource).getParents())
			removeConstituent((BasicSubsystem)subsystem, (BasicSubsystem)resource);
	}
	
	private void removeReferences() {
		if (!isExplicit()) {
			removeReference();
		}
		else {
			for (Subsystem subsystem : ((BasicSubsystem)resource).getParents())
				removeReference((BasicSubsystem)subsystem, (BasicSubsystem)resource);
			Subsystems subsystems = Activator.getInstance().getSubsystems();
			// for explicit uninstall remove all references to subsystem.
			for (BasicSubsystem s : subsystems.getSubsystemsReferencing(resource)) {
				removeReference(s, resource);
			}
		}
	}
	
	private void removeSubsystem() {
		Activator.getInstance().getSubsystems().removeSubsystem((BasicSubsystem)resource);
	}
	
	private void uninstallSubsystem() {
		BasicSubsystem subsystem = (BasicSubsystem) resource;
		try {
			if (subsystem.getState().equals(Subsystem.State.RESOLVED))
				subsystem.setState(State.INSTALLED);
			subsystem.setState(State.UNINSTALLING);
			Throwable firstError = null;
			for (Resource resource : Activator.getInstance().getSubsystems()
					.getResourcesReferencedBy(subsystem)) {
				// Don't uninstall the region context bundle here.
				if (Utils.isRegionContextBundle(resource))
					continue;
				try {
					ResourceUninstaller.newInstance(resource, subsystem)
							.uninstall();
				} catch (Throwable t) {
					logger.error("An error occurred while uninstalling resource "
							+ resource + " of subsystem " + subsystem, t);
					if (firstError == null)
						firstError = t;
				}
			}
			subsystem.setState(State.UNINSTALLED);
			Activator activator = Activator.getInstance();
			activator.getSubsystemServiceRegistrar().unregister(subsystem);
			if (subsystem.isScoped()) {
				RegionContextBundleHelper.uninstallRegionContextBundle(subsystem);
				activator.getRegionDigraph().removeRegion(subsystem.getRegion());
			}
			if (firstError != null)
				throw new SubsystemException(firstError);
		}
		finally {
			// Let's be sure to always clean up the directory.
			IOUtils.deleteRecursive(subsystem.getDirectory());
		}
	}
}