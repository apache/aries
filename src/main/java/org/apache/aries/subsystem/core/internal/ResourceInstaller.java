package org.apache.aries.subsystem.core.internal;

import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
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
		if (resource.equals(provisionTo))
			return;
		Activator.getInstance().getSubsystems().addConstituent(provisionTo, resource, isContent());
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
	
	protected boolean isContent() {
		DeploymentManifest manifest = subsystem.getDeploymentManifest();
		DeployedContentHeader header = manifest.getDeployedContentHeader();
		if (header == null)
			return !isDependency();
		return header.contains(resource) || !isDependency();
	}
	
	protected boolean isDependency() {
		DeploymentManifest manifest = subsystem.getDeploymentManifest();
		if (manifest == null)
			return false;
		ProvisionResourceHeader header = manifest.getProvisionResourceHeader();
		if (header == null)
			return false;
		return header.contains(resource);
	}
}
