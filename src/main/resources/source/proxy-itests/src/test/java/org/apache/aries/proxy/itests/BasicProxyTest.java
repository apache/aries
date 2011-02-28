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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.proxy.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
public class BasicProxyTest 
{
  private final class TestCallable implements Callable<Object> {
    private List<?> list = new ArrayList<Object>();

    public Object call() throws Exception {
      return list;
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testEquals() throws Exception
  {
    ProxyManager mgr = getService(ProxyManager.class);
    Bundle b = FrameworkUtil.getBundle(this.getClass());
    
    Callable<Object> c = new TestCallable();
    Callable<Object> c2 = new TestCallable();
    ((List<Object>)c2.call()).add("Some test data");
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(List.class);
    Object proxy = mgr.createProxy(b, classes, c);
    Object otherProxy = mgr.createProxy(b, classes, c);
    Object totallyOtherProxy = mgr.createProxy(b, classes, c2);
    assertTrue("The object is not equal to itself", proxy.equals(proxy));
    assertTrue("The object is not equal to another proxy of itself", proxy.equals(otherProxy));
    assertFalse("The object is equal to proxy to another object", proxy.equals(totallyOtherProxy));
  }
  
  /**
   * This test does two things. First of all it checks that we throw a FinalModifierException if we
   * try to proxy a final class. It also validates that the message and toString in the exception
   * works as expected.
   */
  @Test
  public void checkProxyFinalClass() throws UnableToProxyException
  {
    ProxyManager mgr = getService(ProxyManager.class);
    Bundle b = FrameworkUtil.getBundle(this.getClass());
    Callable<Object> c = new TestCallable();
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(TestCallable.class);
    try {
      mgr.createProxy(b, classes, c);
    } catch (FinalModifierException e) {
      String msg = e.getMessage();
      assertEquals("The message didn't look right", "The class " + TestCallable.class.getName() + " is final.", msg);
      assertTrue("The message didn't appear in the toString", e.toString().endsWith(msg));
    }
  }
  
  /**
   * This method checks that we correctly fail to proxy a class with final methods.
   * It also does a quick validation on the exception message.
   */
  @Test
  public void checkProxyFinalMethods() throws UnableToProxyException
  {
    ProxyManager mgr = getService(ProxyManager.class);
    Bundle b = FrameworkUtil.getBundle(this.getClass());
    Callable<Object> c = new TestCallable();
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    Runnable r = new Runnable() {
      public final void run() {
      }
    };
    classes.add(r.getClass());
    try {
      mgr.createProxy(b, classes, c);
    } catch (FinalModifierException e) {
      assertTrue("The methods didn't appear in the message", e.getMessage().contains("run"));
    }
  }
  
  private <T> T getService(Class<T> clazz) 
  {
    BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    ServiceReference ref = ctx.getServiceReference(ProxyManager.class.getName());
    if (ref != null) {
      return clazz.cast(ctx.getService(ref));
    }
    return null;
  }
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
      Option[] options = options(
          // Log
          mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
          mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
          // Felix Config Admin
          mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
          // Felix mvn url handler
          mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),


          // this is how you set the default log level when using pax logging (logProfile)
          systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

          // Bundles
          mavenBundle("org.apache.aries", "org.apache.aries.util"),
          mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
          mavenBundle("asm", "asm-all"),
          // don't install the blueprint sample here as it will be installed onto the same framework as the blueprint core bundle
          // mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").noStart(),
          mavenBundle("org.osgi", "org.osgi.compendium"),
//          org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

          equinox().version("3.5.0")
      );

      options = updateOptions(options);
      return options;
  }
  
  public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) 
  {
    return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
  }
  protected static Option[] updateOptions(Option[] options) 
  {
      // We need to add pax-exam-junit here when running with the ibm
      // jdk to avoid the following exception during the test run:
      // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
      if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
          Option[] ibmOptions = options(
              wrappedBundle(mavenBundle("org.ops4j.pax.exam", "pax-exam-junit"))
          );
          options = combine(ibmOptions, options);
      }
  
      return options;
  }
}