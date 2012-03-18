package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;

public class ResourceReferences {
	private final Map<Resource, Set<Subsystem>> resourceToSubsystems = new HashMap<Resource, Set<Subsystem>>();
	private final Map<Subsystem, Set<Resource>> subsystemToResources = new HashMap<Subsystem, Set<Resource>>();
	
	public synchronized void addReference(Subsystem subsystem, Resource resource) {
		addSubsystemToResource(subsystem, resource);
		addResourceToSubsystem(subsystem, resource);
	}
	
	public synchronized Collection<Resource> getResources(Subsystem subsystem) {
		Collection<Resource> result = subsystemToResources.get(subsystem);
		if (result == null)
			result = Collections.emptyList();
		return Collections.unmodifiableCollection(new ArrayList<Resource>(result));
	}
	
	public synchronized Collection<Subsystem> getSubsystems(Resource resource) {
		Collection<Subsystem> result = resourceToSubsystems.get(resource);
		if (result == null)
			result = Collections.emptyList();
		return Collections.unmodifiableCollection(new ArrayList<Subsystem>(result));
	}
	
	public synchronized void removeReference(Subsystem subsystem, Resource resource) {
		removeResourceToSubsystem(subsystem, resource);
		removeSubsystemToResource(subsystem, resource);
	}
	
	private void addResourceToSubsystem(Subsystem subsystem, Resource resource) {
		Set<Subsystem> subsystems = resourceToSubsystems.get(resource);
		if (subsystems == null) {
			subsystems = new HashSet<Subsystem>();
			resourceToSubsystems.put(resource, subsystems);
		}
		subsystems.add(subsystem);
	}
	
	private void addSubsystemToResource(Subsystem subsystem, Resource resource) {
		Set<Resource> resources = subsystemToResources.get(subsystem);
		if (resources == null) {
			resources = new HashSet<Resource>();
			subsystemToResources.put(subsystem, resources);
		}
		resources.add(resource);
	}
	
	private void removeResourceToSubsystem(Subsystem subsystem, Resource resource) {
		Set<Subsystem> subsystems = resourceToSubsystems.get(resource);
		if (subsystems == null)
			return;
		subsystems.remove(subsystem);
		if (subsystems.isEmpty())
			resourceToSubsystems.remove(resource);
	}
	
	private void removeSubsystemToResource(Subsystem subsystem, Resource resource) {
		Set<Resource> resources = subsystemToResources.get(subsystem);
		if (resources == null)
			return;
		resources.remove(resource);
		if (resources.isEmpty())
			subsystemToResources.remove(subsystem);
	}
}
