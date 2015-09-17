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
import static org.junit.Assert.fail;

import org.junit.Test;
import org.osgi.framework.VersionRange;

public class ImportPackageHeaderTest {
	@Test
	public void testVersionAttributeWithMultiplePackages() {
		String headerStr = "org.foo;org.bar;org.foo.bar;version=1.3";
		ImportPackageHeader header = new ImportPackageHeader(headerStr);
		ImportPackageHeader header2 = new ImportPackageHeader(headerStr);
		assertClauses(header, 1);
		assertVersionAttribute(header, "org.foo;org.bar;org.foo.bar", "1.3");
		assertEquals(header, header2);
	}
	
	@Test
	public void testVersionAttributeWithoutMultiplePackages() {
		String headerStr = "org.foo,org.bar,org.foo.bar;version=1.3";
		ImportPackageHeader header = new ImportPackageHeader(headerStr);
		assertClauses(header, 3);
		assertVersionAttribute(header, "org.foo", "0");
		assertVersionAttribute(header, "org.bar", "0.0");
		assertVersionAttribute(header, "org.foo.bar", "1.3");
	}
	
	private void assertClauses(ImportPackageHeader header, int expectedClauses) {
		assertEquals("Wrong number of clauses", expectedClauses, header.getClauses().size());
	}
	
	private void assertVersionAttribute(ImportPackageHeader header, String path, String expectedVersion) {
		for (ImportPackageHeader.Clause clause : header.getClauses())
			if (path.equals(clause.getPath())) {
				assertVersionAttribute(clause, expectedVersion);
				return;
			}
		fail("Path not found: " + path);
	}
	
	private void assertVersionAttribute(ImportPackageHeader.Clause clause, String expectedVersion) {
		assertVersionAttribute(clause, new VersionRange(expectedVersion));
	}
	
	private void assertVersionAttribute(ImportPackageHeader.Clause clause, VersionRange expectedVersion) {
		assertEquals("Wrong version attribute", expectedVersion, clause.getVersionRangeAttribute().getVersionRange());
	}
}
