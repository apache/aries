package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.AriesSubsystem;
import org.apache.aries.subsystem.core.internal.BasicRequirement;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

import aQute.lib.osgi.Constants;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class AriesSubsystemTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Import-Package: org.osgi.framework,org.osgi.resource
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Subsystem-SymbolicName: composite.a.esa
	 * Subsystem-Type: osgi.subsystem.composite
	 */
	private static final String COMPOSITE_A = "composite.a.esa";
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private static void createBundleA() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.osgi.resource");
		createBundle(BUNDLE_A, headers);
	}
	
	private static void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A);
	}
	
	private static void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, "org.osgi.resource");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	private static boolean createdTestFiles;
	@Before
	public static void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createApplicationA();
		createCompositeA();
		createdTestFiles = true;
	}
	
	public void setUp() throws Exception {
		super.setUp();
	}
	
	@Test
	public void testAddRequirements() throws Exception {
		Subsystem compositeA = installSubsystemFromFile(COMPOSITE_A);
		try {
			try {
				installSubsystemFromFile(compositeA, APPLICATION_A);
				fail("Subsystem should not have installed");
			} catch (SubsystemException e) {
				// Okay.
			}
			Requirement requirement = new BasicRequirement.Builder()
					.namespace(PackageNamespace.PACKAGE_NAMESPACE)
					.directive(
							PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, 
							"(osgi.wiring.package=org.osgi.framework)")
					.resource(EasyMock.createMock(Resource.class))
					.build();
			((AriesSubsystem)compositeA).addRequirements(Collections.singleton(requirement));
			try {
				installSubsystemFromFile(compositeA, APPLICATION_A);
			} catch (SubsystemException e) {
				fail("Subsystem should have installed");
			}
		} finally {
			uninstallSubsystemSilently(compositeA);
		}
	}
	
	@Test
	public void testInstallIDirectory() {
		File file = new File(COMPOSITE_A);
		IDirectory directory = FileSystem.getFSRoot(file);
		try {
			AriesSubsystem compositeA = getRootAriesSubsystem().install(COMPOSITE_A, directory);
			uninstallSubsystemSilently(compositeA);
		}
		catch (Exception e) {
			fail("Installation from IDirectory should have succeeded");
		}
	}
	
	@Test
	public void testServiceRegistrations() {
		Subsystem root1 = null;
		try {
			root1 = getRootSubsystem();
		}
		catch (Exception e) {
			fail(Subsystem.class.getName() + " service not registered");
		}
		AriesSubsystem root2 = null;
		try {
			root2 = getRootAriesSubsystem();
		}
		catch (Exception e) {
			fail(AriesSubsystem.class.getName() + " service not registered");
		}
		assertSame("Services should be the same instance", root1, root2);
	}
}
