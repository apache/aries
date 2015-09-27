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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemImportServiceHeader;
import org.apache.aries.subsystem.core.internal.BasicSubsystem;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import aQute.bnd.osgi.Constants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1421
 * 
 * SimpleFilter attribute extraction can not handle version ranges.
 */
public class Aries1421Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * 
	 * Included In Archive
	 * 		bundle.a.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Import-Package: org.osgi.framework;version="[1.7,2)",
	 *  org.osgi.service.coordinator,
	 *  org.osgi.service.resolver;version=1;bundle-version="[0,10)"
	 * Require-Bundle: org.apache.aries.subsystem;bundle-version="[1,1000)";visibility:=private;resolution:=optional,
	 *  org.eclipse.equinox.region,
	 *  org.eclipse.equinox.coordinator;version=1;resolution:=mandatory
	 * Require-Capability: osgi.service;filter:="(objectClass=foo)";effective:=active;resolution:=optional,
	 *  osgi.service;filter:="(&(objectClass=bar)(a=b))";resolution:=optional;effective:=active
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
				importPackage("org.osgi.framework;version=\"[1.7,2)\"" 
						+ ", org.osgi.service.coordinator"
						+ ", org.osgi.service.resolver;version=1;bundle-version=\"[0,10)\""),
				requireBundle("org.apache.aries.subsystem;bundle-version=\"[1,1000)\""
						+ ";visibility:=private;resolution:=optional,"
						+ "org.eclipse.equinox.region,"
						+ "org.eclipse.equinox.coordinator;bundle-version=1;resolution:=mandatory"),
				requireCapability("osgi.service;filter:=\"(objectClass=foo)\";effective:=active;resolution:=optional,"
						+ "osgi.service;filter:=\"(&(objectClass=bar)(a=b))\";resolution:=optional;effective:=active"));
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
    public void testImportPackageVersionRanges() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		Bundle bundleA = getConstituentAsBundle(applicationA, BUNDLE_A, null, null);
    		String expectedStr = bundleA.getHeaders().get(Constants.IMPORT_PACKAGE);
    		ImportPackageHeader expected = new ImportPackageHeader(expectedStr);
    		Map<String, String> headers = ((BasicSubsystem)applicationA).getDeploymentHeaders();
    		String actualStr = headers.get(Constants.IMPORT_PACKAGE);
    		ImportPackageHeader actual = new ImportPackageHeader(actualStr);
    		assertEquals("Wrong header", expected, actual);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testRequireBundleVersionRanges() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		Bundle bundleA = getConstituentAsBundle(applicationA, BUNDLE_A, null, null);
    		String expectedStr = bundleA.getHeaders().get(Constants.REQUIRE_BUNDLE);
    		RequireBundleHeader expected = new RequireBundleHeader(expectedStr);
    		Map<String, String> headers = ((BasicSubsystem)applicationA).getDeploymentHeaders();
    		String actualStr = headers.get(Constants.REQUIRE_BUNDLE);
    		RequireBundleHeader actual = new RequireBundleHeader(actualStr);
    		assertEquals("Wrong header", expected, actual);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testSubsystemImportService() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		String expectedStr = "foo;resolution:=optional,bar;filter:=\"(a=b)\";resolution:=optional";
    		SubsystemImportServiceHeader expected = new SubsystemImportServiceHeader(expectedStr);
    		Map<String, String> headers = ((BasicSubsystem)applicationA).getDeploymentHeaders();
    		String actualStr = headers.get(SubsystemConstants.SUBSYSTEM_IMPORTSERVICE);
    		SubsystemImportServiceHeader actual = new SubsystemImportServiceHeader(actualStr);
    		assertEquals("Wrong header", expected, actual);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
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
}
