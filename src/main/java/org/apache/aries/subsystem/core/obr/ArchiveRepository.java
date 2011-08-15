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
package org.apache.aries.subsystem.core.obr;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.Archive;
import org.apache.aries.subsystem.core.resource.ResourceFactory;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.repository.Repository;

public class ArchiveRepository implements Repository {
	private final Map<Resource, URL> resources;
	
	public ArchiveRepository(Archive archive) throws IOException, URISyntaxException {
		Collection<String> resourceNames = archive.getResourceNames();
		 resources = new HashMap<Resource, URL>(resourceNames.size());
		for (String resourceName : archive.getResourceNames()) {
			URL url = archive.getURL(resourceName);
			resources.put(new ResourceFactory().newResource(url), url);
		}
	}

	@Override
	public Collection<Capability> findProviders(Requirement requirement) {
		Collection<Capability> capabilities = new ArrayList<Capability>();
		for (Resource resource : resources.keySet())
			for (Capability capability : resource.getCapabilities(requirement.getNamespace()))
				if (requirement.matches(capability))
					capabilities.add(capability);
		return capabilities;
	}

	@Override
	public URL getContent(Resource resource) {
		return resources.get(resource);
	}
}
