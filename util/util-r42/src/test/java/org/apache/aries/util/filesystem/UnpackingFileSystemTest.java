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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.aries.unittest.junit.Assert;
import org.apache.aries.util.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class contains tests for the unpacking virtual file system.
 */
public class UnpackingFileSystemTest
{
  /**
   * Make sure we correctly understand the content of the application when the
   * application is a zip. This test just checks that the
   * root directory returns the expected information.
   *
   * @throws IOException
   */
  @Test(expected=UnsupportedOperationException.class)
  public void basicRootDirTestsWithZipInputStream() throws IOException
  {
    File baseDir = new File("fileSystemTest/app2.zip");
    ICloseableDirectory dir = UnpackingFileSystem.getFSRoot(new FileInputStream(baseDir));
    String closeableDirectoryLocation = String.valueOf(dir.getRoot());
    try {
	  File desiredContent = new File(getTestResourceDir(), "/app1");
      runBasicRootDirTests(dir, desiredContent.length());
    } finally {
	  dir.close();
	  assertFalse("Directory was not removed upon closing", new File(closeableDirectoryLocation).exists());
    }
  }

  /**
   * Make sure that the operations work with zip files inside other zip files.
   */
  @Test
  public void nestedZipInZipInputStream() throws Exception
  {
    ICloseableDirectory outer = UnpackingFileSystem.getFSRoot(new FileInputStream("fileSystemTest/outer.zip"));
  	String outerLocation = String.valueOf(outer.getRoot());
    try {
      IFile innerFile = outer.getFile("app2.zip");
      assertNotNull(innerFile);

      IDirectory inner = innerFile.convertNested();
      assertNotNull(inner);

	  File desiredFile = new File("fileSystemTest/app2.zip");
	  runBasicDirTest(inner, "/app2.zip/",  desiredFile.length());
    } finally {
      outer.close();
	  assertFalse("Directory was not removed upon closing", new File(outerLocation).exists());
    }
  }

  @Test
  public void zipCloseableZipSimplePerformanceTest() throws IOException
  {
	  int N = 100000;
	  File baseDir = new File("fileSystemTest/app2.zip");

	  ZipFile zip = new ZipFile(baseDir);

	  long start = System.currentTimeMillis();
	  for (int i=0; i<N; i++) {
		  ZipEntry ze = zip.getEntry("META-INF/APPLICATION.MF");
		  InputStream is = zip.getInputStream(ze);
		  is.close();
	  }
	  long duration = System.currentTimeMillis() - start;

	  // normal zip files

	  ICloseableDirectory dir = UnpackingFileSystem.getFSRoot(new FileInputStream(baseDir)).toCloseable();

	  start = System.currentTimeMillis();
	  for (int i=0; i<N; i++) {
		  IFile appMf = dir.getFile("META-INF/APPLICATION.MF");
		  InputStream is = appMf.open();
		  is.close();
	  }
	  long duration2 = System.currentTimeMillis() - start;

	  dir.close();
	  // within an order of magnitude
	  assertTrue("ZipFile: "+duration+", IDirectory: "+duration2 , duration2 < 10*duration );

	  // nested zip files

	  IDirectory outer = UnpackingFileSystem.getFSRoot(new FileInputStream("fileSystemTest/outer.zip"));
	  IFile innerFile = outer.getFile("app2.zip");
	  dir = innerFile.convertNested().toCloseable();

	  start = System.currentTimeMillis();
	  for (int i=0; i<N; i++) {
		  IFile appMf = dir.getFile("META-INF/APPLICATION.MF");
		  InputStream is = appMf.open();
		  is.close();
	  }
	  long duration3 = System.currentTimeMillis() - start;

	  dir.close();
	  // within an order of magnitude
	  assertTrue("ZipFile: "+duration+", IDirectory: "+duration3 , duration3 < 10*duration );
  }

  /**
   * Zip up the app1 directory to create a zippped version before running any
   * tests.
   *
   * @throws IOException
   */
  @BeforeClass
  public static void makeZip() throws IOException
  {
    File zipFile = new File("fileSystemTest/app2.zip");
    zipFile.getParentFile().mkdirs();
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

    int index = new File(getTestResourceDir(), "/app1").getAbsolutePath().length();
    writeEnties(out, new File(getTestResourceDir(), "/app1"), index);

    out.close();

    zipFile = new File("outer.zip");
    out = new ZipOutputStream(new FileOutputStream(zipFile));
    index = new File("fileSystemTest").getAbsolutePath().length();
    writeEnties(out, new File("fileSystemTest"), index);

    out.close();

    if (!!!zipFile.renameTo(new File("fileSystemTest/outer.zip"))) throw new IOException("Rename failed");
  }

  private static File getTestResourceDir() {
	  File root = new File("").getAbsoluteFile();

	  if (root.getName().equals("target")) return new File("../src/test/resources");
	  else return new File("src/test/resources");
  }

  /**
   * Make sure the test zip is deleted afterwards.
   */
  @AfterClass
  public static void destroyZip()
  {
	  IOUtils.deleteRecursive(new File("fileSystemTest"));
  }

  /**
   * This method writes the given directory into the provided zip output stream.
   * It removes the first <code>index</code> bytes from the absolute path name
   * when building the zip.
   *
   * @param zos   the zip output stream to use
   * @param f     the directory to write into the zip.
   * @param index how much of the file name to chop off.
   * @throws IOException
   */
  private static void writeEnties(ZipOutputStream zos, File f, int index) throws IOException {
    File[] files = f.listFiles();

    if (files != null) {
      for (File file : files) {
        String fileName = file.getAbsolutePath().substring(index + 1);

     // Bug 1954: replace any '\' characters with '/' - required by ZipEntry
        fileName = fileName.replace('\\', '/');

        if (file.isDirectory()) fileName = fileName + "/";

        ZipEntry ze = new ZipEntry(fileName);
        ze.setSize(file.length());
        ze.setTime(file.lastModified());
        zos.putNextEntry(ze);

        if (file.isFile()) {
          InputStream is = new FileInputStream(file);
          byte[] buffer = new byte[(int)file.length()];
          int len = is.read(buffer);
          zos.write(buffer, 0, len);
          is.close();   // Bug 1594
        }

        zos.closeEntry();

        if (file.isDirectory()) {
          writeEnties(zos, file, index);
        }
      }
    }
  }

  /**
   * This method makes sure that the data is correctly understood from disk. It
   * is called for both the file and zip versions of the test to ensure we have
   * consistent results.
   *
   * @param dir   The IDirectory for the root of the vFS.
   * @param len   The size of the file.
   * @param time  The time the file was last updated.
   * @throws IOException
   */
  public void runBasicRootDirTests(IDirectory dir, long len) throws IOException
  {
    assertEquals("The root file system name is not correct", "", dir.getName());
    assertEquals("The size of the file is not correct", len, dir.getSize());
    assertNull("I managed to get a parent of a root", dir.getParent());
    assertTrue("The root dir does not know it is a dir", dir.isDirectory());
    assertFalse("The root dir has an identity crisis and thinks it is a file", dir.isFile());

    dir.open();
  }

  /**
   * This method makes sure that the data is correctly understood from disk. It
   * is called for both the file and zip versions of the test to ensure we have
   * consistent results.
   *
   * @param dir   The IDirectory for the root of the vFS.
   * @param len   The size of the file.
   * @param time  The time the file was last updated.
   * @throws IOException
   */
  private void runBasicDirTest(IDirectory dir, String namePrefix, long len) throws IOException
  {
	assertEquals("The size of the file is not correct", len, dir.getSize());

	assertNull("for some reason our fake app has a fake blueprint file.", dir.getFile("OSGI-INF/blueprint/aries.xml"));

    IFile file = dir.getFile("META-INF/APPLICATION.MF");

    assertNotNull("we could not find the application manifest", file);

    assertNull(file.convert());
    assertNull(file.convertNested());

    assertEquals(namePrefix+"META-INF/APPLICATION.MF", file.getName().replace('\\', '/'));

    assertEquals(namePrefix+"META-INF", file.getParent().getName());
    assertFalse(file.isDirectory());
    assertTrue(file.isFile());

    List<IFile> files = dir.listFiles();
    filterOutSvn(files);
    assertEquals(1, files.size());

    List<IFile> allFiles = dir.listAllFiles();
    filterOutSvn(allFiles);
    assertEquals(3, allFiles.size());

    assertEquals(namePrefix+"META-INF", allFiles.get(1).getParent().getName());

    IFile metaInf = files.get(0);

    assertTrue(metaInf.isDirectory());
    assertEquals(namePrefix+"META-INF", metaInf.getName());
    assertNotNull(metaInf.convert());

    files = metaInf.convert().listAllFiles();
    filterOutSvn(files);
    assertEquals(2, files.size());

    for (IFile aFile : dir) {
      if (!aFile.getName().contains(".svn")) {
        assertTrue(aFile.isDirectory());
        assertEquals(namePrefix+"META-INF", aFile.getName());
        assertNotNull(aFile.convert());
      }
    }

    checkManifest(file.open());

    IFile applicationMF2 = dir.getFile("META-INF/APPLICATION.MF");

    Assert.assertEqualsContract(file, applicationMF2, dir);
    Assert.assertHashCodeEquals(file, applicationMF2, true);
  }

  private void filterOutSvn(Collection<IFile> files) {
	  Iterator<IFile> its = files.iterator();
	  while (its.hasNext()) {
		  IFile f = its.next();
		  if (f.getName().toLowerCase().contains(".svn")) its.remove();
	  }
  }

  private void checkManifest(InputStream is) throws IOException {
	  Manifest man = new Manifest(is);
	  //remember to close the input stream after use
	  is.close();
	  assertEquals("com.travel.reservation", man.getMainAttributes().getValue("Application-SymbolicName"));
  }
}
