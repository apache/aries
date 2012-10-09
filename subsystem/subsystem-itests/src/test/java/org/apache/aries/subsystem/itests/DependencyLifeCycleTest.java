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
package org.apache.aries.subsystem.itests;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class DependencyLifeCycleTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Import-Package: x
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Export-Package: x
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
		headers.put(Constants.IMPORT_PACKAGE, "x");
		createBundle(BUNDLE_A, headers);
	}
	
	private static void createBundleB() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.EXPORT_PACKAGE, "x");
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
		registerRepositoryService(BUNDLE_A, BUNDLE_B);
	}
	
	@Test
	public void testBundleDependencyInstall() throws Exception {
		Subsystem subsystem = installSubsystemFromFile(APPLICATION_A);
		try {
			assertBundleState(Bundle.INSTALLED, BUNDLE_B, getRootSubsystem());
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testBundleDependencyStart() throws Exception {
		Subsystem subsystem = installSubsystemFromFile(APPLICATION_A);
		try {
			subsystem.start();
			try {
				assertBundleState(Bundle.ACTIVE, BUNDLE_B, getRootSubsystem());
			}
			finally {
				stopSubsystemSilently(subsystem);
			}
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testBundleDependencyStop() throws Exception {
		Subsystem subsystem = installSubsystemFromFile(APPLICATION_A);
		try {
			subsystem.start();
			subsystem.stop();
			assertBundleState(Bundle.RESOLVED, BUNDLE_B, getRootSubsystem());
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testBundleDependencyUninstall() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystemFromFile(APPLICATION_A);
		try {
			assertConstituent(root, BUNDLE_B);
			Bundle bundle = getBundle(root, BUNDLE_B);
			subsystem.uninstall();
			assertBundleState(bundle, Bundle.UNINSTALLED);
			assertNotConstituent(root, BUNDLE_B);
		}
		finally {
			if (!EnumSet.of(Subsystem.State.UNINSTALLING, Subsystem.State.UNINSTALLED).contains(subsystem.getState()))
				uninstallSubsystemSilently(subsystem);	
		}
	}
}
