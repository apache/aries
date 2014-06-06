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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class BasicTest extends SubsystemTest {
	
	@Override
	public void createApplications() throws Exception {
		createApplication("emptyFeature", new String[]{});
		createApplication("emptySubsystem", new String[]{});
	}
	
	/*
	 * When the subsystems implementation bundle is installed, there should be
	 * a Subsystem service available.
	 */
    @Test
    public void test1() throws Exception {
    	Bundle[] bundles = bundleContext.getBundles();
    	boolean found = false;
    	for (Bundle bundle : bundles) {
    		if ("org.apache.aries.subsystem.core".equals(bundle.getSymbolicName())) {
    			found = true;
    			break;
    		}
    	}
    	assertTrue("Subsystems implementation bundle not found", found);
    	ServiceReference serviceReference = bundleContext.getServiceReference(Subsystem.class);
    	assertNotNull("Reference to subsystem service not found", serviceReference);
    	Subsystem subsystem = (Subsystem) bundleContext.getService(serviceReference);
    	assertNotNull("Subsystem service not found", subsystem);
    }
    
    @Test
    public void testEmptyFeature() throws Exception {
    	Subsystem emptyFeature = installSubsystemFromFile("emptyFeature.esa");
		AssertionError error = null;
		try {
			assertSymbolicName("org.apache.aries.subsystem.itests.feature.empty", emptyFeature);
			assertVersion("1.1.2", emptyFeature);
			assertType(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, emptyFeature);
			assertConstituents(0, emptyFeature);
			assertChildren(0, emptyFeature);
			startSubsystem(emptyFeature);
			stopSubsystem(emptyFeature);
		}
		catch (AssertionError e) {
			error = e;
			throw e;
		}
		finally {
			try {
				uninstallSubsystemSilently(emptyFeature);
			}
			catch (AssertionError e) {
				if (error == null)
					throw e;
				e.printStackTrace();
			}
		}
    }
    
    /*
     * This tests a subsystem containing only a subsystem manifest which, in
     * turn, contains only a Subsystem-SymbolicName header.
     */
    @Test
    public void testEmptySubsystem() throws Exception {
    	Subsystem emptySubsystem = installSubsystemFromFile("emptySubsystem.esa");
		AssertionError error = null;
		try {
			assertSymbolicName("org.apache.aries.subsystem.itests.subsystem.empty", emptySubsystem);
			// The version should be the default version.
			assertVersion(Version.emptyVersion, emptySubsystem);
			// The type should be the default type.
			assertType(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, emptySubsystem);
			// Since the default type is application, which is a scoped subsystem, there will
			// be one constituent representing the region context bundle.
			assertConstituents(1, emptySubsystem);
			assertChildren(0, emptySubsystem);
			startSubsystem(emptySubsystem);
			stopSubsystem(emptySubsystem);
		}
		catch (AssertionError e) {
			error = e;
			throw e;
		}
		finally {
			try {
				uninstallSubsystemSilently(emptySubsystem);
			}
			catch (AssertionError e) {
				if (error == null)
					throw e;
				e.printStackTrace();
			}
		}
    }
}
