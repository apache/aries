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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.wiring.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemManager {
	private final Map<String, AriesSubsystem> locationToSubsystem = new HashMap<String, AriesSubsystem>();
	
	public Collection<AriesSubsystem> getSubsystems(Resource resource) {
		ArrayList<AriesSubsystem> result = new ArrayList<AriesSubsystem>(locationToSubsystem.size());
		for (AriesSubsystem subsystem : locationToSubsystem.values()) {
			if (subsystem.contains(resource)) {
				result.add(subsystem);
			}
		}
		result.trimToSize();
		return result;
	}
	
	public AriesSubsystem newSubsystem(String location, InputStream content, AriesSubsystem parent) throws Exception {
		if (locationToSubsystem.containsKey(location)) {
			throw new SubsystemException("Subsystem already exists: " + location);
		}
		AriesSubsystem subsystem = new SubsystemFactory().create(location, content, parent);
		locationToSubsystem.put(location, subsystem);
		return subsystem;
	}
	
	public AriesSubsystem removeSubsystem(String location) {
		return locationToSubsystem.remove(location);
	}
	
	public boolean removeSubsystem(Subsystem subsystem) {
		return locationToSubsystem.remove(subsystem.getLocation()) != null;
	}
}
