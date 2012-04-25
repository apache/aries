package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.aries.subsystem.core.ResourceHelper;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;

public class SystemRepository implements Repository {
	private final AriesSubsystem subsystem;
	
	public SystemRepository(AriesSubsystem subsystem) {
		this.subsystem = subsystem;
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
		findProviders(requirement, result);
		return result;
	}
	
	private void findProviders(Requirement requirement, Collection<Capability> capabilities) {
		AriesSubsystem subsystem = this.subsystem;
		if (requirement instanceof OsgiIdentityRequirement) {
			// We only want to return providers from the same region as the subsystem.
			// Find the one and only one scoped subsystem in the region, which
			// will be either the current subsystem or one of its parents.
			do {
				subsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
			} while (!(subsystem.isApplication() || subsystem.isComposite()));
			// Now search the one and only one scoped parent within the same region
			// and all children that are also in the same region for a provider.
			findProviders(subsystem, requirement, capabilities);
			return;
		}
		while (!subsystem.getParents().isEmpty())
			subsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
		findProviders(subsystem, requirement, capabilities);
	}
	
	private void findProviders(AriesSubsystem subsystem, Requirement requirement, Collection<Capability> capabilities) {
		// Because constituent providers are already provisioned resources, the
		// sharing policy check must be between the requiring subsystem and the
		// offering subsystem, not the subsystem the resource would be
		// provisioned to as in the other methods.
		SharingPolicyValidator validator = new SharingPolicyValidator(subsystem.getRegion(), this.subsystem.getRegion());
		for (Resource resource : subsystem.getConstituents()) {
			for (Capability capability : resource.getCapabilities(requirement.getNamespace())) {
				// Filter out capabilities offered by dependencies that will be
				// or already are provisioned to an out of scope region. This
				// filtering does not apply to osgi.identity requirements within
				// the same region.
				if (!(requirement instanceof OsgiIdentityRequirement) && !validator.isValid(capability))
					continue;
				if (ResourceHelper.matches(requirement, capability))
					capabilities.add(capability);
			}
		}
		for (Subsystem child : subsystem.getChildren()) {
			// If the requirement is osgi.identity and the child is not in the
			// same region as the parent, we do not want to search it.
			if (requirement instanceof OsgiIdentityRequirement
					&& !subsystem.getRegion().equals(((AriesSubsystem)child).getRegion()))
				continue;
			findProviders((AriesSubsystem)child, requirement, capabilities);
		}
	}
}
