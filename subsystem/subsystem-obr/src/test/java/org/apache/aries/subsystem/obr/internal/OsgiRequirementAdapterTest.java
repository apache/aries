package org.apache.aries.subsystem.obr.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

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
