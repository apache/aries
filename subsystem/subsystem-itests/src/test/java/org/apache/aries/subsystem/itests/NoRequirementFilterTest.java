package org.apache.aries.subsystem.itests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@RunWith(JUnit4TestRunner.class)
public class NoRequirementFilterTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: y
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Provide-Capability: y;y=foo
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private static void createBundleA() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.REQUIRE_CAPABILITY, "y");
		createBundle(BUNDLE_A, headers);
	}
	
	private static void createBundleB() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.PROVIDE_CAPABILITY, "y;y=foo");
		createBundle(BUNDLE_B, headers);
	}
	
	private static boolean createdTestFiles;
	@Before
	public static void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createBundleB();
		createApplicationA();
		createdTestFiles = true;
	}
	
	public void setUp() throws Exception {
		super.setUp();
		registerRepositoryService(BUNDLE_A);
	}
	
	@Test
	public void testRequireCapability() throws Exception {
		Bundle bundleB = installBundleFromFile(BUNDLE_B);
		try {
			Subsystem subsystem = installSubsystemFromFile(APPLICATION_A);
			try {
				startSubsystem(subsystem);
				stopSubsystem(subsystem);
			}
			finally {
				uninstallSubsystemSilently(subsystem);
			}
		}
		finally {
			uninstallSilently(bundleB);
		}
	}
}
