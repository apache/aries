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

package org.apache.aries.application.utils.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.application.utils.filesystem.IOUtils;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundleManifestTest
{
  private static File _testfile;
  
  @BeforeClass
  public static void setup() throws Exception
  {
    _testfile = new File ("./bundleManifestTest/nonExploded.jar");
    _testfile.getParentFile().mkdirs();
    
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(_testfile));
    ZipEntry ze = new ZipEntry("META-INF/");
    out.putNextEntry(ze);
    
    File f = new File("../src/test/resources/bundles/exploded.jar/META-INF/beforeManifest.file");
    ze = new ZipEntry("META-INF/beforeManifest.file");
    ze.setSize(f.length());
    out.putNextEntry(ze);
    IOUtils.copy(new FileInputStream(f), out);
    
    f = new File("../src/test/resources/bundles/exploded.jar/META-INF/MANIFEST.MF");
    ze = new ZipEntry("META-INF/MANIFEST.MF");
    ze.setSize(f.length());
    out.putNextEntry(ze);
    IOUtils.copy(new FileInputStream(f), out);    
    
    out.close();
  }
  
  @AfterClass
  public static void cleanup()
  {
    _testfile.delete();
  }
  
  @Test
  public void testExploded()
  {
    BundleManifest sut = BundleManifest.fromBundle(new File("../src/test/resources/bundles/exploded.jar"));
    assertEquals("com.ibm.test", sut.getSymbolicName());
    assertEquals("1.0.0", sut.getVersion().toString());
  }
  
  @Test
  public void testZip() throws Exception
  {
    // make sure that the manifest is not the first file in the jar archive
    JarInputStream jarIs = new JarInputStream(new FileInputStream(_testfile));
    assertNull(jarIs.getManifest());
    jarIs.close();
    
    BundleManifest sut = BundleManifest.fromBundle(_testfile);
    assertEquals("com.ibm.test", sut.getSymbolicName());
    assertEquals("1.0.0", sut.getVersion().toString());
  }
}

