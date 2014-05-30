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
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class AutostartTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Subsystem-SymbolicName: application.b.esa
	 * Subsystem-Content: bundle.a.jar,application.a.esa;type=osgi.subsystem.application
	 */
	private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Export-Package: x
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Import-Package: x
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	/*
	 * Subsystem-SymbolicName: composite.a.esa
	 * Subsystem-Type: osgi.subsystem.composite
	 * Subsystem-Content: bundle.a.jar;version="[0,0]"
	 * Export-Package: x
	 */
	private static final String COMPOSITE_A = "composite.a.esa";
	/*
	 * Subsystem-SymbolicName: composite.b.esa
	 * Subsystem-Type: osgi.subsystem.composite
	 * Subsystem-Content: bundle.a.jar;version="[0,0]"
	 * Import-Package: x
	 * Preferred-Provider: composite.a.esa
	 */
	private static final String COMPOSITE_B = "composite.b.esa";
	/*
	 * Subsystem-SymbolicName: feature.a.esa
	 * Subsystem-Type: osgi.subsystem.feature
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String FEATURE_A = "feature.a.esa";
	/*
	 * Subsystem-SymbolicName: feature.b.esa
	 * Subsystem-Type: osgi.subsystem.feature
	 * Subsystem-Content: bundle.a.jar,feature.a.esa;type=osgi.subsystem.feature
	 */
	private static final String FEATURE_B = "feature.b.esa";
	/*
	 * Subsystem-SymbolicName: feature.c.esa
	 * Subsystem-Type: osgi.subsystem.feature
	 * Subsystem-Content: bundle.a.jar,feature.a.esa;type=osgi.subsystem.feature
	 */
	private static final String FEATURE_C = "feature.c.esa";
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createApplicationB() throws IOException {
		createApplicationBManifest();
		createSubsystem(APPLICATION_B, BUNDLE_A, APPLICATION_A);
	}
	
	private void createApplicationBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ',' + APPLICATION_A + ';' + IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + '=' + SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		createManifest(APPLICATION_B + ".mf", attributes);
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), exportPackage("x"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), importPackage("x"));
	}
	
	private void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A, BUNDLE_A);
	}
	
	private void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ';' + IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE + "=\"[0,0]\"");
		attributes.put(Constants.EXPORT_PACKAGE, "x");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	private static void createCompositeB() throws IOException {
		createCompositeBManifest();
		createSubsystem(COMPOSITE_B, BUNDLE_B);
	}
	
	private static void createCompositeBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_B + ';' + IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE + "=\"[0,0]\"");
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(SubsystemConstants.PREFERRED_PROVIDER, COMPOSITE_A);
		createManifest(COMPOSITE_B + ".mf", attributes);
	}
	
	private static void createFeatureA() throws IOException {
		createFeatureAManifest();
		createSubsystem(FEATURE_A, BUNDLE_A);
	}
	
	private static void createFeatureAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
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
	
	private static void createFeatureC() throws IOException {
		createFeatureCManifest();
		createSubsystem(FEATURE_C, BUNDLE_A, FEATURE_A);
	}
	
	private static void createFeatureCManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_C);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ',' + FEATURE_A + ';' + IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + '=' + SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		createManifest(FEATURE_C + ".mf", attributes);
	}

	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createBundleB();
		createApplicationA();
		createApplicationB();
		createCompositeA();
		createCompositeB();
		createFeatureA();
		createFeatureB();
		createFeatureC();
	}
	
	@Test
	public void testAutostartScoped() throws Exception {
		Subsystem subsystem = null;
		try {
			subsystem = installSubsystemFromFile(APPLICATION_A);
			restartSubsystemsImplBundle();
			subsystem = findSubsystemService(subsystem.getSubsystemId());
			assertState(Subsystem.State.INSTALLED, subsystem);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, subsystem);
			startSubsystem(subsystem);
			restartSubsystemsImplBundle();
			subsystem = findSubsystemService(subsystem.getSubsystemId());
			assertState(Subsystem.State.ACTIVE, subsystem);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, subsystem);
			stopSubsystem(subsystem);
			restartSubsystemsImplBundle();
			subsystem = findSubsystemService(subsystem.getSubsystemId());
			assertState(Subsystem.State.RESOLVED, subsystem);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, subsystem);
		}
		finally {
			stopAndUninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testAutostartUnscoped() throws Exception {
		Subsystem subsystem = null;
		try {
			subsystem = installSubsystemFromFile(FEATURE_A);
			restartSubsystemsImplBundle();
			subsystem = findSubsystemService(subsystem.getSubsystemId());
			assertState(Subsystem.State.INSTALLED, subsystem);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, subsystem);
			startSubsystem(subsystem);
			restartSubsystemsImplBundle();
			subsystem = findSubsystemService(subsystem.getSubsystemId());
			assertState(Subsystem.State.ACTIVE, subsystem);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, subsystem);
			stopSubsystem(subsystem);
			restartSubsystemsImplBundle();
			subsystem = findSubsystemService(subsystem.getSubsystemId());
			assertState(Subsystem.State.RESOLVED, subsystem);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, subsystem);
		}
		finally {
			stopAndUninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testAutostartChildScoped() throws Exception {
		Subsystem compositeA = null;
		try {
			compositeA = installSubsystemFromFile(COMPOSITE_A);
			Subsystem applicationA = installSubsystemFromFile(compositeA, APPLICATION_A);
			
			restartSubsystemsImplBundle();
			compositeA = findSubsystemService(compositeA.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());
			assertState(Subsystem.State.INSTALLED, compositeA);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, compositeA);
			assertState(Subsystem.State.INSTALLED, applicationA);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, applicationA);
			startSubsystem(compositeA);
			
			restartSubsystemsImplBundle();
			compositeA = findSubsystemService(compositeA.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, compositeA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, compositeA);
			assertState(Subsystem.State.RESOLVED, applicationA);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, applicationA);
			startSubsystemFromResolved(applicationA);
			
			restartSubsystemsImplBundle();
			compositeA = findSubsystemService(compositeA.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, compositeA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, compositeA);
			assertState(Subsystem.State.ACTIVE, applicationA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, applicationA);
			stopSubsystem(applicationA);
			
			restartSubsystemsImplBundle();
			compositeA = findSubsystemService(compositeA.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, compositeA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, compositeA);
			assertState(Subsystem.State.RESOLVED, applicationA);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, applicationA);
			startSubsystemFromResolved(applicationA);
			stopSubsystem(compositeA);
			
			restartSubsystemsImplBundle();
			compositeA = findSubsystemService(compositeA.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());;
			assertState(Subsystem.State.RESOLVED, compositeA);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, compositeA);
			assertState(Subsystem.State.RESOLVED, applicationA);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, compositeA);
		}
		finally {
			stopAndUninstallSubsystemSilently(compositeA);
		}
	}
	
	@Test
	public void testAutostartChildUnscoped() throws Exception {
		Subsystem featureA = null;
		try {
			featureA = installSubsystemFromFile(FEATURE_A);
			Subsystem featureB = installSubsystemFromFile(featureA, FEATURE_B);
			
			restartSubsystemsImplBundle();
			featureA = findSubsystemService(featureA.getSubsystemId());
			featureB = findSubsystemService(featureB.getSubsystemId());
			assertState(Subsystem.State.INSTALLED, featureA);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, featureA);
			assertState(Subsystem.State.INSTALLED, featureB);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, featureB);
			startSubsystem(featureA);
			
			restartSubsystemsImplBundle();
			featureA = findSubsystemService(featureA.getSubsystemId());
			featureB = findSubsystemService(featureB.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, featureA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureA);
			assertState(Subsystem.State.RESOLVED, featureB);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureB);
			startSubsystemFromResolved(featureB);
			
			restartSubsystemsImplBundle();
			featureA = findSubsystemService(featureA.getSubsystemId());
			featureB = findSubsystemService(featureB.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, featureA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureA);
			assertState(Subsystem.State.ACTIVE, featureB);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureB);
			stopSubsystem(featureB);
			
			restartSubsystemsImplBundle();
			featureA = findSubsystemService(featureA.getSubsystemId());
			featureB = findSubsystemService(featureB.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, featureA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureA);
			assertState(Subsystem.State.RESOLVED, featureB);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureB);
			startSubsystemFromResolved(featureB);
			stopSubsystem(featureA);
			
			restartSubsystemsImplBundle();
			featureA = findSubsystemService(featureA.getSubsystemId());
			featureB = findSubsystemService(featureB.getSubsystemId());;
			assertState(Subsystem.State.RESOLVED, featureA);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, featureA);
			assertState(Subsystem.State.RESOLVED, featureB);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, featureA);
		}
		finally {
			stopAndUninstallSubsystemSilently(featureA);
		}
	}
	
	@Test
	public void testAutostartChildAsContentScoped() throws Exception {
		Subsystem applicationB = null;
		try {
			applicationB = installSubsystemFromFile(APPLICATION_B);
			Subsystem applicationA = applicationB.getChildren().iterator().next();
			
			restartSubsystemsImplBundle();
			applicationB = findSubsystemService(applicationB.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());
			assertState(Subsystem.State.INSTALLED, applicationB);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, applicationB);
			assertState(Subsystem.State.INSTALLED, applicationA);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, applicationA);
			startSubsystem(applicationB);
			
			restartSubsystemsImplBundle();
			applicationB = findSubsystemService(applicationB.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, applicationB);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, applicationB);
			assertState(Subsystem.State.ACTIVE, applicationA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, applicationA);
			stopSubsystem(applicationA);
			
			restartSubsystemsImplBundle();
			applicationB = findSubsystemService(applicationB.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, applicationB);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, applicationB);
			assertState(Subsystem.State.ACTIVE, applicationA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, applicationA);
			stopSubsystem(applicationB);
			
			restartSubsystemsImplBundle();
			applicationB = findSubsystemService(applicationB.getSubsystemId());
			applicationA = findSubsystemService(applicationA.getSubsystemId());;
			assertState(Subsystem.State.RESOLVED, applicationB);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, applicationB);
			assertState(Subsystem.State.RESOLVED, applicationA);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, applicationA);
		}
		finally {
			stopAndUninstallSubsystemSilently(applicationB);
		}
	}
	
	@Test
	public void testAutostartChildAsContentUnscoped() throws Exception {
		Subsystem featureC = null;
		try {
			featureC = installSubsystemFromFile(FEATURE_C);
			Subsystem featureA = featureC.getChildren().iterator().next();
			
			restartSubsystemsImplBundle();
			featureC = findSubsystemService(featureC.getSubsystemId());
			featureA = findSubsystemService(featureA.getSubsystemId());
			assertState(Subsystem.State.INSTALLED, featureC);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, featureC);
			assertState(Subsystem.State.INSTALLED, featureA);
			assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, featureA);
			
			startSubsystem(featureC);
			restartSubsystemsImplBundle();
			featureC = findSubsystemService(featureC.getSubsystemId());
			featureA = findSubsystemService(featureA.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, featureC);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureC);
			assertState(Subsystem.State.ACTIVE, featureA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureA);
			
			stopSubsystem(featureA);
			restartSubsystemsImplBundle();
			featureC = findSubsystemService(featureC.getSubsystemId());
			featureA = findSubsystemService(featureA.getSubsystemId());;
			assertState(Subsystem.State.ACTIVE, featureC);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureC);
			assertState(Subsystem.State.ACTIVE, featureA);
			assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureA);
			
			stopSubsystem(featureC);
			restartSubsystemsImplBundle();
			featureC = findSubsystemService(featureC.getSubsystemId());
			featureA = findSubsystemService(featureA.getSubsystemId());;
			assertState(Subsystem.State.RESOLVED, featureC);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, featureC);
			assertState(Subsystem.State.RESOLVED, featureA);
			assertBundleState(Bundle.RESOLVED, BUNDLE_A, featureA);
		}
		finally {
			stopAndUninstallSubsystemSilently(featureC);
		}
	}
	
	@Test
	public void testAutostartDependency() throws Exception {
		Subsystem compositeA = installSubsystemFromFile(COMPOSITE_A);
		try {
			Subsystem compositeB = installSubsystemFromFile(COMPOSITE_B);
			try {
				restartSubsystemsImplBundle();
				compositeB = findSubsystemService(compositeB.getSubsystemId());
				compositeA = findSubsystemService(compositeA.getSubsystemId());
				assertState(Subsystem.State.INSTALLED, compositeB);
				assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_B, compositeB);
				assertState(Subsystem.State.INSTALLED, compositeA);
				assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_A, compositeA);
				
				startSubsystem(compositeA);
				restartSubsystemsImplBundle();
				compositeB = findSubsystemService(compositeB.getSubsystemId());
				compositeA = findSubsystemService(compositeA.getSubsystemId());
				assertState(Subsystem.State.INSTALLED, compositeB);
				assertBundleState(Bundle.INSTALLED | Bundle.RESOLVED, BUNDLE_B, compositeB);
				assertState(Subsystem.State.ACTIVE, compositeA);
				assertBundleState(Bundle.ACTIVE, BUNDLE_A, compositeA);
				
				stopSubsystem(compositeA);
				startSubsystem(compositeB);
				restartSubsystemsImplBundle();
				compositeB = findSubsystemService(compositeB.getSubsystemId());
				compositeA = findSubsystemService(compositeA.getSubsystemId());
				assertState(Subsystem.State.ACTIVE, compositeB);
				assertBundleState(Bundle.ACTIVE, BUNDLE_B, compositeB);
				assertState(Subsystem.State.ACTIVE, compositeA);
				assertBundleState(Bundle.ACTIVE, BUNDLE_A, compositeA);
				
				stopSubsystem(compositeB);
				restartSubsystemsImplBundle();
				compositeB = findSubsystemService(compositeB.getSubsystemId());
				compositeA = findSubsystemService(compositeA.getSubsystemId());
				assertState(Subsystem.State.RESOLVED, compositeB);
				assertBundleState(Bundle.RESOLVED, BUNDLE_B, compositeB);
				assertState(Subsystem.State.RESOLVED, compositeA);
				assertBundleState(Bundle.RESOLVED, BUNDLE_A, compositeA);
				
				uninstallSubsystem(compositeB);
				restartSubsystemsImplBundle();
				compositeA = findSubsystemService(compositeA.getSubsystemId());
				assertState(Subsystem.State.RESOLVED, compositeA);
				assertBundleState(Bundle.RESOLVED, BUNDLE_A, compositeA);
				
				startSubsystemFromResolved(compositeA);
				restartSubsystemsImplBundle();
				compositeA = findSubsystemService(compositeA.getSubsystemId());
				assertState(Subsystem.State.ACTIVE, compositeA);
				assertBundleState(Bundle.ACTIVE, BUNDLE_A, compositeA);
			}
			finally {
				stopAndUninstallSubsystemSilently(compositeB);
			}
		}
		finally {
			stopAndUninstallSubsystemSilently(compositeA);
		}
	}
}
