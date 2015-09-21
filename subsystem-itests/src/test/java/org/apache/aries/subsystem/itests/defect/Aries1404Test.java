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
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1404
 * 
 * Restart of the osgi container does not restart subsystem core because of an 
 * error related to missing resource 
 * org.apache.aries.subsystem.resource.synthesized.
 */
public class Aries1404Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * Subsystem-Content: bundle.a.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
    /*
     * Subsystem-SymbolicName: application.b.esa
     * Subsystem-Content: bundle.b.jar
     * Application-ImportService: b
     */
    private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: a;resolution:=optional
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Require-Capability: osgi.service;filter:="(objectClass=b)"
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
		createApplicationB();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), new Header(Constants.REQUIRE_CAPABILITY, "a;resolution:=optional"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), new Header(Constants.REQUIRE_CAPABILITY, "osgi.service;filter:=\"(objectClass=b)\""));
	}
    
    private static void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A, BUNDLE_A);
    }
    
    private static void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        createManifest(APPLICATION_A + ".mf", attributes);
    }
    
    private static void createApplicationB() throws IOException {
        createApplicationBManifest();
        createSubsystem(APPLICATION_B, BUNDLE_B);
    }
    
    private static void createApplicationBManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
        attributes.put("Application-ImportService", "b");
        createManifest(APPLICATION_B + ".mf", attributes);
    }
    
    @Test 
    public void testProvisionedSyntheticResourceWithOptionalRequirement() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		try {
    			restartSubsystemsImplBundle();
    		}
    		catch (SubsystemException e) {
    			e.printStackTrace();
    			fail("Core bundle should have restarted");
    		}
    	}
    	finally {
    		stopAndUninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test 
    public void testProvisionedSyntheticResourceWithMandatoryRequirementAndApplicationImportService() throws Exception {
    	Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
    	try {
    		try {
    			restartSubsystemsImplBundle();
    		}
    		catch (SubsystemException e) {
    			e.printStackTrace();
    			fail("Core bundle should have restarted");
    		}
    	}
    	finally {
    		stopAndUninstallSubsystemSilently(applicationB);
    	}
    }
}
