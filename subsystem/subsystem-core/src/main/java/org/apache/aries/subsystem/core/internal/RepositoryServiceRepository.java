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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.SubsystemException;

public class RepositoryServiceRepository implements org.apache.aries.subsystem.core.repository.Repository {
    final BundleContext context;

	public RepositoryServiceRepository() {
		this(Activator.getInstance().getBundleContext());
	}

	RepositoryServiceRepository(BundleContext ctx) {
	    context = ctx;
	}

	@SuppressWarnings("unchecked")
	public Collection<Capability> findProviders(Requirement requirement) {
		Set<Capability> result = new HashSet<Capability>();
		ServiceReference<?>[] references;
		try {
			references = context.getAllServiceReferences("org.osgi.service.repository.Repository", null);
			if (references == null)
				return result;
		}
		catch (InvalidSyntaxException e) {
			throw new IllegalStateException(e);
		}
		for (ServiceReference<?> reference : references) {
			Object repository = context.getService(reference);
			if (repository == null)
				continue;
			try {
			    // Reflection is used here to allow the service to work with a mixture of
			    // Repository services implementing different versions of the API.

				Class<?> clazz = repository.getClass();
				Class<?> repoInterface = null;

				while (clazz != null && repoInterface == null) {
				    for (Class<?> intf : clazz.getInterfaces()) {
				        if (Repository.class.getName().equals(intf.getName())) {
				            // Compare interfaces by name so that we can work with different versions of the
				            // interface.
				            repoInterface = intf;
				            break;
				        }
				    }
                    clazz = clazz.getSuperclass();
				}

				if (repoInterface == null) {
				    continue;
				}

				Map<Requirement, Collection<Capability>> map;
				try {
					Method method = repoInterface.getMethod("findProviders", Collection.class);
					map = (Map<Requirement, Collection<Capability>>)method.invoke(repository, Collections.singleton(requirement));
				}
				catch (Exception e) {
					throw new SubsystemException(e);
				}
				Collection<Capability> capabilities = map.get(requirement);
				if (capabilities == null)
					continue;
				result.addAll(capabilities);
			}
			finally {
				context.ungetService(reference);
			}
		}
		return result;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}
}
