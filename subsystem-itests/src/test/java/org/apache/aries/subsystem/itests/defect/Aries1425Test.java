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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1425
 * 
 * Support both osgi.bundle and osgi.fragment resource types when given a 
 * Subsystem-Content header clause with an unspecified type attribute.
 */
public class Aries1425Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * Subsystem-Content: bundle.a.jar, bundle.b.jar
     * 
	 * Included In Archive
	 * 		bundle.a.fragment.jar
	 * 		bundle.b.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
    /*
     * Subsystem-SymbolicName: application.b.esa
     * Subsystem-Content: bundle.a.jar
     * 
	 * Included In Archive
	 * 		bundle.a.fragment.jar
	 * 		bundle.a.bundle.jar
     */
    private static final String APPLICATION_B = "application.b.esa";
    /*
     * Subsystem-SymbolicName: application.c.esa
     * Subsystem-Content: bundle.a.jar
     */
    private static final String APPLICATION_C = "application.c.esa";
    /*
     * Subsystem-SymbolicName: application.d.esa
     * Subsystem-Content: bundle.a.jar, bundle.b.jar
     * 
     * Included In Archive
	 * 		bundle.a.fragment.jar
     */
    private static final String APPLICATION_D = "application.d.esa";
    
    private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Fragment-Host: bundle.b.jar
	 */
	private static final String BUNDLE_A_FRAGMENT = "bundle.a.fragment.jar";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A_BUNDLE = "bundle.a.bundle.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleABundle();
		createBundleAFragment();
		createBundleB();
		createApplicationA();
		createApplicationB();
		createApplicationC();
		createApplicationD();
		createdTestFiles = true;
	}
	
	private void createBundleABundle() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A);
		createBundle(BUNDLE_A_BUNDLE, Collections.<String>emptyList(), headers);
	}
	
	private void createBundleAFragment() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A);
		headers.put(Constants.FRAGMENT_HOST, BUNDLE_B);
		createBundle(BUNDLE_A_FRAGMENT, Collections.<String>emptyList(), headers);
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B));
	}
	
	private static void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A, BUNDLE_A_FRAGMENT, BUNDLE_B);
    }
    
    private static void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ',' + BUNDLE_B);
        createManifest(APPLICATION_A + ".mf", attributes);
    }
    
    private static void createApplicationB() throws IOException {
        createApplicationBManifest();
        createSubsystem(APPLICATION_B, BUNDLE_A_FRAGMENT, BUNDLE_A_BUNDLE);
    }
    
    private static void createApplicationBManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
        createManifest(APPLICATION_B + ".mf", attributes);
    }
    
    private static void createApplicationC() throws IOException {
        createApplicationCManifest();
        createSubsystem(APPLICATION_C);
    }
    
    private static void createApplicationCManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_C);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
        createManifest(APPLICATION_C + ".mf", attributes);
    }
    
    private static void createApplicationD() throws IOException {
        createApplicationDManifest();
        createSubsystem(APPLICATION_D, BUNDLE_A_FRAGMENT);
    }
    
    private static void createApplicationDManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_D);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
        createManifest(APPLICATION_D + ".mf", attributes);
    }
    
    @Test
    public void testFragmentSelected() throws Exception {
    	try {
    		Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    		try {
    			assertConstituent(applicationA, BUNDLE_A, null, IdentityNamespace.TYPE_FRAGMENT);
    		}
    		finally {
    			uninstallSubsystemSilently(applicationA);
    		}
    	}
    	catch (SubsystemException e) {
    		e.printStackTrace();
    		fail("Subsystem should have installed");
    	}
    }
    
    @Test
    public void testFragmentResolved() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		applicationA.start();
    		try {
    			Bundle bundleA = getConstituentAsBundle(applicationA, BUNDLE_A, null, IdentityNamespace.TYPE_FRAGMENT);
    			assertBundleState(bundleA, Bundle.RESOLVED);
    		}
    		finally {
    			stopSubsystemSilently(applicationA);
    		}
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testDeployedContentHeader() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		Map<String, String> headers = applicationA.getDeploymentHeaders();
    		String header = headers.get(SubsystemConstants.DEPLOYED_CONTENT);
			DeployedContentHeader dch = new DeployedContentHeader(header);
			boolean foundClause = false;
			for (DeployedContentHeader.Clause clause : dch.getClauses()) {
				if (BUNDLE_A.equals(clause.getSymbolicName())) {
					assertEquals("Wrong type", IdentityNamespace.TYPE_FRAGMENT, clause.getType());
					foundClause = true;
					break;
				}
			}
			assertTrue("Missing clause", foundClause);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testProvisionResourceHeader() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		Map<String, String> headers = applicationA.getDeploymentHeaders();
    		String header = headers.get(SubsystemConstants.PROVISION_RESOURCE);
			assertFalse("Fragment content treated as dependency", header != null && header.contains(BUNDLE_A));
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testBundleSelectedFromLocalRepository() throws Exception {
    	Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
    	try {
    		assertNotConstituent(applicationB, BUNDLE_A, null, IdentityNamespace.TYPE_FRAGMENT);
    		assertConstituent(applicationB, BUNDLE_A, null, IdentityNamespace.TYPE_BUNDLE);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationB);
    	}
    }
    
    @Test
    public void testBundleSelectedFromRemoteRepository() throws Exception {
    	// Make sure the repository containing the fragment comes first.
    	registerRepositoryService(BUNDLE_A_FRAGMENT);
    	registerRepositoryService(BUNDLE_A_BUNDLE);
    	
    	Subsystem applicationC = installSubsystemFromFile(APPLICATION_C);
    	try {
    		assertNotConstituent(applicationC, BUNDLE_A, null, IdentityNamespace.TYPE_FRAGMENT);
    		assertConstituent(applicationC, BUNDLE_A, null, IdentityNamespace.TYPE_BUNDLE);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationC);
    	}
    }
    
    @Test
    public void testFragmentFromLocalRepoSelectedBeforeBundleRemoteRepository() throws Exception {
    	registerRepositoryService(BUNDLE_A_BUNDLE, BUNDLE_B);
    	Subsystem applicationD = installSubsystemFromFile(APPLICATION_D);
    	try {
    		assertNotConstituent(applicationD, BUNDLE_A, null, IdentityNamespace.TYPE_BUNDLE);
    		assertConstituent(applicationD, BUNDLE_A, null, IdentityNamespace.TYPE_FRAGMENT);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationD);
    	}
    }
}
