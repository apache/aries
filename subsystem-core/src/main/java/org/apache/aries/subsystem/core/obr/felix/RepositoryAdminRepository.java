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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.repository.Repository;

public class RepositoryAdminRepository implements Repository {
	private final RepositoryAdmin repositoryAdmin;
	
	public RepositoryAdminRepository(RepositoryAdmin repositoryAdmin) {
		this.repositoryAdmin = repositoryAdmin;
	}
	
	@Override
	public Collection<Capability> findProviders(Requirement requirement) {
		org.apache.felix.bundlerepository.Repository[] repositories = repositoryAdmin.listRepositories();
		ArrayList<Capability> result = new ArrayList<Capability>();
		for (org.apache.felix.bundlerepository.Repository repository : repositories) {
			FelixRepositoryAdapter r = new FelixRepositoryAdapter(repository);
			result.addAll(r.findProviders(requirement));
		}
		return result;
	}

	@Override
	public URL getContent(Resource resource) {
		org.apache.felix.bundlerepository.Repository[] repositories = repositoryAdmin.listRepositories();
		for (org.apache.felix.bundlerepository.Repository repository : repositories) {
			FelixRepositoryAdapter r = new FelixRepositoryAdapter(repository);
			URL url = r.getContent(resource);
			if (url != null)
				return url;
		}
		return null;
	}
}
