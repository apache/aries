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
package org.apache.aries.application.runtime.itests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.isolated.sample.HelloWorld;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.util.tracker.ServiceTracker;
@RunWith(JUnit4TestRunner.class)
public class IsolatedRuntimeTest extends AbstractIntegrationTest {
  
  /* Use @Before not @BeforeClass so as to ensure that these resources
   * are created in the paxweb temp directory, and not in the svn tree 
   */
  static boolean createdApplications = false;
  @Before
  public static void createApplications() throws Exception {
       
    if (createdApplications) { 
      return;
    }
    ZipFixture testEba = ArchiveFixture.newZip()
      .jar("sample.jar")
        .manifest().symbolicName("org.apache.aries.isolated.sample")
          .attribute("Bundle-Version", "1.0.0")
          .end()
        .binary("org/apache/aries/isolated/sample/HelloWorld.class", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorld.class"))
        .binary("org/apache/aries/isolated/sample/HelloWorldImpl.class", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorldImpl.class"))
        .binary("OSGI-INF/blueprint/sample-blueprint.xml", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("isolated/sample-blueprint.xml"))
        .end();
      
    FileOutputStream fout = new FileOutputStream("test.eba");
    testEba.writeOut(fout);
    fout.close();
    
    ZipFixture testEba2 = testEba.binary("META-INF/APPLICATION.MF", 
        IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("isolated/APPLICATION.MF"))
        .end();
    fout = new FileOutputStream("test2.eba");
    testEba2.writeOut(fout);
    fout.close();
    createdApplications = true;
  }
  
  @Test
  public void testAppWithoutApplicationManifest() throws Exception {
    
    String result = null;
    
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test.eba")));
    AriesApplicationContext ctx = manager.install(app);
    
    try
    {
      ctx.start();
      assertHelloWorldService("test.eba");
    } finally {
      ctx.stop();
      manager.uninstall(ctx);
    }
  }
  
  @Test
  public void testAppWithApplicationManifest() throws Exception {
        
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    AriesApplicationContext ctx = manager.install(app);
    
    try
    {
      ctx.start();
      
      assertHelloWorldService("org.apache.aries.sample2");
      
    } finally {
      ctx.stop();
      manager.uninstall(ctx);
    }
  }
  
  @Test
  public void testUninstallReinstall() throws Exception {
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    AriesApplicationContext ctx = manager.install(app);
    
    app = ctx.getApplication();
    
    try
    {
      ctx.start();
      
      assertHelloWorldService("org.apache.aries.sample2");
      
      ctx.stop();
      manager.uninstall(ctx);
      
      ctx = manager.install(app);
      ctx.start();
      
      assertHelloWorldService("org.apache.aries.sample2");
      
    } finally {
      ctx.stop();
      manager.uninstall(ctx);
    }    
  }

  
  
  private void assertHelloWorldService(String appName) throws Exception
  {
    BundleContext appContext = getAppIsolatedBundleContext(appName);
    
    if (appContext != null) {  
      // Dive into the context and pull out the composite bundle for the app
      Filter osgiFilter = FrameworkUtil.createFilter("(" + Constants.OBJECTCLASS + "=" + HelloWorld.class.getName() + ")");
      ServiceTracker tracker = new ServiceTracker(appContext, 
          osgiFilter,
          null);
      tracker.open();
      
      Object hw = tracker.waitForService(DEFAULT_TIMEOUT);
      
      tracker.close();
      
      // We can cast to our version of HelloWorld as it is in a different classloader
      // so reflect into it to get the single method
      Class returnClass = hw.getClass();
      Method method = returnClass.getDeclaredMethod("getMessage",null);
      String result = (String)method.invoke(hw);      
      
      assertEquals("hello world", result);
    }
    else {
      fail("No service found inside application framework");
    }
    
  }
  
  private BundleContext getAppIsolatedBundleContext(String appName)
  {
    for (Bundle sharedBundle : bundleContext.getBundles())
    {
      if (sharedBundle.getSymbolicName().equals("shared.bundle.framework"))
      {
        BundleContext sharedContext = ((CompositeBundle)sharedBundle).getCompositeFramework().getBundleContext();
        for (Bundle appBundle : sharedContext.getBundles())
        {
          if (appBundle.getSymbolicName().equals(appName))
          {
            return ((CompositeBundle)appBundle).getCompositeFramework().getBundleContext();
          }
        }
        break;
      }
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

        // this is how you set the default log level when using pax
        // logging (logProfile)
        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

        // Bundles
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.isolated"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.repository"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.geronimo.specs","geronimo-jta_1.1_spec"),
        mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),

        /* For debugging, uncommenting the following two lines and add the imports */
        /*
         * vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5011"),
        waitForFrameworkStartup(),*/

        /*
         * and add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
         */
        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}
