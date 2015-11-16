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
