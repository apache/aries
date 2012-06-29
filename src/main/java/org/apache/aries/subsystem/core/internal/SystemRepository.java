package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class SystemRepository implements Repository {
	private final AriesSubsystem root;
	
	public SystemRepository(AriesSubsystem root) {
		this.root = root;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}
	
	public Collection<Capability> findProviders(Requirement requirement) {
		Collection<Capability> result = new HashSet<Capability>();
		findProviders(requirement, result, root);
		return result;
	}
	
	private void findProviders(Requirement requirement, Collection<Capability> capabilities, AriesSubsystem subsystem) {
		// Need to examine capabilities offered by the subsystem itself.
		// For example, the requirement might be an osgi.identity
		// requirement for a preferred provider that's a subsystem.
		for (Capability capability : subsystem.getCapabilities(requirement.getNamespace()))
			if (ResourceHelper.matches(requirement, capability))
				capabilities.add(capability);
		for (Resource constituent : subsystem.getConstituents()) {
			if (constituent instanceof AriesSubsystem)
				findProviders(requirement, capabilities, (AriesSubsystem)constituent);
			else
				for (Capability capability : constituent.getCapabilities(requirement.getNamespace()))
					if (ResourceHelper.matches(requirement, capability))
						capabilities.add(capability);
		}
	}
}
