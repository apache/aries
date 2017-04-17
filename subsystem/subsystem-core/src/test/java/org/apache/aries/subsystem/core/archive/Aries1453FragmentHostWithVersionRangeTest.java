package org.apache.aries.subsystem.core.archive;

import java.util.ArrayList;
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

public class Aries1453FragmentHostWithVersionRangeTest {
	
	@Test
	public void shouldResolveFragmentHostWithVersionRangeAndMatchWithBundlesInThatRange() {
		FragmentHostHeader header = new FragmentHostHeader("Fragment-Host: host-bundle;bundle-version=\"[9.6.0,10)\"");
		FragmentHostRequirement requirement = new FragmentHostRequirement(header.getClauses().iterator().next(), null);
		Resource fakeResource = createFakeResource();
		FragmentHostCapability capability = new FragmentHostCapability(new BundleSymbolicNameHeader("host-bundle"), new BundleVersionHeader("9.6.1"), fakeResource);
		fakeResource.getCapabilities("").add(capability);
		
		String filterDirective = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        SimpleFilter simpleFilter = SimpleFilter.parse(filterDirective);
        CapabilitySet capabilitySet = new CapabilitySet(Arrays.asList(HostNamespace.HOST_NAMESPACE), true);
        capabilitySet.addCapability(capability);
        Set<Capability> capabilities = capabilitySet.match(simpleFilter, true);
        Assert.assertTrue(capabilities.size() == 1);
        Assert.assertSame(capabilities.iterator().next(), capability);
	}

	private Resource createFakeResource() {
		Resource fakeResource = new Resource() {
			
			@Override
			public List<Requirement> getRequirements(String namespace) {
				return new ArrayList();
			}
			
			@Override
			public List<Capability> getCapabilities(String namespace) {
				return new ArrayList();
			}
		};
		return fakeResource;
	}

}
