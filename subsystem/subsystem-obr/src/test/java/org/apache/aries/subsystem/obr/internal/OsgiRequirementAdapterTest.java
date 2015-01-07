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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.util.felix.OsgiRequirementAdapter;
import org.apache.felix.bundlerepository.Capability;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
public class OsgiRequirementAdapterTest {
	@Test
	public void testGetNameBundle() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getNamespace()).andReturn(BundleNamespace.BUNDLE_NAMESPACE);
		EasyMock.replay(req);
		OsgiRequirementAdapter adapter = new OsgiRequirementAdapter(req);
		assertEquals("Wrong name", Capability.BUNDLE, adapter.getName());
	}
	
	@Test
	public void testGetNamePackage() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getNamespace()).andReturn(PackageNamespace.PACKAGE_NAMESPACE);
		EasyMock.replay(req);
		OsgiRequirementAdapter adapter = new OsgiRequirementAdapter(req);
		assertEquals("Wrong name", Capability.PACKAGE, adapter.getName());
	}
	
	@Test
	public void testGetNameService() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getNamespace()).andReturn(ServiceNamespace.SERVICE_NAMESPACE);
		EasyMock.replay(req);
		OsgiRequirementAdapter adapter = new OsgiRequirementAdapter(req);
		assertEquals("Wrong name", Capability.SERVICE, adapter.getName());
	}
	
	@Test
	public void testIsMultipleFalse() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		Map<String, String> directives = new HashMap<String, String>();
		directives.put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_SINGLE);
		EasyMock.expect(req.getDirectives()).andReturn(directives);
		EasyMock.replay(req);
		OsgiRequirementAdapter adapter = new OsgiRequirementAdapter(req);
		assertFalse("Requirement was multiple", adapter.isMultiple());
	}
	
	@Test
	public void testIsMultipleTrue() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		Map<String, String> directives = new HashMap<String, String>();
		directives.put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
		EasyMock.expect(req.getDirectives()).andReturn(directives);
		EasyMock.replay(req);
		OsgiRequirementAdapter adapter = new OsgiRequirementAdapter(req);
		assertTrue("Requirement was not multiple", adapter.isMultiple());
	}
}
