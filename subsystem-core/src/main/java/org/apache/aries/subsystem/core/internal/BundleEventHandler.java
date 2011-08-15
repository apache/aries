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
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.subsystem.SubsystemConstants;

public class BundleEventHandler implements EventHandler {
	public void handleEvent(Event event) {
		EventAdmin eventAdmin = Activator.getEventAdmin();
		if (eventAdmin == null) {
			return;
		}
		Bundle bundle = (Bundle)event.getProperty(EventConstants.BUNDLE);
		Resource resource = bundle.adapt(BundleRevision.class);
		Collection<AriesSubsystem> subsystems = Activator.getSubsystemManager().getSubsystems(resource);
		for (AriesSubsystem subsystem : subsystems) {
			Map<String, Object> map = new HashMap<String, Object>();
			for (String propertyName : event.getPropertyNames()) {
				map.put(propertyName, event.getProperty(propertyName));
			}
			map.put(SubsystemConstants.SUBSYSTEM_ID, subsystem.getSubsystemId());
			map.put(SubsystemConstants.SUBSYSTEM_LOCATION, subsystem.getLocation());
			map.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, subsystem.getSymbolicName());
			map.put(SubsystemConstants.SUBSYSTEM_VERSION, String.valueOf(subsystem.getVersion()));
			map.put(SubsystemConstants.SUBYSTEM_STATE, String.valueOf(subsystem.getState()));
			Event newEvent = new Event(SubsystemConstants.TOPIC_INTERNALS + event.getTopic(), map);
			eventAdmin.postEvent(newEvent);
		}
	}
}
