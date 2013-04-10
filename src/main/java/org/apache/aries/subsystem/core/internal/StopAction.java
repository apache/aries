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
import java.util.List;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.osgi.framework.BundleException;
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
		checkRoot();
		State state = target.getState();
		if (EnumSet.of(State.INSTALLED, State.RESOLVED).contains(state))
			return null;
		else if (EnumSet.of(State.INSTALL_FAILED, State.UNINSTALLING, State.UNINSTALLED).contains(state))
			throw new IllegalStateException("Cannot stop from state " + state);
		else if (EnumSet.of(State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING).contains(state)) {
			waitForStateChange(state);
			return new StopAction(requestor, target, disableRootCheck).run();
		}
		target.setState(State.STOPPING);
		List<Resource> resources = new ArrayList<Resource>(Activator.getInstance().getSubsystems().getResourcesReferencedBy(target));
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
		catch (Exception e) {
			throw new SubsystemException(e);
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
				|| SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type))
			stopSubsystemResource(resource);
		else if (IdentityNamespace.TYPE_BUNDLE.equals(type))
			stopBundleResource(resource);
		else if (IdentityNamespace.TYPE_FRAGMENT.equals(type))
			return;
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}
	
	private void stopSubsystemResource(Resource resource) throws IOException {
		new StopAction(target, (BasicSubsystem)resource, !((BasicSubsystem)resource).isRoot()).run();
	}
}
