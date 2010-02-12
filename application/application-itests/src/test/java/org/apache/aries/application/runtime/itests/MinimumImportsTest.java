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

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.apache.aries.application.management.ApplicationContext;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;


@RunWith(JUnit4TestRunner.class)
public class MinimumImportsTest extends AbstractIntegrationTest {
  
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
      .jar("org.apache.aries.application.itests.minimports.jar")
        .manifest().symbolicName("org.apache.aries.application.itests.minimports")
          .attribute("Bundle-Version", "1.0.0")
          .attribute("Import-Package", "org.apache.aries.application.management")
// use this line instead of the one above to workaround ARIES-159
//          .attribute("Import-Package", "org.apache.aries.application.management,org.apache.aries.application.filesystem")
          .end()
        .binary("org/apache/aries/application/sample/appmgrclient/AppMgrClient.class", 
            MinimumImportsTest.class.getClassLoader().getResourceAsStream("org/apache/aries/application/sample/appmgrclient/AppMgrClient.class"))
        .binary("OSGI-INF/blueprint/app-mgr-client.xml", 
            MinimumImportsTest.class.getClassLoader().getResourceAsStream("app-mgr-client.xml"))
        .end();
      
    FileOutputStream fout = new FileOutputStream("appmgrclienttest.eba");
    testEba.writeOut(fout);
    fout.close();
    
    createdApplications = true;
  }
  
  public static class AppMgrClientBlueprintListener implements BlueprintListener {
    
    Boolean success = null;
    
    public void blueprintEvent(BlueprintEvent event) {
      if (event.getBundle().getSymbolicName().equals(
          "org.apache.aries.application.itests.minimports")) {
        if (event.getType() == event.FAILURE) {
          success = Boolean.FALSE;
        }
        if (event.getType() == event.CREATED) {
          success = Boolean.TRUE;
        }
      }
    }
  }
  
  @Test
  public void testAppUsingAriesApplicationManager() throws Exception {
    
    // Register a BlueprintListener to listen for the events from the BlueprintContainer for the bundle in the appmgrclienttest.eba
    
    AppMgrClientBlueprintListener acbl = new AppMgrClientBlueprintListener();
    ServiceRegistration sr = bundleContext.registerService("org.osgi.service.blueprint.container.BlueprintListener", acbl, null);
    
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("appmgrclienttest.eba")));
    ApplicationContext ctx = manager.install(app);
    ctx.start();
    
    int sleepfor = 3000;
    while ((acbl.success == null || acbl.success == false) && sleepfor > 0) {
      Thread.sleep(100);
      sleepfor-=100;
    }
    assertNotNull("Timed out - didn't receive Blueprint CREATED or FAILURE event", acbl.success);
    assertTrue("Recevied Blueprint FAILURE event", acbl.success);
    
    ctx.stop();
    manager.uninstall(ctx);
    sr.unregister();
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
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
        
        /* For debugging, uncomment the next two lines
        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
        waitForFrameworkStartup(),
        
        and add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
        */

        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}
