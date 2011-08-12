/*  Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.aries.ejb.container.itest;

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.apache.aries.itest.ExtraOptions.transactionBootDelegation;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ServiceModeller;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class EJBModellingTest extends AbstractIntegrationTest {

  private ModelledResourceManager mrm;
  private ModellingManager mm;

  @Before
  public void setup() {
    mrm = context().getService(ModelledResourceManager.class);
    mm = context().getService(ModellingManager.class);
  }
  
  @Test
  public void modelBasicEJBBundleWithXML() throws Exception {
    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_1.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    zos.close();
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 2, services.size());
        
    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(Collections.disjoint(services, getAnnotatedservices()));
  }
  
  @Test
  public void testEJBJARAndAnnotatedInZip() throws Exception {
    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_1.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    addToZip(zos, "beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 3, services.size());

    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(services.containsAll(getAnnotatedservices()));
  }
  
  @Test
  public void testAnnotatedOnlyInZip() throws Exception {
    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_1.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 1, services.size());
    
    assertTrue(Collections.disjoint(services, getXMLservices()));
    assertTrue(services.containsAll(getAnnotatedservices()));
  }
  
  @Test
  public void testEJBJARAndAnnotatedNotOnClasspathInZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_2.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    addToZip(zos, "beans/StatelessSessionBean.class", "no/beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class", "no/beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 2, services.size());
    
    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(Collections.disjoint(services, getAnnotatedservices()));
  }

  @Test
  public void testEJBJARAndAnnotatedOnClasspathInZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_2.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    addToZip(zos, "beans/StatelessSessionBean.class", "yes/beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class", "yes/beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 3, services.size());
    
    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(services.containsAll(getAnnotatedservices()));
  }
  
  @Test
  public void testEJBJARInWebZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 2, services.size());
    
    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(Collections.disjoint(services, getAnnotatedservices()));
  }

  @Test
  public void testEJBJARInWrongPlaceWebZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 0, services.size());
  }
  
  @Test
  public void testEJBJARAndAnnotatedInWebZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
    addToZip(zos, "beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 3, services.size());
    
    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(services.containsAll(getAnnotatedservices()));
  }
  
  @Test
  public void testAnnotatedOnlyInWebZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 1, services.size());
    
    assertTrue(Collections.disjoint(services, getXMLservices()));
    assertTrue(services.containsAll(getAnnotatedservices()));
  }
  
  @Test
  public void testEJBJARAndAnnotatedNotOnClasspathInWebZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_4.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
    addToZip(zos, "beans/StatelessSessionBean.class", "no/beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class", "no/beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 2, services.size());
    
    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(Collections.disjoint(services, getAnnotatedservices()));
  }
  
  @Test
  public void testEJBJARAndAnnotatedOnClasspathInWebZip() throws Exception {

    context().getService(ServiceModeller.class);
    ModelledResource mr = null;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    addToZip(zos, "MANIFEST_4.MF", "META-INF/MANIFEST.MF");
    addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
    addToZip(zos, "beans/StatelessSessionBean.class", "yes/beans/StatelessSessionBean.class");
    addToZip(zos, "beans/StatefulSessionBean.class", "yes/beans/StatefulSessionBean.class");
    zos.close();
    
    mr = model(baos.toByteArray());

    Collection<? extends ExportedService> services = 
      mr.getExportedServices(); 
    
    assertEquals("Wrong number of services", 3, services.size());
    
    assertTrue(services.containsAll(getXMLservices()));
    assertTrue(services.containsAll(getAnnotatedservices()));
  }

  private ModelledResource model(byte[] bytes) throws ModellerException {
    ICloseableDirectory dir = null;
    try {
      dir = FileSystem.getFSRoot(
           new ByteArrayInputStream(bytes));
      return mrm.getModelledResource(dir);
    } finally {
      IOUtils.close(dir);
    }
  }
  
  private Collection<ExportedService> getXMLservices() {
    Map<String, Object> serviceProperties = new HashMap<String, Object>();
    serviceProperties.put("ejb.name", "XML");
    serviceProperties.put("ejb.type", "Singleton");
    serviceProperties.put("service.exported.interfaces", "remote.Iface");
    
    Map<String, Object> serviceProperties2 = new HashMap<String, Object>();
    serviceProperties2.put("ejb.name", "XML");
    serviceProperties2.put("ejb.type", "Singleton");
    
    return Arrays.asList(mm.getExportedService("XML", 0, 
        Arrays.asList("remote.Iface"), serviceProperties), mm.getExportedService(
            "XML", 0, Arrays.asList("local.Iface"), serviceProperties2));
  }
  
  private Collection<ExportedService> getAnnotatedservices() {
    Map<String, Object> serviceProperties = new HashMap<String, Object>();
    serviceProperties.put("ejb.name", "Annotated");
    serviceProperties.put("ejb.type", "Stateless");
    
    return Arrays.asList(mm.getExportedService("Annotated", 0, 
        Arrays.asList("beans.StatelessSessionBean"), serviceProperties));
  }
  
  private void addToZip(ZipOutputStream zos, String src) throws IOException {
    addToZip(zos, src, src);
  }
  
  private void addToZip(ZipOutputStream zos, String src, String outLocation) throws IOException {
    zos.putNextEntry(new ZipEntry(outLocation));
    IOUtils.copy(getClass().getClassLoader().
        getResourceAsStream(src), zos);
    zos.closeEntry();
  }

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    return testOptions(
        paxLogging("DEBUG"),
        transactionBootDelegation(),
        vmOption("-Dorg.osgi.framework.system.packages.extra=sun.misc"),
        // Bundles
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.ejb", "org.apache.aries.ejb.modeller"),
        mavenBundle("org.apache.openejb", "openejb-core"),
        mavenBundle("org.apache.openejb", "openejb-api"),
        mavenBundle("org.apache.openejb", "openejb-javaagent"),
        mavenBundle("org.apache.openejb", "openejb-jee"),
        mavenBundle("org.apache.openejb", "openejb-loader"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-impl"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-spi"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-ee"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-ejb"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-web"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.wsdl4j-1.6.1"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-annotation_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-ejb_3.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jaxrpc_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-atinject_1.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-saaj_1.3_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-activation_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-management_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-connector_1.6_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jacc_1.4_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jaxrs_1.1_spec"),
        mavenBundle("commons-cli", "commons-cli"),
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-beanutils", "commons-beanutils"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("org.apache.geronimo.components", "geronimo-connector"),
        mavenBundle("org.apache.geronimo.components", "geronimo-transaction"),
        mavenBundle("org.apache.geronimo.bundles", "scannotation"),
        mavenBundle("org.apache.xbean", "xbean-asm-shaded"),
        mavenBundle("org.apache.xbean", "xbean-finder-shaded"),
        mavenBundle("org.apache.xbean", "xbean-naming"),
        mavenBundle("org.apache.xbean", "xbean-reflect"),
//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
//        waitForFrameworkStartup(),
        

        equinox().version("3.5.0"));
  }

}
