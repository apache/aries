package org.apache.aries.subsystem.core.internal;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

public class RegionContextBundleManager {
	private static class BundleListener implements org.osgi.framework.BundleListener {
		private final Map<Bundle, AriesSubsystem> map;
		
		public BundleListener(Map<Bundle, AriesSubsystem> map) {
			this.map = map;
		}
		
		@Override
		public void bundleChanged(BundleEvent event) {
			// Figure out what action needs to be taken based on the event type.
			switch (event.getType()) {
				// We're interested in INSTALLED events because we may need to
				// assign the bundle to a subsystem.
				case BundleEvent.INSTALLED:
					// See if the bundle originating the event is the region
					// context bundle of some subsystem.
					AriesSubsystem subsystem = map.get(event.getOrigin());
					
					break;
				// We're interested in UNINSTALLED events because we may need to
				// do some clean up.
				case BundleEvent.UNINSTALLED:
					break;
			}
		}
	}
}
