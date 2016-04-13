/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.core.archive.DynamicImportPackageHeader;
import org.apache.aries.subsystem.core.archive.DynamicImportPackageRequirement;
import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
import org.apache.aries.subsystem.core.internal.StartAction.Restriction;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.eclipse.equinox.region.RegionDigraphVisitor;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class WovenClassListener implements org.osgi.framework.hooks.weaving.WovenClassListener {
	private static class RegionUpdaterInfo {
		private final Region head;
		private final Collection<DynamicImportPackageRequirement> requirements;
		private final Region tail;
		
		public RegionUpdaterInfo(Region tail, Region head) {
			this.tail = tail;
			this.head = head;
			requirements = new ArrayList<DynamicImportPackageRequirement>();
		}
		
		public Region head() {
			return head;
		}
		
		public void requirement(DynamicImportPackageRequirement requirement) {
			requirements.add(requirement);
		}
		
		public Collection<DynamicImportPackageRequirement> requirements() {
			return requirements;
		}
		
		public Region tail() {
			return tail;
		}
	}
	
	private final BundleContext context;
	private final Subsystems subsystems;
	
	public WovenClassListener(BundleContext context, Subsystems subsystems) {
		this.context = context;
		this.subsystems = subsystems;
	}
	
	@Override
	public void modified(WovenClass wovenClass) {
		if (wovenClass.getState() != WovenClass.TRANSFORMED) {
			// Dynamic package imports must be added when the woven class is in
			// the transformed state in order to ensure the class will load once
			// the defined state is reached.
			return;
		}
		List<String> dynamicImports = wovenClass.getDynamicImports();
		if (dynamicImports.isEmpty()) {
			// Nothing to do if there are no dynamic imports.
			return;
		}
		BundleWiring wiring = wovenClass.getBundleWiring();
		Bundle bundle = wiring.getBundle();
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		BundleConstituent constituent = new BundleConstituent(null, revision);
		Collection<BasicSubsystem> basicSubsystems = subsystems.getSubsystemsByConstituent(constituent);
		BasicSubsystem subsystem = basicSubsystems.iterator().next();
		// Find the scoped subsystem in the region.
		subsystem = scopedSubsystem(subsystem);
		if (subsystem.getSubsystemId() == 0) {
			// The root subsystem needs no sharing policy.
			return;
		}
		if (EnumSet.of(Subsystem.State.INSTALLING, Subsystem.State.INSTALLED).contains(subsystem.getState())) {
			// The scoped subsystem must be resolved before adding dynamic 
			// package imports to the sharing policy in order to minimize 
			// unpredictable wirings. Resolving the scoped subsystem will also
			// resolve all of the unscoped subsystems in the region.
			AccessController.doPrivileged(new StartAction(subsystem, subsystem, subsystem, Restriction.RESOLVE_ONLY));
		}
		Bundle systemBundle = context.getBundle(org.osgi.framework.Constants.SYSTEM_BUNDLE_LOCATION);
		FrameworkWiring frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
		// The following map tracks all of the necessary updates as each dynamic
		// import is processed. The key is the tail region of the connection 
		// whose filter needs updating.
		Map<Region, RegionUpdaterInfo> updates = new HashMap<Region, RegionUpdaterInfo>();
		for (String dynamicImport : dynamicImports) {
			// For each dynamic import, collect the necessary update information.
			DynamicImportPackageHeader header = new DynamicImportPackageHeader(dynamicImport);
			List<DynamicImportPackageRequirement> requirements = header.toRequirements(revision);
			for (DynamicImportPackageRequirement requirement : requirements) {
				Collection<BundleCapability> providers = frameworkWiring.findProviders(requirement);
				if (providers.isEmpty()) {
					// If nothing provides a capability matching the dynamic
					// import, no updates are made.
					continue;
				}
				addSharingPolicyUpdates(requirement, subsystem, providers, updates);
			}
		}
		// Now update each sharing policy only once.
		for (RegionUpdaterInfo update : updates.values()) {
			RegionUpdater updater = new RegionUpdater(update.tail(), update.head());
			try {
				updater.addRequirements(update.requirements());
			}
			catch (IllegalStateException e) {
				// Something outside of the subsystems implementation has
				// deleted the edge between the parent and child subsystems.
				// Assume the dynamic import sharing policy is being handled
				// elsewhere. See ARIES-1429.
			} 
			catch (Exception e) {
				throw new SubsystemException(e);
			} 
		}
	}
	
	private void addSharingPolicyUpdates(
			final DynamicImportPackageRequirement requirement, 
			final BasicSubsystem scopedSubsystem,
			final Collection<BundleCapability> providers,
			Map<Region, RegionUpdaterInfo> updates) {
		
		final List<BasicSubsystem> subsystems = new ArrayList<BasicSubsystem>();
		final Map<Region, BasicSubsystem> regionToSubsystem = new HashMap<Region, BasicSubsystem>();
		regionToSubsystem(scopedSubsystem, regionToSubsystem);
		scopedSubsystem.getRegion().visitSubgraph(new RegionDigraphVisitor() {
			private final List<BasicSubsystem> visited = new ArrayList<BasicSubsystem>();
			
			@Override
			public void postEdgeTraverse(RegionFilter filter) {
				// Nothing.
			}

			@Override
			public boolean preEdgeTraverse(RegionFilter filter) {
				return true;
			}

			@Override
			public boolean visit(Region region) {
				BasicSubsystem subsystem = regionToSubsystem.get(region);
				if (subsystem == null || subsystem.isRoot()) {
					// Don't mess with regions not created by the subsystem
					// implementation. Also, the root subsystem never has a
					// sharing policy.
					return false;
				}
				if (!visited.isEmpty() && !subsystem.equals(scopedParent(visited.get(visited.size() - 1)))) {
					// We're only interested in walking up the scoped parent tree.
					return false;
				}
				visited.add(subsystem);
				if (!requirement.getPackageName().contains("*")) {
					for (BundleCapability provider : providers) {
						BundleRevision br = provider.getResource();
						if (region.contains(br.getBundle())) {
							// The region contains a bundle providing a matching
							// capability, and the dynamic import does not contain a
							// wildcard. The requirement is therefore completely
							// satisfied.
							return false;
						}
					}
				}
				boolean allowed = false;
				Set<FilteredRegion> filters = region.getEdges();
				for (FilteredRegion filteredRegion : filters) {
					RegionFilter filter = filteredRegion.getFilter();
					if (filter.isAllowed(providers.iterator().next())) {
						// The region already allows matching capabilities
						// through so there is no need to update the sharing
						// policy.
						allowed = true;
						break;
					}
				}
				if (!allowed) {
					// The subsystem region requires a sharing policy update.
					subsystems.add(subsystem);
				}
				// Visit the next region.
				return true;
			}
		});
		// Collect the information for the necessary sharing policy updates.
		for (BasicSubsystem subsystem : subsystems) {
			Region tail = subsystem.getRegion();
			Region head = scopedParent(subsystem).getRegion();
			RegionUpdaterInfo info = updates.get(tail);
			if (info == null) {
				info = new RegionUpdaterInfo(tail, head);
				updates.put(tail, info);
			}
			info.requirement(requirement);
		}
	}
	
	private void regionToSubsystem(BasicSubsystem subsystem, Map<Region, BasicSubsystem> map) {
		map.put(subsystem.getRegion(), subsystem);
		subsystem = scopedParent(subsystem);
		if (subsystem == null) {
			return;
		}
		regionToSubsystem(subsystem, map);
	}
	
	private BasicSubsystem scopedParent(BasicSubsystem subsystem) {
		Collection<Subsystem> parents = subsystem.getParents();
		if (parents.isEmpty()) {
			return null;
		}
		subsystem = (BasicSubsystem)parents.iterator().next();
		return scopedSubsystem(subsystem);
	}
	
	private BasicSubsystem scopedSubsystem(BasicSubsystem subsystem) {
		while (!subsystem.isScoped()) {
			subsystem = (BasicSubsystem)subsystem.getParents().iterator().next();
		}
		return subsystem;
	}
}
