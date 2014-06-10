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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipFile;

import org.apache.aries.util.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Test;

public class IOUtilsTest
{
  @AfterClass
  public static void cleanUp()
  {
    new File("ioUtilsTest/test.zip").delete();
    IOUtils.deleteRecursive(new File("ioUtilsTest"));
  }
  
  @Test
  public void testZipUpAndUnzipAndDeleteRecursive() throws IOException
  {
    new File ("ioUtilsTest").mkdir();
    IOUtils.zipUp(new File("../src/test/resources/zip"), new File("ioUtilsTest/test.zip"));
    
    ZipFile zip = new ZipFile("ioUtilsTest/test.zip");
    assertNotNull(zip.getEntry("file.txt"));
    assertNotNull(zip.getEntry("subdir/someFile.txt"));
    zip.close();
    
    IDirectory dir = FileSystem.getFSRoot(new File("ioUtilsTest"));
    IFile izip = dir.getFile("test.zip");
    File output = new File("ioUtilsTest/zipout");
    output.mkdirs();
    IOUtils.unpackZip(izip, output);
    File a = new File(output,"file.txt");
    File b = new File(output,"subdir");
    File c = new File(b,"someFile.txt");
    assertTrue(output.exists());
    assertTrue(a.exists() && a.isFile());
    assertTrue(b.exists() && b.isDirectory());
    assertTrue(c.exists() && c.isFile());
    
    IOUtils.deleteRecursive(output);
    assertFalse(output.exists());
  }
  
  @Test
  public void testWriteOut() throws IOException
  {
    File tmpDir = new File("target/ioUtilsTest/tmp");
    tmpDir.mkdirs();
    
    IOUtils.writeOut(tmpDir, "simple.txt", new ByteArrayInputStream( "abc".getBytes()));
    IOUtils.writeOut(tmpDir, "some/relative/directory/complex.txt", new ByteArrayInputStream( "def".getBytes()));
    IOUtils.writeOut(tmpDir, "some/relative/directory/complex2.txt", new ByteArrayInputStream( "ghi".getBytes()));
    
    File simple = new File(tmpDir, "simple.txt");
    assertTrue(simple.exists());

    File complex = new File(tmpDir, "some/relative/directory/complex.txt");
    assertTrue(complex.exists());

    File complex2 = new File(tmpDir, "some/relative/directory/complex2.txt");
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
  
  @Test 
  public void testWriteOutAndDoNotCloseInputStream() throws IOException{
    InputStream is = new InputStream(){
      int idx=0;
      int data[]=new int[]{1,2,3,4,5,-1};
      @Override
      public int read() throws IOException
      {
        if(idx<data.length)
          return data[idx++];
        else
          return -1;
      }
      @Override
      public void close() throws IOException
      {
        fail("Close was invoked");
      }
    };
    File f = new File("ioUtilsTest/outtest1");
    f.mkdirs();
    IOUtils.writeOutAndDontCloseInputStream(f, "/fred", is);
    File fred = new File(f,"/fred");
    assertTrue(fred.exists());
    File outtest = fred.getParentFile();
    fred.delete();
    outtest.delete();
    
  }
  
  @Test 
  public void testCopy() throws IOException{
    InputStream is = new InputStream(){
      boolean closed=false;
      int idx=0;
      int data[]=new int[]{1,2,3,4,5,-1};
      @Override
      public int read() throws IOException
      {
        if(idx<data.length)
          return data[idx++];
        else
          return -1;
      }
      @Override
      public void close() throws IOException
      {
        closed=true;
      }
      @Override
      public int available() throws IOException
      {
        if(!closed)
          return super.available();
        else
          return 123456789;
      }
      
    };
    
    OutputStream os = new OutputStream(){
      int idx=0;
      int data[]=new int[]{1,2,3,4,5,-1};
      @Override
      public void write(int b) throws IOException
      {
        if(b!=data[idx++]){
          fail("Data written to outputstream was not as expected");
        }
      }
    };
    
    IOUtils.copy(is,os);
    if(is.available()!=123456789){
      fail("close was not invoked");
    }
    
    
  }
  
  @Test
  public void testCopyAndDoNotClose() throws IOException{
    
    InputStream is = new InputStream(){
      int idx=0;
      int data[]=new int[]{1,2,3,4,5,-1};
      @Override
      public int read() throws IOException
      {
        if(idx<data.length)
          return data[idx++];
        else
          return -1;
      }
      @Override
      public void close() throws IOException
      {
        fail("Close invoked");
      }
    };
    
    OutputStream os = new OutputStream(){
      int idx=0;
      int data[]=new int[]{1,2,3,4,5,-1};
      @Override
      public void write(int b) throws IOException
      {
        if(b!=data[idx++]){
          fail("Data written to outputstream was not as expected");
        }
      }
    };
    
    IOUtils.copyAndDoNotCloseInputStream(is,os);
    
  }
}
