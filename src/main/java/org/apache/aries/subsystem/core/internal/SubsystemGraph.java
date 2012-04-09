package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemGraph {
	private static class SubsystemWrapper {
		private final Subsystem s;
		
		public SubsystemWrapper(Subsystem subsystem) {
			s = subsystem;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof SubsystemWrapper))
				return false;
			SubsystemWrapper that = (SubsystemWrapper)o;
			return s.getLocation().equals(that.s.getLocation());
		}
		
		public Subsystem getSubsystem() {
			return s;
		}
		
		@Override
		public int hashCode() {
			return s.getLocation().hashCode();
		}
		
		@Override
		public String toString() {
			return new StringBuilder("location=").append(s.getLocation())
					.append(", symbolicName=").append(s.getSymbolicName())
					.append(", version=").append(s.getVersion())
					.append(", type=").append(s.getType()).toString();
		}
	}
	private final Map<SubsystemWrapper, Collection<SubsystemWrapper>> adjacencyList = new HashMap<SubsystemWrapper, Collection<SubsystemWrapper>>();
	
	public SubsystemGraph(AriesSubsystem root) {
		adjacencyList.put(new SubsystemWrapper(root), new HashSet<SubsystemWrapper>());
	}
	
	public synchronized void add(AriesSubsystem parent, AriesSubsystem child) {
		SubsystemWrapper parentWrap = new SubsystemWrapper(parent);
		SubsystemWrapper childWrap = new SubsystemWrapper(child);
		if (containsAncestor(childWrap, parentWrap))
			throw new SubsystemException("Cycle detected between '" + parentWrap + "' and '" + childWrap + "'");
		Collection<SubsystemWrapper> subsystems = adjacencyList.get(childWrap);
		if (subsystems == null) {
			subsystems = new HashSet<SubsystemWrapper>();
			adjacencyList.put(childWrap, subsystems);
		}
		subsystems = adjacencyList.get(parentWrap);
		if (subsystems == null) {
			subsystems = new HashSet<SubsystemWrapper>();
			adjacencyList.put(parentWrap, subsystems);
		}
		subsystems.add(childWrap);
	}
	
	public synchronized Collection<Subsystem> getChildren(AriesSubsystem parent) {
		Collection<SubsystemWrapper> children = adjacencyList.get(new SubsystemWrapper(parent));
		if (children == null || children.isEmpty())
			return Collections.emptySet();
		Collection<Subsystem> result = new ArrayList<Subsystem>(children.size());
		for (SubsystemWrapper child : children)
			result.add(child.getSubsystem());
 		return Collections.unmodifiableCollection(result);
	}
	
	public synchronized Collection<Subsystem> getParents(AriesSubsystem child) {
		Collection<SubsystemWrapper> parents = getParents(new SubsystemWrapper(child));
		Collection<Subsystem> result = new ArrayList<Subsystem>(parents.size());
		for (SubsystemWrapper parent : parents) {
			result.add(parent.getSubsystem());
		}
		return Collections.unmodifiableCollection(result);
	}
	
	public synchronized void remove(AriesSubsystem subsystem) {
		SubsystemWrapper subsystemWrap = new SubsystemWrapper(subsystem);
		Collection<SubsystemWrapper> parents = getParents(subsystemWrap);
		for (SubsystemWrapper parent : parents)
			adjacencyList.get(parent).remove(subsystemWrap);
		adjacencyList.remove(subsystemWrap);
	}
	
	private boolean containsAncestor(SubsystemWrapper subsystem, SubsystemWrapper ancestor) {
		Collection<SubsystemWrapper> subsystems = adjacencyList.get(subsystem);
		if (subsystems == null)
			return false;
		if (subsystems.contains(ancestor))
			return true;
		for (SubsystemWrapper s : subsystems) {
			return containsAncestor(s, ancestor);
		}
		return false;
	}
	
	private Collection<SubsystemWrapper> getParents(SubsystemWrapper child) {
		ArrayList<SubsystemWrapper> result = new ArrayList<SubsystemWrapper>();
		for (Entry<SubsystemWrapper, Collection<SubsystemWrapper>> entry : adjacencyList.entrySet())
			if (entry.getValue().contains(child))
				result.add(entry.getKey());
		result.trimToSize();
		return result;
	}
}
