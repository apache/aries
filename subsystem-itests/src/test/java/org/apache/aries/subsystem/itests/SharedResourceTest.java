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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class SharedResourceTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Subsystem-SymbolicName: application.b.esa
	 * Subsystem-Content: bundle.b.jar
	 */
	private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Import-Package: x
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Import-Package: x
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	/*
	 * Bundle-SymbolicName: bundle.c.jar
	 * Export-Package: x
	 */
	private static final String BUNDLE_C = "bundle.c.jar";

	
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
	
	private static void createApplicationB() throws IOException {
		createApplicationBManifest();
		createSubsystem(APPLICATION_B, BUNDLE_B);
	}
	
	private static void createApplicationBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_B);
		createManifest(APPLICATION_B + ".mf", attributes);
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), importPackage("x"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), importPackage("x"));
	}
	
	private void createBundleC() throws IOException {
		createBundle(name(BUNDLE_C), exportPackage("x"));
	}
	
	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createBundleB();
		createBundleC();
		createApplicationA();
		createApplicationB();
	}
	
	public void setUp() throws Exception {
		super.setUp();
		registerRepositoryService(BUNDLE_C);
	}
	
	@Test
	public void testSharedBundleNotUninstalledWhileStillReferenced() throws Exception {
		Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
		try {
			startSubsystem(applicationA);
			Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
			try {
				startSubsystem(applicationB);
				stopSubsystem(applicationA);
				uninstallSubsystem(applicationA);
				assertBundleState(Bundle.ACTIVE, BUNDLE_C, getRootSubsystem());
			}
			finally {
				stopAndUninstallSubsystemSilently(applicationB);
			}
		}
		finally {
			stopAndUninstallSubsystemSilently(applicationA);
		}
	}
}
