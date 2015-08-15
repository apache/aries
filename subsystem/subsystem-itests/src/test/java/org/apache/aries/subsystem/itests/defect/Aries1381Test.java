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
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1368
 * 
 * java.lang.ClassCastException: org.apache.aries.subsystem.core.archive.GenericDirective 
 * cannot be cast to org.apache.aries.subsystem.core.archive.VersionRangeAttribute
 */
public class Aries1381Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: composite.a.esa
     * Import-Package: foo;version:="[5.0,6.0)",z;version="2.3";version:="3.2"
     * 
     * Included in archive:
     *      bundle.b.jar
     *      bundle.c.jar
     */
    private static final String COMPOSITE_A = "composite.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Export-Package: foo,z;version=2.3"
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
     * Bundle-SymbolicName: bundle.b.jar
     * Export-Package: x,y;version=1.5
     */
    private static final String BUNDLE_B = "bundle.b.jar";
    /*
     * Bundle-SymbolicName: bundle.c.jar
     * Import-Package: x;version:="[5.0,6.0)",y;version="[1.5,2.0)";version:="[1.0,1.5)"
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
		createCompositeA();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), exportPackage("foo,z;version=2.3"));
	}
	
	private void createBundleB() throws IOException {
        createBundle(name(BUNDLE_B), exportPackage("x,y;version=1.5"));
    }
	
	private void createBundleC() throws IOException {
        createBundle(name(BUNDLE_C), importPackage("x;version:=\"[5.0,6.0)\",y;version=\"[1.5,2.0)\";version:=\"[1.0,1.5)\""));
    }
    
    private static void createCompositeA() throws IOException {
        createCompositeAManifest();
        createSubsystem(COMPOSITE_A, BUNDLE_B, BUNDLE_C);
    }
    
    private static void createCompositeAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_B + ";version=\"[0,0]\"," + BUNDLE_C + ";version=\"[0,0]\"");
        attributes.put(Constants.IMPORT_PACKAGE, "foo;version:=\"[5.0,6.0)\",z;version=\"2.3\";version:=\"3.2\"");
        createManifest(COMPOSITE_A + ".mf", attributes);
    }
    
    @Test
    public void testVersionAttributeVerusVersionDirective() throws Exception {
        Bundle bundleA = installBundleFromFile(BUNDLE_A);
        try {
            Subsystem compositeA = installSubsystemFromFile(COMPOSITE_A);
            uninstallSubsystemSilently(compositeA);
        }
        catch (SubsystemException e) {
            e.printStackTrace();
            fail("Subsystem should have installed");
        }
        finally {
            uninstallSilently(bundleA);
        }
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
}
