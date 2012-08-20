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

import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResourceUninstaller {
	private static final Logger logger = LoggerFactory.getLogger(ResourceUninstaller.class);
	
	public static ResourceUninstaller newInstance(Resource resource, AriesSubsystem subsystem) {
		String type = ResourceHelper.getTypeAttribute(resource);
		if (SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type))
			return new SubsystemResourceUninstaller(resource, subsystem);
		else if (IdentityNamespace.TYPE_BUNDLE.equals(type) || IdentityNamespace.TYPE_FRAGMENT.equals(type))
			return new BundleResourceUninstaller(resource, subsystem);
		else
			throw new SubsystemException("No uninstaller exists for resource type: " + type);
	}
	
	protected static void removeConstituent(AriesSubsystem subsystem, Resource resource) {
		Activator.getInstance().getSubsystems().removeConstituent(subsystem, resource);
	}
	
	protected static void removeReference(AriesSubsystem subsystem, Resource resource) {
		Activator.getInstance().getSubsystems().removeReference(subsystem, resource);
	}
	
	protected final AriesSubsystem provisionTo;
	protected final Resource resource;
	protected final AriesSubsystem subsystem;
	
	public ResourceUninstaller(Resource resource, AriesSubsystem subsystem) {
		if (resource == null)
			throw new NullPointerException("Missing required parameter: resource");
		if (subsystem == null)
			throw new NullPointerException("Missing required parameter: subsystem");
		this.resource = resource;
		this.subsystem = subsystem;
		if (isTransitive())
			provisionTo = Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(subsystem);
		else
			provisionTo = subsystem;
	}
	
	public abstract void uninstall();
	
	protected boolean isExplicit() {
		// The operation is explicit if it was requested by a user, in which
		// case the resource and subsystem are the same.
		if (resource.equals(subsystem))
			return true;
		// The operation is explicit if it was requested by a scoped subsystem
		// on a resource within the same region.
		if (subsystem.isScoped()) {
			if (Utils.isBundle(resource))
				return subsystem.getRegion().contains(((BundleRevision)resource).getBundle());
			// TODO This is insufficient. The unscoped subsystem could be a
			// dependency in another region, which would make it implicit.
			return !((AriesSubsystem)resource).isScoped();
		}
		return false;
	}
	
	protected boolean isTransitive() {
		ProvisionResourceHeader header = subsystem.getDeploymentManifest().getProvisionResourceHeader();
		if (header == null)
			return false;
		return header.contains(resource);
	}
	
	protected boolean isResourceUninstallable() {
		int referenceCount = Activator.getInstance().getSubsystems().getSubsystemsReferencing(resource).size();
		if (referenceCount == 0)
			return true;
		if (isExplicit()) {
			logger.error("Explicitly uninstalling resource still has dependencies: {}", resource);
			return true;
		}
		return false;
	}
	
	protected void removeConstituent() {
		removeConstituent(subsystem, resource);
	}
	
	protected void removeReference() {
		removeReference(subsystem, resource);
	}
}
