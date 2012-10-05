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

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.apache.aries.itest.ExtraOptions.*;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.sample.HelloWorld;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.filesystem.FileSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class BasicAppManagerTest extends AbstractIntegrationTest {
  
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
        .manifest().symbolicName("org.apache.aries.sample")
          .attribute("Bundle-Version", "1.0.0")
          .attribute("Import-Package", "org.apache.aries.sample")
          .end()
        .binary("org/apache/aries/sample/impl/HelloWorldImpl.class", 
            BasicAppManagerTest.class.getClassLoader().getResourceAsStream("org/apache/aries/sample/impl/HelloWorldImpl.class"))
        .binary("OSGI-INF/blueprint/sample-blueprint.xml", 
            BasicAppManagerTest.class.getClassLoader().getResourceAsStream("basic/sample-blueprint.xml"))
        .end();
      
    FileOutputStream fout = new FileOutputStream("test.eba");
    testEba.writeOut(fout);
    fout.close();
    
    ZipFixture testEba2 = testEba.binary("META-INF/APPLICATION.MF", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("basic/APPLICATION.MF"))
        .end();
    fout = new FileOutputStream("test2.eba");
    testEba2.writeOut(fout);
    fout.close();
    createdApplications = true;
  }
  
  @Test
  public void testAppWithoutApplicationManifest() throws Exception {
    
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test.eba")));
    
    // application name should be equal to eba name since application.mf is not provided
    assertEquals("test.eba", app.getApplicationMetadata().getApplicationName());
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();
    
    HelloWorld hw = context().getService(HelloWorld.class);
    String result = hw.getMessage();
    assertEquals (result, "hello world");
    
    ctx.stop();
    manager.uninstall(ctx);
  }

  @Test
  public void testAppWithApplicationManifest() throws Exception {
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    
    // application name should equal to whatever Application name provided in the application.mf
    assertEquals("test application 2", app.getApplicationMetadata().getApplicationName());
    
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();
    
    HelloWorld hw = context().getService(HelloWorld.class);
    String result = hw.getMessage();
    assertEquals (result, "hello world");
    
    ctx.stop();
    manager.uninstall(ctx);
  }

  @Test
  public void testAppStore() throws Exception {
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    app = manager.resolve(app);

    app.store(new FileOutputStream("test2-resolved.eba"));

    app = manager.createApplication(FileSystem.getFSRoot(new File("test2-resolved.eba")));

    // application name should equal to whatever Application name provided in the application.mf
    assertEquals("test application 2", app.getApplicationMetadata().getApplicationName());

    AriesApplicationContext ctx = manager.install(app);
    ctx.start();

    HelloWorld hw = context().getService(HelloWorld.class);
    String result = hw.getMessage();
    assertEquals (result, "hello world");

    ctx.stop();
    manager.uninstall(ctx);
  }

  
  private static Option[] generalConfiguration() {
    return testOptions(
        paxLogging("DEBUG"),

        // Bundles
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform"),
        mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
        mavenBundle("org.ow2.asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.osgi", "org.osgi.compendium"));
        
        
        /* For debugging, uncomment the next two lines
        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
        waitForFrameworkStartup(),
        
        and add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
        */

  }

  /*
   * Commented out to avoid an NPE due to a ConcurrentModificationException in
   * the Aries build. See https://issues.apache.org/jira/browse/ARIES-931.
   */
  //@org.ops4j.pax.exam.junit.Configuration
  public static Option[] equinox35Options()
  {
	  return testOptions(
			  generalConfiguration(),
	          equinox().version("3.5.0")
	          );
  }

  /*
   * Commented out to avoid an NPE due to a ConcurrentModificationException in
   * the Aries build. See https://issues.apache.org/jira/browse/ARIES-931.
   */
  //@org.ops4j.pax.exam.junit.Configuration
  public static Option[] equinox37Options()
  {
	  return testOptions(
			  generalConfiguration(),
			  PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),          
	          equinox().version("3.7.0.v20110613")
	          );
  }
  
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] equinox38Options()
  {
	  return testOptions(
			  generalConfiguration(),
			  PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),          
	          equinox().version("3.8.0.V20120529-1548")
	          );
  }

}
