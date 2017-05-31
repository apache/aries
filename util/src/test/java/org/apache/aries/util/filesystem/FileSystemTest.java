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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.aries.unittest.junit.Assert;
import org.apache.aries.util.IORuntimeException;
import org.apache.aries.util.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * This class contains tests for the virtual file system.
 */
public class FileSystemTest
{
  /**
   * Make sure we correctly understand the content of the application when the
   * application is an exploded directory. This test just checks that the
   * root directory returns the expected information.
   *
   * @throws IOException
   */
  @Test(expected=UnsupportedOperationException.class)
  public void basicRootDirTestsWithFiles() throws IOException
  {
    File baseDir = new File(getTestResourceDir(), "/app1");
    File manifest = new File(baseDir, "META-INF/APPLICATION.MF");
    IDirectory dir = FileSystem.getFSRoot(baseDir);

    runBasicRootDirTests(dir, baseDir.length(), manifest.lastModified());
  }

  /**
   * Make sure we correctly understand the directory structure for exploded
   * directories.
   *
   * @throws IOException
   */
  @Test
  public void basicDirTestsWithFiles() throws IOException
  {
    File baseDir = new File(getTestResourceDir(), "/app1");
    IDirectory dir = FileSystem.getFSRoot(baseDir);

    File desiredFile = new File(baseDir, "META-INF/APPLICATION.MF");

    runBasicDirTest(dir, desiredFile.length(), desiredFile.lastModified());
    runBasicDirTest(dir.toCloseable(), desiredFile.length(), desiredFile.lastModified());
  }

  /**
   * Make sure we correctly understand the content of the application when the
   * application is a zip. This test just checks that the
   * root directory returns the expected information.
   *
   * @throws IOException
   */
  @Test(expected=UnsupportedOperationException.class)
  public void basicRootDirTestsWithZip() throws IOException
  {
    File baseDir = new File("fileSystemTest/app2.zip");
    IDirectory dir = FileSystem.getFSRoot(baseDir);

    runBasicRootDirTests(dir, baseDir.length(), baseDir.lastModified());
  }

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
    ICloseableDirectory dir = FileSystem.getFSRoot(new FileInputStream(baseDir));

    try {
      runBasicRootDirTests(dir, baseDir.length(), baseDir.lastModified());
    } finally {
      dir.close();
    }
  }

  @Test
  public void testInvalidFSRoot() throws IOException
  {
	  File baseDir = new File(getTestResourceDir(), "/app1");
	  File manifest = new File(baseDir, "META-INF/APPLICATION.MF");
	  try {
	      FileSystem.getFSRoot(manifest);
	      fail("Should have thrown an IORuntimeException");
	  } catch (IORuntimeException e) {
	      // good!
	  }
  }

  /**
   * Make sure that operations work on zip files nested in file IDirectories
   * @throws IOException
   */
  @Test
  public void nestedZipInDirectory() throws IOException
  {
	IDirectory dir = FileSystem.getFSRoot(new File("").getAbsoluteFile());

	// base convert does not do nested zips
	IDirectory zip = dir.getFile("fileSystemTest/app2.zip").convert();
	assertNull(zip);

	// basic conversion works
	zip = dir.getFile("fileSystemTest/app2.zip").convertNested();
	assertNotNull(zip);

	// we get the parent and our path right
	assertNotNull(zip.getParent());
	assertEquals("fileSystemTest", zip.getParent().getName());
	assertEquals("fileSystemTest/app2.zip", zip.getName());

	// files inside the nested zip have the correct path as well
	IFile appMf = zip.getFile("META-INF/APPLICATION.MF");
	assertNotNull(appMf);
	assertEquals("fileSystemTest/app2.zip/META-INF/APPLICATION.MF", appMf.getName());
	checkManifest(appMf.open());

	// root is right
	assertFalse(zip.isRoot());
	assertEquals(dir, zip.getRoot());
	assertEquals(dir, appMf.getRoot());

	// check URLs are correct
	checkManifest(appMf.toURL().openStream());

	runBasicDirTest(zip, "fileSystemTest/app2.zip/", appMf.getSize(), appMf.getLastModified());
  }

  /**
   * Make sure that the operations work with zip files inside other zip files. Performance is not going to be great though :)
   */
  @Test
  public void nestedZipInZip() throws IOException
  {
	  IDirectory outer = FileSystem.getFSRoot(new File("fileSystemTest/outer.zip"));

	  IFile innerFile = outer.getFile("app2.zip");
	  assertNotNull(innerFile);

	  IDirectory inner = innerFile.convertNested();
	  assertNotNull(inner);

	  File desiredFile = new File(new File(getTestResourceDir(), "/app1"), "META-INF/APPLICATION.MF");

	  // no size information when stream reading :(
	  runBasicDirTest(inner, "app2.zip/", -1, desiredFile.lastModified());
	  runBasicDirTest(inner.toCloseable(), "app2.zip/", desiredFile.length(), desiredFile.lastModified());
  }

  /**
   * Make sure that the operations work with zip files inside other zip files. Performance is not going to be great though :)
   */
  @Test
  public void nestedZipInZipInputStream() throws Exception
  {
    ICloseableDirectory outer = FileSystem.getFSRoot(new FileInputStream("fileSystemTest/outer.zip"));
    try {
      IFile innerFile = outer.getFile("app2.zip");
      assertNotNull(innerFile);

      IDirectory inner = innerFile.convertNested();
      assertNotNull(inner);

      File desiredFile = new File(new File(getTestResourceDir(), "/app1"), "META-INF/APPLICATION.MF");

      // no size information when stream reading :(
      runBasicDirTest(inner, "app2.zip/", -1, desiredFile.lastModified());
      runBasicDirTest(inner.toCloseable(), "app2.zip/", desiredFile.length(), desiredFile.lastModified());
    } finally {
      outer.close();

      Field f = outer.getClass().getDeclaredField("tempFile");

      f.setAccessible(true);
      assertFalse(((File)f.get(outer)).exists());
    }
  }

  /**
   * Make sure we correctly understand the directory structure for zips.
   *
   * @throws IOException
   */
  @Test
  public void basicDirTestsWithZip() throws IOException
  {
    File baseDir = new File("fileSystemTest/app2.zip");
    IDirectory dir = FileSystem.getFSRoot(baseDir);

    assertTrue(dir.toString(), dir.toString().endsWith("app2.zip"));

    File desiredFile = new File(new File(getTestResourceDir(), "/app1"), "META-INF/APPLICATION.MF");

    runBasicDirTest(dir, desiredFile.length(), desiredFile.lastModified());
    runBasicDirTest(dir.toCloseable(), desiredFile.length(), desiredFile.lastModified());
  }

  /**
   * Make sure we correctly understand the directory structure for zips.
   *
   * @throws IOException
   */
  @Test
  public void basicDirTestsWithZipInputStream() throws IOException
  {
    File baseDir = new File("fileSystemTest/app2.zip");
    ICloseableDirectory dir = FileSystem.getFSRoot(new FileInputStream(baseDir));

    try {
      File desiredFile = new File(new File(getTestResourceDir(), "/app1"), "META-INF/APPLICATION.MF");

      runBasicDirTest(dir, desiredFile.length(), desiredFile.lastModified());
      runBasicDirTest(dir.toCloseable(), desiredFile.length(), desiredFile.lastModified());
    } finally {
      dir.close();
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
	  zip.close();
	  long duration = System.currentTimeMillis() - start;


	  // normal zip files

	  ICloseableDirectory dir = FileSystem.getFSRoot(baseDir).toCloseable();

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

	  IDirectory outer = FileSystem.getFSRoot(new File("fileSystemTest/outer.zip"));
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
  public static void writeEnties(ZipOutputStream zos, File f, int index) throws IOException {
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
  public void runBasicRootDirTests(IDirectory dir, long len, long time) throws IOException
  {
    assertEquals("The root file system name is not correct", "", dir.getName());
    assertEquals("The size of the file is not correct", len, dir.getSize());

    // This assertion just isn't working on Hudson as of build #79
    // assertEquals("The last modified time of the file is not correct", time, dir.getLastModified());

    assertNull("I managed to get a parent of a root", dir.getParent());
    assertTrue("The root dir does not know it is a dir", dir.isDirectory());
    assertFalse("The root dir has an identity crisis and thinks it is a file", dir.isFile());

    dir.open();
  }

  private void runBasicDirTest(IDirectory dir, long len, long time) throws IOException
  {
	  runBasicDirTest(dir, "", len, time);
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
  private void runBasicDirTest(IDirectory dir, String namePrefix, long len, long time) throws IOException
  {
    assertNull("for some reason our fake app has a fake blueprint file.", dir.getFile("OSGI-INF/blueprint/aries.xml"));

    IFile file = dir.getFile("META-INF/APPLICATION.MF");

    assertNotNull("we could not find the application manifest", file);

    assertNull(file.convert());
    assertNull(file.convertNested());

    assertEquals(namePrefix+"META-INF/APPLICATION.MF", file.getName().replace('\\', '/'));
    assertTrue("The last update time is not within 2 seconds of the expected value. Expected: " + time + " Actual: " + file.getLastModified(), Math.abs(time - file.getLastModified()) < 2000);

    assertEquals(len, file.getSize());
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
