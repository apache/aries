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

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.subsystem.core.archive.Attribute;
import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.ImportPackageRequirement;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleRequirement;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityRequirement;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemExportServiceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemImportServiceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemImportServiceRequirement;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemResource implements Resource {
	private Region region;
	
	private final List<Capability> capabilities;
	private final DeploymentManifest deploymentManifest;
	private final long id;
	private final Collection<Resource> installableContent = new HashSet<Resource>();
	private final Collection<Resource> installableDependencies = new HashSet<Resource>();
	private final Collection<Resource> mandatoryResources = new HashSet<Resource>();
	private final Collection<DeployedContentHeader.Clause> missingResources = new HashSet<DeployedContentHeader.Clause>();
	private final Collection<Resource> optionalResources = new HashSet<Resource>();
	private final AriesSubsystem parent;
	private final Repository preferredProviderRepository;
	private final RawSubsystemResource resource;
	private final Collection<Resource> sharedContent = new HashSet<Resource>();
	private final Collection<Resource> sharedDependencies = new HashSet<Resource>();
	
	public SubsystemResource(String location, InputStream content, AriesSubsystem parent) throws URISyntaxException, IOException, ResolutionException, BundleException, InvalidSyntaxException, ModellerException {
		this(new RawSubsystemResource(location, content), parent);
	}
	
	public SubsystemResource(RawSubsystemResource resource, AriesSubsystem parent) throws IOException, BundleException, InvalidSyntaxException, URISyntaxException {
		this.parent = parent;
		this.resource = resource;
		id = SubsystemIdentifier.getNextId();
		preferredProviderRepository = new PreferredProviderRepository(this);
		computeContentResources(resource.getDeploymentManifest());
		capabilities = computeCapabilities();
		computeDependencies(resource.getDeploymentManifest());
		deploymentManifest = computeDeploymentManifest();
	}
	
	public SubsystemResource(File file) throws IOException, URISyntaxException, ResolutionException, BundleException, InvalidSyntaxException {
		this(FileSystem.getFSRoot(file));
	}
	
	public SubsystemResource(IDirectory directory) throws IOException, URISyntaxException, ResolutionException, BundleException, InvalidSyntaxException {
		parent = null;
		resource = new RawSubsystemResource(directory);
		preferredProviderRepository = null;
		deploymentManifest = resource.getDeploymentManifest();
		id = Long.parseLong(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_ID).getValue());
		computeContentResources(deploymentManifest);
		capabilities = computeCapabilities();
		computeDependencies(deploymentManifest);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SubsystemResource))
			return false;
		SubsystemResource that = (SubsystemResource)o;
		return getLocation().equals(that.getLocation());
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		return Collections.unmodifiableList(capabilities);
	}
	
	private List<Capability> computeCapabilities() throws InvalidSyntaxException {
		List<Capability> capabilities = new ArrayList<Capability>();
		if (isScoped())
			computeScopedCapabilities(capabilities);
		else
			computeUnscopedCapabilities(capabilities);
		return capabilities;
	}
	
	private void computeUnscopedCapabilities(List<Capability> capabilities) {
		capabilities.addAll(resource.getCapabilities(null));
		for (Resource r : getContentResources())
			capabilities.addAll(r.getCapabilities(null));
	}
	
	private void computeScopedCapabilities(List<Capability> capabilities) throws InvalidSyntaxException {
		capabilities.addAll(resource.getCapabilities(null));
		computeOsgiServiceCapabilities(capabilities);
	}
	
	public void computeOsgiServiceCapabilities(List<Capability> capabilities) throws InvalidSyntaxException {
		SubsystemExportServiceHeader header = getSubsystemManifest().getSubsystemExportServiceHeader();
		if (header == null)
			return;
		for (Resource resource : getContentResources())
			capabilities.addAll(header.toCapabilities(resource));
	}
	
	public DeploymentManifest getDeploymentManifest() {
		return deploymentManifest;
	}
	
	public long getId() {
		return id;
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
	
	public Collection<DeployedContentHeader.Clause> getMissingResources() {
		return missingResources;
	}
	
	public Collection<AriesSubsystem> getParents() {
		if (parent == null) {
			Header<?> header = getDeploymentManifest().getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_PARENTS);
			if (header == null)
				return Collections.emptyList();
			String[] parentIds = header.getValue().split(",");
			Collection<AriesSubsystem> result = new ArrayList<AriesSubsystem>(parentIds.length);
			for (String parentId : parentIds)
				result.add(Activator.getInstance().getSubsystems().getSubsystemById(Long.valueOf(parentId)));
			return result;
		}
		return Collections.singleton(parent);
	}
	
	public synchronized Region getRegion() throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (region == null) {
			region = createRegion(getId());
			Coordination coordination = Activator.getInstance().getCoordinator().peek();
			coordination.addParticipant(new Participant() {
				@Override
				public void ended(Coordination arg0) throws Exception {
					// Nothing.
				}

				@Override
				public void failed(Coordination arg0) throws Exception {
					if (isScoped())
						region.getRegionDigraph().removeRegion(region);
				}
			});
			setImportIsolationPolicy();
		}
		return region;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		if (isScoped())
			return resource.getRequirements(namespace);
		else {
			ArrayList<Requirement> result = new ArrayList<Requirement>();
			result.addAll(resource.getRequirements(namespace));
			for (Resource r : getContentResources())
				result.addAll(r.getRequirements(namespace));
			result.trimToSize();
			return result;
		}
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
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + getLocation().hashCode();
		return result;
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
	
	private boolean addDependencies(Repository repository, Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (repository == null)
			return false;
		Map<Requirement, Collection<Capability>> m = repository.findProviders(Collections.singleton(requirement));
		if (m.containsKey(requirement)) {
			Collection<Capability> cc = m.get(requirement);
			// TODO The following check only needs to be done on capabilities from the system repository.
			addValidCapabilities(cc, capabilities);
		}
		return !capabilities.isEmpty();
	}
	
	private boolean addDependenciesFromContentRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		// TODO Why create this with each method call? What not cache it as an instance variable?
		Repository repository = new ContentRepository(installableContent, sharedContent);
		return addDependencies(repository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromLocalRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		Repository repository = resource.getLocalRepository();
		return addDependencies(repository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromPreferredProviderRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		return addDependencies(preferredProviderRepository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromRepositoryServiceRepositories(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		Repository repository = new RepositoryServiceRepository();
		return addDependencies(repository, requirement, capabilities);
	}
	
	private boolean addDependenciesFromSystemRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
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
	
	private void addMissingResource(DeployedContentHeader.Clause resource) {
		missingResources.add(resource);
	}
	
	private void addValidCapabilities(Collection<Capability> from, Collection<Capability> to) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		for (Capability c : from)
			if (isValid(c))
				to.add(c);
	}
	
	private void addSubsystemServiceImportToSharingPolicy(
			RegionFilterBuilder builder) throws InvalidSyntaxException, BundleException, IOException, URISyntaxException {
		builder.allow(
				RegionFilter.VISIBLE_SERVICE_NAMESPACE,
				new StringBuilder("(&(")
						.append(org.osgi.framework.Constants.OBJECTCLASS)
						.append('=').append(Subsystem.class.getName())
						.append(")(")
						.append(Constants.SubsystemServicePropertyRegions)
						.append('=').append(getRegion().getName())
						.append("))").toString());
	}
	
	private void addSubsystemServiceImportToSharingPolicy(RegionFilterBuilder builder, Region to)
			throws InvalidSyntaxException, BundleException, IOException, URISyntaxException {
		if (to.getName().equals(AriesSubsystem.ROOT_REGION))
			addSubsystemServiceImportToSharingPolicy(builder);
		else {
			to = Activator.getInstance().getSubsystems().getRootSubsystem().getRegion();
			builder = to.getRegionDigraph().createRegionFilterBuilder();
			addSubsystemServiceImportToSharingPolicy(builder);
			RegionFilter regionFilter = builder.build();
			getRegion().connectRegion(to, regionFilter);
		}
	}
	
	private void computeContentResources(DeploymentManifest manifest) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (manifest == null)
			computeContentResources(getSubsystemManifest());
		else {
			DeployedContentHeader header = manifest.getDeployedContentHeader();
			if (header == null)
				return;
			for (DeployedContentHeader.Clause clause : header.getClauses()) {
				Resource resource = findContent(clause);
				if (resource == null)
					addMissingResource(clause);
				else
					addContentResource(resource);
			}
		}
	}
	
	private void computeContentResources(SubsystemManifest manifest) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		SubsystemContentHeader contentHeader = manifest.getSubsystemContentHeader();
		if (contentHeader == null)
			return;
		for (SubsystemContentHeader.Clause clause : contentHeader.getClauses()) {
			Requirement requirement = clause.toRequirement(this);
			Resource resource = findContent(requirement);
			if (resource == null) {
				if (clause.isMandatory())
					throw new SubsystemException("Resource does not exist: "+ requirement);
				continue;
			}
			addContentResource(resource);
		}
	}
	
	private void computeDependencies(DeploymentManifest manifest) {
		if (manifest == null)
			computeDependencies(getSubsystemManifest());
		else {
			ProvisionResourceHeader header = manifest.getProvisionResourceHeader();
			if (header == null)
				return;
			for (ProvisionResourceHeader.Clause clause : header.getClauses()) {
				Resource resource = findDependency(clause);
				if (resource == null)
					throw new SubsystemException("Resource does not exist: " + clause);
				addDependency(resource);
			}
		}	
	}
	
	private void computeDependencies(SubsystemManifest manifest) {
		SubsystemContentHeader contentHeader = manifest.getSubsystemContentHeader();
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
	
	private Region createRegion(long id) throws BundleException {
		if (!isScoped())
			return getParents().iterator().next().getRegion();
		Activator activator = Activator.getInstance();
		RegionDigraph digraph = activator.getRegionDigraph();
		if (getParents().isEmpty())
			return digraph.getRegion(AriesSubsystem.ROOT_REGION);
		String name = getSubsystemManifest()
				.getSubsystemSymbolicNameHeader().getSymbolicName()
				+ ';'
				+ getSubsystemManifest().getSubsystemVersionHeader()
						.getVersion()
				+ ';'
				+ getSubsystemManifest().getSubsystemTypeHeader()
						.getType() + ';' + Long.toString(id);
		Region region = digraph.getRegion(name);
		// TODO New regions need to be cleaned up if this subsystem fails to
		// install, but there's no access to the coordination here.
		if (region == null)
			return digraph.createRegion(name);
		return region;
	}
	
	private ResolveContext createResolveContext() {
		return new ResolveContext() {
			@Override
			public List<Capability> findProviders(Requirement requirement) {
				List<Capability> result = new ArrayList<Capability>();
				try {
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
				}
				catch (Throwable t) {
					if (t instanceof SubsystemException)
						throw (SubsystemException)t;
					if (t instanceof SecurityException)
						throw (SecurityException)t;
					throw new SubsystemException(t);
				}
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
	
	private Resource findContent(Requirement requirement) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		Map<Requirement, Collection<Capability>> map;
		// TODO System repository for scoped subsystems should be searched in
		// the case of a persisted subsystem.
		if (isUnscoped()) {
			map = Activator.getInstance().getSystemRepository().findProviders(Collections.singleton(requirement));
			if (map.containsKey(requirement)) {
				Collection<Capability> capabilities = map.get(requirement);
				for (Capability capability : capabilities) {
					Resource provider = capability.getResource();
					if (provider instanceof BundleRevision) {
						if (getRegion().contains(((BundleRevision)provider).getBundle())) {
							return provider;
						}
					}
					else if (provider instanceof AriesSubsystem) {
						if (getRegion().equals(((AriesSubsystem)provider).getRegion())) {
							return provider;
						}
					}
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
	
	private Resource findContent(DeployedContentHeader.Clause clause) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		Attribute attribute = clause.getAttribute(DeployedContentHeader.Clause.ATTRIBUTE_RESOURCEID);
		long resourceId = attribute == null ? -1 : Long.parseLong(String.valueOf(attribute.getValue()));
		if (resourceId != -1) {
			String type = clause.getType();
			if (IdentityNamespace.TYPE_BUNDLE.equals(type) || IdentityNamespace.TYPE_FRAGMENT.equals(type)) {
				Bundle resource = Activator.getInstance().getBundleContext().getBundle(0).getBundleContext().getBundle(resourceId);
				if (resource == null)
					return null;
				return resource.adapt(BundleRevision.class);
			}
			else
				return Activator.getInstance().getSubsystems().getSubsystemById(resourceId);
		}
		return findContent(clause.toRequirement(this));
	}
	
	private Resource findDependency(ProvisionResourceHeader.Clause clause) {
		Attribute attribute = clause.getAttribute(DeployedContentHeader.Clause.ATTRIBUTE_RESOURCEID);
		long resourceId = attribute == null ? -1 : Long.parseLong(String.valueOf(attribute.getValue()));
		if (resourceId != -1) {
			String type = clause.getType();
			if (IdentityNamespace.TYPE_BUNDLE.equals(type) || IdentityNamespace.TYPE_FRAGMENT.equals(type))
				return Activator.getInstance().getBundleContext().getBundle(0).getBundleContext().getBundle(resourceId).adapt(BundleRevision.class);
			else
				return Activator.getInstance().getSubsystems().getSubsystemById(resourceId);
		}
		OsgiIdentityRequirement requirement = new OsgiIdentityRequirement(
				clause.getPath(), clause.getDeployedVersion(),
				clause.getType(), true);
		List<Capability> capabilities = createResolveContext().findProviders(requirement);
		if (capabilities.isEmpty())
			return null;
		return capabilities.get(0).getResource();
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
	
	private boolean isContent(Resource resource) {
		return getSubsystemManifest().getSubsystemContentHeader().contains(resource);
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
	
	private boolean isRoot() {
		return AriesSubsystem.ROOT_LOCATION.equals(getLocation());
	}
	
	private boolean isShared(Resource resource) {
		return Utils.isSharedResource(resource);
	}
	
	private boolean isScoped() {
		String type = resource.getSubsystemManifest().getSubsystemTypeHeader().getType();
		return SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type) ||
				SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type);
	}
	
	private boolean isUnscoped() {
		return !isScoped();
	}
	
	private boolean isValid(Capability capability) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (IdentityNamespace.IDENTITY_NAMESPACE.equals(capability.getNamespace()))
			return true;
		Region region;
		if (isInstallable(capability.getResource())) {
			if (isContent(capability.getResource()))
				region = getRegion();
			else
				region = Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(parent).getRegion();
		}
		else
			region = Activator.getInstance().getSubsystems().getSubsystemsReferencing(capability.getResource()).iterator().next().getRegion();
		return new SharingPolicyValidator(region, getRegion()).isValid(capability);
	}
	
	private void setImportIsolationPolicy() throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (isRoot() || !isScoped())
			return;
		Region region = getRegion();
		Region from = region;
		RegionFilterBuilder builder = from.getRegionDigraph().createRegionFilterBuilder();
		Region to = ((AriesSubsystem)getParents().iterator().next()).getRegion();
		addSubsystemServiceImportToSharingPolicy(builder, to);
		// TODO Is this check really necessary? Looks like it was done at the beginning of this method.
		if (isScoped()) {
			// Both applications and composites have Import-Package headers that require processing.
			// In the case of applications, the header is generated.
			Header<?> header = getSubsystemManifest().getImportPackageHeader();
			setImportIsolationPolicy(builder, (ImportPackageHeader)header);
			// Both applications and composites have Require-Capability headers that require processing.
			// In the case of applications, the header is generated.
			header = getSubsystemManifest().getRequireCapabilityHeader();
			setImportIsolationPolicy(builder, (RequireCapabilityHeader)header);
			// Both applications and composites have Subsystem-ImportService headers that require processing.
			// In the case of applications, the header is generated.
			header = getSubsystemManifest().getSubsystemImportServiceHeader();
			setImportIsolationPolicy(builder, (SubsystemImportServiceHeader)header);
			header = getSubsystemManifest().getRequireBundleHeader();
			setImportIsolationPolicy(builder, (RequireBundleHeader)header);
		}
		RegionFilter regionFilter = builder.build();
		from.connectRegion(to, regionFilter);
	}
	
	private void setImportIsolationPolicy(RegionFilterBuilder builder, ImportPackageHeader header) throws InvalidSyntaxException {
		if (header == null)
			return;
		String policy = RegionFilter.VISIBLE_PACKAGE_NAMESPACE;
		for (ImportPackageHeader.Clause clause : header.getClauses()) {
			ImportPackageRequirement requirement = new ImportPackageRequirement(clause, this);
			String filter = requirement.getDirectives().get(ImportPackageRequirement.DIRECTIVE_FILTER);
			builder.allow(policy, filter);
		}
		
		// work around https://www.osgi.org/bugzilla/show_bug.cgi?id=144 
		// In the first instance, what if the various weaving services were to have a property, 
		// osgi.woven.packages, that was a comma separated list of packages that might be woven 
		// by that hook. 
		Collection<String> wovenPackages = getWovenPackages();
		for (String pkg : wovenPackages) { 
			builder.allow(policy, "(osgi.wiring.package=" + pkg + ")");
		}
	}
	
	// First pass at this: really just a sketch. 
	private Collection<String> getWovenPackages() throws InvalidSyntaxException
	{
		// Find all weaving services in our region
		BundleContext bc = Activator.getInstance().getBundleContext();
		Collection<ServiceReference<WeavingHook>> weavers = bc.getServiceReferences(WeavingHook.class, null);
		Collection<String> wovenPackages = new ArrayList<String>();
		for (ServiceReference<WeavingHook> sr : weavers) { 
			String someWovenPackages = (String) sr.getProperty("osgi.woven.packages");
			if (someWovenPackages != null) { 
				wovenPackages.addAll(ManifestHeaderProcessor.split(someWovenPackages, ","));
			}
		}
		return wovenPackages;
	}
	
	private void setImportIsolationPolicy(RegionFilterBuilder builder, RequireBundleHeader header) throws InvalidSyntaxException {
		if (header == null)
			return;
		for (RequireBundleHeader.Clause clause : header.getClauses()) {
			RequireBundleRequirement requirement = new RequireBundleRequirement(clause, this);
			String policy = RegionFilter.VISIBLE_REQUIRE_NAMESPACE;
			String filter = requirement.getDirectives().get(RequireBundleRequirement.DIRECTIVE_FILTER);
			builder.allow(policy, filter);
		}
	}
	
	private void setImportIsolationPolicy(RegionFilterBuilder builder, RequireCapabilityHeader header) throws InvalidSyntaxException {
		if (header == null)
			return;
		for (RequireCapabilityHeader.Clause clause : header.getClauses()) {
			RequireCapabilityRequirement requirement = new RequireCapabilityRequirement(clause, this);
			String policy = requirement.getNamespace();
			String filter = requirement.getDirectives().get(RequireCapabilityRequirement.DIRECTIVE_FILTER);
			if (filter == null)
				// A null filter directive means the requirement matches any
				// capability from the same namespace.
				builder.allowAll(policy);
			else
				// Otherwise, the capabilities must be filtered accordingly.
				builder.allow(policy, filter);
		}
	}
	
	private void setImportIsolationPolicy(RegionFilterBuilder builder, SubsystemImportServiceHeader header) throws InvalidSyntaxException {
		if (header == null)
			return;
		for (SubsystemImportServiceHeader.Clause clause : header.getClauses()) {
			SubsystemImportServiceRequirement requirement = new SubsystemImportServiceRequirement(clause, this);
			String policy = RegionFilter.VISIBLE_SERVICE_NAMESPACE;
			String filter = requirement.getDirectives().get(SubsystemImportServiceRequirement.DIRECTIVE_FILTER);
			builder.allow(policy, filter);
		}
	}
}
