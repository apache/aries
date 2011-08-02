/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.modelling.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.ejb.modelling.EJBRegistry;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.BundleManifest;
import org.junit.Before;
import org.junit.Test;

public class EJBLocatorTest {

  private EJBRegistry registry;
  
  @Before
  public void setup() {
    registry = Skeleton.newMock(EJBRegistry.class);
  }
  
  @Test(expected=ModellerException.class)
  public void testUnavailable() throws ModellerException {
    new EJBLocationUnavailable().findEJBs(null, null, null);
  }
  
  @Test
  public void testEJBJARInZip() throws Exception {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    zos.close();
    
    runTest(baos.toByteArray(), "MANIFEST_1.MF");
    
    assertXML(true);
    assertAnnotation(false);
  }

  @Test
  public void testEJBJARAndAnnotatedInZip() throws Exception {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    addToZip(zos, "test/ejbs/StatelessSessionBean.class");
    addToZip(zos, "test/ejbs/StatefulSessionBean.class");
    zos.close();
    
    runTest(baos.toByteArray(), "MANIFEST_1.MF");
    
    assertXML(true);
    assertAnnotation(true);
  }
  
  @Test
  public void testAnnotatedOnlyInZip() throws Exception {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "test/ejbs/StatelessSessionBean.class");
    addToZip(zos, "test/ejbs/StatefulSessionBean.class");
    zos.close();
    
    runTest(baos.toByteArray(), "MANIFEST_1.MF");
    
    assertXML(false);
    assertAnnotation(true);
  }
  
  @Test
  public void testEJBJARAndAnnotatedNotOnClasspathInZip() throws Exception {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    addToZip(zos, "test/ejb/StatelessSessionBean.class", "no/test/ejb/StatelessSessionBean.class");
    addToZip(zos, "test/ejb/StatefulSessionBean.class", "no/test/ejb/StatefulSessionBean.class");
    zos.close();
    
    runTest(baos.toByteArray(), "MANIFEST_2.MF");
    
    assertXML(true);
    assertAnnotation(false);
  }

  private void runTest(byte[] zip, String manifest) throws ModellerException,
      IOException {
    ICloseableDirectory icd = FileSystem.getFSRoot(new 
        ByteArrayInputStream(zip));
    new OpenEJBLocator().findEJBs(new BundleManifest(getClass().getClassLoader().
        getResourceAsStream(manifest)), icd, registry);
    icd.close();
  }
  
  private void addToZip(ZipOutputStream zos, String src) throws IOException {
    addToZip(zos, src, src);
  }
  
  private void addToZip(ZipOutputStream zos, String src, String outLocation) throws IOException {
    zos.putNextEntry(new ZipEntry(outLocation));
    IOUtils.copy(getClass().getClassLoader().
        getResourceAsStream("ejb-jar.xml"), zos);
    zos.closeEntry();
  }
  
  private void assertXML(boolean b) {

    Skeleton s = Skeleton.getSkeleton(registry);
    MethodCall mc = new MethodCall(EJBRegistry.class, "addEJBView",
        "XML", "SINGLETON", "local.Iface", false);
    
    if(b)
      s.assertCalledExactNumberOfTimes(mc, 1);
    else
      s.assertNotCalled(mc);
    
    mc = new MethodCall(EJBRegistry.class, "addEJBView",
        "XML", "SINGLETON", "remote.Iface", true);
    
    if(b)
      s.assertCalledExactNumberOfTimes(mc, 1);
    else
      s.assertNotCalled(mc);
  }

  private void assertAnnotation(boolean b) {

    Skeleton s = Skeleton.getSkeleton(registry);
    MethodCall mc = new MethodCall(EJBRegistry.class, "addEJBView",
        "Annotated", "STATELESS", "test.ejbs.StatelessSessionBean", false);
    
    if(b)
      s.assertCalledExactNumberOfTimes(mc, 1);
    else
      s.assertNotCalled(mc);
    
    mc = new MethodCall(EJBRegistry.class, "addEJBView",
        String.class, "STATEFUL", String.class, boolean.class);
    
    if(b)
      s.assertCalledExactNumberOfTimes(mc, 1);
    else
      s.assertNotCalled(mc);
  }
  
}
