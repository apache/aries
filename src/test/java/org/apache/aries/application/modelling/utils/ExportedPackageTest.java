package org.apache.aries.application.modelling.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.impl.ExportedPackageImpl;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;
import org.osgi.framework.Constants;

public class ExportedPackageTest {

	@Test
	public void testEqualsForIdenticalPackages() {
		String packageName = "package.name";
		String version = "1.0.0";
		ExportedPackage package1 = instantiatePackage(packageName, version);
		// I should hope so!
		assertEquals(package1, package1);
	}


	@Test
	public void testEqualsForEqualTrivialPackages() {
		String packageName = "package.name";
		String version = "1.0.0";
		ExportedPackage package1 = instantiatePackage(packageName, version);
		ExportedPackage package2 = instantiatePackage(packageName, version);
		assertEquals(package1, package2);
	}

	@Test
	public void testEqualsForTrivialPackagesWithDifferentName() {
		String version = "1.0.0";
		ExportedPackage package1 = instantiatePackage("package.name", version);
		ExportedPackage package2 = instantiatePackage("other.package.name", version);
		assertFalse("Unexpectedly reported as equal" + package1 + "==" + package2, package1.equals(package2));
	}
	
	@Test
	public void testEqualsForTrivialPackagesWithDifferentVersion() {
		String packageName = "package.name";
		ExportedPackage package1 = instantiatePackage(packageName, "1.0.0");
		ExportedPackage package2 = instantiatePackage(packageName, "1.0.1");
		assertFalse("Unexpectedly reported as equal" + package1 + "==" + package2, package1.equals(package2));
	}
	
	@Test
	public void testEqualsForEqualPackagesWithDifferentAttributes() {
		String packageName = "package.name";
		String version = "1.0.0";
		ExportedPackage package1 = instantiatePackage(packageName, version, "att=something");
		ExportedPackage package2 = instantiatePackage(packageName, version, "att=something.else");
		assertFalse("Unexpectedly reported as equal" + package1 + "==" + package2, package1.equals(package2));
	}


	private ExportedPackage instantiatePackage(String packageName,
			String version, String ... attributes) {
		ModelledResource mr = Skeleton.newMock(ModelledResource.class);
		Map<String, Object> generatedAttributes = new HashMap<String, Object>();
		generatedAttributes.put(Constants.VERSION_ATTRIBUTE, version);
		for (String att : attributes)
		{
			String[] bits = att.split("=");
			generatedAttributes.put(bits[0], bits[1]);
		}
		return new ExportedPackageImpl(mr, packageName, generatedAttributes);
	}


}
