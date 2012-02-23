package org.apache.aries.subsystem.itests.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;

public class TestResource implements Resource {
	public static class Builder {
		private final List<TestCapability.Builder> capabilities = new ArrayList<TestCapability.Builder>();
		private final List<Requirement> requirements = new ArrayList<Requirement>();
		
		public TestResource build() {
			return new TestResource(capabilities, requirements);
		}
		
		public Builder capability(TestCapability.Builder value) {
			capabilities.add(value);
			return this;
		}
		
		public Builder requirement(Requirement value) {
			requirements.add(value);
			return this;
		}
	}
	
	private final List<Capability> capabilities;
	private final List<Requirement> requirements;
	
	public TestResource(List<TestCapability.Builder> capabilities, List<Requirement> requirements) {
		this.capabilities = new ArrayList<Capability>(capabilities.size());
		for (TestCapability.Builder builder : capabilities)
			this.capabilities.add(builder.resource(this).build());
		this.requirements = requirements;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(capabilities);
		ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (Capability capability : capabilities)
			if (namespace.equals(capability.getNamespace()))
				result.add(capability);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(requirements);
		ArrayList<Requirement> result = new ArrayList<Requirement>(requirements.size());
		for (Requirement requirement : requirements)
			if (namespace.equals(requirement.getNamespace()))
				result.add(requirement);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}
}
