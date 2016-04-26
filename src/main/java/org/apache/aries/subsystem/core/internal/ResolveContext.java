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
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.ProvisionPolicyDirective;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
import org.apache.aries.subsystem.core.internal.DependencyCalculator.MissingCapability;
import org.apache.aries.subsystem.core.internal.StartAction.Restriction;
import org.apache.aries.subsystem.core.repository.Repository;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.subsystem.Subsystem.State;

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
	
	private void installDependenciesOfRequirerIfNecessary(Requirement requirement) {
		if (requirement == null) {
			return;
		}
		Resource requirer = requirement.getResource();
		if (resource.equals(requirer)) {
			return;
		}
		Collection<BasicSubsystem> subsystems;
		if (requirer instanceof BasicSubsystem) {
			BasicSubsystem subsystem = (BasicSubsystem)requirer;
			subsystems = Collections.singletonList(subsystem);
		}
		else if (requirer instanceof BundleRevision) {
			BundleRevision revision = (BundleRevision)requirer;
			BundleConstituent constituent = new BundleConstituent(null, revision);
			subsystems = Activator.getInstance().getSubsystems().getSubsystemsByConstituent(constituent);
		}
		else {
			return;
		}
		for (BasicSubsystem subsystem : subsystems) {
			if (Utils.isProvisionDependenciesInstall(subsystem) 
					|| !State.INSTALLING.equals(subsystem.getState())) {
				continue;
			}
			AccessController.doPrivileged(new StartAction(subsystem, subsystem, subsystem, Restriction.INSTALL_ONLY));
		}
	}
	
	private boolean isResolved(Resource resource) {
		return wirings.containsKey(resource);
	}
	
	private boolean isProcessableAsFragment(Requirement requirement) {
		Resource resource = requirement.getResource();
		String namespace = requirement.getNamespace();
		return Utils.isFragment(resource)
				&& !(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(namespace)
						|| HostNamespace.HOST_NAMESPACE.equals(namespace));
	}
	
	private void processAsFragment(Requirement requirement, List<Capability> capabilities) {
		String namespace = requirement.getNamespace();
		Resource fragment = requirement.getResource();
		Wiring fragmentWiring = wirings.get(fragment);
		List<Wire> fragmentWires = fragmentWiring.getRequiredResourceWires(HostNamespace.HOST_NAMESPACE);
		for (Wire fragmentWire : fragmentWires) {
			Resource host = fragmentWire.getProvider();
			Wiring hostWiring = wirings.get(host);
			List<Wire> hostWires = hostWiring.getRequiredResourceWires(namespace);
			processWires(hostWires, requirement, capabilities);
		}
	}
	
	private void processWires(Collection<Wire> wires, Requirement requirement, List<Capability> capabilities) {
		for (Wire wire : wires) {
			processWire(wire, requirement, capabilities);
		}
	}
	
	private void processWire(Wire wire, Requirement requirement, List<Capability> capabilities) {
		Capability capability = wire.getCapability();
		processCapability(capability, requirement, capabilities);
	}
	
	private void processCapability(Capability capability, Requirement requirement, List<Capability> capabilities) {
		if (ResourceHelper.matches(requirement, capability)) {
			capabilities.add(capability);
		}
	}
	
	private void processResourceCapabilities(Collection<Capability> resourceCapabilities, Requirement requirement, List<Capability> capabilities) {
		for (Capability resourceCapability : resourceCapabilities) {
			processCapability(resourceCapability, requirement, capabilities);
		}
	}
	
	private void processAsBundle(Requirement requirement, List<Capability> capabilities) {
		String namespace = requirement.getNamespace();
		Resource bundle = requirement.getResource();
		Wiring wiring = wirings.get(bundle);
		List<Wire> wires = wiring.getRequiredResourceWires(namespace);
		processWires(wires, requirement, capabilities);
	}
	
	private void processAsSubstitutableExport(boolean isFragment, Requirement requirement, List<Capability> capabilities) {
		String namespace = requirement.getNamespace();
		if (!PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
			return;
		}
		Resource resource = requirement.getResource();
		Wiring wiring = wirings.get(resource);
		if (isFragment) {
			List<Wire> fragmentWires = wiring.getRequiredResourceWires(HostNamespace.HOST_NAMESPACE);
			for (Wire fragmentWire : fragmentWires) {
				Resource host = fragmentWire.getProvider();
				processResourceCapabilities(
						wirings.get(host).getResourceCapabilities(namespace),
						requirement,
						capabilities);
			}
		}
		else {
			List<Capability> resourceCapabilities = wiring.getResourceCapabilities(namespace);
			processResourceCapabilities(resourceCapabilities, requirement, capabilities);
		}
	}
	
	private void processAlreadyResolvedResource(Resource resource, Requirement requirement, List<Capability> capabilities) {
		boolean isFragment = isProcessableAsFragment(requirement);
		if (isFragment) {
			processAsFragment(requirement, capabilities);
		}
		else {
			processAsBundle(requirement, capabilities);
		}
		if (capabilities.isEmpty() && Utils.isMandatory(requirement)) {
			processAsSubstitutableExport(isFragment, requirement, capabilities);
			if (capabilities.isEmpty()) {
				// ARIES-1538. Do not fail subsystem resolution if an already
				// resolved resource has a missing dependency.
				capabilities.add(new MissingCapability(requirement));
			}
		}
	}
	
	private void processNewlyResolvedResource(Resource resource, Requirement requirement, List<Capability> capabilities) {
		try {
			// Only check the system repository for osgi.ee and osgi.native
			if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(requirement.getNamespace())
					|| NativeNamespace.NATIVE_NAMESPACE.equals(requirement.getNamespace())) {
				addDependenciesFromSystemRepository(requirement, capabilities);
			} else {
				addDependenciesFromContentRepository(requirement, capabilities);
				addDependenciesFromPreferredProviderRepository(requirement, capabilities);
				addDependenciesFromSystemRepository(requirement, capabilities);
				addDependenciesFromLocalRepository(requirement, capabilities);
				if (capabilities.isEmpty()) {
					addDependenciesFromRepositoryServiceRepositories(requirement, capabilities);
				}
			}
			if (capabilities.isEmpty()) {
				// Is the requirement optional?
				String resolution = requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
				if (Namespace.RESOLUTION_OPTIONAL.equals(resolution)) {
					// Yes, it's optional. Add a missing capability to ensure
					// it gets added to the sharing policy per the specification.
					capabilities.add(new MissingCapability(requirement));
				}
				// Is the requirement resource already resolved? See ARIES-1538.
				else if (isResolved(requirement.getResource())) {
					// Yes, the resource has already been resolved. Do not fail
					// the subsystem resolution due to a missing dependency.
					capabilities.add(new MissingCapability(requirement));
				}
			}
		}
		catch (Throwable t) {
			Utils.handleTrowable(t);
		}
	}

	@Override
	public List<Capability> findProviders(Requirement requirement) {
		ArrayList<Capability> capabilities = new ArrayList<Capability>();
		Resource resource = requirement.getResource();
		if (isResolved(resource)
				&& Utils.isEffectiveResolve(requirement)) {
			processAlreadyResolvedResource(resource, requirement, capabilities);
		}
		else {
			installDependenciesOfRequirerIfNecessary(requirement);
			processNewlyResolvedResource(resource, requirement, capabilities);
		}
		capabilities.trimToSize();
		return capabilities;
	}

	@Override
	public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
	    // Must specify the location where the capability is to be added. From the ResoveContext javadoc:
	    // "This method must insert the specified HostedCapability in a place that makes the list maintain
	    // the preference order."
	    // The Felix implementation provides a list that requires the index to be specified in the add() call,
	    // otherwise it will throw an exception.
        int sz = capabilities.size();
		capabilities.add(sz, hostedCapability);
        return sz;
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
		return Collections.emptyMap();
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
		boolean result = addDependencies(systemRepository, requirement, capabilities, true);
		return result;
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
			BundleWiring wiring = bc.getWiring();
			if (wiring != null) {
				wirings.put(bc.getBundle().adapt(BundleRevision.class), wiring);
			}
		}
		else if (resource instanceof BundleRevision) {
			BundleRevision br = (BundleRevision)resource;
			BundleWiring wiring = br.getWiring();
			if (wiring != null) {
				wirings.put(br, wiring);
			}
			
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
	
	private boolean isContent(Resource resource) {
		return this.resource.isContent(resource);
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
		Resource provider = capability.getResource();
		Resource requirer = requirement.getResource();
		SubsystemManifest manifest = resource.getSubsystemManifest();
		SubsystemContentHeader header = manifest.getSubsystemContentHeader();
		if (header.contains(provider) && header.contains(requirer)) {
			// Shortcut. If both the provider and requirer are content then they
			// are in the same region and the capability will be visible.
			return true;
		}
		Region from = findRegionForCapabilityValidation(provider);
		Region to = findRegionForCapabilityValidation(requirer);
		return new SharingPolicyValidator(from, to).isValid(capability);
	}
	
	private boolean isAcceptDependencies() {
		SubsystemManifest manifest = resource.getSubsystemManifest();
		SubsystemTypeHeader header = manifest.getSubsystemTypeHeader();
		ProvisionPolicyDirective directive = header.getProvisionPolicyDirective();
		return directive.isAcceptDependencies();
	}
	
	private Region findRegionForCapabilityValidation(Resource resource) throws BundleException, IOException, InvalidSyntaxException, URISyntaxException {
		if (isInstallable(resource)) {
			// This is an installable resource so we need to figure out where it
			// will be installed.
			if (isContent(resource) // If the resource is content of this subsystem, it will be installed here.
					// Or if this subsystem accepts dependencies, the resource will be installed here.
					|| isAcceptDependencies()) {
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
				BasicSubsystem parent = this.resource.getParents().iterator().next();
				// If the parent accepts dependencies, the resource will 
				// be installed there and all capabilities will be visible.
				if (parent.getSubsystemManifest().getSubsystemTypeHeader().getProvisionPolicyDirective().isAcceptDependencies()) {
					return parent.getRegion();
				}
				// Otherwise, the "parent" is defined as the first scoped 
				// ancestor whose sharing policy has already been set. This 
				// covers the case of multiple subsystems from the same archive 
				// being installed whose regions will form a tree of depth N.
				parent = Utils.findFirstScopedAncestorWithSharingPolicy(this.resource);
				return parent.getRegion();
			}
			return Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(this.resource.getParents().iterator().next()).getRegion();
		}
		else {
			// This is an already installed resource from the system repository.
			if (Utils.isBundle(resource)) {
				if (isContent(resource) 
						&& this.resource.getSubsystemManifest().getSubsystemTypeHeader().getAriesProvisionDependenciesDirective().isResolve()) {
					// If we get here with a subsystem that is 
					// apache-aries-provision-dependencies:=resolve, it means
					// that a restart has occurred with the subsystem in the
					// INSTALLING state. Its content has already been installed.
					// However, because the sharing policy has not yet been set,
					// we must treat it similarly to the installable content case
					// above.
					return Utils.findFirstScopedAncestorWithSharingPolicy(this.resource).getRegion();
				}
			    BundleRevision revision = resource instanceof BundleRevision ? (BundleRevision)resource : ((BundleRevisionResource)resource).getRevision();
				// If it's a bundle, use region digraph to get the region in order
				// to account for bundles in isolated regions outside of the
				// subsystems API.
				return Activator.getInstance().getRegionDigraph().getRegion(revision.getBundle());
			}
			else {
				if (this.resource.getSubsystemManifest().getSubsystemTypeHeader().getAriesProvisionDependenciesDirective().isResolve()) {
					return Utils.findFirstScopedAncestorWithSharingPolicy(this.resource).getRegion();
				}
				// If it's anything else, get the region from one of the
				// subsystems referencing it.
				return Activator.getInstance().getSubsystems().getSubsystemsReferencing(resource).iterator().next().getRegion();
			}
		}
	}
}
