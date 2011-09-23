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
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.aries.subsystem.itests.util.Utils;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Version;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@RunWith(JUnit4TestRunner.class)
public class FeatureTest extends SubsystemTest {
	private static void createApplication(String name, String[] content) throws Exception {
		ZipFixture feature = ArchiveFixture
				.newZip()
				.binary("OSGI-INF/SUBSYSTEM.MF",
						FeatureTest.class.getClassLoader().getResourceAsStream(
								name + "/OSGI-INF/SUBSYSTEM.MF"));
		for (String s : content) {
			try {
				feature.binary(s,
						FeatureTest.class.getClassLoader().getResourceAsStream(
								name + '/' + s));
			}
			catch (Exception e) {
				feature.binary(s, new FileInputStream(new File(s)));
			}
		}
		feature.end();
		FileOutputStream fos = new FileOutputStream(name + ".ssa");
		try {
			feature.writeOut(fos);
		} finally {
			Utils.closeQuietly(fos);
		}
	}
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		createApplication("feature2", new String[]{"tb2.jar", "tb3.jar"});
		createApplication("feature1", new String[]{"tb1.jar", "feature2.ssa", "tb3.jar"});
		createdApplications = true;
	}

	@Test
	public void testFeature1() throws Exception {
		Subsystem feature1 = installSubsystemFromFile("feature1.ssa");
		Subsystem feature2 = null;
		AssertionError error = null;
		try {
			assertSymbolicName("org.apache.aries.subsystem.feature1", feature1);
			assertVersion("1.0.0", feature1);
			assertConstituents(5, feature1);
			assertChildren(1, feature1);
			feature2 = feature1.getChildren().iterator().next();
			assertEvent(feature2, Subsystem.State.INSTALLING, SubsystemConstants.EVENT_TYPE.INSTALLING, 5000);
			assertEvent(feature2, Subsystem.State.INSTALLED, SubsystemConstants.EVENT_TYPE.INSTALLED, 5000);
			assertSymbolicName("org.apache.aries.subsystem.feature2", feature2);
			assertVersion("1.0.0", feature2);
			assertConstituents(2, feature2);
			assertChildren(0, feature2);
			// TODO Test internal events for installation.
			startSubsystem(feature1);
			assertEvent(feature2, Subsystem.State.RESOLVING, SubsystemConstants.EVENT_TYPE.RESOLVING, 5000);
			assertEvent(feature2, Subsystem.State.RESOLVED, SubsystemConstants.EVENT_TYPE.RESOLVED, 5000);
			assertEvent(feature2, Subsystem.State.STARTING, SubsystemConstants.EVENT_TYPE.STARTING, 5000);
			assertEvent(feature2, Subsystem.State.ACTIVE, SubsystemConstants.EVENT_TYPE.STARTED, 5000);
			// TODO Test internal events for starting.
			stopSubsystem(feature1);
			assertEvent(feature2, Subsystem.State.STOPPING, SubsystemConstants.EVENT_TYPE.STOPPING, 5000);
			assertEvent(feature2, Subsystem.State.RESOLVED, SubsystemConstants.EVENT_TYPE.STOPPED, 5000);
			// TODO Test internal events for stopping.
		}
		catch (AssertionError e) {
			error = e;
			throw e;
		}
		finally {
			try {
				uninstallSubsystem(feature1);
				if (feature2 != null) {
					assertEvent(feature2, Subsystem.State.UNINSTALLING, SubsystemConstants.EVENT_TYPE.UNINSTALLING, 5000);
					assertEvent(feature2, Subsystem.State.UNINSTALLED, SubsystemConstants.EVENT_TYPE.UNINSTALLED, 5000);
					// TODO Test internal events for uninstalling.
					assertNotChild(feature1, feature2);
				}
			}
			catch (AssertionError e) {
				if (error == null)
					throw e;
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testSharedFeatureResource() throws Exception {
		Subsystem feature1 = installSubsystemFromFile("feature1.ssa");
		AssertionError error = null;
		try {
			assertConstituent(feature1, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), ResourceConstants.IDENTITY_TYPE_BUNDLE);
			Subsystem feature2 = feature1.getChildren().iterator().next();
			// TODO This needs to be better implemented and put into a utility method on the superclass.
			while (!feature2.getState().equals(Subsystem.State.INSTALLED))
				Thread.sleep(100);
			assertConstituent(feature2, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), ResourceConstants.IDENTITY_TYPE_BUNDLE);
			uninstallSubsystem(feature2);
			assertNotChild(feature1, feature2);
			assertConstituent(feature1, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), ResourceConstants.IDENTITY_TYPE_BUNDLE);
		}
		catch (AssertionError e) {
			error = e;
			throw e;
		}
		finally {
			try {
				uninstallSubsystem(feature1);
			}
			catch (AssertionError e) {
				if (error == null)
					throw e;
				e.printStackTrace();
			}
		}
	}
}
