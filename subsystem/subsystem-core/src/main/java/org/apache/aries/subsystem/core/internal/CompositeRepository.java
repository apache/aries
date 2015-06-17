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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class CompositeRepository implements org.apache.aries.subsystem.core.repository.Repository {
	private final Collection<org.apache.aries.subsystem.core.repository.Repository> repositories;
	
	public CompositeRepository(org.apache.aries.subsystem.core.repository.Repository...repositories) {
		this(Arrays.asList(repositories));
	}
	
	public CompositeRepository(Collection<org.apache.aries.subsystem.core.repository.Repository> repositories) {
		this.repositories = repositories;
	}
	
	public Collection<Capability> findProviders(Requirement requirement) {
		Set<Capability> result = new HashSet<Capability>();
		for (org.apache.aries.subsystem.core.repository.Repository repository : repositories) {
			Map<Requirement, Collection<Capability>> map = repository.findProviders(Collections.singleton(requirement));
			Collection<Capability> capabilities = map.get(requirement);
			if (capabilities == null)
				continue;
			result.addAll(capabilities);
		}
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
