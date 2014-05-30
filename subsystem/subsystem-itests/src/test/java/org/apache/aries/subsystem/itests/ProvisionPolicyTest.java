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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class ProvisionPolicyTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Type: osgi.subsystem.application;provision-policy:=acceptDependencies
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
	/*
	 * Subsystem-SymbolicName: composite.a.esa
	 * Subsystem-Type: osgi.subsystem.composite
	 * Import-Package: x
	 */
	private static final String COMPOSITE_A = "composite.a.esa";
	/*
	 * Subsystem-SymbolicName: feature.a.esa
	 * Subsystem-Type: osgi.subsystem.feature;provision-policy:=acceptDependencies
	 */
	private static final String FEATURE_A = "feature.a.esa";
	/*
	 * Subsystem-SymbolicName: feature.b.esa
	 * Subsystem-Type: osgi.subsystem.feature
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String FEATURE_B = "feature.b.esa";
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes
		.put(SubsystemConstants.SUBSYSTEM_TYPE,
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION
						+ ';'
						+ SubsystemConstants.PROVISION_POLICY_DIRECTIVE
						+ ":="
						+ SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), importPackage("x"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), exportPackage("x"));
	}
	
	private void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A);
	}
	
	private void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	private void createFeatureA() throws IOException {
		createFeatureAManifest();
		createSubsystem(FEATURE_A);
	}
	
	private void createFeatureAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_A);
		attributes
				.put(SubsystemConstants.SUBSYSTEM_TYPE,
						SubsystemConstants.SUBSYSTEM_TYPE_FEATURE
								+ ';'
								+ SubsystemConstants.PROVISION_POLICY_DIRECTIVE
								+ ":="
								+ SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES);
		createManifest(FEATURE_A + ".mf", attributes);
	}
	
	private static void createFeatureB() throws IOException {
		createFeatureBManifest();
		createSubsystem(FEATURE_B, BUNDLE_A);
	}
	
	private static void createFeatureBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		createManifest(FEATURE_B + ".mf", attributes);
	}
	
	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createBundleB();
		createApplicationA();
		createCompositeA();
		createFeatureA();
		createFeatureB();
	}
	
	public void setUp() throws Exception {
		super.setUp();
		Subsystem root = getRootSubsystem();
		assertProvisionPolicy(root, true);
		registerRepositoryService(BUNDLE_B);
	}
	
	@Test
	public void testFailInstallFeatureAcceptDependencies() throws Exception {
		Subsystem subsystem = null;
		try {
			subsystem = installSubsystemFromFile(FEATURE_A);
			fail("Feature with provision-policy:=acceptDependencies did not fail installation");
		}
		catch (SubsystemException e) {
			// TODO Brittle...
			assertTrue(e.getMessage().contains("Feature subsystems may not declare a provision-policy of acceptDependencies"));
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testProvisionToNonRootAncestor() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem application = installSubsystemFromFile(root, APPLICATION_A);
		try {
			assertProvisionPolicy(application, true);
			Subsystem composite = installSubsystemFromFile(application, COMPOSITE_A);
			try {
				assertProvisionPolicy(composite, false);
				Subsystem feature = installSubsystemFromFile(composite, FEATURE_B);
				try {
					assertProvisionPolicy(feature, false);
					assertConstituent(feature, BUNDLE_A);
					assertNotConstituent(feature, BUNDLE_B);
					assertNotConstituent(composite, BUNDLE_A);
					assertNotConstituent(composite, BUNDLE_B);
					assertConstituent(application, BUNDLE_A);
					assertConstituent(application, BUNDLE_B);
					assertNotConstituent(root, BUNDLE_A);
					assertNotConstituent(root, BUNDLE_B);
				}
				finally {
					uninstallSubsystemSilently(feature);
				}
			}
			finally {
				uninstallSubsystemSilently(composite);
			}
		}
		finally {
			uninstallSubsystemSilently(application);
		}
	}
	
	@Test
	public void testProvisionToRoot() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem composite = installSubsystemFromFile(root, COMPOSITE_A);
		try {
			assertProvisionPolicy(composite, false);
			Subsystem feature = installSubsystemFromFile(composite, FEATURE_B);
			try {
				assertProvisionPolicy(feature, false);
				assertConstituent(feature, BUNDLE_A);
				assertNotConstituent(feature, BUNDLE_B);
				assertNotConstituent(composite, BUNDLE_A);
				assertNotConstituent(composite, BUNDLE_B);
				assertNotConstituent(root, BUNDLE_A);
				assertConstituent(root, BUNDLE_B);
			}
			finally {
				uninstallSubsystemSilently(feature);
			}
		}
		finally {
			uninstallSubsystemSilently(composite);
		}
	}
	
	@Test
	public void testProvisionToSelf() throws Exception {
		Subsystem root = getRootSubsystem();
		assertProvisionPolicy(root, true);
		registerRepositoryService(BUNDLE_B);
		Subsystem subsystem = installSubsystemFromFile(root, APPLICATION_A);
		try {
			assertProvisionPolicy(subsystem, true);
			assertConstituent(subsystem, BUNDLE_A);
			assertConstituent(subsystem, BUNDLE_B);
			assertNotConstituent(root, BUNDLE_A);
			assertNotConstituent(root, BUNDLE_B);
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
}
