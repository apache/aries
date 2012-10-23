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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class Subsystems {
	private AriesSubsystem root;
	private volatile SubsystemGraph graph;
	
	private final Map<Long, AriesSubsystem> idToSubsystem = new HashMap<Long, AriesSubsystem>();
	private final Map<String, AriesSubsystem> locationToSubsystem = new HashMap<String, AriesSubsystem>();
	private final ResourceReferences resourceReferences = new ResourceReferences();
	private final Map<AriesSubsystem, Set<Resource>> subsystemToConstituents = new HashMap<AriesSubsystem, Set<Resource>>();
	
	public void addChild(AriesSubsystem parent, AriesSubsystem child, boolean referenceCount) {
		graph.add(parent, child);
		child.addedParent(parent, referenceCount);
	}
	
	public void addConstituent(AriesSubsystem subsystem, Resource constituent, boolean referenced) {
		synchronized (subsystemToConstituents) {
			Set<Resource> constituents = subsystemToConstituents.get(subsystem);
			if (constituents == null) {
				constituents = new HashSet<Resource>();
				subsystemToConstituents.put(subsystem, constituents);
			}
			constituents.add(constituent);
		}
		subsystem.addedConstituent(constituent, referenced);
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
			return Collections.unmodifiableCollection(new ArrayList<Resource>(result));
		}
	}
	
	public Collection<Subsystem> getParents(AriesSubsystem child) {
		return graph.getParents(child);
	}
	
	public Collection<Resource> getResourcesReferencedBy(AriesSubsystem subsystem) {
		return resourceReferences.getResources(subsystem);
	}
	
	public synchronized AriesSubsystem getRootSubsystem() {
		if (root == null) {
			File file = Activator.getInstance().getBundleContext().getDataFile("");
			File[] fileArray = file.listFiles();
			List<File> fileList = new ArrayList<File>(Arrays.asList(fileArray));
			Collections.sort(fileList, new Comparator<File>() {
				@Override
				public int compare(File file1, File file2) {
					String name1 = file1.getName();
					String name2 = file2.getName();
					return Long.valueOf(name1).compareTo(Long.valueOf(name2));
				}
			});
			if (fileList.isEmpty()) {
				// There are no persisted subsystems, including root.
				SubsystemResource resource;
				try {
					resource = new SubsystemResource(file);
				}
				catch (SubsystemException e) {
					throw e;
				}
				catch (Exception e) {
					throw new SubsystemException(e);
				}
				Coordination coordination = Utils.createCoordination();
				try {
					root = new AriesSubsystem(resource);
					// TODO This initialization is a bit brittle. The root subsystem
					// must be gotten before anything else will be able to use the
					// graph. At the very least, throw IllegalStateException where
					// appropriate.
					graph = new SubsystemGraph(root);
					ResourceInstaller.newInstance(coordination, root, root).install();
					// TODO Begin proof of concept.
					// This is a proof of concept for initializing the relationships between the root subsystem and bundles
					// that already existed in its region. Not sure this will be the final resting place. Plus, there are issues
					// since this does not take into account the possibility of already existing bundles going away or new bundles
					// being installed out of band while this initialization is taking place. Need a bundle event hook for that.
					BundleContext context = Activator.getInstance().getBundleContext().getBundle(org.osgi.framework.Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
					for (Bundle b : context.getBundles())
						ResourceInstaller.newInstance(coordination, b.adapt(BundleRevision.class), root).install();
					// TODO End proof of concept.
				} catch (Exception e) {
					coordination.fail(e);
				} finally {
					coordination.end();
				}
			}
			else {
				// There are persisted subsystems.
				Coordination coordination = Utils.createCoordination();
				Collection<AriesSubsystem> subsystems = new ArrayList<AriesSubsystem>(fileList.size());
				try {
					for (File f : fileList) {
						AriesSubsystem s = new AriesSubsystem(f);
						subsystems.add(s);
						addSubsystem(s);
					}
					root = getSubsystemById(0);
					graph = new SubsystemGraph(root);
					ResourceInstaller.newInstance(coordination, root, root).install();
				} catch (Exception e) {
					coordination.fail(e);
				} finally {
					coordination.end();
				}
			}
		}
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
				if (getConstituents(subsystem).contains(constituent))
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
			if (constituents != null) {
				constituents.remove(constituent);
				if (constituents.isEmpty())
					subsystemToConstituents.remove(subsystem);
			}
		}
		subsystem.removedContent(constituent);
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
		idToSubsystem.put(id, subsystem);
	}
	
	private void addLocationToSubsystem(AriesSubsystem subsystem) {
		String location = subsystem.getLocation();
		locationToSubsystem.put(location, subsystem);
	}
	
	private void removeIdToSubsystem(AriesSubsystem subsystem) {
		long id = subsystem.getSubsystemId();
		idToSubsystem.remove(id);
	}
	
	private void removeLocationToSubsystem(AriesSubsystem subsystem) {
		String location = subsystem.getLocation();
		locationToSubsystem.remove(location);
	}
}
