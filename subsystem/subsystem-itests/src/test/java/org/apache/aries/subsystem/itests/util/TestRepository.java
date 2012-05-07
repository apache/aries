package org.apache.aries.subsystem.itests.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class TestRepository implements Repository {
	public static class Builder {
		private final Collection<Resource> resources = new HashSet<Resource>();
		
		public TestRepository build() {
			return new TestRepository(resources);
		}
		
		public Builder resource(Resource value) {
			resources.add(value);
			return this;
		}
	}
	
	private final Collection<Resource> resources;
	
	public TestRepository(Collection<Resource> resources) {
		this.resources = resources;
	}
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for (Requirement requirement : requirements) {
			for (Resource resource : resources) {
				List<Capability> capabilities = resource.getCapabilities(requirement.getNamespace());
				for (Capability capability : capabilities) {
					if (ResourceHelper.matches(requirement, capability)) {
						Collection<Capability> c = result.get(requirement);
						if (c == null) {
							c = new HashSet<Capability>();
							result.put(requirement, c);
						}
						c.add(capability);
					}
				}
			}
		}
		return result;
	}
}
