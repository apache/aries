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
	private final Map<Resource, Set<AriesSubsystem>> resourceToSubsystems = new HashMap<Resource, Set<AriesSubsystem>>();
	private final Map<AriesSubsystem, Set<Resource>> subsystemToResources = new HashMap<AriesSubsystem, Set<Resource>>();
	
	public synchronized void addReference(AriesSubsystem subsystem, Resource resource) {
		addSubsystemToResource(subsystem, resource);
		addResourceToSubsystem(subsystem, resource);
	}
	
	public synchronized Collection<Resource> getResources(AriesSubsystem subsystem) {
		Collection<Resource> result = subsystemToResources.get(subsystem);
		if (result == null)
			result = Collections.emptyList();
		return Collections.unmodifiableCollection(new ArrayList<Resource>(result));
	}
	
	public synchronized Collection<AriesSubsystem> getSubsystems(Resource resource) {
		Collection<AriesSubsystem> result = resourceToSubsystems.get(resource);
		if (result == null)
			result = Collections.emptyList();
		return Collections.unmodifiableCollection(new ArrayList<AriesSubsystem>(result));
	}
	
	public synchronized void removeReference(AriesSubsystem subsystem, Resource resource) {
		removeResourceToSubsystem(subsystem, resource);
		removeSubsystemToResource(subsystem, resource);
	}
	
	private void addResourceToSubsystem(AriesSubsystem subsystem, Resource resource) {
		Set<AriesSubsystem> subsystems = resourceToSubsystems.get(resource);
		if (subsystems == null) {
			subsystems = new HashSet<AriesSubsystem>();
			resourceToSubsystems.put(resource, subsystems);
		}
		subsystems.add(subsystem);
	}
	
	private void addSubsystemToResource(AriesSubsystem subsystem, Resource resource) {
		Set<Resource> resources = subsystemToResources.get(subsystem);
		if (resources == null) {
			resources = new HashSet<Resource>();
			subsystemToResources.put(subsystem, resources);
		}
		resources.add(resource);
	}
	
	private void removeResourceToSubsystem(AriesSubsystem subsystem, Resource resource) {
		Set<AriesSubsystem> subsystems = resourceToSubsystems.get(resource);
		if (subsystems == null)
			return;
		subsystems.remove(subsystem);
		if (subsystems.isEmpty())
			resourceToSubsystems.remove(resource);
	}
	
	private void removeSubsystemToResource(AriesSubsystem subsystem, Resource resource) {
		Set<Resource> resources = subsystemToResources.get(subsystem);
		if (resources == null)
			return;
		resources.remove(resource);
		if (resources.isEmpty())
			subsystemToResources.remove(subsystem);
	}
}
