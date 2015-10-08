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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

/*
 * https://issues.apache.org/jira/browse/ARIES-1428
 * 
 * org.osgi.framework.BundleException: Could not resolve module: <module> Bundle 
 * was filtered by a resolver hook.
 */
public class Aries1428Test extends SubsystemTest {
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	private void createBundleA() throws IOException {
		createBundle(
				name(BUNDLE_A));
	}
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createdTestFiles = true;
	}
    
    @Test
    public void testBundleNotPartOfSubsystemInstallationResolves() throws Exception {
    	final Bundle core = getSubsystemCoreBundle();
    	core.stop();
    	bundleContext.addServiceListener(
    			new ServiceListener() {
    				@Override
    				public void serviceChanged(ServiceEvent event) {
    					if (event.getType() == ServiceEvent.REGISTERED) {
    						File file = new File(BUNDLE_A);
    						try {
    							Bundle bundleA = bundleContext.installBundle(
    									file.toURI().toString(), new FileInputStream(file));
    							bundleA.start();
    						}
    						catch (Exception e) {
    							e.printStackTrace();
    						}
    					}
    				}
    			},
    			"(objectClass=org.osgi.service.subsystem.Subsystem)"
    	);
    	core.start();
    	assertBundleState(Bundle.RESOLVED | Bundle.ACTIVE, BUNDLE_A, getRootSubsystem());
    }
}
