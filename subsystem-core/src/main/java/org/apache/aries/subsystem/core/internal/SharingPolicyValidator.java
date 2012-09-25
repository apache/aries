/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraphVisitor;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.namespace.service.ServiceNamespace;
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
		// The osgi.service namespace must be translated into the
		// org.eclipse.equinox.allow.service namespace in order to validate
		// service sharing policies.
		Visitor visitor = new Visitor(
				ServiceNamespace.SERVICE_NAMESPACE.equals(capability
						.getNamespace()) ? RegionFilter.VISIBLE_SERVICE_NAMESPACE
						: capability.getNamespace(), capability.getAttributes());
		to.visitSubgraph(visitor);
		return visitor.contains(from);
	}
}
