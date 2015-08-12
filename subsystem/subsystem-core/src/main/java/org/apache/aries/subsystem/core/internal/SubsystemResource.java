/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemResource implements Resource {
	private Region region;

	private final List<Capability> capabilities;
	private final DeploymentManifest deploymentManifest;
	private final Collection<Resource> installableContent = new HashSet<Resource>();
	private final Collection<Resource> installableDependencies = new HashSet<Resource>();
	private final Collection<Resource> mandatoryResources = new HashSet<Resource>();
	private final Collection<DeployedContentHeader.Clause> missingResources = new HashSet<DeployedContentHeader.Clause>();
	private final Collection<Resource> optionalResources = new HashSet<Resource>();
	private final BasicSubsystem parent;
	private final RawSubsystemResource resource;
	private final Collection<Resource> sharedContent = new HashSet<Resource>();
	private final Collection<Resource> sharedDependencies = new HashSet<Resource>();

	public SubsystemResource(String location, IDirectory content, BasicSubsystem parent) throws URISyntaxException, IOException, ResolutionException, BundleException, InvalidSyntaxException {
		this(new RawSubsystemResource(location, content, parent), parent);
	}

	public SubsystemResource(RawSubsystemResource resource, BasicSubsystem parent) throws IOException, BundleException, InvalidSyntaxException, URISyntaxException {
		this.parent = parent;
		this.resource = resource;
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
		resource = new RawSubsystemResource(directory, parent);
		deploymentManifest = resource.getDeploymentManifest();
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

	private void computeOsgiServiceCapabilities(List<Capability> capabilities) throws InvalidSyntaxException {
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
		return resource.getId();
	}

	public Collection<Resource> getInstallableContent() {
		return installableContent;
	}

	public Collection<Resource> getInstallableDependencies() {
		return installableDependencies;
	}

	public org.apache.aries.subsystem.core.repository.Repository getLocalRepository() {
		return resource.getLocalRepository();
	}

	public String getLocation() {
		return resource.getLocation().getValue();
	}

	Collection<Resource> getMandatoryResources() {
		return mandatoryResources;
	}
	
	public Collection<DeployedContentHeader.Clause> getMissingResources() {
		return missingResources;
	}

	Collection<Resource> getOptionalResources() {
		return optionalResources;
	}
	
	public Collection<BasicSubsystem> getParents() {
		if (parent == null) {
			Header<?> header = getDeploymentManifest().getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_PARENTS);
			if (header == null)
				return Collections.emptyList();
			String[] parentIds = header.getValue().split(",");
			Collection<BasicSubsystem> result = new ArrayList<BasicSubsystem>(parentIds.length);
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
			if (!isApplication()) {
				setImportIsolationPolicy();
			}
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

	public Collection<Resource> getSharedContent() {
		return sharedContent;
	}

	public Collection<Resource> getSharedDependencies() {
		return sharedDependencies;
	}

	public SubsystemManifest getSubsystemManifest() {
		return resource.getSubsystemManifest();
	}

	public Collection<TranslationFile> getTranslations() {
		return resource.getTranslations();
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
		Region root = Activator.getInstance().getSubsystems().getRootSubsystem().getRegion();
		if (to.getName().equals(root.getName()))
			addSubsystemServiceImportToSharingPolicy(builder);
		else {
			to = root;
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
					throw new SubsystemException("A required content resource could not be found. This means the resource was either missing or not recognized as a supported resource format due to, for example, an invalid bundle manifest or blueprint XML file. Turn on debug logging for more information. The resource was: " + requirement);
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
					throw new SubsystemException("A required dependency could not be found. This means the resource was either missing or not recognized as a supported resource format due to, for example, an invalid bundle manifest or blueprint XML file. Turn on debug logging for more information. The resource was: " + resource);
				addDependency(resource);
			}
		}
	}

	private void computeDependencies(SubsystemManifest manifest)  {
		SubsystemContentHeader contentHeader = manifest.getSubsystemContentHeader();
		try {
			Map<Resource, List<Wire>> resolution = Activator.getInstance().getResolver().resolve(createResolveContext());
			setImportIsolationPolicy(resolution);
			for (Map.Entry<Resource, List<Wire>> entry : resolution.entrySet()) {
				Resource key = entry.getKey();
				if (!contentHeader.contains(key)) {
					addDependency(key);
				}
				for (Wire wire : entry.getValue()) {
					Resource provider = wire.getProvider();
					if (!contentHeader.contains(provider)) {
						addDependency(provider);
					}
				}
			}
		}
		catch (ResolutionException e) {
			throw new SubsystemException(e);
		}
		catch (Exception e) {
			if (e instanceof SubsystemException) {
				throw (SubsystemException)e;
			}
			if (e instanceof SecurityException) {
				throw (SecurityException)e;
			}
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
			// This is the root subsystem. Associate it with the region in which
			// the subsystems implementation bundle was installed.
			return digraph.getRegion(activator.getBundleContext().getBundle());
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
		return new org.apache.aries.subsystem.core.internal.ResolveContext(this);
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
					else if (provider instanceof BasicSubsystem) {
						if (getRegion().equals(((BasicSubsystem)provider).getRegion())) {
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

	boolean isApplication() {
		String type = resource.getSubsystemManifest().getSubsystemTypeHeader().getType();
		return SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type);
	}

	boolean isComposite() {
		String type = resource.getSubsystemManifest().getSubsystemTypeHeader().getType();
		return SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type);
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

	boolean isRoot() {
		return BasicSubsystem.ROOT_LOCATION.equals(getLocation());
	}

	private boolean isShared(Resource resource) {
		return Utils.isSharedResource(resource);
	}

	private boolean isScoped() {
		return isApplication() || isComposite();
	}

	private boolean isUnscoped() {
		return !isScoped();
	}

	private void setImportIsolationPolicy(Map<Resource, List<Wire>> resolution) throws Exception {
		if (!isApplication()) {
			return;
		}
		SubsystemContentHeader contentHeader = getSubsystemManifest().getSubsystemContentHeader();
		// Prepare the regions and filter builder to set the sharing policy.
		Region from = getRegion();
		Region to = ((BasicSubsystem)getParents().iterator().next()).getRegion();
		RegionFilterBuilder builder = from.getRegionDigraph().createRegionFilterBuilder();
		// Always provide visibility to this subsystem's service registration.
		addSubsystemServiceImportToSharingPolicy(builder, to);
		for (Resource resource : resolution.keySet()) {
			// If the resource is content but the wire provider is not,
			// the sharing policy must be updated.
			List<Wire> wires = resolution.get(resource);
			for (Wire wire : wires) {
				Resource provider = wire.getProvider();
				if (contentHeader.contains(provider)) {
					// The provider is content so the requirement does
					// not need to become part of the sharing policy.
					continue;
				}
				// The provider is not content, so the requirement must
				// be added to the sharing policy.
				Requirement requirement = wire.getRequirement();
				String namespace = requirement.getNamespace();
				if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)) {
					// The osgi.service namespace must be translated to one
					// that region digraph understands.
					namespace = RegionFilter.VISIBLE_SERVICE_NAMESPACE;
				}
				String filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
				if (filter == null) {
					builder.allowAll(namespace);
				}
				else {
					builder.allow(namespace, filter);
				}
			}
		}
		// Always add access to osgi.ee and osgi.native namespaces
		setImplicitAccessToNativeAndEECapabilities(builder);
		// Now set the sharing policy, if the regions are different.
		RegionFilter regionFilter = builder.build();
		from.connectRegion(to, regionFilter);
	}

	private void setImportIsolationPolicy() throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (isRoot() || !isScoped())
			return;
		Region region = getRegion();
		Region from = region;
		RegionFilterBuilder builder = from.getRegionDigraph().createRegionFilterBuilder();
		Region to = getParents().iterator().next().getRegion();
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
			// Always add access to osgi.ee and osgi.native namespaces
			setImplicitAccessToNativeAndEECapabilities(builder);
		}
		RegionFilter regionFilter = builder.build();
		from.connectRegion(to, regionFilter);
	}

	private void setImportIsolationPolicy(RegionFilterBuilder builder, ImportPackageHeader header) throws InvalidSyntaxException {
		String policy = RegionFilter.VISIBLE_PACKAGE_NAMESPACE;
		if (header == null)
			return;
		for (ImportPackageHeader.Clause clause : header.getClauses()) {
			ImportPackageRequirement requirement = new ImportPackageRequirement(clause, this);
			String filter = requirement.getDirectives().get(ImportPackageRequirement.DIRECTIVE_FILTER);
			builder.allow(policy, filter);
		}
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

	private void setImplicitAccessToNativeAndEECapabilities(RegionFilterBuilder builder) {
		builder.allowAll(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		builder.allowAll(NativeNamespace.NATIVE_NAMESPACE);
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
