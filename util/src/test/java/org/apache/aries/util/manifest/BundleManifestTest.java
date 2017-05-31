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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.util.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundleManifestTest
{
  private static final String EXPECTED_VERSION = "1.0.0";
  private static final String EXPECTED_SYMBOLIC_NAME = "com.ibm.test";
  private static File BUNDLE_WITHOUT_NAME_HEADER;
  private static File BUNDLE_WITH_NAME_HEADER;

  @BeforeClass
  public static void setup() throws Exception
  {
    BUNDLE_WITHOUT_NAME_HEADER = new File ("./bundleManifestTest/nonExploded.jar");
    BUNDLE_WITHOUT_NAME_HEADER.getParentFile().mkdirs();
    BUNDLE_WITH_NAME_HEADER = new File ("./bundleManifestTest/nonExplodedWithName.jar");
    BUNDLE_WITH_NAME_HEADER.getParentFile().mkdirs();
    
    createZippedJar(BUNDLE_WITHOUT_NAME_HEADER, "exploded.jar");
    createZippedJar(BUNDLE_WITH_NAME_HEADER, "exploded-jar-with-name.jar");
    
  }

  private static void createZippedJar(File outputFile, String inputFolderName) throws FileNotFoundException, IOException {
	ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile));
    ZipEntry ze = new ZipEntry("META-INF/");
    out.putNextEntry(ze);
    
    File f = new File("../src/test/resources/bundles/" + inputFolderName + "/META-INF/beforeManifest.file");
    ze = new ZipEntry("META-INF/beforeManifest.file");
    ze.setSize(f.length());
    out.putNextEntry(ze);
    IOUtils.copy(new FileInputStream(f), out);
    
    f = new File("../src/test/resources/bundles/" + inputFolderName + "/META-INF/MANIFEST.MF");
    ze = new ZipEntry("META-INF/MANIFEST.MF");
    ze.setSize(f.length());
    out.putNextEntry(ze);
    IOUtils.copy(new FileInputStream(f), out);    
    
    out.close();
}
  
  @AfterClass
  public static void cleanup()
  {
    IOUtils.deleteRecursive(new File("bundleManifestTest/"));
  }

  @Test
  public void testExploded()
  {
    BundleManifest sut = BundleManifest.fromBundle(new File("../src/test/resources/bundles/exploded.jar"));
    assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
    assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }

  @Test
  public void testExplodedFromIDirectory()
  {
	  BundleManifest sut = BundleManifest.fromBundle(FileSystem.getFSRoot(
			  new File("../src/test/resources/bundles/exploded.jar")));
	  assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
	  assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }

  @Test
  public void testExplodedWithName()
  {
	  BundleManifest sut = BundleManifest.fromBundle(new File("../src/test/resources/bundles/exploded-jar-with-name.jar"));
	  assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
	  assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }

  @Test
  public void testExplodedWithNameFromIDirectory()
  {
	  BundleManifest sut = BundleManifest.fromBundle(FileSystem.getFSRoot(
			  new File("../src/test/resources/bundles/exploded-jar-with-name.jar")));
	  assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
	  assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }

  @Test
  public void testZip() throws Exception
  {
    // make sure that the manifest is not the first file in the jar archive
    JarInputStream jarIs = new JarInputStream(new FileInputStream(BUNDLE_WITHOUT_NAME_HEADER));
    assertNull(jarIs.getManifest());
    jarIs.close();
    
    BundleManifest sut = BundleManifest.fromBundle(BUNDLE_WITHOUT_NAME_HEADER);
    assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
    assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }

  @Test
  public void testZipFromIDirectory() throws Exception
  {
	  // make sure that the manifest is not the first file in the jar archive
	  JarInputStream jarIs = new JarInputStream(new FileInputStream(BUNDLE_WITHOUT_NAME_HEADER));
	  assertNull(jarIs.getManifest());
	  jarIs.close();
	  
	  BundleManifest sut = BundleManifest.fromBundle(
			  FileSystem.getFSRoot(BUNDLE_WITHOUT_NAME_HEADER));
	  assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
	  assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }

  @Test
  public void testZipWithName() throws Exception
  {
	  // make sure that the manifest is not the first file in the jar archive
	  JarInputStream jarIs = new JarInputStream(new FileInputStream(BUNDLE_WITH_NAME_HEADER));
	  assertNull(jarIs.getManifest());
	  jarIs.close();
	  
	  BundleManifest sut = BundleManifest.fromBundle(BUNDLE_WITH_NAME_HEADER);
	  assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
	  assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }
  
  @Test
  public void testZipWithNameFromIDirectory() throws Exception
  {
	  // make sure that the manifest is not the first file in the jar archive
	  JarInputStream jarIs = new JarInputStream(new FileInputStream(BUNDLE_WITH_NAME_HEADER));
	  assertNull(jarIs.getManifest());
	  jarIs.close();
	  
	  BundleManifest sut = BundleManifest.fromBundle(
			  FileSystem.getFSRoot(BUNDLE_WITH_NAME_HEADER));
	  assertEquals(EXPECTED_SYMBOLIC_NAME, sut.getSymbolicName());
	  assertEquals(EXPECTED_VERSION, sut.getVersion().toString());
  }

}

