package org.apache.aries.subsystem.core.internal;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
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
		if (isTransitive()) {
			// The resource is a dependency and not content.
			if (Utils.isInstallableResource(resource))
				// If the dependency needs to be installed, it must go into the
				// first subsystem in the parent chain that accepts
				// dependencies.
				provisionTo = Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(subsystem);
			else
				// If the dependency has already been installed, it does not
				// need to be provisioned.
				provisionTo = null;
		}
		else
			// The resource is content and must go into the subsystem declaring
			// it as such.
			provisionTo = subsystem;
	}
	
	public abstract Resource install() throws Exception;
	
	protected void addConstituent(final Resource resource) {
		// provisionTo will be null when the resource is an already installed
		// dependency.
		if (provisionTo == null)
			return;
		Activator.getInstance().getSubsystems().addConstituent(provisionTo, resource);
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
		// subsystem will be null when the root or a persisted subsystem is
		// being installed
		if (subsystem == null)
			return;
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
		return provisionTo.getSubsystemId() + "@" + provisionTo.getSymbolicName() + "@" + ResourceHelper.getSymbolicNameAttribute(resource);
	}
	
	protected boolean isTransitive() {
		if (subsystem == null)
			return false;
		DeploymentManifest manifest = subsystem.getDeploymentManifest();
		if (manifest == null)
			return false;
		ProvisionResourceHeader header = manifest.getProvisionResourceHeader();
		if (header == null)
			return false;
		return header.contains(resource);
	}
}
