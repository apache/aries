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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.internal.BasicSubsystem;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

import aQute.bnd.osgi.Constants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1419
 * 
 * Provide-Capability header parser does not support typed attributes.
 */
public class Aries1419Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * 
	 * Included In Archive
	 * 		bundle.a.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
    /*
     * Subsystem-SymbolicName: application.b.esa
     * 
	 * Included In Archive
	 * 		bundle.c.jar
     */
    private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: "a;resolution:=optional;filter:=\"(b=c)\";d=e;
	 *  f:String=g;h:Long=21474836470;i:Double=3.4028234663852886E39;
	 *  j:Version=2.1;k:List=\"foo,bar,acme\";l:List<Version>=\"1.1,2.2,3.3\""
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Provide-Capability: "a;d=e;f:String=g;h:Long=21474836470;
	 *  i:Double=3.4028234663852886E39;j:Version=2.1;k:List=\"foo,bar,acme\";
	 *  l:List<Version>=\"1.1,2.2,3.3\""
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	/*
	 * Bundle-SymbolicName: bundle.c.jar
	 * Require-Capability: "a;filter:="(d=e)"
	 */
	private static final String BUNDLE_C = "bundle.c.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createBundleB();
		createBundleC();
		createApplicationA();
		createApplicationB();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(
				name(BUNDLE_A), 
				requireCapability("a;resolution:=optional;filter:=\"(b=c)\"" 
						+ ";d=e;f:String=g;h:Long=21474836470;i:Double=3.4028234663852886E39"
						+ ";j:Version=2.1;k:List=\"foo,bar,acme\";l:List<Version>=\"1.1,2.2,3.3\""));
	}
	
	private void createBundleB() throws IOException {
		createBundle(
				name(BUNDLE_B), 
				provideCapability("a;b=c;d=e;f:String=g;h:Long=21474836470"
						+ ";i:Double=3.4028234663852886E39;j:Version=2.1;"
						+ "k:List=\"foo,bar,acme\";l:List<Version>=\"1.1,2.2,3.3\""));
	}
	
	private void createBundleC() throws IOException {
		createBundle(
				name(BUNDLE_C), 
				requireCapability("a;filter:=\"(&(b=c)(d=e)(f=g)(h<=21474836470)"
						+ "(!(i>=3.4028234663852886E40))(&(j>=2)(!(version>=3)))"
						+ "(|(k=foo)(k=bar)(k=acme))(&(l=1.1.0)(l=2.2)))\""));
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
        createSubsystem(APPLICATION_B, BUNDLE_C);
    }
    
    private static void createApplicationBManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
        createManifest(APPLICATION_B + ".mf", attributes);
    }
    
    @Test
    public void testRequireCapability() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		Bundle bundleA = getConstituentAsBundle(applicationA, BUNDLE_A, null, null);
    		String expectedStr = bundleA.getHeaders().get(Constants.REQUIRE_CAPABILITY);
    		RequireCapabilityHeader expected = new RequireCapabilityHeader(expectedStr);
    		Map<String, String> headers = ((BasicSubsystem)applicationA).getDeploymentHeaders();
    		String actualStr = headers.get(Constants.REQUIRE_CAPABILITY);
    		RequireCapabilityHeader actual = new RequireCapabilityHeader(actualStr);
    		assertEquals("Wrong header", expected, actual);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testProvideCapability() throws Exception {
    	Bundle bundleB = installBundleFromFile(BUNDLE_B);
    	try {
    		Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
    		uninstallSubsystemSilently(applicationB);
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
