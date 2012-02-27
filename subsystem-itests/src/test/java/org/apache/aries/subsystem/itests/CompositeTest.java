package org.apache.aries.subsystem.itests;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.JarFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ManifestFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@RunWith(JUnit4TestRunner.class)
public class CompositeTest extends SubsystemTest {
	private static final String BUNDLE_A = "org.apache.aries.subsystem.itests.bundle.a";
	private static final String BUNDLE_B = "org.apache.aries.subsystem.itests.bundle.b";
	private static final String BUNDLE_C = "org.apache.aries.subsystem.itests.bundle.c";
	private static final String BUNDLE_D = "org.apache.aries.subsystem.itests.bundle.d";
	private static final String BUNDLE_E = "org.apache.aries.subsystem.itests.bundle.e";
	private static final String COMPOSITE_B = "org.apache.aries.subsystem.itests.subsystem.composite.b";
	private static final String COMPOSITE_C = "org.apache.aries.subsystem.itests.subsystem.composite.c";
	private static final String COMPOSITE_D = "org.apache.aries.subsystem.itests.subsystem.composite.d";
	
	private static boolean createdTestFiles;
	
	@Before
	public static void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createBundleB();
		createBundleC();
		createBundleD();
		createBundleE();
		createCompositeB();
		createCompositeC();
		createCompositeD();
		createdTestFiles = true;
	}
	
	private static void createBundle(String symbolicName) throws IOException {
		createBundle(symbolicName, null);
	}
	
	private static void createBundle(String symbolicName, Map<String, String> headers) throws IOException {
		createBundle(symbolicName, null, headers);
	}
	
	private static void createBundle(String symbolicName, String version, Map<String, String> headers) throws IOException {
		if (headers == null)
			headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		if (version != null)
			headers.put(Constants.BUNDLE_VERSION, version);
		createBundle(headers);
	}
	
	private static void createBundle(Map<String, String> headers) throws IOException {
		String symbolicName = headers.get(Constants.BUNDLE_SYMBOLICNAME);
		JarFixture bundle = ArchiveFixture.newJar();
		ManifestFixture manifest = bundle.manifest();
		for (Entry<String, String> header : headers.entrySet()) {
			manifest.attribute(header.getKey(), header.getValue());
		}
		write(symbolicName, bundle);
	}
	
	private static void createBundleA() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.EXPORT_PACKAGE, BUNDLE_A + ".x;version=\"1.0\"");
		createBundle(BUNDLE_A, "1.0.0", headers);
	}
	
	private static void createBundleB() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.PROVIDE_CAPABILITY, "y; y=test; version:Version=1.0");
		createBundle(BUNDLE_B, "1.0.0", headers);
	}
	
	private static void createBundleC() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.IMPORT_PACKAGE, BUNDLE_A + ".x;version=\"[1.0,2.0)\"");
		createBundle(BUNDLE_C, headers);
	}
	
	private static void createBundleD() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		createBundle(BUNDLE_D, headers);
	}
	
	private static void createBundleE() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.REQUIRE_CAPABILITY, "y; filter:=(y=test)");
		createBundle(BUNDLE_E, headers);
	}
	
	private static void createCompositeB() throws IOException {
		createCompositeBManifest();
		createSubsystem(COMPOSITE_B, "COMPOSITE.B.MF");
	}
	
	private static void createCompositeC() throws IOException {
		createCompositeCManifest();
		createSubsystem(COMPOSITE_C, "COMPOSITE.C.MF");
	}
	
	private static void createCompositeD() throws IOException {
		createCompositeDManifest();
		createSubsystem(COMPOSITE_D, "COMPOSITE.D.MF");
	}
	
	private static void createCompositeBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A + "; bundle-version=\"[1.0, 2.0)\", does.not.exist; bundle-version=\"[1.0, 2.0)\"");
		createManifest("COMPOSITE.B.MF", attributes);
	}
	
	private static void createCompositeCManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_C);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, BUNDLE_A + ".x, does.not.exist; a=b");
		createManifest("COMPOSITE.C.MF", attributes);
	}
	
	private static void createCompositeDManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_D);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y; filter:=\"(y=test)\", does.not.exist; filter:=\"(a=b)\"");
		createManifest("COMPOSITE.D.MF", attributes);
	}
	
	private static void createManifest(String name, Map<String, String> headers) throws IOException {
		ManifestFixture manifest = ArchiveFixture.newJar().manifest();
		for (Entry<String, String> header : headers.entrySet()) {
			manifest.attribute(header.getKey(), header.getValue());
		}
		write(name, manifest);
	}
	
	private static void createSubsystem(String name, String manifest) throws IOException {
		write(name, ArchiveFixture.newZip().binary("OSGI-INF/SUBSYSTEM.MF", new FileInputStream(manifest)));
	}
	
	@Test
	public void testImportPackage() throws Exception {
		Bundle bundleA = installBundleFromFile(BUNDLE_A);
		try {
			Subsystem compositeC = installSubsystemFromFile(COMPOSITE_C);
			try {
				Bundle bundleC = installBundleFromFile(BUNDLE_C, compositeC);
				try {
					startBundle(bundleC, compositeC);
				}
				finally {
					bundleC.uninstall();
				}
			}
			finally {
				uninstallScopedSubsystem(compositeC);
			}
		}
		finally {
			bundleA.uninstall();
		}
	}
	
	@Test
	public void testRequireBundle() throws Exception {
		Bundle bundleA = installBundleFromFile(BUNDLE_A);
		try {
			Subsystem compositeB = installSubsystemFromFile(COMPOSITE_B);
			try {
				Bundle bundleD = installBundleFromFile(BUNDLE_D, compositeB);
				try {
					startBundle(bundleD, compositeB);
				}
				finally {
					bundleD.uninstall();
				}
			}
			finally {
				uninstallScopedSubsystem(compositeB);
			}
		}
		finally {
			bundleA.uninstall();
		}
	}
	
	@Test
	public void testRequireCapability() throws Exception {
		Bundle bundleB = installBundleFromFile(BUNDLE_B);
		try {
			Subsystem compositeD = installSubsystemFromFile(COMPOSITE_D);
			try {
				Bundle bundleE = installBundleFromFile(BUNDLE_E, compositeD);
				try {
					startBundle(bundleE, compositeD);
				}
				finally {
					bundleE.uninstall();
				}
			}
			finally {
				uninstallScopedSubsystem(compositeD);
			}
		}
		finally {
			bundleB.uninstall();
		}
	}
}
