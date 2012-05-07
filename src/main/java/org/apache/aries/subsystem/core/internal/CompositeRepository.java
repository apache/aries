package org.apache.aries.subsystem.core.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

public class CompositeRepository implements Repository {
	private final Collection<Repository> repositories;
	
	public CompositeRepository(Repository...repositories) {
		this(Arrays.asList(repositories));
	}
	
	public CompositeRepository(Collection<Repository> repositories) {
		this.repositories = repositories;
	}
	
	public Collection<Capability> findProviders(Requirement requirement) {
		Set<Capability> result = new HashSet<Capability>();
		for (Repository repository : repositories) {
			Map<Requirement, Collection<Capability>> map = repository.findProviders(Collections.singleton(requirement));
			Collection<Capability> capabilities = map.get(requirement);
			if (capabilities == null)
				continue;
			result.addAll(capabilities);
		}
		return result;	
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}
}
