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
	
	private boolean handleExplicitlyInstalledBundleBundleContext(BundleRevision originRevision, BundleRevision bundleRevision) {
		// This means that some other bundle's context was used. The bundle
		// needs to be associated with all subsystems that are associated with
		// the bundle whose context was used to install the bundle.
		Collection<AriesSubsystem> subsystems = AriesSubsystem.getSubsystems(originRevision);
		if (subsystems.isEmpty())
			throw new IllegalStateException("Orphaned bundle revision detected: " + originRevision);
		for (AriesSubsystem s : subsystems)
			s.bundleInstalled(bundleRevision);
		return true;
	}
	
	private boolean handleExplicitlyInstalledBundleRegionDigraph(Bundle origin, BundleRevision bundleRevision) {
		// Otherwise, this is an explicitly installed bundle. That is, the
		// bundle is being installed outside of the Subsystem API using Region
		// Digraph or some other bundle's context.
		if ("org.eclipse.equionox.region".equals(origin.getSymbolicName())) {
			// This means Region Digraph was used to install the bundle. The
			// bundle needs to be associated with the scoped subsystem of the
			// region used to install the bundle.
			RegionDigraph digraph = Activator.getInstance().getRegionDigraph();
			Region region = digraph.getRegion(origin);
			for (AriesSubsystem s : AriesSubsystem.getSubsystems(null)) {
				if ((s.isApplication() || s.isComposite())
						&& region.equals(s.getRegion())) {
					s.bundleInstalled(bundleRevision);
					return true;
				}
			}
			throw new IllegalStateException("No subsystem found for bundle " + bundleRevision + " in region " + region);
		}
		return false;
	}
	
	private boolean handleImplicitlyInstalledResource(BundleRevision bundleRevision) {
		// If the thread local variable is set, this is an implicitly installed
		// bundle and needs to be associated with the subsystem installing it.
		AriesSubsystem subsystem = ThreadLocalSubsystem.get();
		if (subsystem != null) {
			subsystem.bundleInstalled(bundleRevision);
			return true;
		}
		return false;
	}
	
	private void handleInstalledEvent(BundleEvent event) {
		Bundle origin = event.getOrigin();
		BundleRevision originRevision = origin.adapt(BundleRevision.class);
		Bundle bundle = event.getBundle();
		BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
		if (!handleImplicitlyInstalledResource(bundleRevision)) {
			if (!handleExplicitlyInstalledBundleRegionDigraph(origin, bundleRevision)) {
				handleExplicitlyInstalledBundleBundleContext(originRevision, bundleRevision);
			}
		}
		bundleToRevision.put(bundle, bundleRevision);
	}
	
	private void handleUninstalledEvent(BundleEvent event) {
		Bundle bundle = event.getBundle();
		BundleRevision revision = bundleToRevision.remove(bundle);
		Collection<AriesSubsystem> subsystems = AriesSubsystem.getSubsystems(revision);
		if (subsystems.isEmpty())
			throw new IllegalStateException("Orphaned bundle revision detected: " + revision);
		for (AriesSubsystem subsystem : subsystems)
			subsystem.bundleUninstalled(revision);
	}
}
