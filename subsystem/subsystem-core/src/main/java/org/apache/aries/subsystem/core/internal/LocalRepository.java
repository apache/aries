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
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class LocalRepository implements org.apache.aries.subsystem.core.repository.Repository {
	private final Collection<Resource> resources;
	
	public LocalRepository(Collection<Resource> resources) {
		this.resources = resources;
	}
	
	public Collection<Capability> findProviders(Requirement requirement) {
		ArrayList<Capability> result = new ArrayList<Capability>();
		for (Resource resource : resources)
			for (Capability capability : resource.getCapabilities(requirement.getNamespace()))
				if (ResourceHelper.matches(requirement, capability))
					result.add(capability);
		result.trimToSize();
		return result;
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}
}
