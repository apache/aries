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
import java.util.Set;

import org.osgi.resource.Resource;

public class ResourceReferences {
	private final Map<Resource, Set<BasicSubsystem>> resourceToSubsystems = new HashMap<Resource, Set<BasicSubsystem>>();
	private final Map<BasicSubsystem, Set<Resource>> subsystemToResources = new HashMap<BasicSubsystem, Set<Resource>>();
	
	public synchronized void addReference(BasicSubsystem subsystem, Resource resource) {
		addSubsystemToResource(subsystem, resource);
		addResourceToSubsystem(subsystem, resource);
	}
	
	public synchronized Collection<Resource> getResources(BasicSubsystem subsystem) {
		Collection<Resource> result = subsystemToResources.get(subsystem);
		if (result == null)
			result = Collections.emptyList();
		return Collections.unmodifiableCollection(new ArrayList<Resource>(result));
	}
	
	public synchronized Collection<BasicSubsystem> getSubsystems(Resource resource) {
		Collection<BasicSubsystem> result = resourceToSubsystems.get(resource);
		if (result == null)
			result = Collections.emptyList();
		return Collections.unmodifiableCollection(new ArrayList<BasicSubsystem>(result));
	}
	
	public synchronized void removeReference(BasicSubsystem subsystem, Resource resource) {
		removeResourceToSubsystem(subsystem, resource);
		removeSubsystemToResource(subsystem, resource);
	}
	
	private void addResourceToSubsystem(BasicSubsystem subsystem, Resource resource) {
		Set<BasicSubsystem> subsystems = resourceToSubsystems.get(resource);
		if (subsystems == null) {
			subsystems = new HashSet<BasicSubsystem>();
			resourceToSubsystems.put(resource, subsystems);
		}
		subsystems.add(subsystem);
	}
	
	private void addSubsystemToResource(BasicSubsystem subsystem, Resource resource) {
		Set<Resource> resources = subsystemToResources.get(subsystem);
		if (resources == null) {
			resources = new HashSet<Resource>();
			subsystemToResources.put(subsystem, resources);
		}
		resources.add(resource);
	}
	
	private void removeResourceToSubsystem(BasicSubsystem subsystem, Resource resource) {
		Set<BasicSubsystem> subsystems = resourceToSubsystems.get(resource);
		if (subsystems == null)
			return;
		subsystems.remove(subsystem);
		if (subsystems.isEmpty())
			resourceToSubsystems.remove(resource);
	}
	
	private void removeSubsystemToResource(BasicSubsystem subsystem, Resource resource) {
		Set<Resource> resources = subsystemToResources.get(subsystem);
		if (resources == null)
			return;
		resources.remove(resource);
		if (resources.isEmpty())
			subsystemToResources.remove(subsystem);
	}
}
