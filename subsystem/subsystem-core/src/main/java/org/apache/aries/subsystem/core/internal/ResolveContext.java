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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
import org.apache.aries.subsystem.core.internal.DependencyCalculator.MissingCapability;
import org.apache.aries.subsystem.core.repository.Repository;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.subsystem.SubsystemException;

public class ResolveContext extends org.osgi.service.resolver.ResolveContext {
	private final Repository contentRepository;
	private final Repository localRepository;
	private final Repository preferredProviderRepository;
	private final Repository repositoryServiceRepository;
	private final SubsystemResource resource;
	private final Repository systemRepository;
	private final Map<Resource, Wiring> wirings = computeWirings();
	
	public ResolveContext(SubsystemResource resource) {
		this.resource = resource;
		contentRepository = new ContentRepository(resource.getInstallableContent(), resource.getSharedContent());
		localRepository = resource.getLocalRepository();
		preferredProviderRepository = new PreferredProviderRepository(resource);
		repositoryServiceRepository = new RepositoryServiceRepository();
		systemRepository = Activator.getInstance().getSystemRepository();
	}
	
	@Override
	public List<Capability> findProviders(Requirement requirement) {
		ArrayList<Capability> result = new ArrayList<Capability>();
		try {
			// Only check the system repository for osgi.ee and osgi.native
			if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(requirement.getNamespace()) 
					|| NativeNamespace.NATIVE_NAMESPACE.equals(requirement.getNamespace())) {
				addDependenciesFromSystemRepository(requirement, result);
			} else {
				addDependenciesFromContentRepository(requirement, result);
				addDependenciesFromPreferredProviderRepository(requirement, result);
				addDependenciesFromSystemRepository(requirement, result);
				addDependenciesFromLocalRepository(requirement, result);
				if (result.isEmpty()) {
					addDependenciesFromRepositoryServiceRepositories(requirement, result);
				}
			}
			if (result.isEmpty()) {
				// Is the requirement optional?
				String resolution = requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
				if (Namespace.RESOLUTION_OPTIONAL.equals(resolution)) {
					// Yes, it's optional. Add a missing capability to ensure
					// it gets added to the sharing policy per the specification.
					result.add(new MissingCapability(requirement));
				}
			}
		}
		catch (Throwable t) {
			if (t instanceof SubsystemException)
				throw (SubsystemException)t;
			if (t instanceof SecurityException)
				throw (SecurityException)t;
			throw new SubsystemException(t);
		}
		result.trimToSize();
		return result;
	}

	@Override
	public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
		capabilities.add(hostedCapability);
		return capabilities.size() - 1;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		return true;
	}
	
	@Override
	public Collection<Resource> getMandatoryResources() {
		return resource.getMandatoryResources();
	}
	
	@Override 
	public Collection<Resource> getOptionalResources() {
		return resource.getOptionalResources();
	}

	@Override
	public Map<Resource, Wiring> getWirings() {
		return wirings;
	}
	
	private boolean addDependencies(Repository repository, Requirement requirement, List<Capability> capabilities, boolean validate) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (repository == null)
			return false;
		Map<Requirement, Collection<Capability>> m = repository.findProviders(Collections.singleton(requirement));
		if (m.containsKey(requirement)) {
			Collection<Capability> cc = m.get(requirement);
			addValidCapabilities(cc, capabilities, requirement, validate);
		}
		return !capabilities.isEmpty();
	}

	private boolean addDependenciesFromContentRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		return addDependencies(contentRepository, requirement, capabilities, false);
	}

	private boolean addDependenciesFromLocalRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		return addDependencies(localRepository, requirement, capabilities, true);
	}

	private boolean addDependenciesFromPreferredProviderRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		return addDependencies(preferredProviderRepository, requirement, capabilities, true);
	}

	private boolean addDependenciesFromRepositoryServiceRepositories(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		return addDependencies(repositoryServiceRepository, requirement, capabilities, true);
	}

	private boolean addDependenciesFromSystemRepository(Requirement requirement, List<Capability> capabilities) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		return addDependencies(systemRepository, requirement, capabilities, true);
	}

	private void addValidCapabilities(Collection<Capability> from, Collection<Capability> to, Requirement requirement, boolean validate) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		for (Capability c : from) {
			if (!validate || isValid(c, requirement)) {
				// either validation is not requested or the capability is valid.
				to.add(c);
			}
		}
	}

	private void addWiring(Resource resource, Map<Resource, Wiring> wirings) {
		if (resource instanceof BundleConstituent) {
			BundleConstituent bc = (BundleConstituent)resource;
			wirings.put(bc.getBundle().adapt(BundleRevision.class), bc.getWiring());
		}
		else if (resource instanceof BundleRevision) {
			BundleRevision br = (BundleRevision)resource;
			wirings.put(br, br.getWiring());
		}
	}
	
	private Map<Resource, Wiring> computeWirings() {
		Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
		for (BasicSubsystem subsystem : Activator.getInstance().getSubsystems().getSubsystems()) { // NEED
			for (Resource constituent : subsystem.getConstituents()) {
				addWiring(constituent, wirings);
			}
		}
		return Collections.unmodifiableMap(wirings);
	}

	private Region findRegionForCapabilityValidation(Resource resource) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (isInstallable(resource)) {
			// This is an installable resource so we need to figure out where it
			// will be installed.
			if (isContent(resource) // If the resource is content of this subsystem, it will be installed here.
					// Or if this subsystem accepts dependencies, the resource will be installed here.
					|| this.resource.getSubsystemManifest().getSubsystemTypeHeader().getProvisionPolicyDirective().isAcceptDependencies()) {
				if (this.resource.isComposite()) {
					// Composites define their own sharing policy with which
					// their regions are already configured by the time we get
					// here. We ensure capabilities are visible to this region.
					return this.resource.getRegion();
				}
				// For applications and features, we must ensure capabilities
				// are visible to their scoped parent. Features import
				// everything. Applications have their sharing policies 
				// computed, so if capabilities are visible to the parent, we
				// know we can make them visible to the application.
				return this.resource.getParents().iterator().next().getRegion();
			}
			// Same reasoning as above applies here.
			if (this.resource.isComposite() && this.resource.getSubsystemManifest().getSubsystemTypeHeader().getProvisionPolicyDirective().isAcceptDependencies()) {
				 return this.resource.getRegion();
			}
			return Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(this.resource.getParents().iterator().next()).getRegion();
		}
		else {
			// This is an already installed resource from the system repository.
			if (Utils.isBundle(resource))
				// If it's a bundle, use region digraph to get the region in order
				// to account for bundles in isolated regions outside of the
				// subsystems API.
				return Activator.getInstance().getRegionDigraph().getRegion(((BundleRevision)resource).getBundle());
			else
				// If it's anything else, get the region from one of the
				// subsystems referencing it.
				return Activator.getInstance().getSubsystems().getSubsystemsReferencing(resource).iterator().next().getRegion();
		}
	}

	private boolean isContent(Resource resource) {
		return this.resource.getSubsystemManifest().getSubsystemContentHeader().contains(resource);
	}

	private boolean isInstallable(Resource resource) {
		return !isShared(resource);
	}

	private boolean isShared(Resource resource) {
		return Utils.isSharedResource(resource);
	}

	private boolean isValid(Capability capability, Requirement requirement) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (IdentityNamespace.IDENTITY_NAMESPACE.equals(capability.getNamespace()))
			return true;
		Region from = findRegionForCapabilityValidation(capability.getResource());
		Region to = findRegionForCapabilityValidation(requirement.getResource());
		return new SharingPolicyValidator(from, to).isValid(capability);
	}
}
