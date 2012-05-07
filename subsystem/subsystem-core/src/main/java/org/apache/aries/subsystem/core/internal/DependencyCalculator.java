package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

public class DependencyCalculator {
	private static class ResolveContext extends
			org.osgi.service.resolver.ResolveContext {
		private final Collection<Resource> resources;

		public ResolveContext(Collection<Resource> resources) {
			this.resources = resources;
		}

		@Override
		public List<Capability> findProviders(Requirement requirement) {
			ArrayList<Capability> capabilities = new ArrayList<Capability>();
			for (Resource resource : resources)
				for (Capability capability : resource
						.getCapabilities(requirement.getNamespace()))
					if (ResourceHelper.matches(requirement, capability))
						capabilities.add(capability);
			if (capabilities.isEmpty())
				capabilities.add(new MissingCapability(requirement));
			capabilities.trimToSize();
			return capabilities;
		}

		@Override
		public boolean isEffective(Requirement requirement) {
			return true;
		}
		
		@Override
		public Collection<Resource> getMandatoryResources() {
			return resources;
		}

		@Override
		public Map<Resource, Wiring> getWirings() {
			return Collections.emptyMap();
		}

		@Override
		public int insertHostedCapability(List<Capability> capabilities,
				HostedCapability hostedCapability) {
			capabilities.add(hostedCapability);
			return capabilities.size() - 1;
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
		
		private final Map<String, Object> attributes = new HashMap<String, Object>();
		private final Requirement requirement;

		public MissingCapability(Requirement requirement) {
			this.requirement = requirement;
			initializeAttributes();
		}

		@Override
		public Map<String, Object> getAttributes() {
			return Collections.unmodifiableMap(attributes);
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
		
		private void initializeAttributes() {
			Pattern pattern = Pattern.compile("\\(([^(=]+)=([^)]+)\\)");
			Matcher matcher = pattern.matcher(requirement.getDirectives().get(Constants.FILTER_DIRECTIVE));
			while (matcher.find())
				attributes.put(matcher.group(1), matcher.group(2));
		}
	}

	private final ResolveContext context;

	public DependencyCalculator(Collection<Resource> resources) {
		context = new ResolveContext(resources);
	}

	public List<Requirement> calculateDependencies() throws ResolutionException {
		ArrayList<Requirement> result = new ArrayList<Requirement>();
		Resolver resolver = Activator.getInstance().getResolver();
		Map<Resource, List<Wire>> resolution = resolver.resolve(context);
		for (List<Wire> wires : resolution.values())
			for (Wire wire : wires)
				if (wire.getCapability() instanceof MissingCapability)
					result.add(wire.getRequirement());
		result.trimToSize();
		return result;
	}
}
