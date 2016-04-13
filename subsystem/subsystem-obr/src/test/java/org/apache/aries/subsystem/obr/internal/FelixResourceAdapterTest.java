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
package org.apache.aries.subsystem.obr.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.util.felix.FelixResourceAdapter;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Resource;
import org.easymock.EasyMock;
import org.junit.Test;

public class FelixResourceAdapterTest {
	@Test
	public void testGetCapabilitiesWithNullNamespace() {
		Resource resource = EasyMock.createNiceMock(Resource.class);
		Capability capability = EasyMock.createNiceMock(Capability.class);
		EasyMock.expect(capability.getName()).andReturn(Capability.PACKAGE);
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(Capability.PACKAGE, "org.foo.bar");
		EasyMock.expect(capability.getPropertiesAsMap()).andReturn(properties);
		Capability[] capabilities = new Capability[] {
				capability
		};
		EasyMock.expect(resource.getCapabilities()).andReturn(capabilities);
		EasyMock.replay(resource);
		FelixResourceAdapter adapter = new FelixResourceAdapter(resource);
		List<org.osgi.resource.Capability> caps = adapter.getCapabilities(null);
		// osgi.identity, osgi.content. osgi.wiring.host, and osgi.wiring.package
		assertEquals("Null namespace should return all capabilities", 4, caps.size());
	}
}
