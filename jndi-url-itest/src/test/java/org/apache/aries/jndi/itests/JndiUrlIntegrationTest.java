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

package org.apache.aries.jndi.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class JndiUrlIntegrationTest extends AbstractIntegrationTest {

  /**
   * This test exercises the blueprint:comp/ jndi namespace by driving
   * a Servlet which then looks up some blueprint components from its own
   * bundle, including a reference which it uses to call a service from a 
   * second bundle.  
   * @throws Exception
   */
  @Test
  public void testBlueprintCompNamespaceWorks() throws Exception { 

    Bundle bBiz = getInstalledBundle("org.apache.aries.jndi.url.itest.biz");
    assertNotNull(bBiz);
    
    Bundle bweb = getInstalledBundle("org.apache.aries.jndi.url.itest.web");
    assertNotNull(bweb);
    
    printBundleStatus ("Before making web request");
    try { 
      Thread.sleep(5000);
    } catch (InterruptedException ix) {}
    
    System.out.println("In test and trying to get connection....");
    String response = getTestServletResponse();
    System.out.println("Got response `" + response + "`");
    assertEquals("ITest servlet response wrong", "Mark.2.0.three", response);
  }
  
  private void printBundleStatus (String msg) { 
    System.out.println("-----\nprintBundleStatus: " + msg + "\n-----");
    for (Bundle b : bundleContext.getBundles()) { 
      System.out.println (b.getSymbolicName() + " " + "state=" + formatState(b.getState()));
    }
    System.out.println();
  }
  
  private String formatState (int state) {
    String result = Integer.toString(state);
    switch (state) { 
    case Bundle.ACTIVE: 
      result = "Active";
      break;
    case Bundle.INSTALLED: 
      result = "Installed";
      break;
    case Bundle.RESOLVED: 
      result = "Resolved";
      break;
    }
    return result;
  }
  
  private String getTestServletResponse() throws IOException { 
    HttpURLConnection conn = makeConnection("http://localhost:8080/jndiUrlItest/ITestServlet");
    String response = getHTTPResponse(conn).trim();
    return response;
  }
  
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration()
  {
    Option[] options = options(
        vmOption("-Dorg.osgi.framework.system.packages=javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers,javax.transaction;partial=true;mandatory:=partial,javax.transaction.xa;partial=true;mandatory:=partial"),
        // Log
        mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
        mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
        // Felix mvn url handler - do we need this?
        mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

        // Bundles
        mavenBundle("org.eclipse.equinox", "cm"),
        mavenBundle("org.eclipse.osgi", "services"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec"),

        mavenBundle("org.ops4j.pax.web", "pax-web-extender-war"),
        mavenBundle("org.ops4j.pax.web", "pax-web-jetty-bundle"),
        
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),
      
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url.itest.web"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url.itest.biz"),
        mavenBundle("asm", "asm-all"),
        
        /* For debugging, uncomment the next two lines */
        // vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777"),
        // waitForFrameworkStartup(),
        /*
         * For debugging, add these imports: 
         * import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup; 
         * import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
         */
        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}
