package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

public class Region implements Repository {
	private final AriesSubsystem scopedSubsystem;
	private final Collection<AriesSubsystem> subsystems = new HashSet<AriesSubsystem>();
	
	public Region(AriesSubsystem scopedSubsystem) {
		if (!scopedSubsystem.isApplication() || !scopedSubsystem.isComposite())
			throw new IllegalArgumentException("A region may only start with a scoped subsystem");
		this.scopedSubsystem = scopedSubsystem;
		subsystems.add(scopedSubsystem);
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
		
	}
}
