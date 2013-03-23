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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Requirement;

public class RegionUpdater {
	public static final int MAX_ATTEMPTS_DEFAULT = 10;
	
	private final RegionDigraph digraph;
	private final Region head;
	private final Region tail;
	
	public RegionUpdater(Region tail, Region head) {
		if (tail == null)
			throw new NullPointerException("Missing required parameter: tail");
		if (head == null)
			throw new NullPointerException("Missing required parameter: head");
		this.tail = tail;
		this.head = head;
		digraph = tail.getRegionDigraph();
	}
	
	public void addRequirements(Collection<? extends Requirement> requirements) throws BundleException, InvalidSyntaxException {
		for (int i = 0; i < MAX_ATTEMPTS_DEFAULT; i++) {
			RegionDigraph copy = copyDigraph();
			Region tail = copyTail(copy);
			Region head = copyHead(copy);
			Set<Long> bundleIds = copyBundleIds(tail);
			Map<String, RegionFilterBuilder> heads = copyHeadRegions(tail, copy);
			Map<String, RegionFilterBuilder> tails = copyTailRegions(tail, copy);
			copy.removeRegion(tail);
			tail = copy.createRegion(tail.getName());
			addBundleIds(bundleIds, tail);
			addRequirements(requirements, heads.get(head.getName()));
			addHeadRegions(heads, tail, copy);
			addTailRegions(tails, tail, copy);
			// Replace the current digraph.
			try {
				digraph.replace(copy);
			}
			catch (BundleException e) {
				// Something modified digraph since the copy was made.
				if (i < MAX_ATTEMPTS_DEFAULT)
					// There are more attempts to make.
					continue;
				// Number of attempts has been exhausted.
				throw e;
			}
			// Success! No need to continue looping.
			break;
		}
	}
	
	private void addBundleIds(Set<Long> ids, Region region) throws BundleException {
		for (Long id : ids)
			region.addBundle(id);
	}
	
	private void addHeadRegions(Map<String, RegionFilterBuilder> heads, Region tail, RegionDigraph digraph) throws BundleException {
		for (Map.Entry<String, RegionFilterBuilder> entry : heads.entrySet())
			tail.connectRegion(digraph.getRegion(entry.getKey()), entry.getValue().build());
	}
	
	private void addTailRegions(Map<String, RegionFilterBuilder> tails, Region head, RegionDigraph digraph) throws BundleException {
		for (Map.Entry<String, RegionFilterBuilder> entry : tails.entrySet())
			digraph.getRegion(entry.getKey()).connectRegion(head, entry.getValue().build());
	}
	
	private void addRequirements(Collection<? extends Requirement> requirements, RegionFilterBuilder builder) throws InvalidSyntaxException {
		for (Requirement requirement : requirements) {
			String namespace = requirement.getNamespace();
			// The osgi.service namespace requires translation.
			if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace))
				namespace = RegionFilter.VISIBLE_SERVICE_NAMESPACE;
			String filter = requirement.getDirectives().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE);
			// A null filter means import everything from that namespace.
			if (filter == null)
				builder.allowAll(namespace);
			else
				builder.allow(namespace, filter);
		}
	}
	
	private Set<Long> copyBundleIds(Region region) {
		return region.getBundleIds();
	}
	
	private RegionDigraph copyDigraph() throws BundleException {
		return digraph.copy();
	}
	
	private Region copyHead(RegionDigraph digraph) {
		return digraph.getRegion(head.getName());
	}
	
	private Map<String, RegionFilterBuilder> copyHeadRegions(Region tail, RegionDigraph digraph) throws InvalidSyntaxException {
		Map<String, RegionFilterBuilder> result = new HashMap<String, RegionFilterBuilder>();
		for (FilteredRegion edge : tail.getEdges())
			result.put(edge.getRegion().getName(), createRegionFilterBuilder(edge.getFilter().getSharingPolicy(), digraph));
		return result;
	}
	
	private Region copyTail(RegionDigraph digraph) {
		return digraph.getRegion(tail.getName());
	}
	
	private Map<String, RegionFilterBuilder> copyTailRegions(Region tail, RegionDigraph digraph) throws InvalidSyntaxException {
		Map<String, RegionFilterBuilder> result = new HashMap<String, RegionFilterBuilder>();
		for (Region head : digraph.getRegions()) {
			if (head.equals(tail))
				continue;
			for (FilteredRegion edge : head.getEdges())
				if (edge.getRegion().equals(tail))
					result.put(head.getName(), createRegionFilterBuilder(edge.getFilter().getSharingPolicy(), digraph));
		}
		return result;
	}
	
	private RegionFilterBuilder createRegionFilterBuilder(Map<String, Collection<String>> sharingPolicy, RegionDigraph digraph) throws InvalidSyntaxException {
		RegionFilterBuilder result = digraph.createRegionFilterBuilder();
		for (Map.Entry<String, Collection<String>> entry : sharingPolicy.entrySet())
			for (String filter : entry.getValue())
				result.allow(entry.getKey(), filter);
		return result;
	}
}
