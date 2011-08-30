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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.wiring.BundleRevision;

public class SubsystemSynchronousBundleListener implements SynchronousBundleListener {
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		if (bundle.getBundleId() == 0) {
			// TODO If this event is associated with the system bundle, then all subsystems are affected.
			return;
		}
		Resource resource = bundle.adapt(BundleRevision.class);
		Collection<AriesSubsystem> subsystems = AriesSubsystem.getSubsystems(resource);
		for (AriesSubsystem subsystem : subsystems) {
			subsystem.bundleChanged(event);
		}
	}
}
