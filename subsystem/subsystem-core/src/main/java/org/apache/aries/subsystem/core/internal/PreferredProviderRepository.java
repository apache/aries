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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.apache.aries.subsystem.core.archive.PreferredProviderRequirement;
import org.apache.aries.subsystem.core.capabilityset.CapabilitySetRepository;
import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
import org.apache.aries.subsystem.core.repository.Repository;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class PreferredProviderRepository implements org.apache.aries.subsystem.core.repository.Repository {
    private final CapabilitySetRepository repository;
	private final SubsystemResource resource;
	
	public PreferredProviderRepository(SubsystemResource resource) {
		this.resource = resource;
		repository = new CapabilitySetRepository();
		PreferredProviderHeader header = resource.getSubsystemManifest().getPreferredProviderHeader();
		if (header != null) {
		    Collection<PreferredProviderRequirement> requirements = header.toRequirements(resource);
	        for (PreferredProviderRequirement requirement : requirements) {
	            if (!addProviders(requirement, Activator.getInstance().getSystemRepository(), true)) {
	                if (!addProviders(requirement, resource.getLocalRepository(), false)) {
	                    addProviders(requirement, new RepositoryServiceRepository(), false);
	                }
	            }
	        }
		}
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return repository.findProviders(requirements);
	}
	
	private boolean addProviders(Requirement requirement, Repository repository, boolean checkValid) {
	    boolean result = false;
		Map<Requirement, Collection<Capability>> map = repository.findProviders(Collections.singleton(requirement));
		Collection<Capability> capabilities = map.get(requirement);
		for (Capability capability : capabilities) {
		    if (checkValid ? isValid(capability) : true) {
		        this.repository.addResource(capability.getResource());
		        result = true;
		    }
		}
		return result;
	}
	
	/*
	 * This check is only done on capabilities provided by resources in the
	 * system repository. This currently includes only BasicSubsystem and
	 * BundleRevision.
	 */
	private boolean isValid(Capability capability) {
		for (BasicSubsystem parent : resource.getParents()) {
			Resource provider = capability.getResource();
			if (provider instanceof BundleRevision) {
				// To keep the optimization below, wrap bundle revisions with
				// a bundle constituent so that the comparison works.
				provider = new BundleConstituent(null, (BundleRevision)provider);
			}
			// Optimization from ARIES-1397. Perform a contains operation on the
			// parent constituents rather than use ResourceHelper.
		    if (parent.getConstituents().contains(provider)) {
		        return true;
		    }
		}
		return false;
	}
}
