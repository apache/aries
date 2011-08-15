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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.subsystem.Subsystem;

public class SubsystemServiceFactory implements ServiceFactory<Subsystem> {
	private final Map<Region, Subsystem> regionsToSubsystems = new HashMap<Region, Subsystem>();
	
	private final AriesSubsystem rootSubsystem;
	
	public SubsystemServiceFactory() throws Exception {
		rootSubsystem = new AriesSubsystem();
		Region region = Activator.getRegionDigraph().getRegion(Activator.getBundleContext().getBundle());
		regionsToSubsystems.put(region, rootSubsystem);
	}
	
	public Subsystem getService(Bundle bundle, ServiceRegistration<Subsystem> registration) {
		RegionDigraph digraph = Activator.getRegionDigraph();
		Region region = digraph.getRegion(bundle);
		Subsystem subsystem = regionsToSubsystems.get(region);
		if (subsystem == null) {
			subsystem = rootSubsystem;
			regionsToSubsystems.put(region, subsystem);
		}
		return subsystem;
	}

	public void ungetService(Bundle bundle, ServiceRegistration<Subsystem> registration, Subsystem service) {
		RegionDigraph digraph = Activator.getRegionDigraph();
		Region region = digraph.getRegion(bundle);
		regionsToSubsystems.remove(region);
	}
}
