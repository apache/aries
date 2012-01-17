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

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.apache.aries.itest.ExtraOptions.transactionBootDelegation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
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

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration()
  {
    return testOptions(
        paxLogging("DEBUG"),
        transactionBootDelegation(),

        // Bundles
        mavenBundle("org.eclipse.equinox", "cm"),
        mavenBundle("org.eclipse.osgi", "services"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec"),

        mavenBundle("org.ops4j.pax.web", "pax-web-extender-war"),
        mavenBundle("org.ops4j.pax.web", "pax-web-jetty-bundle"),

        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi"),

        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url.itest.web"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url.itest.biz"),
        mavenBundle("asm", "asm-all"),

        /* For debugging, uncomment the next two lines */
        // org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777"),
        // org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup(),
        equinox().version("3.5.0"));
  }
}
