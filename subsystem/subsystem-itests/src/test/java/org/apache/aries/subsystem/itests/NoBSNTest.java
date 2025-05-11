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
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class NoBSNTest extends SubsystemTest {
	
	@Override
	public void createApplications() throws Exception {
		createApplication("nobsn", "tb1.jar");
	}
	

    
	/*
	 * Subsystem application1 has content bundle tb1.jar.
	 * Bundle tb1.jar has an import package dependency on org.apache.aries.subsystem.itests.tb3.
	 */
    @Test
    public void testApplication1() throws Exception {
        Subsystem nobsn = installSubsystemFromFile("nobsn.esa");
        try {
            assertSymbolicName("org.apache.aries.subsystem.nobsn", nobsn);
            assertVersion("0.0.0", nobsn);
            assertType(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, nobsn);
            assertChildren(0, nobsn);
            assertConstituents(1, nobsn);
            startSubsystem(nobsn);
        }
        finally {
           stopSubsystemSilently(nobsn);
           uninstallSubsystemSilently(nobsn);
        }
    }
    
}
