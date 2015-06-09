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

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ContentRepository implements org.apache.aries.subsystem.core.repository.Repository {
	private static void findContent(Requirement requirement, Collection<Capability> capabilities, Collection<Resource> resources) {
		for (Resource resource : resources)
			for (Capability capability : resource.getCapabilities(requirement.getNamespace()))
				if (ResourceHelper.matches(requirement, capability))
					capabilities.add(capability);
	}
	
	private final Collection<Resource> installableContent;
	private final Collection<Resource> sharedContent;
	
	public ContentRepository(Collection<Resource> installableContent, Collection<Resource> sharedContent) {
		this.installableContent = new HashSet<Resource>(installableContent);
		this.sharedContent = new HashSet<Resource>(sharedContent);
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}

	public Collection<Capability> findProviders(Requirement requirement) {
		ArrayList<Capability> result = new ArrayList<Capability>();
		if (findSharedContent(requirement, result))
			return result;
		findInstallableContent(requirement, result);
		result.trimToSize();
		return result;
	}
	
	public Collection<Resource> getInstallableContent() {
		return Collections.unmodifiableCollection(installableContent);
	}
	
	public Collection<Resource> getSharedContent() {
		return Collections.unmodifiableCollection(sharedContent);
	}
	
	private void findInstallableContent(Requirement requirement, Collection<Capability> capabilities) {
		findContent(requirement, capabilities, installableContent);
	}
	
	private boolean findSharedContent(Requirement requirement, Collection<Capability> capabilities) {
		int size = capabilities.size();
		findContent(requirement, capabilities, sharedContent);
		return size < capabilities.size();
	}
}
