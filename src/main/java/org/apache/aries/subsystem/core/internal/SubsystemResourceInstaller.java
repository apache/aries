package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.Subsystem.State;

public class SubsystemResourceInstaller extends ResourceInstaller {
	public SubsystemResourceInstaller(Coordination coordination, Resource resource, AriesSubsystem subsystem) {
		super(coordination, resource, subsystem);
	}
	
	public Resource install() throws Exception {
		AriesSubsystem result;
		if (resource instanceof RepositoryContent)
			result = installRepositoryContent((RepositoryContent)resource);
		else if (resource instanceof AriesSubsystem)
			result = installAriesSubsystem((AriesSubsystem)resource);
		else if (resource instanceof RawSubsystemResource)
			result = installRawSubsystemResource((RawSubsystemResource)resource);
		else
			result = installSubsystemResource((SubsystemResource)resource);
		return result;
	}
	
	private void addChild(final AriesSubsystem child) {
		if (provisionTo == null || subsystem == null)
			return;
		Activator.getInstance().getSubsystems().addChild(subsystem, child, true);
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination arg0) throws Exception {
				// Nothing
			}

			@Override
			public void failed(Coordination arg0) throws Exception {
				Activator.getInstance().getSubsystems().removeChild(subsystem, child);
			}
		});
	}
	
	private void addSubsystem(final AriesSubsystem subsystem) {
		Activator.getInstance().getSubsystems().addSubsystem(subsystem);
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination arg0) throws Exception {
				// Nothing
			}
	
			@Override
			public void failed(Coordination arg0) throws Exception {
				Activator.getInstance().getSubsystems().removeSubsystem(subsystem);
			}
		});
	}
	
	private AriesSubsystem installAriesSubsystem(AriesSubsystem subsystem) throws Exception {
		addChild(subsystem);
		addConstituent(subsystem);
		if (!isTransitive())
			addReference(subsystem);
		// TODO Is this check really necessary?
		if (!State.INSTALLING.equals(subsystem.getState()))
			return subsystem;
		addSubsystem(subsystem);
		if (subsystem.isScoped())
			RegionContextBundleHelper.installRegionContextBundle(subsystem);
		Activator.getInstance().getSubsystemServiceRegistrar().register(subsystem, this.subsystem);
		if (!subsystem.isRoot()) {
			Comparator<Resource> comparator = new InstallResourceComparator();
			// Install dependencies first...
			List<Resource> dependencies = new ArrayList<Resource>(subsystem.getResource().getInstallableDependencies());
			Collections.sort(dependencies, comparator);
			for (Resource dependency : dependencies)
				ResourceInstaller.newInstance(coordination, dependency, subsystem).install();
			for (Resource dependency : subsystem.getResource().getSharedDependencies())
				ResourceInstaller.newInstance(coordination, dependency, subsystem).install();
			// ...followed by content.
			List<Resource> installableContent = new ArrayList<Resource>(subsystem.getResource().getInstallableContent());
			Collections.sort(installableContent, comparator);
			for (Resource content : installableContent)
				ResourceInstaller.newInstance(coordination, content, subsystem).install();
			// Simulate installation of shared content so that necessary relationships are established.
			for (Resource content : subsystem.getResource().getSharedContent())
				ResourceInstaller.newInstance(coordination, content, subsystem).install();
		}
		subsystem.setState(State.INSTALLED);
		return subsystem;
	}
	
	private AriesSubsystem installRawSubsystemResource(RawSubsystemResource resource) throws Exception {
		SubsystemResource subsystemResource = new SubsystemResource(resource, provisionTo);
		return installSubsystemResource(subsystemResource);
	}
	
	private AriesSubsystem installRepositoryContent(RepositoryContent resource) throws Exception {
		RawSubsystemResource rawSubsystemResource = new RawSubsystemResource(getLocation(), resource.getContent());
		return installRawSubsystemResource(rawSubsystemResource);
	}
	
	private AriesSubsystem installSubsystemResource(SubsystemResource resource) throws Exception {
		AriesSubsystem subsystem = new AriesSubsystem(resource);
		installAriesSubsystem(subsystem);
		return subsystem;
	}
}
