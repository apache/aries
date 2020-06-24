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

import org.apache.aries.subsystem.ContentHandler;
import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public abstract class ResourceInstaller {
	public static ResourceInstaller newInstance(Coordination coordination, Resource resource, BasicSubsystem subsystem) {
		String type = ResourceHelper.getTypeAttribute(resource);
		if (SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)
//IC see: https://issues.apache.org/jira/browse/ARIES-1252
				|| SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type)) {
			return new SubsystemResourceInstaller(coordination, resource, subsystem);
		} else if (IdentityNamespace.TYPE_BUNDLE.equals(type) || IdentityNamespace.TYPE_FRAGMENT.equals(type)) {
			return new BundleResourceInstaller(coordination, resource, subsystem);
		} else if (Constants.ResourceTypeSynthesized.equals(type)) {
//IC see: https://issues.apache.org/jira/browse/ARIES-997
			return new ResourceInstaller(coordination, resource, subsystem) {
				@Override
				public Resource install() throws Exception {
					// do nothing;
					return resource;
				}
			};
		} else {
		    ServiceReference<ContentHandler> handlerRef = CustomResources.getCustomContentHandler(subsystem, type);
		    if (handlerRef != null)
		        return new CustomResourceInstaller(coordination, resource, type, subsystem, handlerRef);

		}
		throw new SubsystemException("No installer exists for resource type: " + type);
	}

    protected final Coordination coordination;
	protected final BasicSubsystem provisionTo;
	/* resource to install */
	protected final Resource resource;
	/* parent subsystem being installed into */
	protected final BasicSubsystem subsystem;

	public ResourceInstaller(Coordination coordination, Resource resource, BasicSubsystem subsystem) {
		if (coordination == null || resource == null || subsystem == null) {
			// We're assuming these are not null post construction, so enforce it here.
			throw new NullPointerException();
		}
		this.coordination = coordination;
		this.resource = resource;
		this.subsystem = subsystem;
		if (isDependency()) {
//IC see: https://issues.apache.org/jira/browse/ARIES-825
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
//IC see: https://issues.apache.org/jira/browse/ARIES-907
		if (provisionTo == null || resource.equals(provisionTo))
			return;
		Activator.getInstance().getSubsystems().addConstituent(provisionTo, resource, isReferencedProvisionTo());
	}

	protected void addReference(final Resource resource) {
		// Don't let a resource reference itself.
//IC see: https://issues.apache.org/jira/browse/ARIES-825
		if (resource.equals(subsystem))
			return;
		// The following check protects against resources posing as content
		// during a restart since the Deployed-Content header is currently used
		// to track all constituents for persistence purposes, which includes
		// resources that were provisioned to the subsystem as dependencies of
		// other resources.
		if (isReferencedSubsystem()) {
//IC see: https://issues.apache.org/jira/browse/ARIES-907
			Activator.getInstance().getSubsystems().addReference(subsystem, resource);
		}
	}

	protected String getLocation() {
		return provisionTo.getLocation() + "!/" + ResourceHelper.getLocation(resource);
	}

	protected boolean isContent() {
//IC see: https://issues.apache.org/jira/browse/ARIES-907
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
