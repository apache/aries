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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.internal.BasicSubsystem;
import org.apache.aries.subsystem.itests.util.Utils;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.io.IOUtils;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

@ExamReactorStrategy(PerMethod.class)
public class InstallTest extends SubsystemTest {
	public InputStream getResourceAsStream(String path) {
		return SubsystemTest.class.getClassLoader().getResourceAsStream(path);
	}
	
	@Override
	public void createApplications() throws Exception {
		createCompositeDirEsa();
		createApplication("feature3", "tb3.jar");
		createApplication("feature2", "tb3.jar", "tb2.jar");
		createBundleA();
		createBundleB();
		createApplicationA();
		createCompositeA();
		createFeatureA();
	}

	private void createCompositeDirEsa() throws IOException,
			FileNotFoundException {
		ZipFixture feature = ArchiveFixture
				.newZip()
				.binary("OSGI-INF/SUBSYSTEM.MF", getResourceAsStream("compositeDir" + "/OSGI-INF/SUBSYSTEM.MF"))
				.binary("a.jar/META-INF/MANIFEST.MF", getResourceAsStream("compositeDir" + "/a.jar/META-INF/MANIFEST.MF"))
				.binary("a.jar/a/A.class", getResourceAsStream("a/A.class"))
				.binary("applicationDir.esa/OSGI-INF/SUBSYSTEM.MF", getResourceAsStream("compositeDir" + "/applicationDir/OSGI-INF/SUBSYSTEM.MF"))
				.binary("applicationDir.esa/b.jar/META-INF/MANIFEST.MF", getResourceAsStream("compositeDir" + "/applicationDir/b.jar/META-INF/MANIFEST.MF"))
				.binary("applicationDir.esa/b.jar/b/B.class", getResourceAsStream("b/B.class"))
				.binary("applicationDir.esa/featureDir.esa/OSGI-INF/SUBSYSTEM.MF", getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/OSGI-INF/SUBSYSTEM.MF"))
				.binary("applicationDir.esa/featureDir.esa/a.jar/META-INF/MANIFEST.MF", getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/a.jar/META-INF/MANIFEST.MF"))
				.binary("applicationDir.esa/featureDir.esa/a.jar/a/A.class", getResourceAsStream("a/A.class"))
				.binary("applicationDir.esa/featureDir.esa/b.jar/META-INF/MANIFEST.MF", getResourceAsStream(
								"compositeDir" + "/applicationDir/featureDir/b.jar/META-INF/MANIFEST.MF"))
				.binary("applicationDir.esa/featureDir.esa/b.jar/b/B.class", getResourceAsStream("b/B.class"));
		feature.end();
		FileOutputStream fos = new FileOutputStream("compositeDir" + ".esa");
		try {
			feature.writeOut(fos);
		} finally {
			Utils.closeQuietly(fos);
		}

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
    
    /*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A));
	}
    
    /*
	 * No symbolic name. No manifest.
	 */
	private static final String APPLICATION_A = "application.a.esa";
	
	private void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private void createApplicationAManifest() throws IOException {
		File manifest = new File(APPLICATION_A + ".mf");
		if (manifest.exists())
			assertTrue("Could not delete manifest", manifest.delete());
	}
    
    @Test
    public void testGeneratedSymbolicNameWithoutManifest() throws Exception {
    	String expected = "org.apache.aries.subsystem.1";
    	Subsystem a = installSubsystemFromFile(APPLICATION_A);
    	try {
    		assertSymbolicName(expected, a);
    		assertSymbolicName(expected, a.getSubsystemHeaders(null).get(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME));
    	}
    	finally {
    		uninstallSubsystemSilently(a);
    	}
    }
    
    /*
	 * Manifest with no symbolic name header.
	 */
	private static final String COMPOSITE_A = "composite.a.esa";
	
	private static void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A);
	}
	
	private static void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	@Test
    public void testGeneratedSymbolicNameWithManifest() throws Exception {
    	String expected = "org.apache.aries.subsystem.1";
    	Subsystem a = installSubsystemFromFile(COMPOSITE_A);
    	try {
    		assertSymbolicName(expected, a);
    		assertSymbolicName(expected, a.getSubsystemHeaders(null).get(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME));
    	}
    	finally {
    		uninstallSubsystemSilently(a);
    	}
    }
	
	/*
	 * A bundle whose file extension does not end with ".jar".
	 * 
	 * Bundle-SymbolicName: bundle.b.war
	 */
	private static final String BUNDLE_B = "bundle.b.war";
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B));
	}
	
	/*
	 * Subsystem-SymbolicName: feature.a.esa
	 * Subsystem-Type: osgi.subsystem.feature
	 * Subsystem-Content: bundle.b.war
	 */
	private static final String FEATURE_A = "feature.a.esa";
	
	private static void createFeatureA() throws IOException {
		createFeatureAManifest();
		createSubsystem(FEATURE_A, BUNDLE_B);
	}
	
	private static void createFeatureAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		createManifest(FEATURE_A + ".mf", attributes);
	}
	
	@Test
	public void testSupportBundleResourcesNotEndingWithJar() throws Exception {
		Subsystem featureA = installSubsystemFromFile(FEATURE_A);
		try {
			assertConstituents(1, featureA);
			assertConstituent(featureA, BUNDLE_B);
		}
		finally {
			uninstallSubsystemSilently(featureA);
		}
	}
	
	@Test
    public void testLocationAsEmptyString() throws Exception {
    	try {
    		Subsystem a = installSubsystemFromFile(getRootSubsystem(), new File(APPLICATION_A), "");
    		try {
    			BasicSubsystem basic = (BasicSubsystem)a;
    			String location = basic.getLocation();
    			assertEquals("Location value should be an empty string", "", location);
    		}
    		finally {
    			uninstallSubsystemSilently(a);
    		}
    	}
    	catch (SubsystemException e) {
    		e.printStackTrace();
    		fail("Subsystem should have installed");
    	}
    }
}
