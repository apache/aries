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

import java.io.File;
import java.io.FileOutputStream;

import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.subsystem.Subsystem;

@RunWith(JUnit4TestRunner.class)
public class ApplicationTest extends SubsystemTest {
	@Before
    public static void doCreateApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		ZipFixture testEba = ArchiveFixture
				.newZip()
				.binary("OSGI-INF/APPLICATION.MF",
						ApplicationTest.class.getClassLoader()
								.getResourceAsStream(
										"application1/OSGI-INF/APPLICATION.MF"))
				.binary("tb1.jar",
						ApplicationTest.class.getClassLoader()
								.getResourceAsStream("application1/tb1.jar"))
				.end();
		FileOutputStream fout = new FileOutputStream("application1.eba");
		try {
			testEba.writeOut(fout);
			createdApplications = true;
		} finally {
			Utils.closeQuietly(fout);
		}
    }
    
    //@Test
    public void testApplication1() throws Exception {
    	String application = "application1.eba";
    	Subsystem subsystem = assertSubsystemLifeCycle(new File(application));
    	assertId(subsystem);
        assertLocation(application, subsystem);
        assertSymbolicName("org.apache.aries.subsystem.application1", subsystem);
        assertVersion("1.0.0", subsystem);
        assertConstituents(2, subsystem);
    }
}
