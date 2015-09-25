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

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1419
 * 
 * Provide-Capability header parser does not support typed attributes.
 */
public class Aries1328Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * 
     * Included In Archive:
     * 		bundle.a.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: osgi.service;filter:="(objectClass=service.a)",
	 *  osgi.service;filter:="(objectClass=service.b)";effective:=resolve
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Provide-Capability: osgi.service;objectClass=service.a",
	 *  osgi.service;objectClass=service.b
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
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(
				name(BUNDLE_A), 
				requireCapability("osgi.service;filter:=\"(objectClass=service.a)\"" +
						", osgi.service;filter:=\"(objectClass=service.b)\";effective:=resolve"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(
				name(BUNDLE_B), 
				provideCapability("osgi.service;objectClass=service.a, osgi.service;objectClass=service.b"));
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
    
    @Test 
    public void testServiceAndRequireCapabilityInServiceNamespaceVisibilityInImportSharingPolicy() throws Exception {
    	Bundle bundleB = installBundleFromFile(BUNDLE_B);
    	try {
    		// Install application A containing content bundle A requiring two
    		// service capabilities each with effective:=resolve. Both the
    		// services and service capabilities should be visible to the
    		// bundle.
    		Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    		try {
    			// Start the application to ensure the runtime resolution
    			// succeeds.
    			applicationA.start();
    		}
    		catch (SubsystemException e) {
    			e.printStackTrace();
    			fail("Subsystem should have started");
    		}
    		finally {
    			stopAndUninstallSubsystemSilently(applicationA);
    		}
    	}
    	catch (SubsystemException e) {
    		e.printStackTrace();
    		fail("Subsystem should have installed");
    	}
    	finally {
    		uninstallSilently(bundleB);
    	}
    }
}
