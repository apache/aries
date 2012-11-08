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
	private BasicSubsystem root;
	private volatile SubsystemGraph graph;
	
	private final Map<Long, BasicSubsystem> idToSubsystem = new HashMap<Long, BasicSubsystem>();
	private final Map<String, BasicSubsystem> locationToSubsystem = new HashMap<String, BasicSubsystem>();
	private final ResourceReferences resourceReferences = new ResourceReferences();
	private final Map<BasicSubsystem, Set<Resource>> subsystemToConstituents = new HashMap<BasicSubsystem, Set<Resource>>();
	
	public void addChild(BasicSubsystem parent, BasicSubsystem child, boolean referenceCount) {
		graph.add(parent, child);
		child.addedParent(parent, referenceCount);
	}
	
	public void addConstituent(BasicSubsystem subsystem, Resource constituent, boolean referenced) {
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
	
	public void addReference(BasicSubsystem subsystem, Resource resource) {
		resourceReferences.addReference(subsystem, resource);
	}
	
	public void addSubsystem(BasicSubsystem subsystem) {
		synchronized (idToSubsystem) {
			synchronized (locationToSubsystem) {
				addIdToSubsystem(subsystem);
				addLocationToSubsystem(subsystem);
			}
		}
	}
	
	public Collection<Subsystem> getChildren(BasicSubsystem parent) {
		return graph.getChildren(parent);
	}
	
	public Collection<Resource> getConstituents(BasicSubsystem subsystem) {
		synchronized (subsystemToConstituents) {
			Collection<Resource> result = subsystemToConstituents.get(subsystem);
			if (result == null)
				return Collections.emptyList();
			return Collections.unmodifiableCollection(new ArrayList<Resource>(result));
		}
	}
	
	public Collection<Subsystem> getParents(BasicSubsystem child) {
		return graph.getParents(child);
	}
	
	public Collection<Resource> getResourcesReferencedBy(BasicSubsystem subsystem) {
		return resourceReferences.getResources(subsystem);
	}
	
	public synchronized BasicSubsystem getRootSubsystem() {
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
					root = new BasicSubsystem(resource);
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
				Collection<BasicSubsystem> subsystems = new ArrayList<BasicSubsystem>(fileList.size());
				try {
					for (File f : fileList) {
						BasicSubsystem s = new BasicSubsystem(f);
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
	
	public BasicSubsystem getSubsystemById(long id) {
		synchronized (idToSubsystem) {
			return idToSubsystem.get(id);
		}
	}
	
	public BasicSubsystem getSubsystemByLocation(String location) {
		synchronized (locationToSubsystem) {
			return locationToSubsystem.get(location);
		}
	}
	
	public Collection<BasicSubsystem> getSubsystems() {
		return new ArrayList<BasicSubsystem>(idToSubsystem.values());
	}
	
	public Collection<BasicSubsystem> getSubsystemsByConstituent(Resource constituent) {
		ArrayList<BasicSubsystem> result = new ArrayList<BasicSubsystem>();
		synchronized (subsystemToConstituents) {
			for (BasicSubsystem subsystem : subsystemToConstituents.keySet())
				if (getConstituents(subsystem).contains(constituent))
					result.add(subsystem);
		}
		result.trimToSize();
		return result;
	}
	
	public Collection<BasicSubsystem> getSubsystemsReferencing(Resource resource) {
		return resourceReferences.getSubsystems(resource);
	}
	
	public void removeChild(BasicSubsystem child) {
		graph.remove(child);
	}
	
	public void removeChild(BasicSubsystem parent, BasicSubsystem child) {
		graph.remove(parent, child);
	}
	
	public void removeConstituent(BasicSubsystem subsystem, Resource constituent) {
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
	
	public void removeReference(BasicSubsystem subsystem, Resource resource) {
		resourceReferences.removeReference(subsystem, resource);
	}
	
	public void removeSubsystem(BasicSubsystem subsystem) {
		synchronized (idToSubsystem) {
			synchronized (locationToSubsystem) {
				removeLocationToSubsystem(subsystem);
				removeIdToSubsystem(subsystem);
			}
		}
	}
	
	private void addIdToSubsystem(BasicSubsystem subsystem) {
		long id = subsystem.getSubsystemId();
		idToSubsystem.put(id, subsystem);
	}
	
	private void addLocationToSubsystem(BasicSubsystem subsystem) {
		String location = subsystem.getLocation();
		locationToSubsystem.put(location, subsystem);
	}
	
	private void removeIdToSubsystem(BasicSubsystem subsystem) {
		long id = subsystem.getSubsystemId();
		idToSubsystem.remove(id);
	}
	
	private void removeLocationToSubsystem(BasicSubsystem subsystem) {
		String location = subsystem.getLocation();
		locationToSubsystem.remove(location);
	}
}
