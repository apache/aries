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
		assertEquals("Wrong value for directive " + Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE, adapter.getDirectives().get(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE));
	}
	
	@Test
	public void testCardinalityDirectiveSingle() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getFilter()).andReturn("");
		EasyMock.expect(req.isMultiple()).andReturn(false);
		EasyMock.replay(req);
		FelixRequirementAdapter adapter = new FelixRequirementAdapter(req, EasyMock.createNiceMock(Resource.class));
		assertEquals("Wrong value for directive " + Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_SINGLE, adapter.getDirectives().get(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE));
	}
	
	@Test
	public void testResolutionDirectiveMandatory() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getFilter()).andReturn("");
		EasyMock.expect(req.isOptional()).andReturn(false);
		EasyMock.replay(req);
		FelixRequirementAdapter adapter = new FelixRequirementAdapter(req, EasyMock.createNiceMock(Resource.class));
		assertEquals("Wrong value for directive " + Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_MANDATORY, adapter.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
	}
	
	@Test
	public void testResolutionDirectiveOptional() {
		Requirement req = EasyMock.createNiceMock(Requirement.class);
		EasyMock.expect(req.getFilter()).andReturn("");
		EasyMock.expect(req.isOptional()).andReturn(true);
		EasyMock.replay(req);
		FelixRequirementAdapter adapter = new FelixRequirementAdapter(req, EasyMock.createNiceMock(Resource.class));
		assertEquals("Wrong value for directive " + Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL, adapter.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
	}
}
