/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.itests.defect;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.itests.Header;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1416
 * 
 * BundleException "bundle is already installed" when the Preferred-Provider 
 * subsystem header points to a bundle.
 */
public class Aries1416Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * Subsystem-Content: bundle.a.jar
     * Preferred-Provider: bundle.b.jar;type=osgi.bundle
     */
    private static final String APPLICATION_A = "application.a.esa";
    /*
     * Subsystem-SymbolicName: feature.a.esa
     * Subsystem-Content: application.a.esa
     */
    private static final String FEATURE_A = "feature.a.esa";
    /*
     * Subsystem-SymbolicName: feature.b.esa
     * Subsystem-Content: application.a.esa
     */
    private static final String FEATURE_B = "feature.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: b
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Provide-Capability: b
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createBundleB();
		createApplicationA();
		createFeatureA();
		createFeatureB();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), new Header(Constants.REQUIRE_CAPABILITY, "b"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), new Header(Constants.PROVIDE_CAPABILITY, "b"));
	}
    
    private static void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A, BUNDLE_A, BUNDLE_B);
    }
    
    private static void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
        attributes.put(SubsystemConstants.PREFERRED_PROVIDER, BUNDLE_B + ";type=osgi.bundle");
        createManifest(APPLICATION_A + ".mf", attributes);
    }
    
    private static void createFeatureA() throws IOException {
        createFeatureAManifest();
        createSubsystem(FEATURE_A, BUNDLE_B, APPLICATION_A);
    }
    
    private static void createFeatureAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_B + ',' + 
        		APPLICATION_A + ";type=osgi.subsystem.application");
        attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
        createManifest(FEATURE_A + ".mf", attributes);
    }
    
    private static void createFeatureB() throws IOException {
        createFeatureBManifest();
        createSubsystem(FEATURE_B, BUNDLE_B, APPLICATION_A);
    }
    
    private static void createFeatureBManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_B);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_B + ',' + 
        		APPLICATION_A + ";type=osgi.subsystem.application");
        attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
        createManifest(FEATURE_B + ".mf", attributes);
    }
    
    @Test 
    public void testSystemRepositoryBundlePreferredProvider() throws Exception {
    	Subsystem root = getRootSubsystem();
    	// Install bundle B providing capability b into the root subsystem's
    	// region.
    	Bundle bundleB = installBundleFromFile(BUNDLE_B, root);
    	try {
    		// Install application A containing content bundle A requiring 
    		// capability b and dependency bundle B providing capability b.
    		// Bundle B is not content but will become part of the local 
    		// repository. The preferred provider is bundle B. Bundle B from the
    		// system repository should be used. Bundle B from the local
    		// repository should not be provisioned.
    		Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    		uninstallSubsystemSilently(applicationA);
    	}
    	catch (SubsystemException e) {
    		e.printStackTrace();
    		fail("Subsystem should have installed");
    	}
    	finally {
    		uninstallSilently(bundleB);
    	}
    }
    
    @Test
    public void testSharedContentBundlePreferredProvider() throws Exception {
    	// Install feature A containing bundle B and application A both in the
    	// archive and as content into the root subsystem region. Bundle B 
    	// provides capability b. Application A contains bundle A requiring 
    	// capability b both in the archive and as content. Preferred provider 
    	// bundle B is also included in the archive but not as content.
    	Subsystem featureA = installSubsystemFromFile(FEATURE_A);
    	try {
    		// Install feature B having the same characteristics as feature A
    		// described above into the root subsystem region. Bundle B will 
    		// become shared content of features A and B. Shared content bundle
    		// B from the system repository should be used as the preferred
    		// provider. Bundle B from the local repository should not be
    		// provisioned.
    		Subsystem featureB = installSubsystemFromFile(FEATURE_B);
    		uninstallSubsystemSilently(featureB);
    	}
    	catch (SubsystemException e) {
    		e.printStackTrace();
    		fail("Subsystem should have installed");
    	}
    	finally {
    		uninstallSubsystemSilently(featureA);
    	}
    }
}
