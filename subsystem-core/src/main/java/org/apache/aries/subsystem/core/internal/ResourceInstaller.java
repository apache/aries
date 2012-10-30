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

import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public abstract class ResourceInstaller {
	public static ResourceInstaller newInstance(Coordination coordination, Resource resource, AriesSubsystem subsystem) {
		String type = ResourceHelper.getTypeAttribute(resource);
		if (SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type))
			return new SubsystemResourceInstaller(coordination, resource, subsystem);
		else if (IdentityNamespace.TYPE_BUNDLE.equals(type) || IdentityNamespace.TYPE_FRAGMENT.equals(type))
			return new BundleResourceInstaller(coordination, resource, subsystem);
		else
			throw new SubsystemException("No installer exists for resource type: " + type);
	}
	
	protected final Coordination coordination;
	protected final AriesSubsystem provisionTo;
	protected final Resource resource;
	protected final AriesSubsystem subsystem;
	
	public ResourceInstaller(Coordination coordination, Resource resource, AriesSubsystem subsystem) {
		this.coordination = coordination;
		this.resource = resource;
		this.subsystem = subsystem;
		if (isDependency()) {
			if (Utils.isInstallableResource(resource))
				provisionTo = Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(subsystem);
			else
				provisionTo = null;
		}
		else
			provisionTo = subsystem;
	}
	
	public abstract Resource install() throws Exception;
	
	protected void addConstituent(final Resource resource) {
		// Don't let a resource become a constituent of itself.
		if (provisionTo == null || resource.equals(provisionTo))
			return;
		Activator.getInstance().getSubsystems().addConstituent(provisionTo, resource, isReferencedProvisionTo());
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination arg0) throws Exception {
				// Nothing
			}

			@Override
			public void failed(Coordination arg0) throws Exception {
				Activator.getInstance().getSubsystems().removeConstituent(provisionTo, resource);
			}
		});
	}
	
	protected void addReference(final Resource resource) {
		// Don't let a resource reference itself.
		if (resource.equals(subsystem))
			return;
		// The following check protects against resources posing as content
		// during a restart since the Deployed-Content header is currently used
		// to track all constituents for persistence purposes, which includes
		// resources that were provisioned to the subsystem as dependencies of
		// other resources.
		if (isReferencedSubsystem())
			Activator.getInstance().getSubsystems().addReference(subsystem, resource);
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination arg0) throws Exception {
				// Nothing
			}

			@Override
			public void failed(Coordination arg0) throws Exception {
				Activator.getInstance().getSubsystems().removeReference(subsystem, resource);
			}
		});
	}
	
	protected String getLocation() {
		return provisionTo.getLocation() + "!/" + ResourceHelper.getLocation(resource);
	}
	
	protected boolean isContent() {
		return Utils.isContent(subsystem, resource);
	}
	
	protected boolean isDependency() {
		return Utils.isDependency(subsystem, resource);
	}
	
	protected boolean isReferencedProvisionTo() {
		DeploymentManifest manifest = subsystem.getDeploymentManifest();
		if (manifest != null) {
			DeployedContentHeader header = manifest.getDeployedContentHeader();
			if (header != null && header.contains(resource))
				return subsystem.isReferenced(resource);
		}
		if (subsystem.equals(provisionTo))
			return isReferencedSubsystem();
		return false;
	}
	
	protected boolean isReferencedSubsystem() {
		DeploymentManifest manifest = subsystem.getDeploymentManifest();
		if (manifest != null) {
			DeployedContentHeader header = manifest.getDeployedContentHeader();
			if (header != null && header.contains(resource))
				return subsystem.isReferenced(resource);
		}
		return true;
	}
}
