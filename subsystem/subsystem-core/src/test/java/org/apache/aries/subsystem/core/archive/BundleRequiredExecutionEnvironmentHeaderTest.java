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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.aries.subsystem.core.archive.BundleRequiredExecutionEnvironmentHeader.Clause.ExecutionEnvironment;
import org.apache.aries.subsystem.core.archive.BundleRequiredExecutionEnvironmentHeader.Clause.ExecutionEnvironment.Parser;
import org.apache.aries.subsystem.core.internal.BasicRequirement;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class BundleRequiredExecutionEnvironmentHeaderTest {
	@Test
	public void testClause() {
		String clauseStr = "CDC-1.0/Foundation-1.0";
		BundleRequiredExecutionEnvironmentHeader.Clause clause = new BundleRequiredExecutionEnvironmentHeader.Clause(clauseStr);
		assertClause(clause, clauseStr, "CDC/Foundation", "1.0", "(&(osgi.ee=CDC/Foundation)(version=1.0.0))");
	}
	
	@Test
	public void testExecutionEnvironment1() {
		String name = "foo";
		ExecutionEnvironment ee = new ExecutionEnvironment(name);
		assertExecutionEnvironmentName(ee, name);
		assertExecutionEnvironmentVersion(ee, (Version)null);
	}
	
	@Test
	public void testExecutionEnvironment2() {
		String name = "bar";
		Version version = Version.parseVersion("2.0.0.qualifier");
		ExecutionEnvironment ee = new ExecutionEnvironment(name, version);
		assertExecutionEnvironmentName(ee, name);
		assertExecutionEnvironmentVersion(ee, version);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testHeaderWithOneClause() {
		String value = "OSGi/Minimum-1.2";
		String filter = "(&(osgi.ee=OSGi/Minimum)(version=1.2.0))";
		BundleRequiredExecutionEnvironmentHeader header = new BundleRequiredExecutionEnvironmentHeader(value);
		assertEquals("Wrong number of clauses", 1, header.getClauses().size());
		assertClause(header.getClauses().iterator().next(), value, "OSGi/Minimum", "1.2", filter);
		assertEquals("Wrong name", Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, header.getName());
		assertEquals("Wrong value", value, header.getValue());
		Resource resource = EasyMock.createNiceMock(Resource.class);
		List<? extends Requirement> requirements = header.toRequirements(resource);
		assertEquals("Wrong number of requirements", 1, requirements.size());
		assertRequirement(requirements.get(0), filter, resource);
	}
	
	@Test
    @SuppressWarnings("deprecation")
	public void testHeaderWithMultipleClauses() {
		String value = "CDC-1.0/Foundation-1.0,OSGi/Minimum-1.2,J2SE-1.4,JavaSE-1.6,AA/BB-1.7,V1-1.5/V2-1.6,MyEE-badVersion";
		String filter = "(|" +
				"(&(osgi.ee=CDC/Foundation)(version=1.0.0))" +
				"(&(osgi.ee=OSGi/Minimum)(version=1.2.0))" +
				"(&(osgi.ee=JavaSE)(version=1.4.0))" +
				"(&(osgi.ee=JavaSE)(version=1.6.0))" +
				"(&(osgi.ee=AA/BB)(version=1.7.0))" +
				"(osgi.ee=V1-1.5/V2-1.6)" +
				"(osgi.ee=MyEE-badVersion))";
		BundleRequiredExecutionEnvironmentHeader header = new BundleRequiredExecutionEnvironmentHeader(value);
		assertEquals("Wrong number of clauses", 7, header.getClauses().size());
		assertClause(header.getClauses().iterator().next(), "CDC-1.0/Foundation-1.0", "CDC/Foundation", "1.0", "(&(osgi.ee=CDC/Foundation)(version=1.0.0))");
		assertEquals("Wrong name", Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, header.getName());
		assertEquals("Wrong value", value, header.getValue());
		Resource resource = EasyMock.createNiceMock(Resource.class);
		List<? extends Requirement> requirements = header.toRequirements(resource);
		assertEquals("Wrong number of requirements", 1, requirements.size());
		assertRequirement(requirements.get(0), filter, resource);
	}
	
	@Test
	public void testParser1() {
		doTestParser("CDC-1.0/Foundation-1.0", "CDC/Foundation", "1.0");
	}
	
	@Test
	public void testParser2() {
		doTestParser("OSGi/Minimum-1.2", "OSGi/Minimum", "1.2");
	}
	
	@Test
	public void testParser3() {
		doTestParser("J2SE-1.4", "JavaSE", "1.4");
	}
	
	@Test
	public void testParser4() {
		doTestParser("JavaSE-1.6", "JavaSE", "1.6");
	}
	
	@Test
	public void testParser5() {
		doTestParser("AA/BB-1.7", "AA/BB", "1.7");
	}
	
	@Test
	public void testParser6() {
		doTestParser("V1-1.5/V2-1.6", "V1-1.5/V2-1.6", (Version)null);
	}
	
	@Test
	public void testParser7() {
		doTestParser("MyEE-badVersion", "MyEE-badVersion", (Version)null);
	}
	
	private void assertClause(BundleRequiredExecutionEnvironmentHeader.Clause clause, String clauseStr, String name, String version, String filter) {
		assertClause(clause, clauseStr, name, Version.parseVersion(version), filter);
	}
	
	private void assertClause(BundleRequiredExecutionEnvironmentHeader.Clause clause, String clauseStr, String name, Version version, String filter) {
		assertNull("Attribute should not exist", clause.getAttribute(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE));
		assertTrue("Should have no attributes", clause.getAttributes().isEmpty());
		assertNull("Directive should not exist", clause.getDirective(ExecutionEnvironmentNamespace.REQUIREMENT_FILTER_DIRECTIVE));
		assertExecutionEnvironmentName(clause.getExecutionEnvironment(), name);
		assertExecutionEnvironmentVersion(clause.getExecutionEnvironment(), version);
		assertNull("Parameter should not exist", clause.getAttribute(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE));
		assertTrue("Should have no parameters", clause.getParameters().isEmpty());
		assertEquals("Wrong path", clauseStr, clause.getPath());
		assertRequirement(clause, filter);
	}
	
	private void assertExecutionEnvironmentName(ExecutionEnvironment ee, String name) {
		assertEquals("Wrong name", name, ee.getName());
	}
	
	private void assertExecutionEnvironmentVersion(ExecutionEnvironment ee, Version version) {
		assertEquals("Wrong version", version, ee.getVersion());
	}
	
	private void assertRequirement(BundleRequiredExecutionEnvironmentHeader.Clause clause, String filter) {
		Resource resource = EasyMock.createNiceMock(Resource.class);
		assertRequirement(clause.toRequirement(resource), filter, resource);
	}
	
	private void assertRequirement(Requirement requirement, String filter, Resource resource) {
		Requirement r = new BasicRequirement.Builder()
				.namespace(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)
				.directive(ExecutionEnvironmentNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter)
				.resource(resource)
				.build();
		assertEquals("Wrong requirement", r, requirement);
	}
	
	private void doTestParser(String clause, String name, String version) {
		doTestParser(clause, name, Version.parseVersion(version));
	}
	
	private void doTestParser(String clause, String name, Version version) {
		ExecutionEnvironment ee = null;
		try {
			ee = new Parser().parse(clause);
		}
		catch (Exception e) {
			fail("Unable to parse execution environment from clause " + clause);
		}
		assertExecutionEnvironmentName(ee, name);
		assertExecutionEnvironmentVersion(ee, version);
	}
}
