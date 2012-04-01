package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraphVisitor;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.resource.Capability;

public class SharingPolicyValidator {
	private static class Visitor implements RegionDigraphVisitor {
		private final Map<String, ?> attributes;
		private final String namespace;
		private final Collection<Region> visited;
		
		public Visitor(String namespace, Map<String, ?> attributes) {
			this.namespace = namespace;
			this.attributes = attributes;
			visited = new HashSet<Region>();
		}
		
		public boolean contains(Region region) {
			return visited.contains(region);
		}
		
		@Override
		public void postEdgeTraverse(RegionFilter filter) {
			// noop
		}

		@Override
		public boolean preEdgeTraverse(RegionFilter filter) {
			return filter.isAllowed(namespace, attributes);
		}

		@Override
		public boolean visit(Region region) {
			visited.add(region);
			return true;
		}
	}
	
	private final Region from;
	private final Region to;
	
	public SharingPolicyValidator(Region from, Region to) {
		this.from = from;
		this.to = to;
	}
	
	public boolean isValid(Capability capability) {
		Visitor visitor = new Visitor(capability.getNamespace(), capability.getAttributes());
		to.visitSubgraph(visitor);
		return visitor.contains(from);
	}
}
