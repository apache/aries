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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.sample.HelloWorld;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class OBRAppManagerTest extends AbstractIntegrationTest {

  /* Use @Before not @BeforeClass so as to ensure that these resources
   * are created in the paxweb temp directory, and not in the svn tree
   */
  static boolean createdApplications = false;
  
  @Before
  public static void createApplications() throws Exception {
	    if (createdApplications) {
	      return;
	    }
	    ZipFixture testBundle = ArchiveFixture.newZip()
	        .manifest().symbolicName("org.apache.aries.sample.bundle")
	          .attribute("Bundle-Version", "1.0.0")
	          .attribute("Import-Package", "org.apache.aries.sample")
	          .attribute("Export-Package", "org.apache.aries.sample.impl")
	          .end()
	        .binary("org/apache/aries/sample/impl/HelloWorldImpl.class",
	            OBRAppManagerTest.class.getClassLoader().getResourceAsStream("org/apache/aries/sample/impl/HelloWorldImpl.class"))
	        .end();

	    FileOutputStream fout = new FileOutputStream("bundle.jar");
	    testBundle.writeOut(fout);
	    fout.close();

	    ZipFixture testEba = ArchiveFixture.newZip()
	      .jar("sample.jar")
	        .manifest().symbolicName("org.apache.aries.sample")
	          .attribute("Bundle-Version", "1.0.0")
	          .attribute("Import-Package", "org.apache.aries.sample.impl,org.apache.aries.sample")
	          .end()
	        .binary("OSGI-INF/blueprint/sample-blueprint.xml",
	            OBRAppManagerTest.class.getClassLoader().getResourceAsStream("basic/sample-blueprint.xml"))
	        .end()
	         .binary("META-INF/APPLICATION.MF",
	        OBRAppManagerTest.class.getClassLoader().getResourceAsStream("basic/APPLICATION.MF"))
	        .end();
	    fout = new FileOutputStream("test.eba");
	    testEba.writeOut(fout);
	    fout.close();
	    
	    StringBuilder repositoryXML = new StringBuilder();
	    
	    BufferedReader reader = new BufferedReader(new InputStreamReader(OBRAppManagerTest.class.getResourceAsStream("/obr/repository.xml")));
	    String line;
	    
	    while ((line = reader.readLine()) != null) {
	      repositoryXML.append(line);
	      repositoryXML.append("\r\n");
	    }
	    
	    String repo = repositoryXML.toString().replaceAll("bundle_location", new File("bundle.jar").toURI().toString());
	    
	    System.out.println(repo);
	    
	    FileWriter writer = new FileWriter("repository.xml");
	    writer.write(repo);
	    writer.close();
	    
	    createdApplications = true;
	  }

	  @Test
	  public void testAppWithApplicationManifest() throws Exception {
	    
	    RepositoryAdmin repositoryAdmin = context().getService(RepositoryAdmin.class);
	    
	    repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

	    Repository[] repos = repositoryAdmin.listRepositories();
	    
	    for (Repository repo : repos) {
	      Resource[] resources = repo.getResources();
	      
	      for (Resource r : resources) {
	        Capability[] cs = r.getCapabilities();
	        
	        for (Capability c : cs) {
	          System.out.println(c.getName() + " : " + c.getProperties());
	        }
	      }
	    }
	    
	    AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
	    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test.eba")));
	    app = manager.resolve(app);
	    //installing requires a valid url for the bundle in repository.xml.
	    AriesApplicationContext ctx = manager.install(app);
	    ctx.start();

	    HelloWorld hw = context().getService(HelloWorld.class);
	    String result = hw.getMessage();
	    assertEquals (result, "hello world");

	    ctx.stop();
	    manager.uninstall(ctx);
	  }

  public static Option[] generalConfiguration() {
    return testOptions(
        paxLogging("DEBUG"),

        // Bundles
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("org.ow2.asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform"),
        mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces"),

        mavenBundle("org.osgi", "org.osgi.compendium")

        //        /* For debugging, uncomment the next two lines
        //        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
        //        waitForFrameworkStartup(),

        /* For debugging, uncomment the next two lines
        and add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
         */

        );
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