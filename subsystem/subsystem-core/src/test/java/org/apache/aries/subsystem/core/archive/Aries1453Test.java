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
package org.apache.aries.subsystem.core.archive;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.aries.subsystem.core.capabilityset.CapabilitySet;
import org.apache.aries.subsystem.core.capabilityset.SimpleFilter;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/*
 * https://issues.apache.org/jira/browse/ARIES-1453
 * 
 * Fragment-Host requirements with version range do not match with 
 * FragmentHostCapability
 */
public class Aries1453Test {
	@Test
	public void shouldResolveFragmentHostWithVersionRangeAndMatchWithBundlesInThatRange() {
		FragmentHostHeader header = new FragmentHostHeader("host-bundle;bundle-version=\"[9.6.0,10)\"");
		FragmentHostRequirement requirement = new FragmentHostRequirement(
				header.getClauses().iterator().next(), null);
		FragmentHostCapability capability = new FragmentHostCapability(
				new BundleSymbolicNameHeader("host-bundle"), 
				new BundleVersionHeader("9.6.1"), 
				new Resource() {
					@Override
					public List<Capability> getCapabilities(String namespace) {
						return null;
					}

					@Override
					public List<Requirement> getRequirements(String namespace) {
						return null;
					}
				});
		String filterDirective = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        SimpleFilter simpleFilter = SimpleFilter.parse(filterDirective);
        CapabilitySet capabilitySet = new CapabilitySet(Arrays.asList(HostNamespace.HOST_NAMESPACE), true);
        capabilitySet.addCapability(capability);
        Set<Capability> capabilities = capabilitySet.match(simpleFilter, true);
        Assert.assertTrue(capabilities.size() == 1);
        Assert.assertSame(capabilities.iterator().next(), capability);
	}
}
