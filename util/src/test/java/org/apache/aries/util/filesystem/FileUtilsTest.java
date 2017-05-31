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

package org.apache.aries.util.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.io.IOUtils;
import org.junit.Test;
import org.osgi.framework.Constants;


/**
 * This class contains tests for the virtual file system.
 */
public class FileUtilsTest
{


  /**
   * Make sure we get the bundles files recursively regardless of the file extension.
   * @throws IOException
   */

  @SuppressWarnings("deprecation")
  @Test
  public void testGetBundlesRecursive() throws IOException {
    File tmpDir = new File("../src/test/resources/tmpJars");
    tmpDir.mkdirs();
    for (int n =0; n< 2; n++) {
      ZipFixture bundle = ArchiveFixture.newJar().manifest()
      .attribute(Constants.BUNDLE_SYMBOLICNAME, "aa" + n)
      .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
      .attribute(Constants.IMPORT_PACKAGE, "a.b.c, p.q.r, x.y.z, javax.naming")
      .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();
      FileOutputStream fout = new FileOutputStream(new File (tmpDir.getAbsoluteFile(), "aa" + n + ((n == 0)? ".jar": ".war")));
      bundle.writeOut(fout);
      fout.close();
    }
    File subDir = new File(tmpDir, "subDir");
    subDir.mkdirs();
    for (int n =0; n< 2; n++) {
      ZipFixture bundle = ArchiveFixture.newJar().manifest()
      .attribute(Constants.BUNDLE_SYMBOLICNAME, "aa" + n)
      .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
      .attribute(Constants.IMPORT_PACKAGE, "a.b.c, p.q.r, x.y.z, javax.naming")
      .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();



      FileOutputStream fout = new FileOutputStream(new File (subDir.getAbsoluteFile(), "aa" + n + ((n == 0)? ".jar": ".war")));
      bundle.writeOut(fout);
      fout.close();
    }

    for (int n =0; n< 2; n++) {
      ZipFixture bundle = ArchiveFixture.newJar().manifest()
      .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
      .attribute(Constants.IMPORT_PACKAGE, "a.b.c, p.q.r, x.y.z, javax.naming")
      .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();


      FileOutputStream fout = new FileOutputStream(new File (tmpDir, "bb" + n + ".jar"));
      bundle.writeOut(fout);
      fout.close();
    }

    IOUtils.writeOut(tmpDir, "simple.jar", new ByteArrayInputStream("abc".getBytes()));
    IOUtils.writeOut(tmpDir, "simple.war", new ByteArrayInputStream("sss".getBytes()));
    IOUtils.writeOut(tmpDir, "simple.txt", new ByteArrayInputStream("abc".getBytes()));
    IOUtils.writeOut(tmpDir, "some/relative/directory/complex.jar", new ByteArrayInputStream("def".getBytes()));
    IOUtils.writeOut(tmpDir, "some/relative/directory/aa/complex2.war", new ByteArrayInputStream("ghi".getBytes()));
    IOUtils.writeOut(tmpDir, "simple", new ByteArrayInputStream("abc".getBytes()));

    List<URI> jarFiles = FileUtils.getBundlesRecursive(tmpDir.toURI());
    assertEquals("There should be 4 entries.", 4, jarFiles.size());
    assertTrue("The entry should contain this aa0.jar", jarFiles.contains(new File(tmpDir, "aa0.jar").toURI()));
    assertTrue("The entry should contain this aa1.war", jarFiles.contains(new File(tmpDir, "aa1.war").toURI()));
    assertTrue("The entry should contain this aa0.jar", jarFiles.contains(new File(subDir, "aa0.jar").toURI()));
    assertTrue("The entry should contain this aa1.war", jarFiles.contains(new File(subDir, "aa1.war").toURI()));

    IOUtils.deleteRecursive(tmpDir);
  }


}
