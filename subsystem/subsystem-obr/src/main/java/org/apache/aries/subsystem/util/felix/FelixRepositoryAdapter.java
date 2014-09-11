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
package org.apache.aries.subsystem.util.felix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.obr.internal.ResourceHelper;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FelixRepositoryAdapter implements Repository {
	private static class IdentityRequirementFilter {
		private static final String REGEX = "\\(osgi.identity=([^\\)]*)\\)";
		private static final Pattern PATTERN = Pattern.compile(REGEX);
		
		private final String symbolicName;
		
		public IdentityRequirementFilter(String filter) {
			Matcher matcher = PATTERN.matcher(filter);
			if (!matcher.find())
				throw new IllegalArgumentException("Could not find pattern '" + REGEX + "' in filter string '" + filter + "'");
			symbolicName = matcher.group(1);
		}
		
		public String getSymbolicName() {
			return symbolicName;
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(FelixRepositoryAdapter.class);
	
	private final Map<String, Collection<Capability>> identityIndex = Collections.synchronizedMap(new HashMap<String, Collection<Capability>>());
	private final org.apache.felix.bundlerepository.Repository repository;
	
	private long lastUpdated;
	
	public FelixRepositoryAdapter(org.apache.felix.bundlerepository.Repository repository) {
		if (repository == null)
			throw new NullPointerException("Missing required parameter: repository");
		this.repository = repository;
	}
	
	public Collection<Capability> findProviders(Requirement requirement) {
		update();
		List<Capability> result = Collections.emptyList();
		if (IdentityNamespace.IDENTITY_NAMESPACE.equals(requirement.getNamespace())) {
			String symbolicName = new IdentityRequirementFilter(requirement.getDirectives().get(Constants.FILTER_DIRECTIVE)).getSymbolicName();
			logger.debug("Looking for symbolic name {}", symbolicName);
			Collection<Capability> capabilities = identityIndex.get(symbolicName);
			if (capabilities != null) {
				result = new ArrayList<Capability>(capabilities.size());
				for (Capability capability : capabilities) {
					if (ResourceHelper.matches(requirement, capability)) {
						result.add(capability);
					}
				}
				((ArrayList<Capability>)result).trimToSize();
			}
		}
		else {
			org.apache.felix.bundlerepository.Resource[] resources = repository.getResources();
			if (resources != null && resources.length != 0) {
				result = new ArrayList<Capability>(resources.length);
				for (final org.apache.felix.bundlerepository.Resource resource : resources) {
					Resource r = new FelixResourceAdapter(resource);
					for (Capability capability : r.getCapabilities(requirement.getNamespace()))
						if (ResourceHelper.matches(requirement, capability))
							result.add(capability);
				}
				((ArrayList<Capability>)result).trimToSize();
			}
		}
		return result;
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>(requirements.size());
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}
	
	private synchronized void update() {
		long lastModified = repository.getLastModified();
		logger.debug("The repository adaptor was last updated at {}. The repository was last modified at {}", lastUpdated, lastModified);
		if (lastModified > lastUpdated) {
			logger.debug("Updating the adapter with the modified repository contents...");
			lastUpdated = lastModified;
			synchronized (identityIndex) {
				identityIndex.clear();
				org.apache.felix.bundlerepository.Resource[] resources = repository.getResources();
				logger.debug("There are {} resources to evaluate", resources == null ? 0 : resources.length);
				if (resources != null && resources.length != 0) {
					for (org.apache.felix.bundlerepository.Resource resource : resources) {
						logger.debug("Evaluating resource {}", resource);
						String symbolicName = resource.getSymbolicName();
						Collection<Capability> capabilities = identityIndex.get(symbolicName);
						if (capabilities == null) {
							capabilities = new HashSet<Capability>();
							identityIndex.put(symbolicName, capabilities);
						}
						OsgiIdentityCapability capability = 
								new OsgiIdentityCapability(
									new FelixResourceAdapter(resource),
									symbolicName,
									resource.getVersion(),
									// TODO Assuming all resources are bundles. Need to support 
									// type fragment as well, but how do we know?
									IdentityNamespace.TYPE_BUNDLE);
						logger.debug("Indexing capability {}", capability);
						capabilities.add(capability);
					}
				}
			}
		}
	}
}
