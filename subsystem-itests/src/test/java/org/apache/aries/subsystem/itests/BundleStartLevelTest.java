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
package org.apache.aries.subsystem.itests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import aQute.lib.osgi.Constants;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class BundleStartLevelTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.b.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
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
	 * Bundle-SymbolicName: bundle.c.jar
	 */
	private static final String BUNDLE_C = "bundle.c.jar";
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		createBundleA();
		createBundleB();
		createBundleC();
		createApplicationA();
		createdApplications = true;
	}
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_B);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private static void createBundleA() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.EXPORT_PACKAGE, "x");
		createBundle(BUNDLE_A, headers);
	}
	
	private static void createBundleB() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.IMPORT_PACKAGE, "x");
		createBundle(BUNDLE_B, headers);
	}
	
	private static void createBundleC() throws IOException {
		createBundle(BUNDLE_C);
	}
	
	public void setUp() throws Exception {
		super.setUp();
		registerRepositoryService(BUNDLE_A, BUNDLE_B);
	}
    
    /*
     * Tests the start level of bundle constituents.
     * 
     * A managed bundle is a bundle that was installed via the Subsystems API
     * either as content or a dependency. This includes the region context
     * bundle. The life cycle of managed bundles should follow the life cycle of
     * the subsystem of which they are constituents. They therefore receive a
     * start level of 1 to ensure they will be started and stopped at the same
     * time as the subsystem.
     * 
     * An unmanaged bundle is a bundle that was installed outside of the
     * Subsystem API. For example, the root subsystem may contain bundles that
     * were installed prior to the subsystems bundle. It's also possible to
     * install bundles via subsystem.getBundleContext().install(...). Unmanaged
     * bundles retain the start level setting assigned by the framework or
     * third party.
     */
    @Test
    public void testBundleStartLevel() throws Exception {
    	// Set the default bundle start level to something other than 1.
    	getSystemBundleAsFrameworkStartLevel().setInitialBundleStartLevel(5);
    	Subsystem a = installSubsystemFromFile(APPLICATION_A);
    	try {
    		startSubsystem(a);
    		try {
    			// Test managed bundles.
    			assertStartLevel(getBundle(a, BUNDLE_B), 1);
    			assertStartLevel(getRegionContextBundle(a), 1);
    			assertStartLevel(getBundle(getRootSubsystem(), BUNDLE_A), 1);
    			// Test unmanaged bundle.
    			Bundle c = installBundleFromFile(BUNDLE_C, a);
    			try {
    				assertConstituent(a, BUNDLE_C);
    				assertStartLevel(c, 5);
    			}
    			finally {
    				uninstallSilently(c);
    			}
    		}
    		finally {
    			stopSubsystemSilently(a);
    		}
    	}
    	finally {
    		uninstallSubsystemSilently(a);
    	}
    }
}
