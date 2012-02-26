package org.apache.aries.subsystem.itests;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.aries.unittest.fixture.ArchiveFixture;
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
	private static boolean createdTestFiles;
	
	@Before
	public static void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createBundleB();
		createBundleC();
		createBundleE();
		createCompositeC();
		createCompositeD();
		createdTestFiles = true;
	}
	
	private static void createBundleA() throws IOException {
		write("bundle.a.jar",
				ArchiveFixture
						.newJar()
						.manifest()
						.symbolicName(
								"org.apache.aries.subsystem.itests.bundle.a")
						.attribute(Constants.EXPORT_PACKAGE,
								"org.apache.aries.subsystem.itests.bundle.a.x;version=\"1.0\"")
						.end());
	}
	
	private static void createBundleB() throws IOException {
		write("bundle.b.jar",
				ArchiveFixture
						.newJar()
						.manifest()
						.symbolicName(
								"org.apache.aries.subsystem.itests.bundle.b")
						.version("1.0.0")
						.attribute(Constants.PROVIDE_CAPABILITY,
								"y; y=test; version:Version=1.0")
						.end());
	}
	
	private static void createBundleC() throws IOException {
		write("bundle.c.jar",
				ArchiveFixture
						.newJar()
						.manifest()
						.symbolicName(
								"org.apache.aries.subsystem.itests.bundle.c")
						.attribute(Constants.IMPORT_PACKAGE,
								"org.apache.aries.subsystem.itests.bundle.a.x;version=\"[1.0,2.0)\"")
						.end());
	}
	
	private static void createBundleE() throws IOException {
		write("bundle.e.jar",
				ArchiveFixture
						.newJar()
						.manifest()
						.symbolicName(
								"org.apache.aries.subsystem.itests.bundle.e")
						.version("1.0.0")
						.attribute(Constants.REQUIRE_CAPABILITY,
								"y; filter:=(y=test)")
						.end());
	}
	
	private static void createCompositeC() throws IOException {
		createCompositeCManifest();
		write("composite.c.esa",
				ArchiveFixture.newZip().binary("OSGI-INF/SUBSYSTEM.MF",
						new FileInputStream("COMPOSITE.C.MF")));
	}
	
	private static void createCompositeD() throws IOException {
		createCompositeDManifest();
		write("composite.d.esa",
				ArchiveFixture.newZip().binary("OSGI-INF/SUBSYSTEM.MF",
						new FileInputStream("COMPOSITE.D.MF")));
	}
	
	private static void createCompositeCManifest() throws IOException {
		write("COMPOSITE.C.MF",
				ArchiveFixture
						.newJar()
						.manifest()
						.attribute(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME,
								"org.apache.aries.subsystem.itests.subsystem.composite.c")
						.attribute(SubsystemConstants.SUBSYSTEM_TYPE,
								SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE)
						.attribute(Constants.IMPORT_PACKAGE,
								"org.apache.aries.subsystem.itests.bundle.a.x, does.not.exist; a=b"));
	}
	
	private static void createCompositeDManifest() throws IOException {
		write("COMPOSITE.D.MF",
				ArchiveFixture
						.newJar()
						.manifest()
						.attribute(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME,
								"org.apache.aries.subsystem.itests.subsystem.composite.d")
						.attribute(SubsystemConstants.SUBSYSTEM_TYPE,
								SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE)
						.attribute(Constants.REQUIRE_CAPABILITY,
								"y; filter:=\"(y=test)\", does.not.exist; filter:=\"(a=b)\""));
	}
	
	@Test
	public void testImportPackage() throws Exception {
		Bundle bundleA = installBundleFromFile("bundle.a.jar");
		try {
			Subsystem compositeC = installSubsystemFromFile("composite.c.esa");
			try {
				Bundle bundleC = installBundleFromFile("bundle.c.jar", compositeC);
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
	public void testRequireCapability() throws Exception {
		Bundle bundleB = installBundleFromFile("bundle.b.jar");
		try {
			Subsystem compositeD = installSubsystemFromFile("composite.d.esa");
			try {
				Bundle bundleE = installBundleFromFile("bundle.e.jar", compositeD);
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
