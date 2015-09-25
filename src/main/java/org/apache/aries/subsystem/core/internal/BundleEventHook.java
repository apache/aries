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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;

public class BundleEventHook implements EventHook {
	private final Activator activator;
	private final ConcurrentHashMap<Bundle, BundleRevision> bundleToRevision;
	
	private boolean active;
	private List<BundleEvent> events;
	
	public BundleEventHook() {
		activator = Activator.getInstance();
		bundleToRevision = new ConcurrentHashMap<Bundle, BundleRevision>();
	}
	
	@Override
	public void event(BundleEvent event, Collection<BundleContext> contexts) {
		if ((event.getType() & (BundleEvent.INSTALLED | BundleEvent.UNINSTALLED)) == 0)
			return;
		// Protect against deadlock when the bundle event hook receives an
		// event before subsystems has fully initialized, in which case the
		// events are queued and processed once initialization is complete.
		synchronized (this) {
			if (!active) {
				if (events == null)
					events = new ArrayList<BundleEvent>();
				events.add(event);
				return;
			}
		}
		handleEvent(event);
	}
	
	// Events must be processed in order. Don't allow events to go through
	// synchronously before all pending events have been processed.
	synchronized void activate() {
		active = true;
		processPendingEvents();
	}
	
	synchronized void deactivate() {
		active = false;
	}
	
	synchronized void processPendingEvents() {
		if (events == null)
			return;
		for (BundleEvent event : events)
			handleEvent(event);
		events = null;
	}
	
	private Subsystems getSubsystems() {
		return activator.getSubsystems();
	}
	
	/*
	 * Note that because some events may be processed asynchronously, we can no
	 * longer rely on the guarantees that a synchronous event brings. For
	 * example, bundle revisions adapted from bundles included in events may be
	 * null.
	 */
	private void handleEvent(BundleEvent event) {
		switch (event.getType()) {
			case BundleEvent.INSTALLED:
				handleInstalledEvent(event);
				break;
			// TODO I think updates will play a role here as well. Need to keep
			// track of the most current bundle revision?
			case BundleEvent.UNINSTALLED:
				handleUninstalledEvent(event);
				break;
		}
	}
	
	/*
	 * This method guards against an uninstalled origin bundle. Guards against a
	 * null bundle revision are done elsewhere. It is assumed the bundle
	 * revision is never null once we get here.
	 */
	private void handleExplicitlyInstalledBundleBundleContext(BundleRevision originRevision, BundleRevision bundleRevision) {
		/* 
		 * The newly installed bundle must become a constituent of all the Subsystems of which the bundle
		 * whose context was used to perform the install is a constituent (OSGI.enterprise spec. 134.10.1.1).
		 */
		Collection<BasicSubsystem> subsystems = getSubsystems().getSubsystemsReferencing(originRevision);
		boolean bundleRevisionInstalled=false;
		for (BasicSubsystem s : subsystems) {
			for (Resource constituent : s.getConstituents()) {
				if (constituent instanceof BundleConstituent) {
					BundleRevision rev = ((BundleConstituent) constituent).getRevision();
					if (originRevision.equals(rev)) {
						Utils.installResource(bundleRevision, s);
						bundleRevisionInstalled=true;
					}
				}
			}
		}
		/* if the bundle is not made constituent of any subsystem then make it constituent of root */
		if (!bundleRevisionInstalled) {
			Utils.installResource(bundleRevision, getSubsystems().getRootSubsystem());
		}
	}	
	private void handleExplicitlyInstalledBundleRegionDigraph(Bundle origin, BundleRevision bundleRevision) {
			// The bundle needs to be associated with the scoped subsystem of 
			// the region used to install the bundle.
			RegionDigraph digraph = activator.getRegionDigraph();
			Region region = digraph.getRegion(origin);
			for (BasicSubsystem s : getSubsystems().getSubsystems()) {
				if ((s.isApplication() || s.isComposite())
						&& region.equals(s.getRegion())) {
					Utils.installResource(bundleRevision, s);
					return;
				}
			}
			throw new IllegalStateException("No subsystem found for bundle " + bundleRevision + " in region " + region);
	}
	
	private void handleInstalledEvent(BundleEvent event) {
		Bundle origin = event.getOrigin();
		BundleRevision originRevision = origin.adapt(BundleRevision.class);
		Bundle bundle = event.getBundle();
		BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
		if (bundleRevision == null) {
			// The event is being processed asynchronously and the installed
			// bundle has been uninstalled. Nothing we can do.
			return;
		}
		bundleToRevision.put(bundle, bundleRevision);
		// Only handle explicitly installed bundles. An explicitly installed
		// bundle is a bundle that was installed using some other bundle's
		// BundleContext or using RegionDigraph.
		if (ThreadLocalSubsystem.get() != null
				// Region context bundles must be treated as explicit installations.
				|| bundleRevision.getSymbolicName().startsWith(Constants.RegionContextBundleSymbolicNamePrefix)) {
			return;
		}
		// Indicate that a bundle is being explicitly installed on this thread.
		// This protects against attempts to resolve the bundle as part of
		// processing the explicit installation.
		ThreadLocalBundleRevision.set(bundleRevision);
		try {
			if ("org.eclipse.equinox.region".equals(origin.getSymbolicName())) {
				// The bundle was installed using RegionDigraph.
				handleExplicitlyInstalledBundleRegionDigraph(origin, bundleRevision);
			}
			else {
				// The bundle was installed using some other bundle's BundleContext.
				handleExplicitlyInstalledBundleBundleContext(originRevision, bundleRevision);
			}
		}
		finally {
			// Always remove the bundle so that it can be resolved no matter
			// what happens here.
			ThreadLocalBundleRevision.remove();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handleUninstalledEvent(BundleEvent event) {
		Bundle bundle = event.getBundle();
		BundleRevision revision = bundleToRevision.remove(bundle);
		if (ThreadLocalSubsystem.get() != null
				|| (revision == null ? false : 
					// Region context bundles must be treated as explicit installations.
					revision.getSymbolicName().startsWith(Constants.RegionContextBundleSymbolicNamePrefix))) {
			return;
		}
		Collection<BasicSubsystem> subsystems;
		if (revision == null) {
			// The bundle was installed while the bundle event hook was unregistered.
			Object[] o = activator.getSubsystems().getSubsystemsByBundle(bundle);
			if (o == null)
				return;
			revision = (BundleRevision)o[0];
			subsystems = (Collection<BasicSubsystem>)o[1];
		}
		else {
			subsystems = activator.getSubsystems().getSubsystemsByConstituent(new BundleConstituent(null, revision));
		}
		for (BasicSubsystem subsystem : subsystems) {
			ResourceUninstaller.newInstance(revision, subsystem).uninstall();
		}
	}
}
