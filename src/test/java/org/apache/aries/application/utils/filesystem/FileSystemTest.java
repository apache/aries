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

package org.apache.aries.application.utils.filesystem;

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
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.unittest.junit.Assert;
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
    File baseDir = new File("../src/test/resources/app1");
    File manifest = new File(baseDir, AppConstants.APPLICATION_MF);
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
    File baseDir = new File("../src/test/resources/app1");
    IDirectory dir = FileSystem.getFSRoot(baseDir);

    File desiredFile = new File(baseDir, AppConstants.APPLICATION_MF);
    
    runBasicDirTest(dir, desiredFile.length(), desiredFile.lastModified());
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
   * Make sure we correctly understand the directory structure for zips.
   * 
   * @throws IOException
   */
  @Test
  public void basicDirTestsWithZip() throws IOException
  {
    File baseDir = new File("fileSystemTest/app2.zip");
    IDirectory dir = FileSystem.getFSRoot(baseDir);

    File desiredFile = new File(new File("../src/test/resources/app1"), AppConstants.APPLICATION_MF);
    
    runBasicDirTest(dir, desiredFile.length(), desiredFile.lastModified());
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
    
    int index = new File("../src/test/resources/app1").getAbsolutePath().length();
    
    writeEnties(out, new File("../src/test/resources/app1"), index);
    
    out.close();
  }
  
  /**
   * Make sure the test zip is deleted afterwards.
   */
  @AfterClass
  public static void destroyZip()
  {
    new File("fileSystemTest/app2.zip").delete();
    new File("fileSystemTest").delete();
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
  public void runBasicDirTest(IDirectory dir, long len, long time) throws IOException
  {
    assertNull("for some reason our fake app has a fake blueprint file.", dir.getFile("OSGI-INF/blueprint/aries.xml"));
    
    IFile file = dir.getFile(AppConstants.APPLICATION_MF);
    
    assertNotNull("we could not find the application manifest", file);
    
    assertEquals(AppConstants.APPLICATION_MF, file.getName().replace('\\', '/'));
    assertTrue("The last update time is not within 2 seconds of the expected value. Expected: " + time + " Actual: " + file.getLastModified(), Math.abs(time - file.getLastModified()) < 2000);
    assertEquals(len, file.getSize());
    assertEquals("META-INF", file.getParent().getName());
    assertFalse(file.isDirectory());
    assertTrue(file.isFile());
    
    List<IFile> files = dir.listFiles();
    Iterator<IFile> it = files.iterator();
    while (it.hasNext()) { 
      IFile f = it.next();
      if (f.getName().equalsIgnoreCase(".svn")) { 
        it.remove();
      }
    }
    
    assertEquals(1, files.size());
    
    IFile metaInf = files.get(0);
    
    assertTrue(metaInf.isDirectory());
    assertEquals("META-INF", metaInf.getName());
    assertNotNull(metaInf.convert());
    
    for (IFile aFile : dir) {
      if (!aFile.getName().equalsIgnoreCase(".svn")) { 
        assertTrue(aFile.isDirectory());
        assertEquals("META-INF", aFile.getName());
        assertNotNull(aFile.convert());
      }
    }
    
    InputStream is = file.open();
    
    Manifest man = new Manifest(is);
    //remember to close the input stream after use
    is.close();
    assertEquals("com.travel.reservation", man.getMainAttributes().getValue("Application-SymbolicName"));
    
    IFile applicationMF2 = dir.getFile(AppConstants.APPLICATION_MF);
    
    Assert.assertEqualsContract(file, applicationMF2, dir);
    Assert.assertHashCodeEquals(file, applicationMF2, true);
  }
}
