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

import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class BasicTest extends SubsystemTest {
	
	@Override
	public void createApplications() throws Exception {
		createApplication("emptyFeature", new String[]{});
		createApplication("emptySubsystem", new String[]{});
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
