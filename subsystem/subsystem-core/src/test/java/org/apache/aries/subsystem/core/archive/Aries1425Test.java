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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/*
 * https://issues.apache.org/jira/browse/ARIES-1425
 * 
 * Support both osgi.bundle and osgi.fragment resource types when given a 
 * Subsystem-Content header clause with an unspecified type attribute.
 */
public class Aries1425Test {
	private static final String BUNDLE_A = "bundle.a";
	private static final String BUNDLE_B = "bundle.b;type=osgi.bundle";
	private static final String BUNDLE_C = "bundle.c;type=osgi.fragment";
	private static final String BUNDLE_D = "bundle.a;type=osgi.bundle";
	private static final String BUNDLE_E = "bundle.b";
	
	private static final String HEADER_1 = BUNDLE_A + ',' + BUNDLE_B + ',' + BUNDLE_C;
	private static final String HEADER_2 = BUNDLE_C + ',' + BUNDLE_D + ',' + BUNDLE_E;
	
	@Test
	public void testGetValue() {
		SubsystemContentHeader header = new SubsystemContentHeader(HEADER_1);
		assertEquals("Wrong value", HEADER_1, header.getValue());
	}
	
	@Test
	public void testToString() {
		SubsystemContentHeader header = new SubsystemContentHeader(HEADER_1);
		assertEquals("Wrong value", HEADER_1, header.toString());
	}
	
	@Test
	public void testClauseToString() {
		Set<String> clauseStrs = new HashSet<String>(Arrays.asList(HEADER_1.split(",")));
		SubsystemContentHeader header = new SubsystemContentHeader(HEADER_1);
		Collection<SubsystemContentHeader.Clause> clauses = header.getClauses();
		assertEquals("Wrong size", 3, clauses.size());
		for (SubsystemContentHeader.Clause clause : clauses) {
			String clauseStr = clause.toString();
			assertTrue("Wrong clause", clauseStrs.remove(clauseStr));
		}
	}
	
	@Test
	public void testGetType() {
		SubsystemContentHeader header = new SubsystemContentHeader(HEADER_1);
		Collection<SubsystemContentHeader.Clause> clauses = header.getClauses();
		assertEquals("Wrong size", 3, clauses.size());
		Map<String, SubsystemContentHeader.Clause> map = new HashMap<String, SubsystemContentHeader.Clause>(3);
		for (SubsystemContentHeader.Clause clause : clauses) {
			map.put(clause.toString(), clause);
		}
		SubsystemContentHeader.Clause clause = map.get(BUNDLE_A);
		assertEquals("Wrong type", IdentityNamespace.TYPE_BUNDLE, clause.getType());
		clause = map.get(BUNDLE_B);
		assertEquals("Wrong type", IdentityNamespace.TYPE_BUNDLE, clause.getType());
		clause = map.get(BUNDLE_C);
		assertEquals("Wrong type", IdentityNamespace.TYPE_FRAGMENT, clause.getType());
	}
	
	@Test
	public void testIsTypeSpecified() {
		SubsystemContentHeader header = new SubsystemContentHeader(HEADER_1);
		Collection<SubsystemContentHeader.Clause> clauses = header.getClauses();
		assertEquals("Wrong size", 3, clauses.size());
		Map<String, SubsystemContentHeader.Clause> map = new HashMap<String, SubsystemContentHeader.Clause>(3);
		for (SubsystemContentHeader.Clause clause : clauses) {
			map.put(clause.toString(), clause);
		}
		SubsystemContentHeader.Clause clause = map.get(BUNDLE_A);
		assertEquals("Should not be specified", Boolean.FALSE, clause.isTypeSpecified());
		clause = map.get(BUNDLE_B);
		assertEquals("Should be specified", Boolean.TRUE, clause.isTypeSpecified());
		clause = map.get(BUNDLE_C);
		assertEquals("Should be specified", Boolean.TRUE, clause.isTypeSpecified());
	}
	
	@Test
	public void testToRequirement() {
		SubsystemContentHeader header = new SubsystemContentHeader(HEADER_1);
		Collection<SubsystemContentHeader.Clause> clauses = header.getClauses();
		assertEquals("Wrong size", 3, clauses.size());
		Map<String, SubsystemContentHeader.Clause> map = new HashMap<String, SubsystemContentHeader.Clause>(3);
		for (SubsystemContentHeader.Clause clause : clauses) {
			map.put(clause.toString(), clause);
		}
		Resource resource = new Resource() {
			@Override
			public List<Capability> getCapabilities(String namespace) {
				return Collections.emptyList();
			}

			@Override
			public List<Requirement> getRequirements(String namespace) {
				return Collections.emptyList();
			}
		};
		SubsystemContentHeader.Clause clause = map.get(BUNDLE_A);
		Requirement requirement = clause.toRequirement(resource);
		assertTrue("Wrong requirement", ResourceHelper.matches(
				requirement, 
				new OsgiIdentityCapability(
						resource,
						BUNDLE_A,
						Version.emptyVersion,
						IdentityNamespace.TYPE_FRAGMENT)));
		assertTrue("Wrong requirement", ResourceHelper.matches(
				requirement, 
				new OsgiIdentityCapability(
						resource,
						BUNDLE_A,
						Version.emptyVersion,
						IdentityNamespace.TYPE_BUNDLE)));
		clause = map.get(BUNDLE_B);
		requirement = clause.toRequirement(resource);
		assertFalse("Wrong requirement", ResourceHelper.matches(
				requirement, 
				new OsgiIdentityCapability(
						resource,
						"bundle.b",
						Version.emptyVersion,
						IdentityNamespace.TYPE_FRAGMENT)));
		assertTrue("Wrong requirement", ResourceHelper.matches(
				requirement, 
				new OsgiIdentityCapability(
						resource,
						"bundle.b",
						Version.emptyVersion,
						IdentityNamespace.TYPE_BUNDLE)));
		clause = map.get(BUNDLE_C);
		requirement = clause.toRequirement(resource);
		assertTrue("Wrong requirement", ResourceHelper.matches(
				requirement, 
				new OsgiIdentityCapability(
						resource,
						"bundle.c",
						Version.emptyVersion,
						IdentityNamespace.TYPE_FRAGMENT)));
		assertFalse("Wrong requirement", ResourceHelper.matches(
				requirement, 
				new OsgiIdentityCapability(
						resource,
						"bundle.c",
						Version.emptyVersion,
						IdentityNamespace.TYPE_BUNDLE)));
	}
	
	@Test
	public void testEquals() {
		SubsystemContentHeader header1 = new SubsystemContentHeader(HEADER_1);
		SubsystemContentHeader header2 = new SubsystemContentHeader(HEADER_2);
		assertEquals("Headers are equal", header1, header2);
	}
	
	@Test
	public void testHashcode() {
		SubsystemContentHeader header1 = new SubsystemContentHeader(HEADER_1);
		SubsystemContentHeader header2 = new SubsystemContentHeader(HEADER_2);
		assertEquals("Headers are equal", header1.hashCode(), header2.hashCode());
	}
}
