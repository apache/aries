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
package org.apache.aries.application.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;
import org.osgi.framework.Constants;

/**
 * These tests do not cover the complete functionality (as yet). Rather this gives a place for adding
 * smaller tests for individual units of work that don't need to be tested by converting a whole WAR file.
 */
public class WabConverterTest
{
  /**
   * Test that we can handle a null manifest (in case a jar archive was created without manifest)
   */
  @Test
  public void testNullManifest() throws Exception
  {
    WarToWabConverter sut = new WarToWabConverter(makeTestFile(new byte[0]), new Properties());
    
    Manifest res = sut.updateManifest(null);
    Attributes attrs = res.getMainAttributes();
    
    assertTrue(attrs.getValue("Import-Package").contains("javax.servlet"));
  }
  
  @Test
  public void testImportPackageMerge() throws Exception
  {
    WarToWabConverter sut = new WarToWabConverter(makeTestFile(new byte[0]), new Properties());
    
    Manifest input = new Manifest();
    input.getMainAttributes().putValue("Import-Package", "com.ibm.test,javax.servlet.http");
    
    Manifest res = sut.updateManifest(input);
    Attributes attrs = res.getMainAttributes();
    
    assertEquals(
        "com.ibm.test,"+
        "javax.servlet.http,"+
        "javax.servlet;version=2.5,"+
        "javax.el;version=2.1,"+
        "javax.servlet.jsp;version=2.1,"+
        "javax.servlet.jsp.el;version=2.1,"+
        "javax.servlet.jsp.tagext;version=2.1",
        attrs.getValue("Import-Package"));
  }
  
  
  @Test
  public void testAcceptNoManifest() throws Exception
  {
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    JarOutputStream out = new JarOutputStream(bout);
    out.putNextEntry(new ZipEntry("random.html"));
    out.write("hello world".getBytes());
    out.close();
    
    IFile input = makeTestFile(bout.toByteArray());
    
    Properties props = new Properties();
    props.put(Constants.BUNDLE_SYMBOLICNAME, "test.bundle");
    WarToWabConverter sut = new WarToWabConverter(input, props);
    
    Manifest m = new JarInputStream(sut.getWAB()).getManifest();
    assertEquals("test.bundle", m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME));
  }
  
  private IFile makeTestFile(byte[] content) {
    return Skeleton.newMock(new IFileProxy(content), IFile.class);
  }
  
  private static class IFileProxy {
    private byte[] content;
    
    public IFileProxy(byte[] content) {
      this.content = content;
    }
    
    public InputStream open() {
      return new ByteArrayInputStream(content);
    }
    
    public String getName() { return "test.war"; }
  }
  
}
