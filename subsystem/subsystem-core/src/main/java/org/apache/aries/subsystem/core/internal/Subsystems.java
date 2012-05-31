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

public class Subsystems {
	private final SubsystemGraph graph;
	private final Map<Long, AriesSubsystem> idToSubsystem = new HashMap<Long, AriesSubsystem>();
	private final Map<String, AriesSubsystem> locationToSubsystem = new HashMap<String, AriesSubsystem>();
	private final ResourceReferences resourceReferences = new ResourceReferences();
	private final AriesSubsystem root;
	private final Map<AriesSubsystem, Set<Resource>> subsystemToConstituents = new HashMap<AriesSubsystem, Set<Resource>>();
	
	public Subsystems() throws Exception {
		root = new AriesSubsystem();
		graph = new SubsystemGraph(root);
	}
	
	public void addChild(AriesSubsystem parent, AriesSubsystem child) {
		graph.add(parent, child);
	}
	
	public void addConstituent(AriesSubsystem subsystem, Resource constituent) {
		synchronized (subsystemToConstituents) {
			Set<Resource> constituents = subsystemToConstituents.get(subsystem);
			if (constituents == null) {
				constituents = new HashSet<Resource>();
				subsystemToConstituents.put(subsystem, constituents);
			}
			if (!constituents.add(constituent))
				throw new IllegalArgumentException("Constituent already exists");
		}
	}
	
	public void addReference(AriesSubsystem subsystem, Resource resource) {
		resourceReferences.addReference(subsystem, resource);
	}
	
	public void addSubsystem(AriesSubsystem subsystem) {
		synchronized (idToSubsystem) {
			synchronized (locationToSubsystem) {
				addIdToSubsystem(subsystem);
				addLocationToSubsystem(subsystem);
			}
		}
	}
	
	public Collection<Subsystem> getChildren(AriesSubsystem parent) {
		return graph.getChildren(parent);
	}
	
	public Collection<Resource> getConstituents(AriesSubsystem subsystem) {
		synchronized (subsystemToConstituents) {
			Collection<Resource> result = subsystemToConstituents.get(subsystem);
			if (result == null)
				return Collections.emptyList();
			return Collections.unmodifiableCollection(result);
		}
	}
	
	public Collection<Subsystem> getParents(AriesSubsystem child) {
		return graph.getParents(child);
	}
	
	public Collection<Resource> getResourcesReferencedBy(AriesSubsystem subsystem) {
		return resourceReferences.getResources(subsystem);
	}
	
	public AriesSubsystem getRootSubsystem() {
		return root;
	}
	
	public AriesSubsystem getSubsystemById(long id) {
		synchronized (idToSubsystem) {
			return idToSubsystem.get(id);
		}
	}
	
	public AriesSubsystem getSubsystemByLocation(String location) {
		synchronized (locationToSubsystem) {
			return locationToSubsystem.get(location);
		}
	}
	
	public Collection<AriesSubsystem> getSubsystems() {
		return new ArrayList<AriesSubsystem>(idToSubsystem.values());
	}
	
	public Collection<AriesSubsystem> getSubsystemsByConstituent(Resource constituent) {
		ArrayList<AriesSubsystem> result = new ArrayList<AriesSubsystem>();
		synchronized (subsystemToConstituents) {
			for (AriesSubsystem subsystem : subsystemToConstituents.keySet())
				if (subsystem.contains(constituent))
					result.add(subsystem);
		}
		result.trimToSize();
		return result;
	}
	
	public Collection<AriesSubsystem> getSubsystemsReferencing(Resource resource) {
		return resourceReferences.getSubsystems(resource);
	}
	
	public void removeChild(AriesSubsystem child) {
		graph.remove(child);
	}
	
	public void removeChild(AriesSubsystem parent, AriesSubsystem child) {
		graph.remove(parent, child);
	}
	
	public void removeConstituent(AriesSubsystem subsystem, Resource constituent) {
		synchronized (subsystemToConstituents) {
			Set<Resource> constituents = subsystemToConstituents.get(subsystem);
			if (!constituents.remove(constituent))
				throw new IllegalArgumentException("Constituent does not exist");
		}
	}
	
	public void removeReference(AriesSubsystem subsystem, Resource resource) {
		resourceReferences.removeReference(subsystem, resource);
	}
	
	public void removeSubsystem(AriesSubsystem subsystem) {
		synchronized (idToSubsystem) {
			synchronized (locationToSubsystem) {
				removeLocationToSubsystem(subsystem);
				removeIdToSubsystem(subsystem);
			}
		}
	}
	
	private void addIdToSubsystem(AriesSubsystem subsystem) {
		long id = subsystem.getSubsystemId();
		if (idToSubsystem.containsKey(id))
			throw new IllegalArgumentException("Subsystem ID already exists: " + id);
		idToSubsystem.put(id, subsystem);
	}
	
	private void addLocationToSubsystem(AriesSubsystem subsystem) {
		String location = subsystem.getLocation();
		if (locationToSubsystem.containsKey(location))
			throw new IllegalArgumentException("Subsystem location already exists: " + location);
		locationToSubsystem.put(location, subsystem);
	}
	
	private void removeIdToSubsystem(AriesSubsystem subsystem) {
		long id = subsystem.getSubsystemId();
		if (idToSubsystem.remove(id) == null)
			throw new IllegalArgumentException("Subsystem ID does not exist: " + id);
	}
	
	private void removeLocationToSubsystem(AriesSubsystem subsystem) {
		String location = subsystem.getLocation();
		if (locationToSubsystem.remove(location) == null)
			throw new IllegalArgumentException("Subsystem location does not exist: " + location);
	}
}
