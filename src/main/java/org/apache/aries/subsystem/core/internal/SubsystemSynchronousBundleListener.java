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

import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemSynchronousBundleListener implements SynchronousBundleListener {
	private static final Logger logger = LoggerFactory.getLogger(SubsystemSynchronousBundleListener.class);
	
	public void bundleChanged(BundleEvent event) {
		String type;
		switch (event.getType()) {
			case BundleEvent.INSTALLED:
				type = "INSTALLED";
				break;
			case BundleEvent.LAZY_ACTIVATION:
				type = "LAZY_ACTIVATION";
				break;
			case BundleEvent.RESOLVED:
				type = "RESOLVED";
				break;
			case BundleEvent.STARTED:
				type = "STARTED";
				break;
			case BundleEvent.STARTING:
				type = "STARTING";
				break;
			case BundleEvent.STOPPED:
				type = "STOPPED";
				break;
			case BundleEvent.STOPPING:
				type = "STOPPING";
				break;
			case BundleEvent.UNINSTALLED:
				type = "UNINSTALLED";
				break;
			case BundleEvent.UNRESOLVED:
				type = "UNRESOLVED";
				break;
			case BundleEvent.UPDATED:
				type = "UPDATED";
				break;
			default:
				type = "Unknown (" + event.getType() + ")";
		}
		logger.debug("Received {} event for bundle {};{};{}", new Object[]{
				type,
				event.getBundle().getSymbolicName(),
				event.getBundle().getVersion(),
				event.getBundle().getBundleId()
		});
//		Bundle bundle = event.getBundle();
//		if (bundle.getBundleId() == 0) {
//			// TODO If this event is associated with the system bundle, then all subsystems are affected.
//			return;
//		}
//		Resource resource = bundle.adapt(BundleRevision.class);
//		Collection<AriesSubsystem> subsystems = AriesSubsystem.getSubsystems(resource);
//		for (AriesSubsystem subsystem : subsystems) {
//			subsystem.bundleChanged(event);
//		}
	}
}
