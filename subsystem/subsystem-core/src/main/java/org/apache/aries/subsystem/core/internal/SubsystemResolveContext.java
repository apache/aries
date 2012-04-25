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

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.subsystem.Subsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * TODO
 * The locating of providers for transitive dependencies needs to have subsystem type and share policies taken into account.
 * So does the locating of providers for feature content with respect to children of the first parent that is not a feature.
 */
public class SubsystemResolveContext extends ResolveContext {
	private static final class Comparator implements java.util.Comparator<Capability> {
		private final AriesSubsystem subsystem;
		
		public Comparator(AriesSubsystem subsystem) {
			this.subsystem = subsystem;
		}
		
		@Override
		public int compare(Capability c1, Capability c2) {
			if (logger.isDebugEnabled())
				logger.debug(LOG_ENTRY, "compare", new Object[]{c1, c2});
			int result = comparePreferredProvider(c1, c2);
			if (result == 0)
				result = compareRuntimeResource(c1, c2);
			logger.debug(LOG_EXIT, "compare", result);
			return result;
		}
		
		private int comparePreferredProvider(Capability c1, Capability c2) {
			boolean pp1 = isPreferredProvider(c1);
			boolean pp2 = isPreferredProvider(c2);
			if (pp1 && !pp2)
				return -1;
			else if (!pp1 && pp2)
				return 1;
			return 0;
		}
		
		private int compareRuntimeResource(Capability c1, Capability c2) {
			boolean rr1 = isRuntimeResource(c1);
			boolean rr2 = isRuntimeResource(c2);
			if (rr1 && !rr2)
				return -1;
			else if (!rr1 && rr2)
				return 1;
			return 0;
		}
		
		private boolean isPreferredProvider(Resource resource) {
			PreferredProviderHeader header = subsystem.getArchive().getSubsystemManifest().getPreferredProviderHeader();
			if (header == null)
				return false;
			return header.contains(resource);
		}
		
		private boolean isPreferredProvider(Capability capability) {
			return isPreferredProvider(capability.getResource());
		}
		
		private boolean isRuntimeResource(Resource resource) {
			return resource instanceof BundleRevision || resource instanceof AriesSubsystem;
		}
		
		private boolean isRuntimeResource(Capability capability) {
			return isRuntimeResource(capability.getResource());
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(SubsystemResolveContext.class);
	
	private final Collection<Resource> mandatoryResources = new ArrayList<Resource>();
	private final Collection<Resource> optionalResources = new ArrayList<Resource>();
	private final AriesSubsystem subsystem;
	private final SharingPolicyValidator validator;
	
	public SubsystemResolveContext(AriesSubsystem subsystem, Collection<Resource> resources) throws IOException, URISyntaxException {
		this.subsystem = subsystem;
		Region regionTo = subsystem.getRegion();
		while (subsystem.getArchive().getSubsystemManifest()
				.getSubsystemTypeHeader().getProvisionPolicyDirective()
				.isRejectDependencies()) {
			subsystem = (AriesSubsystem) subsystem.getParents()
					.iterator().next();
		}
		Region regionFrom = subsystem.getRegion();
		validator = new SharingPolicyValidator(regionFrom, regionTo);
		initializeResources(resources);
	}
	
	@Override
	public List<Capability> findProviders(Requirement requirement) {
		logger.debug(LOG_ENTRY, "findProviders", requirement);
		SortedSet<Capability> capabilities = new TreeSet<Capability>(new Comparator(subsystem));
		if (requirement instanceof OsgiIdentityRequirement) { 
			logger.debug("The requirement is an instance of OsgiIdentityRequirement");
			// TODO Consider returning only the first capability matched by the requirement in this case.
			OsgiIdentityRequirement identity = (OsgiIdentityRequirement)requirement;
			// Unscoped subsystems share content resources as well as transitive
			// dependencies. Scoped subsystems share transitive dependencies as
			// long as they're in the same region.
			if (subsystem.isFeature() || identity.isTransitiveDependency())
				capabilities.addAll(new SystemRepository(subsystem).findProviders(requirement));
			findArchiveProviders(capabilities, identity);
			findRepositoryServiceProviders(capabilities, identity);
		}
		else {
			logger.debug("The requirement is NOT an instance of OsgiIdentityRequirement");
			// This means we're looking for capabilities satisfying a requirement within a content resource or transitive dependency.
			findArchiveProviders(capabilities, requirement);
			findRepositoryServiceProviders(capabilities, requirement);
			// TODO The following is a quick fix to ensure this environment always returns capabilities provided by the system bundle. Needs some more thought.
			capabilities.addAll(new SystemRepository(subsystem).findProviders(requirement));
		}
		logger.debug(LOG_EXIT, "findProviders", capabilities);
		return new ArrayList<Capability>(capabilities);
	}
	
	public Resource findResource(OsgiIdentityRequirement requirement) {
		logger.debug(LOG_ENTRY, "findResource", requirement);
		List<Capability> capabilities = findProviders(requirement);
		Resource result = null;
		if (!capabilities.isEmpty())
			result = capabilities.get(0).getResource();
		logger.debug(LOG_EXIT, "findResource", result);
		return result;
	}
	
	@Override
	public Collection<Resource> getMandatoryResources() {
		return Collections.unmodifiableCollection(mandatoryResources);
	}
	
	@Override
	public Collection<Resource> getOptionalResources() {
		return Collections.unmodifiableCollection(optionalResources);
	}
	
	public AriesSubsystem getSubsystem() {
		return subsystem;
	}

	@Override
	public Map<Resource, Wiring> getWirings() {
		logger.debug(LOG_ENTRY, "getWirings");
		Map<Resource, Wiring> result = new HashMap<Resource, Wiring>();
		BundleContext bundleContext = Activator.getInstance().getBundleContext().getBundle(0).getBundleContext();
		for (Bundle bundle : bundleContext.getBundles()) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			Wiring wiring = revision.getWiring();
			if (wiring != null) {
				result.put(
						revision, 
						revision.getWiring());
			}
		}
		logger.debug(LOG_EXIT, "getWirings", result);
		return result;
	}
	
	@Override
	public int insertHostedCapability(List<Capability> capabilities,
			HostedCapability hostedCapability) {
		capabilities.add(hostedCapability);
		Collections.sort(capabilities, new Comparator(subsystem));
		return capabilities.indexOf(hostedCapability);
	}
	
	public boolean isContentResource(Resource resource) {
		return subsystem.getArchive().getSubsystemManifest().getSubsystemContentHeader().contains(resource);
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		logger.debug(LOG_ENTRY, "isEffective", requirement);
		boolean result = true;
		logger.debug(LOG_EXIT, "isEffective", result);
		return true;
	}
	
	private void findArchiveProviders(Collection<Capability> capabilities, Requirement requirement) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findArchiveProviders", new Object[]{capabilities, requirement});
		AriesSubsystem subsystem = this.subsystem;
		logger.debug("Navigating up the parent hierarchy...");
		while (!subsystem.getParents().isEmpty()) {
			subsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
			logger.debug("Next parent is: {}", subsystem);
		}
		findArchiveProviders(capabilities, requirement, subsystem);
		logger.debug(LOG_EXIT, "findArchiveProviders");
	}
	
	private void findArchiveProviders(Collection<Capability> capabilities, Requirement requirement, AriesSubsystem subsystem) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findArchiveProviders", new Object[]{capabilities, requirement, subsystem});
		for (Capability capability : subsystem.getArchive().findProviders(requirement)) {
			logger.debug("Adding capability: {}", capability);
			// Filter out capabilities offered by dependencies that will be or
			// already are provisioned to an out of scope region.
			if (!requirement.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE) && !isContentResource(capability.getResource()) && !validator.isValid(capability))
				continue;
			capabilities.add(capability);
		}
		findArchiveProviders(capabilities, requirement, subsystem.getChildren());
		logger.debug(LOG_EXIT, "findArchiveProviders");
	}
	
	private void findArchiveProviders(Collection<Capability> capabilities, Requirement requirement, Collection<Subsystem> children) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findArchiveProviders", new Object[]{capabilities, requirement, children});
		for (Subsystem child : children) {
			logger.debug("Evaluating child subsystem: {}", child);
			findArchiveProviders(capabilities, requirement, (AriesSubsystem)child);
		}
		logger.debug(LOG_EXIT, "findArchiveProviders");
	}
	
	private void findRepositoryServiceProviders(Collection<Capability> capabilities, Requirement requirement) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findRepositoryServiceProviders", new Object[]{capabilities, requirement});
		Collection<Repository> repositories = Activator.getInstance().getRepositories();
		for (Repository repository : repositories) {
			logger.debug("Evaluating repository: {}", repository);
			Map<Requirement, Collection<Capability>> map = repository.findProviders(Arrays.asList(requirement));
			Collection<Capability> caps = map.get(requirement);
			if (caps != null) {
				for (Capability capability : caps) {
					logger.debug("Adding capability: {}", capability);
					// Filter out capabilities offered by dependencies that will be or
					// already are provisioned to an out of scope region.
					if (!requirement.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE) && !isContentResource(capability.getResource()) && !validator.isValid(capability))
						continue;
					capabilities.add(capability);
				}
			}
		}
		logger.debug(LOG_EXIT, "findRepositoryServiceProviders");
	}
	
	private void initializeResources(Collection<Resource> resources) {
		for (Resource resource : resources) {
			if (isMandatory(resource))
				mandatoryResources.add(resource);
			else
				optionalResources.add(resource);
		}
	}
	
	private boolean isMandatory(Resource resource) {
		return subsystem.getArchive().getSubsystemManifest().getSubsystemContentHeader().isMandatory(resource);
	}
}
