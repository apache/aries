package org.apache.aries.subsystem.core.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.ResourceHelper;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class LocalRepository implements Repository {
	private final Collection<Resource> resources;
	
	public LocalRepository(Collection<Resource> resources) {
		this.resources = resources;
	}
	
	public Collection<Capability> findProviders(Requirement requirement) {
		ArrayList<Capability> result = new ArrayList<Capability>();
		for (Resource resource : resources)
			for (Capability capability : resource.getCapabilities(requirement.getNamespace()))
				if (ResourceHelper.matches(requirement, capability))
					result.add(capability);
		result.trimToSize();
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
