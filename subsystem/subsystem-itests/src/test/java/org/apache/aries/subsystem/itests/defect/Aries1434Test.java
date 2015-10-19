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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1434
 * 
 * org.osgi.framework.BundleException: Region 
 * 'application.a.esa;0.0.0;osgi.subsystem.application;1' is already connected 
 * to region 'composite.a.esa;0.0.0;osgi.subsystem.composite;2
 * 
 */
public class Aries1434Test extends SubsystemTest {
	private static final String APPLICATION_A = "application.a.esa";
	private static final String COMPOSITE_A = "composite.a.esa";
	
	private static boolean createdTestFiles;
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createCompositeA();
		createApplicationA();
		createdTestFiles = true;
	}
	
	private void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, COMPOSITE_A);
	}
	
	private void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A);
	}
	
	private void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.EXPORT_PACKAGE, "x");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	@Test
    public void testResolvedChild() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
	    	Subsystem compositeA = getChild(applicationA, COMPOSITE_A, null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
	    	startSubsystem(compositeA);
	    	stopSubsystem(compositeA);
	    	try {
		    	assertState(State.RESOLVED, compositeA);
		    	startSubsystem(applicationA);
		    	try {
			    	assertState(State.ACTIVE, applicationA);
			    	assertState(State.ACTIVE, compositeA);
		    	}
		    	finally {
		    		stopSubsystemSilently(applicationA);
		    	}
	    	}
	    	finally {
	    		stopSubsystemSilently(compositeA);
	    	}
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
	
	@Test
    public void testActiveChild() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
	    	Subsystem compositeA = getChild(applicationA, COMPOSITE_A, null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
	    	startSubsystem(compositeA);
	    	try {
		    	assertState(State.ACTIVE, compositeA);
		    	startSubsystem(applicationA);
		    	try {
			    	assertState(State.ACTIVE, applicationA);
			    	assertState(State.ACTIVE, compositeA);
		    	}
		    	finally {
		    		stopSubsystemSilently(applicationA);
		    	}
	    	}
	    	finally {
	    		stopSubsystemSilently(compositeA);
	    	}
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
}
