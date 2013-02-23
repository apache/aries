package org.apache.aries.subsystem.obr.internal;

import static org.junit.Assert.assertEquals;

import org.apache.felix.bundlerepository.Requirement;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

public class FelixRequirementAdapterTest {
	@Test
	public void testCardinalityDirectiveMultiple() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getFilter()).andReturn("");
		EasyMock.expect(req.isMultiple()).andReturn(true);
		EasyMock.replay(req);
		FelixRequirementAdapter adapter = new FelixRequirementAdapter(req, EasyMock.createNiceMock(Resource.class));
		assertEquals(adapter.getDirectives().get(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE), Namespace.CARDINALITY_MULTIPLE);
	}
	
	@Test
	public void testCardinalityDirectiveSingle() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getFilter()).andReturn("");
		EasyMock.expect(req.isMultiple()).andReturn(false);
		EasyMock.replay(req);
		FelixRequirementAdapter adapter = new FelixRequirementAdapter(req, EasyMock.createNiceMock(Resource.class));
		assertEquals(adapter.getDirectives().get(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE), Namespace.CARDINALITY_SINGLE);
	}
	
	@Test
	public void testResolutionDirectiveMandatory() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getFilter()).andReturn("");
		EasyMock.expect(req.isOptional()).andReturn(false);
		EasyMock.replay(req);
		FelixRequirementAdapter adapter = new FelixRequirementAdapter(req, EasyMock.createNiceMock(Resource.class));
		assertEquals(adapter.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE), Namespace.RESOLUTION_MANDATORY);
	}
	
	@Test
	public void testResolutionDirectiveOptional() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getFilter()).andReturn("");
		EasyMock.expect(req.isOptional()).andReturn(true);
		EasyMock.replay(req);
		FelixRequirementAdapter adapter = new FelixRequirementAdapter(req, EasyMock.createNiceMock(Resource.class));
		assertEquals(adapter.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE), Namespace.RESOLUTION_OPTIONAL);
	}
}
