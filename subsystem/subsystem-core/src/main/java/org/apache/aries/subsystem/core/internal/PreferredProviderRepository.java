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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.apache.aries.subsystem.core.archive.PreferredProviderRequirement;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class PreferredProviderRepository implements org.apache.aries.subsystem.core.repository.Repository {
	// @GuardedBy("this")
	private boolean initialized;
	
	private final org.apache.aries.subsystem.core.repository.Repository repositoryServiceRepository = new RepositoryServiceRepository();
	private final SubsystemResource resource;
	private final Collection<Resource> resources = new HashSet<Resource>();
	private final org.apache.aries.subsystem.core.repository.Repository systemRepository = Activator.getInstance().getSystemRepository();
	
	public PreferredProviderRepository(SubsystemResource resource) {
		this.resource = resource;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		synchronized (this) {
			if (!initialized) {
				initialize();
				initialized = true;
			}
		}
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}
	
	private boolean addLocalRepositoryProviders(Requirement requirement) {
		return addProviders(requirement, resource.getLocalRepository(), false);
	}
	
	private boolean addProviders(Requirement requirement, org.apache.aries.subsystem.core.repository.Repository repository, boolean checkValid) {
		Map<Requirement, Collection<Capability>> map = repository.findProviders(Collections.singleton(requirement));
		Collection<Capability> capabilities = map.get(requirement);
		if (capabilities == null || capabilities.isEmpty())
			return false;
		for (Capability capability : map.get(requirement)) {
			if (checkValid && !isValid(capability))
				continue;
			resources.add(capability.getResource());
		}
		return true;
	}
	
	private boolean addRepositoryServiceProviders(Requirement requirement) {
		return addProviders(requirement, repositoryServiceRepository, false);
	}
	
	private boolean addSystemRepositoryProviders(Requirement requirement) {
		return addProviders(requirement, systemRepository, true);
	}
	
	private Collection<Capability> findProviders(Requirement requirement) {
		ArrayList<Capability> result = new ArrayList<Capability>(resources.size());
		for (Resource resource : resources)
			for (Capability capability : resource.getCapabilities(requirement.getNamespace()))
				if (ResourceHelper.matches(requirement, capability))
					result.add(capability);
		result.trimToSize();
		return result;
	}
	
	private void initialize() {
		PreferredProviderHeader header = resource.getSubsystemManifest().getPreferredProviderHeader();
		if (header == null)
			return;
		Collection<PreferredProviderRequirement> requirements = header.toRequirements(resource);
		for (PreferredProviderRequirement requirement : requirements)
			if (!addSystemRepositoryProviders(requirement))
				if (!addLocalRepositoryProviders(requirement))
					addRepositoryServiceProviders(requirement);
	}
	
	private boolean isValid(Capability capability) {
		for (BasicSubsystem parent : resource.getParents())
			for (Resource constituent : parent.getConstituents())
				if (ResourceHelper.areEqual(constituent, capability.getResource()))
					return true;
		return false;
	}
}
