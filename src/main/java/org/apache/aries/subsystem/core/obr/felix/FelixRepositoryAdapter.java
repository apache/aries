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
package org.apache.aries.subsystem.core.obr.felix;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.aries.subsystem.core.ResourceHelper;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.SubsystemException;

public class FelixRepositoryAdapter implements Repository {
	private final org.apache.felix.bundlerepository.Repository repository;
	
	public FelixRepositoryAdapter(org.apache.felix.bundlerepository.Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public Collection<Capability> findProviders(Requirement requirement) throws NullPointerException {
		org.apache.felix.bundlerepository.Resource[] resources = repository.getResources();
		ArrayList<Capability> result = new ArrayList<Capability>(resources.length);
		for (final org.apache.felix.bundlerepository.Resource resource : resources) {
			Resource r = new FelixResourceAdapter(resource);
			for (Capability capability : r.getCapabilities(requirement.getNamespace()))
				if (requirement.matches(capability))
					result.add(capability);
		}
		result.trimToSize();
		return result;
	}

	@Override
	public URL getContent(Resource resource) {
		for (final org.apache.felix.bundlerepository.Resource r : repository.getResources()) {
			final Resource sr = new FelixResourceAdapter(r);
			if (ResourceHelper.getTypeAttribute(resource).equals(ResourceHelper.getTypeAttribute(sr)))
				if (ResourceHelper.getSymbolicNameAttribute(resource).equals(ResourceHelper.getSymbolicNameAttribute(sr)))
					if (ResourceHelper.getVersionAttribute(resource).equals(ResourceHelper.getVersionAttribute(sr))) {
						try {
							return new URI(r.getURI()).toURL();
						}
						catch (Exception e) {
							// TODO Is this really what we want to do?
							throw new SubsystemException(e);
						}
					}
		}
		return null;
	}
}
