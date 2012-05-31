package org.apache.aries.subsystem.core.internal;

import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public abstract class ResourceUninstaller {
	public static ResourceUninstaller newInstance(Resource resource) {
		return newInstance(resource, null);
	}
	
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
		this.resource = resource;
		this.subsystem = subsystem;
		if (isTransitive())
			provisionTo = Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(subsystem);
		else
			provisionTo = subsystem;
	}
	
	public abstract void uninstall();
	
	protected boolean isImplicit() {
		return subsystem != null;
	}
	
	protected boolean isTransitive() {
		if (subsystem == null)
			return false;
		ProvisionResourceHeader header = subsystem.getArchive().getDeploymentManifest().getProvisionResourceHeader();
		if (header == null)
			return false;
		return header.contains(resource);
	}
	
	protected boolean isResourceUninstallable() {
		return Activator.getInstance().getSubsystems().getSubsystemsReferencing(resource).size() <= 1;
	}
	
	protected void removeConstituent() {
		removeConstituent(provisionTo, resource);
	}
	
	protected void removeReference() {
		removeReference(subsystem, resource);
	}
}
