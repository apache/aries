package org.apache.aries.subsystem.core.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.aries.subsystem.core.Resolver;
import org.apache.aries.subsystem.core.ResourceHelper;
import org.apache.felix.resolver.impl.ResolverImpl;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;

public class DependencyCalculator {
	private static class Environment implements
			org.apache.aries.subsystem.core.Environment {
		private final Collection<Resource> resources;

		public Environment(Collection<Resource> resources) {
			this.resources = resources;
		}

		@Override
		public SortedSet<Capability> findProviders(Requirement requirement) {
			ArrayList<Capability> capabilities = new ArrayList<Capability>();
			for (Resource resource : resources)
				for (Capability capability : resource
						.getCapabilities(requirement.getNamespace()))
					if (ResourceHelper.matches(requirement, capability))
						capabilities.add(capability);
			if (capabilities.isEmpty())
				capabilities.add(new MissingCapability(requirement));
			capabilities.trimToSize();
			return new TreeSet<Capability>(capabilities);
		}

		@Override
		public Map<Requirement, SortedSet<Capability>> findProviders(
				Collection<? extends Requirement> requirements) {
			Map<Requirement, SortedSet<Capability>> result = new HashMap<Requirement, SortedSet<Capability>>();
			for (Requirement requirement : requirements)
				result.put(requirement, findProviders(requirement));
			return result;
		}

		@Override
		public boolean isEffective(Requirement requirement) {
			return true;
		}

		@Override
		public Map<Resource, Wiring> getWirings() {
			return Collections.emptyMap();
		}
	}

	private static class MissingCapability extends AbstractCapability {
		private static class Resource implements org.osgi.resource.Resource {
			public static final Resource INSTANCE = new Resource();
			
			private final Capability identity;
			
			public Resource() {
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, "org.apache.aries.subsystem.resource.dummy");
				attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_UNKNOWN);
				identity = new BasicCapability(IdentityNamespace.IDENTITY_NAMESPACE, attributes, null, this);
			}
			
			@Override
			public List<Capability> getCapabilities(String namespace) {
				return Collections.singletonList(identity);
			}

			@Override
			public List<Requirement> getRequirements(String namespace) {
				return Collections.emptyList();
			}
		}
		
		private final Requirement requirement;

		public MissingCapability(Requirement requirement) {
			this.requirement = requirement;
		}

		@Override
		public Map<String, Object> getAttributes() {
			return Collections.emptyMap();
		}

		@Override
		public Map<String, String> getDirectives() {
			return Collections.emptyMap();
		}

		@Override
		public String getNamespace() {
			return requirement.getNamespace();
		}

		@Override
		public Resource getResource() {
			return Resource.INSTANCE;
		}
	}

	private final Environment environment;
	private final Collection<Resource> resources;

	public DependencyCalculator(Collection<Resource> resources) {
		environment = new Environment(resources);
		this.resources = resources;
	}

	public List<Requirement> calculateDependencies() {
		ArrayList<Requirement> result = new ArrayList<Requirement>();
		Resolver resolver = new ResolverImpl(null);
		Map<Resource, List<Wire>> resolution = resolver.resolve(environment,
				Collections.EMPTY_LIST, resources);
		for (List<Wire> wires : resolution.values())
			for (Wire wire : wires)
				if (wire.getCapability() instanceof MissingCapability)
					result.add(wire.getRequirement());
		result.trimToSize();
		return result;
	}
}
