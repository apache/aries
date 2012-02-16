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
package org.apache.aries.subsystem.core.obr;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.aries.subsystem.core.Environment;
import org.apache.aries.subsystem.core.ResourceHelper;
import org.apache.aries.subsystem.core.internal.Activator;
import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.apache.aries.subsystem.core.internal.OsgiIdentityRequirement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.Wiring;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * TODO
 * The locating of providers for transitive dependencies needs to have subsystem type and share policies taken into account.
 * So does the locating of providers for feature content with respect to children of the first parent that is not a feature.
 */
public class SubsystemEnvironment implements Environment {
	private static final Logger logger = LoggerFactory.getLogger(SubsystemEnvironment.class);
	
	private final Set<Resource> resources = new HashSet<Resource>();
	private final AriesSubsystem subsystem;
	
	public SubsystemEnvironment(AriesSubsystem subsystem) throws IOException, URISyntaxException {
		this.subsystem = subsystem;
	}
	
	@Override
	public SortedSet<Capability> findProviders(Requirement requirement) {
		logger.debug(LOG_ENTRY, "findProviders", requirement);
		// TODO Need a more robust comparator. This is just a temporary place holder.
		SortedSet<Capability> capabilities = new TreeSet<Capability>(
				new Comparator<Capability>() {
					@Override
					public int compare(Capability capability1, Capability capability2) {
						if (logger.isDebugEnabled())
							logger.debug(LOG_ENTRY, "compare", new Object[]{capability1, capability2});
						int result = 0;
						boolean br1 = capability1.getResource() instanceof BundleRevision;
						boolean br2 = capability2.getResource() instanceof BundleRevision;
						if (br1 && !br2)
							result = -1;
						else if (!br1 && br2)
							result = 1;
						logger.debug(LOG_EXIT, "compare", result);
						return result;
					}
				});
		if (requirement instanceof OsgiIdentityRequirement) {
			logger.debug("The requirement is an instance of OsgiIdentityRequirement");
			// TODO Consider returning only the first capability matched by the requirement in this case.
			// This means we're looking for a content resource.
			OsgiIdentityRequirement identity = (OsgiIdentityRequirement)requirement;
			if (subsystem.isFeature()) {
				// Features share content resources as well as transitive dependencies.
				findConstituentProviders(requirement, capabilities);
			}
			findArchiveProviders(capabilities, identity, !identity.isTransitiveDependency());
			findRepositoryServiceProviders(capabilities, identity, !identity.isTransitiveDependency());
		}
		else {
			logger.debug("The requirement is NOT an instance of OsgiIdentityRequirement");
			// This means we're looking for capabilities satisfying a requirement within a content resource or transitive dependency.
			findArchiveProviders(capabilities, requirement, false);
			findRepositoryServiceProviders(capabilities, requirement, false);
			// TODO The following is a quick fix to ensure this environment always returns capabilities provided by the system bundle. Needs some more thought.
			findConstituentProviders(requirement, capabilities);
		}
		logger.debug(LOG_EXIT, "findProviders", capabilities);
		return capabilities;
	}
	
	@Override
	public Map<Requirement, SortedSet<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		logger.debug(LOG_ENTRY, "findProviders", requirements);
		Map<Requirement, SortedSet<Capability>> result = new HashMap<Requirement, SortedSet<Capability>>(requirements.size());
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		logger.debug(LOG_EXIT, "findProviders", result);
		return result;
	}
	
	public Resource findResource(OsgiIdentityRequirement requirement) {
		logger.debug(LOG_ENTRY, "findResource", requirement);
		Collection<Capability> capabilities = findProviders(requirement);
		Resource result = null;
		if (!capabilities.isEmpty())
			result = capabilities.iterator().next().getResource();
		logger.debug(LOG_EXIT, "findResource", result);
		return result;
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
	
	public boolean isContentResource(Resource resource) {
		logger.debug(LOG_ENTRY, "isContentResource", resource);
		boolean result = resources.contains(resource);
		logger.debug(LOG_EXIT, "isContentResource", result);
		return result;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		logger.debug(LOG_ENTRY, "isEffective", requirement);
		boolean result = true;
		logger.debug(LOG_EXIT, "isEffective", result);
		return true;
	}
	
	private void findConstituentProviders(Requirement requirement, Collection<Capability> capabilities) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findConstituentProviders", new Object[]{requirement, capabilities});
		Subsystem subsystem = this.subsystem;
		logger.debug("Navigating up the parent hierarchy...");
		while (!subsystem.getParents().isEmpty()) {
			subsystem = subsystem.getParents().iterator().next();
			logger.debug("Next parent is: {}", subsystem);
		}
		findConstituentProviders(subsystem, requirement, capabilities);
		logger.debug(LOG_EXIT, "findConstituentProviders");
	}
	
	private void findConstituentProviders(Subsystem subsystem, Requirement requirement, Collection<Capability> capabilities) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findConstituentProviders", new Object[]{subsystem, requirement, capabilities});
		for (Resource resource : subsystem.getConstituents()) {
			logger.debug("Evaluating resource: {}", resource);
			for (Capability capability : resource.getCapabilities(requirement.getNamespace())) {
				logger.debug("Evaluating capability: {}", capability);
				if (ResourceHelper.matches(requirement, capability)) {
					logger.debug("Adding capability: {}", capability);
					capabilities.add(capability);
				}
			}
		}
		findConstituentProviders(subsystem.getChildren(), requirement, capabilities);
		logger.debug(LOG_EXIT, "findConstituentProviders");
	}
	
	private void findConstituentProviders(Collection<Subsystem> children, Requirement requirement, Collection<Capability> capabilities) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findConstituentProviders", new Object[]{children, requirement, capabilities});
		for (Subsystem child : children) {
			logger.debug("Evaluating child subsystem: {}", child);
			findConstituentProviders(child, requirement, capabilities);
		}
	}
	
	private void findArchiveProviders(Collection<Capability> capabilities, Requirement requirement, boolean content) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findArchiveProviders", new Object[]{capabilities, requirement, content});
		AriesSubsystem subsystem = this.subsystem;
		logger.debug("Navigating up the parent hierarchy...");
		while (!subsystem.getParents().isEmpty()) {
			subsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
			logger.debug("Next parent is: {}", subsystem);
		}
		findArchiveProviders(capabilities, requirement, subsystem, content);
		logger.debug(LOG_EXIT, "findArchiveProviders");
	}
	
	private void findArchiveProviders(Collection<Capability> capabilities, Requirement requirement, AriesSubsystem subsystem, boolean content) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findArchiveProviders", new Object[]{capabilities, requirement, subsystem, content});
		for (Capability capability : subsystem.getArchive().findProviders(requirement)) {
			logger.debug("Adding capability: {}", capability);
			capabilities.add(capability);
			if (content) {
				Resource resource = capability.getResource();
				logger.debug("Adding content resource: {}", resource);
				resources.add(resource);
			}
		}
		findArchiveProviders(capabilities, requirement, subsystem.getChildren(), content);
		logger.debug(LOG_EXIT, "findArchiveProviders");
	}
	
	private void findArchiveProviders(Collection<Capability> capabilities, Requirement requirement, Collection<Subsystem> children, boolean content) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findArchiveProviders", new Object[]{capabilities, requirement, children, content});
		for (Subsystem child : children) {
			logger.debug("Evaluating child subsystem: {}", child);
			findArchiveProviders(capabilities, requirement, (AriesSubsystem)child, content);
		}
		logger.debug(LOG_EXIT, "findArchiveProviders");
	}
	
	private void findRepositoryServiceProviders(Collection<Capability> capabilities, Requirement requirement, boolean content) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "findRepositoryServiceProviders", new Object[]{capabilities, requirement, content});
		Collection<Repository> repositories = Activator.getInstance().getServiceProvider().getServices(Repository.class);
		for (Repository repository : repositories) {
			logger.debug("Evaluating repository: {}", repository);
			for (Capability capability : repository.findProviders(requirement)) {
				logger.debug("Adding capability: {}", capability);
				capabilities.add(capability);
				if (content) {
					Resource resource = capability.getResource();
					logger.debug("Adding content resource: {}", resource);
					resources.add(resource);
				}
			}
		}
		logger.debug(LOG_EXIT, "findRepositoryServiceProviders");
	}
}
