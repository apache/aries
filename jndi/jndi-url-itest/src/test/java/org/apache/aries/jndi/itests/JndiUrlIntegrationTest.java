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
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JndiUrlIntegrationTest extends AbstractIntegrationTest {

  private static final int CONNECTION_TIMEOUT = 10000;
  

    
  /**
   * This test exercises the blueprint:comp/ jndi namespace by driving
   * a Servlet which then looks up some blueprint components from its own
   * bundle, including a reference which it uses to call a service from a 
   * second bundle.  
   * @throws Exception
   */
  @Test
  public void testBlueprintCompNamespaceWorks() throws Exception { 

    Bundle bBiz = context().getBundleByName("org.apache.aries.jndi.url.itest.biz");
    assertNotNull(bBiz);
    
    Bundle bweb = context().getBundleByName("org.apache.aries.jndi.url.itest.web");
    assertNotNull(bweb);
    context().getBundleByName("org.ops4j.pax.web.pax-web-extender-war").start();
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
  
  private static HttpURLConnection makeConnection(String contextPath) throws IOException
  {
    URL url = new URL(contextPath);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setConnectTimeout(CONNECTION_TIMEOUT);
    conn.connect();

    return conn;
  }
  
  private static String getHTTPResponse(HttpURLConnection conn) throws IOException
  {
    StringBuilder response = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "ISO-8859-1"));
    try {
      for (String s = reader.readLine(); s != null; s = reader.readLine()) {
        response.append(s).append("\r\n");
      }
    } finally {
      reader.close();
    }

    return response.toString();
  }
  
  public Option baseOptions() {
      String localRepo = System.getProperty("maven.repo.local");
      if (localRepo == null) {
          localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
      }
      return composite(
              junitBundles(),
              // this is how you set the default log level when using pax
              // logging (logProfile)
              systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
              when(localRepo != null).useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo))
       );
  }
  
  @Configuration
  public Option[] configuration()
  {
    return CoreOptions.options(
    	baseOptions(),
        
        // Bundles
        mavenBundle("org.eclipse.equinox", "cm").versionAsInProject(),
        mavenBundle("org.eclipse.osgi", "services").versionAsInProject(),
        mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec").versionAsInProject(),

        mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").versionAsInProject(),
        mavenBundle("org.ops4j.pax.web", "pax-web-jetty-bundle").versionAsInProject(),
        
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api").versionAsInProject(),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core").versionAsInProject(),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
        mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi").versionAsInProject(),
        
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url.itest.web").versionAsInProject(),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url.itest.biz").versionAsInProject(),
        mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
        mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject()
        );

        // org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777"),
        // org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup(),
  }
}
