package org.apache.aries.subsystem.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemResource implements Resource {
	private final DeploymentManifest deploymentManifest;
	private final Collection<Resource> installableContent = new HashSet<Resource>();
	private final Collection<Resource> installableDependencies = new HashSet<Resource>();
	private final Collection<Resource> mandatoryResources = new HashSet<Resource>();
	private final Collection<Resource> optionalResources = new HashSet<Resource>();
	private final AriesSubsystem parent;
	private final Repository preferredProviderRepository;
	private final RawSubsystemResource resource;
	private final Collection<Resource> sharedContent = new HashSet<Resource>();
	private final Collection<Resource> sharedDependencies = new HashSet<Resource>();
	
	public SubsystemResource(String location, InputStream content, AriesSubsystem parent) throws URISyntaxException, IOException, ResolutionException {
		this(new RawSubsystemResource(location, content), parent);
	}
	
	public SubsystemResource(RawSubsystemResource resource, AriesSubsystem parent) throws IOException {
		this.parent = parent;
		this.resource = resource;
		preferredProviderRepository = new PreferredProviderRepository(this);
		computeContentResources();
		computeDependencies();
		deploymentManifest = computeDeploymentManifest();
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		return resource.getCapabilities(namespace);
	}
	
	public DeploymentManifest getDeploymentManifest() {
		return deploymentManifest;
	}
	
	public File getDirectory() {
		return resource.getDirectory();
	}
	
	public long getId() {
		return resource.getId();
	}
	
	public Collection<Resource> getInstallableContent() {
		return installableContent;
	}
	
	public Collection<Resource> getInstallableDependencies() {
		return installableDependencies;
	}
	
	public Repository getLocalRepository() {
		return resource.getLocalRepository();
	}
	
	public String getLocation() {
		return resource.getLocation().getValue();
	}
	
	public AriesSubsystem getParent() {
		return parent;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return resource.getRequirements(namespace);
	}
	
	public Collection<Resource> getResources() {
		return resource.getResources();
	}
	
	public Collection<Resource> getSharedContent() {
		return sharedContent;
	}
	
	public Collection<Resource> getSharedDependencies() {
		return sharedDependencies;
	}
	
	public SubsystemManifest getSubsystemManifest() {
		return resource.getSubsystemManifest();
	}
	
	private void addContentResource(Resource resource) {
		if (resource == null)
			return;
		if (isMandatory(resource))
			mandatoryResources.add(resource);
		else
			optionalResources.add(resource);
		if (isInstallable(resource))
			installableContent.add(resource);
		else
			sharedContent.add(resource);
	}
	
	private boolean addDependencies(Repository repository, Requirement requirement, List<Capability> capabilities) {
		Map<Requirement, Collection<Capability>> m = repository.findProviders(Collections.singleton(requirement));
		if (m.containsKey(requirement)) {
			Collection<Capability> cc = m.get(requirement);
			// TODO The following check only needs to be done on capabilities from the system repository.
			addValidCapabilities(cc, capabilities);
		}
		return !capabilities.isEmpty();
	}
	
	private boolean addDependenciesFromContentRepository(Requirement requirement, List<Capability> capabilities) {
		Repository repository = new ContentRepository(installableContent, sharedContent);
		return addDependencies(repository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromLocalRepository(Requirement requirement, List<Capability> capabilities) {
		Repository repository = resource.getLocalRepository();
		return addDependencies(repository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromPreferredProviderRepository(Requirement requirement, List<Capability> capabilities) {
		return addDependencies(preferredProviderRepository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromRepositoryServiceRepositories(Requirement requirement, List<Capability> capabilities) {
		Repository repository = new RepositoryServiceRepository();
		return addDependencies(repository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromSystemRepository(Requirement requirement, List<Capability> capabilities) {
		Repository repository = Activator.getInstance().getSystemRepository();
		return addDependencies(repository, requirement, capabilities);
	}
	
	private void addDependency(Resource resource) {
		if (resource == null)
			return;
		if (isInstallable(resource))
			installableDependencies.add(resource);
		else
			sharedDependencies.add(resource);
	}
	
	private void addValidCapabilities(Collection<Capability> from, Collection<Capability> to) {
		for (Capability c : from)
			if (isValid(c))
				to.add(c);
	}
	
	private void computeContentResources() {
		SubsystemContentHeader contentHeader = resource.getSubsystemManifest().getSubsystemContentHeader();
		if (contentHeader == null)
			return;
		for (SubsystemContentHeader.Content content : contentHeader.getContents()) {
			OsgiIdentityRequirement requirement = new OsgiIdentityRequirement(
					content.getName(), content.getVersionRange(),
					content.getType(), false);
			Resource resource = findContent(requirement);
			if (resource == null) {
				if (content.isMandatory())
					throw new SubsystemException("Resource does not exist: "+ requirement);
				continue;
			}
			addContentResource(resource);
		}
	}
	
	private void computeDependencies() {
		SubsystemContentHeader contentHeader = resource.getSubsystemManifest().getSubsystemContentHeader();
		try {
			Map<Resource, List<Wire>> resolution = Activator.getInstance().getResolver().resolve(createResolveContext());
			for (Resource resource : resolution.keySet())
				if (!contentHeader.contains(resource))
					addDependency(resource);
		}
		catch (ResolutionException e) {
			throw new SubsystemException(e);
		}
	}
	
	private DeployedContentHeader computeDeployedContentHeader() {
		Collection<Resource> content = getContentResources();
		if (content.isEmpty())
			return null;
		return DeployedContentHeader.newInstance(content);
	}
	
	private DeploymentManifest computeDeploymentManifest() throws IOException {
		DeploymentManifest result = computeExistingDeploymentManifest();
		if (result != null)
			return result;
		result = new DeploymentManifest.Builder().manifest(resource.getSubsystemManifest())
				.header(computeDeployedContentHeader())
				.header(computeProvisionResourceHeader()).build();
		return result;
	}
	
	private DeploymentManifest computeExistingDeploymentManifest() throws IOException {
		return resource.getDeploymentManifest();
	}
	
	private ProvisionResourceHeader computeProvisionResourceHeader() {
		Collection<Resource> dependencies = getDepedencies();
		if (dependencies.isEmpty())
			return null;
		return ProvisionResourceHeader.newInstance(dependencies);
	}
	
	private ResolveContext createResolveContext() {
		return new ResolveContext() {
			@Override
			public List<Capability> findProviders(Requirement requirement) {
				List<Capability> result = new ArrayList<Capability>();
				if (addDependenciesFromContentRepository(requirement, result))
					return result;
				if (addDependenciesFromPreferredProviderRepository(requirement, result))
					return result;
				if (addDependenciesFromSystemRepository(requirement, result))
					return result;
				if (addDependenciesFromLocalRepository(requirement, result))
					return result;
				if (addDependenciesFromRepositoryServiceRepositories(requirement, result))
					return result;
				return result;
			}
			
			@Override
			public Collection<Resource> getMandatoryResources() {
				return SubsystemResource.this.mandatoryResources;
			}
			
			@Override
			public Collection<Resource> getOptionalResources() {
				return SubsystemResource.this.optionalResources;
			}

			@Override
			public int insertHostedCapability(List<Capability> capabilities,
					HostedCapability hostedCapability) {
				capabilities.add(hostedCapability);
				return capabilities.size() - 1;
			}

			@Override
			public boolean isEffective(Requirement requirement) {
				return true;
			}

			@Override
			public Map<Resource, Wiring> getWirings() {
				return Collections.emptyMap();
			}
		};
	}
	
	private Resource findContent(OsgiIdentityRequirement requirement) {
		Map<Requirement, Collection<Capability>> map;
		if (isUnscoped()) {
			map = Activator.getInstance().getSystemRepository().findProviders(Collections.singleton(requirement));
			if (map.containsKey(requirement)) {
				Collection<Capability> capabilities = map.get(requirement);
				for (Capability capability : capabilities) {
					Collection<AriesSubsystem> subsystems = Activator.getInstance().getSubsystems().getSubsystemsReferencing(capability.getResource());
					if (!subsystems.isEmpty())
						if (subsystems.iterator().next().getRegion().equals(parent.getRegion()))
							return capability.getResource();
				}
			}
		}
		map = resource.getLocalRepository().findProviders(Collections.singleton(requirement));
		if (map.containsKey(requirement)) {
			Collection<Capability> capabilities = map.get(requirement);
			if (!capabilities.isEmpty())
				return capabilities.iterator().next().getResource();
		}
		Collection<Capability> capabilities = new RepositoryServiceRepository().findProviders(requirement);
		if (!capabilities.isEmpty())
			return capabilities.iterator().next().getResource();
		return null;
	}
	
	private Collection<Resource> getContentResources() {
		Collection<Resource> result = new ArrayList<Resource>(installableContent.size() + sharedContent.size());
		result.addAll(installableContent);
		result.addAll(sharedContent);
		return result;
	}
	
	private Collection<Resource> getDepedencies() {
		Collection<Resource> result = new ArrayList<Resource>(installableDependencies.size() + sharedDependencies.size());
		result.addAll(installableDependencies);
		result.addAll(sharedDependencies);
		return result;
	}
	
	private boolean isInstallable(Resource resource) {
		return !isShared(resource);
	}
	
	private boolean isMandatory(Resource resource) {
		SubsystemContentHeader header = this.resource.getSubsystemManifest().getSubsystemContentHeader();
		if (header == null)
			return false;
		return header.isMandatory(resource);
	}
	
	private boolean isShared(Resource resource) {
		return resource instanceof AriesSubsystem || resource instanceof BundleRevision;
	}
	
	private boolean isScoped() {
		String type = resource.getSubsystemManifest().getSubsystemTypeHeader().getType();
		return SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type) ||
				SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type);
	}
	
	private boolean isUnscoped() {
		return !isScoped();
	}
	
	private boolean isValid(Capability capability) {
		AriesSubsystem subsystem;
		if (isInstallable(capability.getResource()))
			subsystem = Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(parent);
		else
			subsystem = Activator.getInstance().getSubsystems().getSubsystemsReferencing(capability.getResource()).iterator().next();
		return new SharingPolicyValidator(subsystem.getRegion(), parent.getRegion()).isValid(capability);
	}
}
