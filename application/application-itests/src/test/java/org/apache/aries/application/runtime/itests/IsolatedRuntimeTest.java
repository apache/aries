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

import static org.apache.aries.itest.ExtraOptions.flatOptions;
import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.spi.repository.RepositoryGenerator;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.runtime.itests.util.IsolationTestUtils;
import org.apache.aries.isolated.sample.HelloWorld;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.VersionRange;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.framework.CompositeBundle;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
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
          .attribute("Import-Package", "org.osgi.service.blueprint, org.apache.aries.isolated.shared")
          // needed for testFrameworkResolvedBeforeInnerBundlesStart()
          .attribute("Bundle-ActivationPolicy", "lazy")
          .end()
        .binary("org/apache/aries/isolated/sample/HelloWorld.class", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorld.class"))
        .binary("org/apache/aries/isolated/sample/HelloWorldImpl.class", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorldImpl.class"))
        .binary("org/apache/aries/isolated/sample/SharedImpl.class", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/SharedImpl.class"))
        .binary("OSGI-INF/blueprint/sample-blueprint.xml", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("isolated/sample-blueprint.xml"))
        .end()
      .jar("shared.jar")
        .manifest().symbolicName("org.apache.aries.isolated.shared")
          .attribute("Bundle-Version", "1.0.0")
          .attribute("Export-Package", "org.apache.aries.isolated.shared")
        .end()
        .binary("org/apache/aries/isolated/shared/Shared.class",
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/shared/Shared.class"))
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
    
    ZipFixture sampleJar2 = ArchiveFixture.newJar()
      .manifest().symbolicName("org.apache.aries.isolated.sample")
        .attribute("Bundle-Version", "2.0.0")
      .end()
      .binary("org/apache/aries/isolated/sample/HelloWorld.class", 
          IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorld.class"))
      .binary("org/apache/aries/isolated/sample/HelloWorldImpl.class", 
          IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorldImpl.class"))
      .binary("OSGI-INF/blueprint/aries.xml", 
          IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("isolated/sample2-blueprint.xml"))
      .end();
  
    fout = new FileOutputStream("sample_2.0.0.jar");
    sampleJar2.writeOut(fout);
    fout.close();
    
    ZipFixture ebaWithFragment = ArchiveFixture.newZip()
      .jar("sample.jar")
        .manifest().symbolicName("org.apache.aries.isolated.sample")
          .attribute("Bundle-Version", "1.0.0")
          .end()
      .end()
      .jar("fragment.jar")
        .manifest().symbolicName("org.apache.aries.isolated.fragment")
          .attribute("Bundle-Version", "1.0.0")
          .attribute("Fragment-Host", "org.apache.aries.isolated.sample")
        .end()
        .binary("org/apache/aries/isolated/sample/HelloWorld.class", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorld.class"))
        .binary("org/apache/aries/isolated/sample/HelloWorldImpl.class", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorldImpl.class"))
        .binary("OSGI-INF/blueprint/sample-blueprint.xml", 
            IsolatedRuntimeTest.class.getClassLoader().getResourceAsStream("isolated/sample-blueprint.xml"))
        .end();
    
    fout = new FileOutputStream("withFragment.eba");
    ebaWithFragment.writeOut(fout);
    fout.close();
    
    createdApplications = true;
  }
  
  @Test
  public void testAppWithoutApplicationManifest() throws Exception {
    
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test.eba")));
    AriesApplicationContext ctx = manager.install(app);
    
    ctx.start();
    assertHelloWorldService("test.eba");
    
    manager.uninstall(ctx);
  }
  
  @Test
  public void testAppWithApplicationManifest() throws Exception {
        
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    AriesApplicationContext ctx = manager.install(app);
    
    ctx.start();
    assertHelloWorldService("org.apache.aries.sample2");
    
    manager.uninstall(ctx);
  }
  
  @Test
  public void testUninstallReinstall() throws Exception {
    
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    AriesApplicationContext ctx = manager.install(app);
    
    app = ctx.getApplication();

    ctx.start();

    assertHelloWorldService("org.apache.aries.sample2");

    ctx.stop();
    manager.uninstall(ctx);
    
    assertNull(IsolationTestUtils.findIsolatedAppBundleContext(bundleContext, "org.apache.aries.sample2"));

    ctx = manager.install(app);
    ctx.start();

    assertHelloWorldService("org.apache.aries.sample2");
    
    manager.uninstall(ctx);
  }
  
  @Test
  public void testAppWithFragment() throws Exception
  {
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("withFragment.eba")));
    AriesApplicationContext ctx = manager.install(app);

    ctx.start();
    
    assertHelloWorldService("withFragment.eba");
    
    manager.uninstall(ctx);
  }

  @Test
  public void testAppWithGlobalRepositoryBundle() throws Exception
  {
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    
    IsolationTestUtils.prepareSampleBundleV2(bundleContext, 
        context().getService(RepositoryGenerator.class), 
        context().getService(RepositoryAdmin.class), 
        context().getService(ModellingManager.class));

    AriesApplication newApp = manager.resolve(app, new ResolveConstraint() {
      @Override
	public String getBundleName() {
        return "org.apache.aries.isolated.sample";
      }

      @Override
	public VersionRange getVersionRange() {
        return ManifestHeaderProcessor.parseVersionRange("[2.0.0,2.0.0]", true);
      }
    });
    
    AriesApplicationContext ctx = manager.install(newApp);
    ctx.start();
    
    assertHelloWorldService("org.apache.aries.sample2", "hello brave new world");
    
    manager.uninstall(ctx);
  }  
  
  @Test
  public void testFrameworkResolvedBeforeInnerBundlesStart() throws Exception {
      /*
       * Lazy bundles have in the past triggered recursive bundle trackers to handle them before
       * the composite bundle framework was even resolved. In such a case the below loadClass
       * operation on a class that depends on a class imported from the outside of the composite 
       * will fail with an NPE.
       */
      
      final AtomicBoolean loadedClass = new AtomicBoolean(false);
      
      context().addBundleListener(new SynchronousBundleListener() {
        public void bundleChanged(BundleEvent event) {
            Bundle b = event.getBundle();
            if (event.getType() == BundleEvent.STARTING || event.getType() == BundleEvent.LAZY_ACTIVATION) {
                if (b.getEntry("org/apache/aries/isolated/sample/SharedImpl.class") != null) {
                    try {
                        Class<?> cl = b.loadClass("org.apache.aries.isolated.sample.SharedImpl");
                        cl.newInstance();
                        loadedClass.set(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            } else if (event.getType() == BundleEvent.INSTALLED && b instanceof CompositeBundle) {
                ((CompositeBundle) b).getCompositeFramework().getBundleContext().addBundleListener(this);
            }
        }
    });
      
    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    AriesApplicationContext ctx = manager.install(app);      
    
    try {
        ctx.start();
        
        app = ctx.getApplication();
        assertEquals(1, app.getDeploymentMetadata().getApplicationDeploymentContents().size());
        assertEquals(1, app.getDeploymentMetadata().getApplicationProvisionBundles().size());
        assertTrue(loadedClass.get());
    } finally {
        manager.uninstall(ctx);
    }
  }
  
  private void assertHelloWorldService(String appName) throws Exception
  {
    assertHelloWorldService(appName, "hello world");
  }
  
  private void assertHelloWorldService(String appName, String message) throws Exception
  {
    HelloWorld hw = IsolationTestUtils.findHelloWorldService(bundleContext, appName);
    assertNotNull("The Hello World service could not be found.", hw);
    assertEquals(message, hw.getMessage());
  }
  
  private static Option[] generalConfiguration() {
    return flatOptions(
        repository( "http://repository.ops4j.org/maven2" ),
        
        paxLogging("DEBUG"),

        // do not provision against the local runtime
        // Bundles
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("org.ow2.asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.isolated"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.repository"),
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.geronimo.specs","geronimo-jta_1.1_spec")

        /* For debugging, uncommenting the following two lines and add the imports */
        /*
         * vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5011"),
        waitForFrameworkStartup(),*/
//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5011"),
        /*
         * and add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
         */
          );
  }
  
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration()
  {
	  return testOptions(
			  generalConfiguration(),
			  PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties")        
	          );
  }

}
