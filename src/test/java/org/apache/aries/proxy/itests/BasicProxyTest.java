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
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.apache.aries.itest.ExtraOptions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

@RunWith(JUnit4TestRunner.class)
public class BasicProxyTest extends AbstractProxyTest
{
  /**
   * This test does two things. First of all it checks that we throw a FinalModifierException if we
   * try to proxy a final class. It also validates that the message and toString in the exception
   * works as expected.
   */
  @Test
  public void checkProxyFinalClass() throws UnableToProxyException
  {
    ProxyManager mgr = context().getService(ProxyManager.class);
    Bundle b = FrameworkUtil.getBundle(this.getClass());
    Callable<Object> c = new TestCallable();
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(TestCallable.class);
    try {
      mgr.createDelegatingProxy(b, classes, c, null);
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
    ProxyManager mgr = context().getService(ProxyManager.class);
    Bundle b = FrameworkUtil.getBundle(this.getClass());
    Callable<Object> c = new TestCallable();
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    Runnable r = new Runnable() {
      public final void run() {
      }
    };
    classes.add(r.getClass());
    try {
      mgr.createDelegatingProxy(b, classes, c, null);
    } catch (FinalModifierException e) {
      assertTrue("The methods didn't appear in the message", e.getMessage().contains("run"));
    }
  }
  
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
      return testOptions(
          paxLogging("DEBUG"),

          // Bundles
          mavenBundle("org.apache.aries", "org.apache.aries.util"),
          mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
          mavenBundle("org.ow2.asm", "asm-all"),
          // don't install the blueprint sample here as it will be installed onto the same framework as the blueprint core bundle
          // mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").noStart(),
          mavenBundle("org.osgi", "org.osgi.compendium"),
         /* vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
          waitForFrameworkStartup(),*/
          
          
          
          equinox().version("3.5.0")
      );
  }
}