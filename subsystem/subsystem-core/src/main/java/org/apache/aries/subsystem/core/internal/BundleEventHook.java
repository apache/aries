package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
		Collection<AriesSubsystem> subsystems = Activator.getInstance().getSubsystems().getSubsystemsReferencing(originRevision);
		if (subsystems.isEmpty())
			throw new IllegalStateException("Orphaned bundle revision detected: " + originRevision);
		for (AriesSubsystem s : subsystems)
			Utils.installResource(bundleRevision, s);
	}
	
	private void handleExplicitlyInstalledBundleRegionDigraph(Bundle origin, BundleRevision bundleRevision) {
			// The bundle needs to be associated with the scoped subsystem of 
			// the region used to install the bundle.
			RegionDigraph digraph = Activator.getInstance().getRegionDigraph();
			Region region = digraph.getRegion(origin);
			for (AriesSubsystem s : Activator.getInstance().getSubsystems().getSubsystems()) {
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
	
	private void handleUninstalledEvent(BundleEvent event) {
		Bundle bundle = event.getBundle();
		BundleRevision revision = bundleToRevision.remove(bundle);
		if (ThreadLocalSubsystem.get() != null)
			return;
		for (AriesSubsystem subsystem : Activator.getInstance().getSubsystems().getSubsystemsByConstituent(revision))
			ResourceUninstaller.newInstance(revision, subsystem).uninstall();
	}
}
