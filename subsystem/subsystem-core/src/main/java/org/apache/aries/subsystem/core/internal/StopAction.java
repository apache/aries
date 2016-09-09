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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.aries.subsystem.ContentHandler;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopAction extends AbstractAction {
	private static final Logger logger = LoggerFactory.getLogger(StopAction.class);

	public StopAction(BasicSubsystem requestor, BasicSubsystem target, boolean disableRootCheck) {
		super(requestor, target, disableRootCheck);
	}

	@Override
	public Object run() {
		// Protect against re-entry now that cycles are supported.
		if (!Activator.getInstance().getLockingStrategy().set(State.STOPPING, target)) {
			return null;
		}
		try {
			// We are now protected against re-entry.
			// Acquire the global read lock to prevent installs and uninstalls
			// but allow starts and stops.
			Activator.getInstance().getLockingStrategy().readLock();
			try {
				// We are now protected against installs and uninstalls.
				checkRoot();
				// Compute affected subsystems. This is safe to do while only 
				// holding the read lock since we know that nothing can be added 
				// or removed.
				LinkedHashSet<BasicSubsystem> subsystems = new LinkedHashSet<BasicSubsystem>();
				subsystems.add(target);
				List<Resource> resources = new ArrayList<Resource>(Activator.getInstance().getSubsystems().getResourcesReferencedBy(target));
				for (Resource resource : resources) {
					if (resource instanceof BasicSubsystem) {
						subsystems.add((BasicSubsystem)resource);
					}
				}
				// Acquire the global mutual exclusion lock while acquiring the
				// state change locks of affected subsystems.
				Activator.getInstance().getLockingStrategy().lock();
				try {
					// We are now protected against cycles.
					// Acquire the state change locks of affected subsystems.
					Activator.getInstance().getLockingStrategy().lock(subsystems);
				}
				finally {
					// Release the global mutual exclusion lock as soon as possible.
					Activator.getInstance().getLockingStrategy().unlock();
				}
				try {
					// We are now protected against other starts and stops of the affected subsystems.
					State state = target.getState();
					if (EnumSet.of(State.INSTALLED, State.INSTALLING, State.RESOLVED).contains(state)) {
						// INSTALLING is included because a subsystem may
						// persist in this state without being locked when
						// apache-aries-provision-dependencies:=resolve.
						return null;
					}
					else if (EnumSet.of(State.INSTALL_FAILED, State.UNINSTALLED).contains(state)) {
						throw new IllegalStateException("Cannot stop from state " + state);
					}
					target.setState(State.STOPPING);
					SubsystemContentHeader header = target.getSubsystemManifest().getSubsystemContentHeader();
					if (header != null) {
						Collections.sort(resources, new StartResourceComparator(target.getSubsystemManifest().getSubsystemContentHeader()));
						Collections.reverse(resources);
					}
					for (Resource resource : resources) {
						// Don't stop the region context bundle.
						if (Utils.isRegionContextBundle(resource))
							continue;
						try {
							stopResource(resource);
						}
						catch (Exception e) {
							logger.error("An error occurred while stopping resource " + resource + " of subsystem " + target, e);
						}
					}
					// TODO Can we automatically assume it actually is resolved?
					target.setState(State.RESOLVED);
					try {
						synchronized (target) {
							target.setDeploymentManifest(new DeploymentManifest(
									target.getDeploymentManifest(),
									null,
									target.isAutostart(),
									target.getSubsystemId(),
									SubsystemIdentifier.getLastId(),
									target.getLocation(),
									false,
									false));
						}
					}
					catch (Exception e) {
						throw new SubsystemException(e);
					}
				}
				finally {
					// Release the state change locks of affected subsystems.
					Activator.getInstance().getLockingStrategy().unlock(subsystems);
				}
			}
			finally {
				// Release the read lock.
				Activator.getInstance().getLockingStrategy().readUnlock();
			}
		}
		finally {
			// Protection against re-entry no longer required.
			Activator.getInstance().getLockingStrategy().unset(State.STOPPING, target);
		}
		return null;
	}

	private void stopBundleResource(Resource resource) throws BundleException {
		if (target.isRoot())
			return;
		((BundleRevision)resource).getBundle().stop();
	}

	private void stopResource(Resource resource) throws BundleException, IOException {
		if (Utils.getActiveUseCount(resource) > 0)
			return;
		String type = ResourceHelper.getTypeAttribute(resource);
		if (SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type)) {
			stopSubsystemResource(resource);
		} else if (IdentityNamespace.TYPE_BUNDLE.equals(type)) {
			stopBundleResource(resource);
		} else if (IdentityNamespace.TYPE_FRAGMENT.equals(type)) {
			return;
		} else {
		    if (!stopCustomHandler(resource, type))
		        throw new SubsystemException("Unsupported resource type: " + type);
		}
	}

    private boolean stopCustomHandler(Resource resource, String type) {
        ServiceReference<ContentHandler> customHandlerRef = CustomResources.getCustomContentHandler(target, type);
        if (customHandlerRef != null) {
            ContentHandler customHandler = target.getBundleContext().getService(customHandlerRef);
            if (customHandler != null) {
                try {
                    customHandler.stop(ResourceHelper.getSymbolicNameAttribute(resource), type, target);
                    return true;
                } finally {
                    target.getBundleContext().ungetService(customHandlerRef);
                }
            }
        }
        return false;
    }

	private void stopSubsystemResource(Resource resource) throws IOException {
		new StopAction(target, (BasicSubsystem)resource, !((BasicSubsystem)resource).isRoot()).run();
	}
}
