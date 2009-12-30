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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.zip.ZipFile;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class IOUtilsTest
{
  @BeforeClass 
  public static void setup()
  { 
    new File("ioUtilsTest").mkdir();
  }
  
  @AfterClass
  public static void cleanUp()
  {
    new File("ioUtilsTest/test.zip").delete();
    IOUtils.deleteRecursive(new File("ioUtilsTest"));
  }
  
  @Test
  public void testZipUp() throws IOException
  {
    IOUtils.zipUp(new File("../src/test/resources/zip"), new File("ioUtilsTest/test.zip"));
    
    ZipFile zip = new ZipFile("ioUtilsTest/test.zip");
    assertNotNull(zip.getEntry("file.txt"));
    assertNotNull(zip.getEntry("subdir/someFile.txt"));
    zip.close();
  }
  
  @Test
  public void testWriteOut() throws IOException
  {
    File tmpDir = new File("ioUtilsTest/tmp");
    tmpDir.mkdirs();
    
    IOUtils.writeOut(tmpDir, "simple.txt", new StringBufferInputStream("abc"));
    IOUtils.writeOut(tmpDir, "some/relative/directory/complex.txt", new StringBufferInputStream("def"));
    IOUtils.writeOut(tmpDir, "some/relative/directory/complex2.txt", new StringBufferInputStream("ghi"));
    
    File simple = new File("ioUtilsTest/tmp/simple.txt");
    assertTrue(simple.exists());

    File complex = new File("ioUtilsTest/tmp/some/relative/directory/complex.txt");
    assertTrue(complex.exists());

    File complex2 = new File("ioUtilsTest/tmp/some/relative/directory/complex2.txt");
    assertTrue(complex2.exists());
    
    BufferedReader r = new BufferedReader(new FileReader(simple));
    assertEquals("abc", r.readLine());
    assertNull(r.readLine());
    r.close();
    
    r = new BufferedReader(new FileReader(complex));
    assertEquals("def", r.readLine());
    assertNull(r.readLine());
    r.close();

    r = new BufferedReader(new FileReader(complex2));
    assertEquals("ghi", r.readLine());
    assertNull(r.readLine());
    r.close();
  }
}

