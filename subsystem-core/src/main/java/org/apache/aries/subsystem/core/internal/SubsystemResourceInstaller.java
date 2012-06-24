package org.apache.aries.subsystem.core.internal;

import java.security.AccessController;
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
	public SubsystemResourceInstaller(Coordination coordination, Resource resource, AriesSubsystem subsystem, boolean transitive) {
		super(coordination, resource, subsystem, transitive);
	}
	
	public Resource install() throws Exception {
		if (resource instanceof RepositoryContent)
			return installRepositoryContent((RepositoryContent)resource);
		else if (resource instanceof AriesSubsystem)
			return installAriesSubsystem((AriesSubsystem)resource);
		else if (resource instanceof RawSubsystemResource)
			return installRawSubsystemResource((RawSubsystemResource)resource);
		else
			return installSubsystemResource((SubsystemResource)resource);
	}
	
	private void addChild(final AriesSubsystem child) {
		if (provisionTo == null || subsystem == null)
			return;
		Activator.getInstance().getSubsystems().addChild(subsystem, child);
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
	
//	private void addSubsystemServiceImportToSharingPolicy(
//			RegionFilterBuilder builder, AriesSubsystem subsystem) throws InvalidSyntaxException {
//		builder.allow(
//				RegionFilter.VISIBLE_SERVICE_NAMESPACE,
//				new StringBuilder("(&(")
//						.append(org.osgi.framework.Constants.OBJECTCLASS)
//						.append('=').append(Subsystem.class.getName())
//						.append(")(")
//						.append(Constants.SubsystemServicePropertyRegions)
//						.append('=').append(subsystem.getRegion().getName())
//						.append("))").toString());
//	}
	
//	private void addSubsystemServiceImportToSharingPolicy(RegionFilterBuilder builder, Region to, AriesSubsystem subsystem)
//			throws InvalidSyntaxException, BundleException {
//		if (to.getName().equals(AriesSubsystem.ROOT_REGION))
//			addSubsystemServiceImportToSharingPolicy(builder, subsystem);
//		else {
//			to = Activator.getInstance().getSubsystems().getRootSubsystem().getRegion();
//			builder = to.getRegionDigraph().createRegionFilterBuilder();
//			addSubsystemServiceImportToSharingPolicy(builder, subsystem);
//			RegionFilter regionFilter = builder.build();
//			subsystem.getRegion().connectRegion(to, regionFilter);
//		}
//	}
	
	private Resource installAriesSubsystem(AriesSubsystem subsystem) throws Exception {
		addChild(subsystem);
		addConstituent(subsystem);
		addReference(subsystem);
		// TODO Is this check really necessary?
		if (!State.INSTALLING.equals(subsystem.getState()))
			return subsystem;
		addSubsystem(subsystem);
		if (subsystem.isScoped())
			RegionContextBundleHelper.installRegionContextBundle(subsystem);
		Activator.getInstance().getSubsystemServiceRegistrar().register(subsystem, this.subsystem);
		// Set up the sharing policy before installing the resources so that the
		// environment can filter out capabilities from dependencies being
		// provisioned to regions that are out of scope. This doesn't hurt
		// anything since the resources are disabled from resolving anyway.
//		setImportIsolationPolicy(subsystem);
		if (!subsystem.isRoot()) {
			Comparator<Resource> comparator = new InstallResourceComparator();
			// Install dependencies first...
			List<Resource> dependencies = new ArrayList<Resource>(subsystem.getResource().getInstallableDependencies());
			Collections.sort(dependencies, comparator);
			for (Resource dependency : dependencies)
				ResourceInstaller.newInstance(coordination, dependency, subsystem, true).install();
			for (Resource dependency : subsystem.getResource().getSharedDependencies())
				ResourceInstaller.newInstance(coordination, dependency, subsystem, true).install();
			// ...followed by content.
			List<Resource> installableContent = new ArrayList<Resource>(subsystem.getResource().getInstallableContent());
			Collections.sort(installableContent, comparator);
			for (Resource content : installableContent)
				ResourceInstaller.newInstance(coordination, content, subsystem, false).install();
			// Simulate installation of shared content so that necessary relationships are established.
			for (Resource content : subsystem.getResource().getSharedContent())
				ResourceInstaller.newInstance(coordination, content, subsystem, false).install();
		}
		subsystem.setState(State.INSTALLED);
		return subsystem;
	}
	
	private Resource installRawSubsystemResource(RawSubsystemResource resource) throws Exception {
		SubsystemResource subsystemResource = new SubsystemResource(resource, provisionTo);
		return installSubsystemResource(subsystemResource);
	}
	
	private Resource installRepositoryContent(RepositoryContent resource) {
		return AccessController.doPrivileged(new InstallAction(getLocation(), resource.getContent(), provisionTo, null, coordination, true));
	}
	
	private Resource installSubsystemResource(SubsystemResource resource) throws Exception {
		AriesSubsystem subsystem = new AriesSubsystem(resource);
		installAriesSubsystem(subsystem);
		if (subsystem.isAutostart())
			subsystem.start();
		return subsystem;
	}
	
//	private void setImportIsolationPolicy(AriesSubsystem subsystem) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
//		if (subsystem.isRoot() || !subsystem.isScoped())
//			return;
//		Region region = subsystem.getRegion();
//		Region from = region;
//		RegionFilterBuilder builder = from.getRegionDigraph().createRegionFilterBuilder();
//		Region to = ((AriesSubsystem)subsystem.getParents().iterator().next()).getRegion();
//		addSubsystemServiceImportToSharingPolicy(builder, to, subsystem);
//		// TODO Is this check really necessary? Looks like it was done at the beginning of this method.
//		if (subsystem.isScoped()) {
//			// Both applications and composites have Import-Package headers that require processing.
//			// In the case of applications, the header is generated.
//			Header<?> header = subsystem.getSubsystemManifest().getImportPackageHeader();
//			setImportIsolationPolicy(builder, (ImportPackageHeader)header, subsystem);
//			// Both applications and composites have Require-Capability headers that require processing.
//			// In the case of applications, the header is generated.
//			header = subsystem.getSubsystemManifest().getRequireCapabilityHeader();
//			setImportIsolationPolicy(builder, (RequireCapabilityHeader)header, subsystem);
//			// Both applications and composites have Subsystem-ImportService headers that require processing.
//			// In the case of applications, the header is generated.
//			header = subsystem.getSubsystemManifest().getSubsystemImportServiceHeader();
//			setImportIsolationPolicy(builder, (SubsystemImportServiceHeader)header, subsystem);
//			header = subsystem.getSubsystemManifest().getRequireBundleHeader();
//			setImportIsolationPolicy(builder, (RequireBundleHeader)header, subsystem);
//		}
//		RegionFilter regionFilter = builder.build();
//		from.connectRegion(to, regionFilter);
//	}
//	
//	private void setImportIsolationPolicy(RegionFilterBuilder builder, ImportPackageHeader header, AriesSubsystem subsystem) throws InvalidSyntaxException {
//		if (header == null)
//			return;
//		String policy = RegionFilter.VISIBLE_PACKAGE_NAMESPACE;
//		for (ImportPackageHeader.Clause clause : header.getClauses()) {
//			ImportPackageRequirement requirement = new ImportPackageRequirement(clause, subsystem);
//			String filter = requirement.getDirectives().get(ImportPackageRequirement.DIRECTIVE_FILTER);
//			builder.allow(policy, filter);
//		}
//	}
//	
//	private void setImportIsolationPolicy(RegionFilterBuilder builder, RequireBundleHeader header, AriesSubsystem subsystem) throws InvalidSyntaxException {
//		if (header == null)
//			return;
//		for (RequireBundleHeader.Clause clause : header.getClauses()) {
//			RequireBundleRequirement requirement = new RequireBundleRequirement(clause, subsystem);
//			String policy = RegionFilter.VISIBLE_REQUIRE_NAMESPACE;
//			String filter = requirement.getDirectives().get(RequireBundleRequirement.DIRECTIVE_FILTER);
//			builder.allow(policy, filter);
//		}
//	}
//	
//	private void setImportIsolationPolicy(RegionFilterBuilder builder, RequireCapabilityHeader header, AriesSubsystem subsystem) throws InvalidSyntaxException {
//		if (header == null)
//			return;
//		for (RequireCapabilityHeader.Clause clause : header.getClauses()) {
//			RequireCapabilityRequirement requirement = new RequireCapabilityRequirement(clause, subsystem);
//			String policy = requirement.getNamespace();
//			String filter = requirement.getDirectives().get(RequireCapabilityRequirement.DIRECTIVE_FILTER);
//			builder.allow(policy, filter);
//		}
//	}
//	
//	private void setImportIsolationPolicy(RegionFilterBuilder builder, SubsystemImportServiceHeader header, AriesSubsystem subsystem) throws InvalidSyntaxException {
//		if (header == null)
//			return;
//		for (SubsystemImportServiceHeader.Clause clause : header.getClauses()) {
//			SubsystemImportServiceRequirement requirement = new SubsystemImportServiceRequirement(clause, subsystem);
//			String policy = RegionFilter.VISIBLE_SERVICE_NAMESPACE;
//			String filter = requirement.getDirectives().get(SubsystemImportServiceRequirement.DIRECTIVE_FILTER);
//			builder.allow(policy, filter);
//		}
//	}
}
