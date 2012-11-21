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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.wiring.BundleRevision;

public class BundleEventHook implements EventHook {
	private final Map<Bundle, BundleRevision> bundleToRevision;
	
	public BundleEventHook() {
		bundleToRevision = Collections.synchronizedMap(new HashMap<Bundle, BundleRevision>());
	}
	
	@Override
	public void event(BundleEvent event, Collection<BundleContext> contexts) {
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
	
	private void handleExplicitlyInstalledBundleBundleContext(BundleRevision originRevision, BundleRevision bundleRevision) {
		// The bundle needs to be associated with all subsystems that are 
		// associated with the bundle whose context was used to install the 
		// bundle.
		Collection<BasicSubsystem> subsystems = Activator.getInstance().getSubsystems().getSubsystemsReferencing(originRevision);
		if (subsystems.isEmpty())
			throw new IllegalStateException("Orphaned bundle revision detected: " + originRevision);
		for (BasicSubsystem s : subsystems)
			Utils.installResource(bundleRevision, s);
	}
	
	private void handleExplicitlyInstalledBundleRegionDigraph(Bundle origin, BundleRevision bundleRevision) {
			// The bundle needs to be associated with the scoped subsystem of 
			// the region used to install the bundle.
			RegionDigraph digraph = Activator.getInstance().getRegionDigraph();
			Region region = digraph.getRegion(origin);
			for (BasicSubsystem s : Activator.getInstance().getSubsystems().getSubsystems()) {
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
		bundleToRevision.put(bundle, bundleRevision);
		// Only handle explicitly installed bundles. An explicitly installed
		// bundle is a bundle that was installed using some other bundle's
		// BundleContext or using RegionDigraph.
		if (ThreadLocalSubsystem.get() != null)
			return;
		if ("org.eclipse.equionox.region".equals(origin.getSymbolicName()))
			// The bundle was installed using RegionDigraph.
			handleExplicitlyInstalledBundleRegionDigraph(origin, bundleRevision);
		else
			// The bundle was installed using some other bundle's BundleContext.
			handleExplicitlyInstalledBundleBundleContext(originRevision, bundleRevision);
	}
	
	@SuppressWarnings("unchecked")
	private void handleUninstalledEvent(BundleEvent event) {
		Bundle bundle = event.getBundle();
		BundleRevision revision = bundleToRevision.remove(bundle);
		if (ThreadLocalSubsystem.get() != null)
			return;
		Collection<BasicSubsystem> subsystems;
		if (revision == null) {
			// The bundle was installed while the bundle event hook was unregistered.
			Object[] o = Activator.getInstance().getSubsystems().getSubsystemsByBundle(bundle);
			if (o == null)
				return;
			revision = (BundleRevision)o[0];
			subsystems = (Collection<BasicSubsystem>)o[1];
		}
		else
			subsystems = Activator.getInstance().getSubsystems().getSubsystemsByConstituent(new BundleConstituent(null, revision));
		for (BasicSubsystem subsystem : subsystems)
			ResourceUninstaller.newInstance(revision, subsystem).uninstall();
	}
}
