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
		assertClauses(header, 1);
		assertVersionAttribute(header, "org.foo;org.bar;org.foo.bar", "1.3");
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
