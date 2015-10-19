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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.Grammar;
import org.eclipse.equinox.region.Region;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * This test ensures that names given to subsystem regions follow a predictable
 * pattern and will not inadvertently change without a discussion. At least one
 * major Apache Aries Subsystems consumer has a business requirement relying on
 * it.
 * 
 * The current naming convention has the following pattern:
 * 
 * subsystemSymbolicName;subsystemVersion;subsystemType;subsystemId
 */
public class RegionNameTest extends SubsystemTest {
	/*
     * Subsystem-SymbolicName: application.a.esa
     * 
     * Included In Archive
     * 		feature.a.esa
     */
    private static final String APPLICATION_A = "application.a.esa";
    /*
     * Subsystem-SymbolicName: composite.a.esa
     * Subsystem-Type: osgi.subsystem.composite
     * Subsystem-Content: feature.a.esa
     */
    private static final String COMPOSITE_A = "composite.a.esa";
    /*
     * Subsystem-SymbolicName: feature.a.esa
     * Subsystem-Type: osgi.subsystem.feature
     */
    private static final String FEATURE_A = "feature.a.esa";

	private static final String regexp = Grammar.SYMBOLICNAME + ';' +
			Grammar.VERSION + ";(?:" + 
			SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + '|' +
			SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE + ");[0-9]+";
	private static final Pattern pattern = Pattern.compile(regexp);
    
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createFeatureA();
		createApplicationA();
		createCompositeA();
		createdTestFiles = true;
	}
	
	private void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A, FEATURE_A);
    }
	
	private void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        createManifest(APPLICATION_A + ".mf", attributes);
    }
	
	private void createCompositeA() throws IOException {
        createCompositeAManifest();
        createSubsystem(COMPOSITE_A, FEATURE_A);
    }
	
	private void createCompositeAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, FEATURE_A + ";version=\"[0,0]\";type=osgi.subsystem.feature");
        createManifest(COMPOSITE_A + ".mf", attributes);
    }
	
	private void createFeatureA() throws IOException {
        createFeatureAManifest();
        createSubsystem(FEATURE_A);
    }
    
    private void createFeatureAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
        createManifest(FEATURE_A + ".mf", attributes);
    }
    
    @Test
    public void testApplicationRegionName() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		testRegionName(applicationA);
    		Subsystem featureA = getChild(applicationA, FEATURE_A, null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
    		testRegionName(featureA);
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testCompositeRegionName() throws Exception {
    	Subsystem compositeA = installSubsystemFromFile(COMPOSITE_A);
    	try {
    		testRegionName(compositeA);
    		Subsystem featureA = getChild(compositeA, FEATURE_A, null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
    		testRegionName(featureA);
    	}
    	finally {
    		uninstallSubsystemSilently(compositeA);
    	}
    }
    
    private void testRegionName(Subsystem subsystem) throws Exception {
    	Method getRegion = subsystem.getClass().getDeclaredMethod("getRegion");
    	getRegion.setAccessible(true);
    	Region region = (Region)getRegion.invoke(subsystem);
    	String regionName = region.getName();
    	Matcher matcher = pattern.matcher(regionName);
    	boolean matches = matcher.matches();
    	assertTrue("Invalid region name", matches);
    }
}
