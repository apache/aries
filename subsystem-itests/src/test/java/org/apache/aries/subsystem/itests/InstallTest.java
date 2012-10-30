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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.aries.subsystem.itests.util.Utils;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.subsystem.Subsystem;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class InstallTest extends SubsystemTest {
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		ZipFixture feature = ArchiveFixture
				.newZip()
				.binary("OSGI-INF/SUBSYSTEM.MF",
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/OSGI-INF/SUBSYSTEM.MF"))
				.binary("a.jar/META-INF/MANIFEST.MF", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/a.jar/META-INF/MANIFEST.MF"))
				.binary("a.jar/a/A.class", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/a.jar/a/A.class"))
				.binary("applicationDir.esa/OSGI-INF/SUBSYSTEM.MF",
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/OSGI-INF/SUBSYSTEM.MF"))
				.binary("applicationDir.esa/b.jar/META-INF/MANIFEST.MF", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/b.jar/META-INF/MANIFEST.MF"))
				.binary("applicationDir.esa/b.jar/b/B.class", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/b.jar/b/B.class"))
				.binary("applicationDir.esa/featureDir.esa/OSGI-INF/SUBSYSTEM.MF",
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/OSGI-INF/SUBSYSTEM.MF"))
				.binary("applicationDir.esa/featureDir.esa/a.jar/META-INF/MANIFEST.MF", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/a.jar/META-INF/MANIFEST.MF"))
				.binary("applicationDir.esa/featureDir.esa/a.jar/a/A.class", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/a.jar/a/A.class"))
				.binary("applicationDir.esa/featureDir.esa/b.jar/META-INF/MANIFEST.MF", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/b.jar/META-INF/MANIFEST.MF"))
				.binary("applicationDir.esa/featureDir.esa/b.jar/b/B.class", 
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/b.jar/b/B.class"));
		feature.end();
		FileOutputStream fos = new FileOutputStream("compositeDir" + ".esa");
		try {
			feature.writeOut(fos);
		} finally {
			Utils.closeQuietly(fos);
		}
		createApplication("feature3", new String[]{"tb3.jar"});
		createApplication("feature2", new String[]{"tb3.jar", "tb2.jar"});
		createdApplications = true;
	}
	
	public void setUp() throws Exception {
		super.setUp();
		File userDir = new File(System.getProperty("user.dir"));
    	IDirectory idir = FileSystem.getFSRoot(userDir);
    	File compositeDir = new File(userDir, "compositeDir");
    	compositeDir.mkdir();
    	IOUtils.unpackZip(idir.getFile("compositeDir.esa"), compositeDir);
	}

	@Test
	public void testReturnExistingSubsystemWithSameLocation() throws Exception {
		Subsystem subsystem1 = installSubsystemFromFile("feature3.esa");
		try {
			Subsystem subsystem2 = subsystem1.install(subsystem1.getLocation());
			assertSame(subsystem1, subsystem2);
		}
		finally {
			uninstallSubsystemSilently(subsystem1);
		}
	}
	
	/*
     * Install a subsystem using a location string and a null input stream. The
     * location string is a file URL pointing to a subsystem directory 
     * containing nested subsystem and bundle directories.
     */
    @Test
    public void testLocationAsDirectoryUrl() throws Exception {
    	File file = new File("compositeDir");
    	try {
    		Subsystem subsystem = installSubsystem(getRootSubsystem(), file.toURI().toString(), null);
    		try {
    			assertSymbolicName("org.apache.aries.subsystem.itests.composite.dir", subsystem); 
    			assertConstituents(3, subsystem);
    			assertConstituent(subsystem, "org.apache.aries.subsystem.itests.composite.dir.bundle.a");
    			Bundle b = getConstituentAsBundle(
    					subsystem, 
    					"org.apache.aries.subsystem.itests.composite.dir.bundle.a", 
    					null, null);
    			assertLocation(subsystem.getLocation() + "!/" + "a.jar", b.getLocation());
    			assertClassLoadable("a.A", b);
    			assertChildren(1, subsystem);
    			Subsystem child = subsystem.getChildren().iterator().next();
    			assertSymbolicName(
    					"org.apache.aries.subsystem.itests.application.dir",
    					child);
    			assertConstituent(child, "org.apache.aries.subsystem.itests.composite.dir.bundle.b");
    			b = getConstituentAsBundle(
    					child, 
    					"org.apache.aries.subsystem.itests.composite.dir.bundle.b", 
    					null, null);
    			assertLocation(child.getLocation() + "!/" + "b.jar", b.getLocation());
    			assertClassLoadable("b.B", b);
    			assertChildren(1, child);
    			child = child.getChildren().iterator().next();
    			assertSymbolicName(
    					"org.apache.aries.subsystem.itests.feature.dir",
    					child);
    			assertConstituent(subsystem, "org.apache.aries.subsystem.itests.composite.dir.bundle.a");
    			b = getConstituentAsBundle(
    					child, 
    					"org.apache.aries.subsystem.itests.composite.dir.bundle.a", 
    					null, null);
    			assertLocation(child.getLocation() + "!/" + "a.jar", b.getLocation());
    			assertClassLoadable("a.A", b);
    			assertConstituent(child, "org.apache.aries.subsystem.itests.composite.dir.bundle.b", Version.parseVersion("1"));
    			b = getConstituentAsBundle(
    					child, 
    					"org.apache.aries.subsystem.itests.composite.dir.bundle.b", 
    					Version.parseVersion("1"), null);
    			assertLocation(child.getLocation() + "!/" + "b.jar", b.getLocation());
    			assertClassLoadable("b.B", b);
    		}
    		finally {
    			uninstallSubsystemSilently(subsystem);
    		}
    		
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		fail("Subsystem installation using directory URL as location failed");
    	}
    }
    
    @Test
    public void testManagedBundleStartLevel() throws Exception {
    	bundleContext.getBundle(0).adapt(FrameworkStartLevel.class).setInitialBundleStartLevel(5);
    	Bundle tb1 = bundleContext.installBundle("tb1.jar", SubsystemTest.class.getClassLoader().getResourceAsStream("feature1/tb1.jar"));
    	try {
    		Subsystem feature2 = installSubsystemFromFile("feature2.esa");
    		try {
    			startSubsystem(feature2);
    			try {
    				assertEquals("Wrong start level for unmanaged bundle", 5, tb1.adapt(BundleStartLevel.class).getStartLevel());
    				assertEquals("Wrong start level for managed bundle", 1, getBundle(feature2, "org.apache.aries.subsystem.itests.tb2").adapt(BundleStartLevel.class).getStartLevel());
    				assertEquals("Wrong start level for managed bundle", 1, getBundle(feature2, "org.apache.aries.subsystem.itests.tb3").adapt(BundleStartLevel.class).getStartLevel());
    			}
    			finally {
    				stopSubsystemSilently(feature2);
    			}
    		}
    		finally {
    			uninstallSubsystemSilently(feature2);
    		}
    	}
    	finally {
    		uninstallSilently(tb1);
    	}
    }
}
