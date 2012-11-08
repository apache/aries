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
			int result = 17;
			result = 31 * result + s.getLocation().hashCode();
			return result;
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
	
	public SubsystemGraph(BasicSubsystem root) {
		adjacencyList.put(new SubsystemWrapper(root), new HashSet<SubsystemWrapper>());
	}
	
	public synchronized void add(BasicSubsystem parent, BasicSubsystem child) {
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
	
	public synchronized Collection<Subsystem> getChildren(BasicSubsystem parent) {
		Collection<SubsystemWrapper> children = adjacencyList.get(new SubsystemWrapper(parent));
		if (children == null || children.isEmpty())
			return Collections.emptySet();
		Collection<Subsystem> result = new ArrayList<Subsystem>(children.size());
		for (SubsystemWrapper child : children)
			result.add(child.getSubsystem());
 		return Collections.unmodifiableCollection(result);
	}
	
	public synchronized Collection<Subsystem> getParents(BasicSubsystem child) {
		Collection<SubsystemWrapper> parents = getParents(new SubsystemWrapper(child));
		Collection<Subsystem> result = new ArrayList<Subsystem>(parents.size());
		for (SubsystemWrapper parent : parents) {
			result.add(parent.getSubsystem());
		}
		return Collections.unmodifiableCollection(result);
	}
	
	public synchronized void remove(BasicSubsystem child) {
		SubsystemWrapper subsystemWrap = new SubsystemWrapper(child);
		Collection<SubsystemWrapper> parents = getParents(subsystemWrap);
		for (SubsystemWrapper parent : parents)
			adjacencyList.get(parent).remove(subsystemWrap);
		adjacencyList.remove(subsystemWrap);
	}
	
	public synchronized void remove(BasicSubsystem parent, BasicSubsystem child) {
		SubsystemWrapper parentWrap = new SubsystemWrapper(parent);
		SubsystemWrapper childWrap = new SubsystemWrapper(child);
		adjacencyList.get(parentWrap).remove(childWrap);
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
