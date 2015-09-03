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

import org.apache.aries.subsystem.core.capabilityset.CapabilitySetRepository;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ContentRepository implements org.apache.aries.subsystem.core.repository.Repository {
	private final CapabilitySetRepository installableContent;
	private final CapabilitySetRepository sharedContent;
	
	public ContentRepository(Collection<Resource> installableContent, Collection<Resource> sharedContent) {
	    this.installableContent = new CapabilitySetRepository();
	    for (Resource resource : installableContent) {
	        this.installableContent.addResource(resource);
	    }
	    this.sharedContent = new CapabilitySetRepository();
	    for (Resource resource : sharedContent) {
	        this.sharedContent.addResource(resource);
	    }
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
	    Map<Requirement, Collection<Capability>> result = sharedContent.findProviders(requirements);
	    for (Map.Entry<Requirement, Collection<Capability>> entry : result.entrySet()) {
	        if (entry.getValue().isEmpty()) {
	            entry.setValue(
	                    installableContent.findProviders(
	                            Collections.singletonList(entry.getKey())).get(entry.getKey()));
	        }
	    }
	    return result;
	}
}
