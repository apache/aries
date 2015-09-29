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
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1423
 * 
 * IllegalArgumentException when GenericHeader has no clauses
 */
public class Aries1423Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * 
	 * Included In Archive
	 * 		bundle.a.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Build-Plan:
	 * Build-Number: 
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createApplicationA();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(
				name(BUNDLE_A), 
				new Header("Build-Plan", ""),
				new Header("Build-Number", "") {});
	}
	
	private static void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A, BUNDLE_A);
    }
    
    private static void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        attributes.put("Build-Plan", "");
        attributes.put("Build-Number", "");
        createManifest(APPLICATION_A + ".mf", attributes);
    }
    
    @Test
    public void testEmptyNonOsgiHeaders() throws Exception {
    	try {
    		Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    		uninstallSubsystemSilently(applicationA);
    	}
    	catch (SubsystemException e) {
    		e.printStackTrace();
    		fail("Subsystem should have installed");
    	}
    }
}
