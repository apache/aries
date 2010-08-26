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
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.RepositoryGenerator;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.aries.sample.HelloWorld;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

@RunWith(JUnit4TestRunner.class)
public class OBRResolverAdvancedTest extends AbstractIntegrationTest 
{
  public static final String CORE_BUNDLE_BY_VALUE = "core.bundle.by.value";
  public static final String CORE_BUNDLE_BY_REFERENCE = "core.bundle.by.reference";
  public static final String TRANSITIVE_BUNDLE_BY_VALUE = "transitive.bundle.by.value";
  public static final String TRANSITIVE_BUNDLE_BY_REFERENCE = "transitive.bundle.by.reference";
  public static final String USE_BUNDLE_BY_REFERENCE = "use.bundle.by.reference";
  public static final String REPO_BUNDLE = "aries.bundle1";
  public static final String HELLO_WORLD_CLIENT_BUNDLE="hello.world.client.bundle";
  public static final String HELLO_WORLD_SERVICE_BUNDLE1="hello.world.service.bundle1";
  public static final String HELLO_WORLD_SERVICE_BUNDLE2="hello.world.service.bundle2";
  
  /* Use @Before not @BeforeClass so as to ensure that these resources
   * are created in the paxweb temp directory, and not in the svn tree
   */
  @Before
  public static void createApplications() throws Exception 
  {
    ZipFixture bundle = ArchiveFixture.newJar().manifest()
                            .attribute(Constants.BUNDLE_SYMBOLICNAME, CORE_BUNDLE_BY_VALUE)
                            .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                            .attribute(Constants.IMPORT_PACKAGE, "a.b.c, p.q.r, x.y.z, javax.naming")
                            .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();

    
    FileOutputStream fout = new FileOutputStream(CORE_BUNDLE_BY_VALUE + ".jar");
    bundle.writeOut(fout);
    fout.close();

    bundle = ArchiveFixture.newJar().manifest()
                            .attribute(Constants.BUNDLE_SYMBOLICNAME, TRANSITIVE_BUNDLE_BY_VALUE)
                            .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                            .attribute(Constants.EXPORT_PACKAGE, "p.q.r")
                            .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();

    fout = new FileOutputStream(TRANSITIVE_BUNDLE_BY_VALUE + ".jar");
    bundle.writeOut(fout);
    fout.close();

    bundle = ArchiveFixture.newJar().manifest()
                            .attribute(Constants.BUNDLE_SYMBOLICNAME, TRANSITIVE_BUNDLE_BY_REFERENCE)
                            .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                            .attribute(Constants.EXPORT_PACKAGE, "x.y.z")
                            .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();
    
    fout = new FileOutputStream(TRANSITIVE_BUNDLE_BY_REFERENCE + ".jar");
    bundle.writeOut(fout);
    fout.close();

    bundle = ArchiveFixture.newJar().manifest()
                            .attribute(Constants.BUNDLE_SYMBOLICNAME, CORE_BUNDLE_BY_REFERENCE)
                            .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                            .attribute(Constants.EXPORT_PACKAGE, "d.e.f")
                            .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();
    
    fout = new FileOutputStream(CORE_BUNDLE_BY_REFERENCE + ".jar");
    bundle.writeOut(fout);
    fout.close();

    
    
    // jar up a use bundle
    bundle = ArchiveFixture.newJar().manifest()
    .attribute(Constants.BUNDLE_SYMBOLICNAME, USE_BUNDLE_BY_REFERENCE)
    .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
    .attribute(Constants.EXPORT_PACKAGE, "a.b.c")
    .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();

    fout = new FileOutputStream(USE_BUNDLE_BY_REFERENCE + ".jar");
    bundle.writeOut(fout);
    fout.close();
    // Create the EBA application
    ZipFixture testEba = ArchiveFixture.newZip()
     .binary("META-INF/APPLICATION.MF",
        OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("obr/APPLICATION-UseBundle.MF"))
        .end()
      .binary(CORE_BUNDLE_BY_VALUE + ".jar", new FileInputStream(CORE_BUNDLE_BY_VALUE + ".jar")).end()
      .binary(TRANSITIVE_BUNDLE_BY_VALUE + ".jar", new FileInputStream(TRANSITIVE_BUNDLE_BY_VALUE + ".jar")).end();

    fout = new FileOutputStream("demo.eba");
    testEba.writeOut(fout);
    fout.close();
    

    //create the bundle
    bundle = ArchiveFixture.newJar()
    .binary("META-INF/MANIFEST.MF", OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("obr/aries.bundle1/META-INF/MANIFEST.MF")).end()
    .binary("OSGI-INF/blueprint/blueprint.xml", OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("obr/hello-world-client.xml")).end();
    fout = new FileOutputStream(REPO_BUNDLE + ".jar");
    bundle.writeOut(fout);
    fout.close();
    

    ///////////////////////////////////////////////
    //create an eba with a helloworld client, which get all 'HelloWorld' services
    //create a helloworld client
    bundle = ArchiveFixture.newJar().manifest()
    .attribute(Constants.BUNDLE_SYMBOLICNAME, HELLO_WORLD_CLIENT_BUNDLE)
    .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
    .attribute("Import-Package", "org.apache.aries.sample")
    .attribute(Constants.BUNDLE_VERSION, "1.0.0").end()
    .binary("org/apache/aries/application/helloworld/client/HelloWorldClientImpl.class", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("org/apache/aries/application/helloworld/client/HelloWorldClientImpl.class"))
    .binary("OSGI-INF/blueprint/helloClient.xml", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("obr/hello-world-client.xml"))
    .end();

    
    fout = new FileOutputStream(HELLO_WORLD_CLIENT_BUNDLE + ".jar");
    bundle.writeOut(fout);
    fout.close();
    
  
    //create two helloworld services
    // create the 1st helloworld service
    bundle = ArchiveFixture.newJar().manifest()
    .attribute(Constants.BUNDLE_SYMBOLICNAME, HELLO_WORLD_SERVICE_BUNDLE1)
    .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
    .attribute("Import-Package", "org.apache.aries.sample")
    .attribute(Constants.BUNDLE_VERSION, "1.0.0").end()
    .binary("org/apache/aries/sample/impl/HelloWorldImpl.class", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("org/apache/aries/sample/impl/HelloWorldImpl.class"))
    .binary("OSGI-INF/blueprint/sample-blueprint.xml", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("basic/sample-blueprint.xml"))
    .end();

    //create the 2nd helloworld service
    fout = new FileOutputStream(HELLO_WORLD_SERVICE_BUNDLE1 + ".jar");
    bundle.writeOut(fout);
    fout.close();
    
    bundle = ArchiveFixture.newJar().manifest()
    .attribute(Constants.BUNDLE_SYMBOLICNAME, HELLO_WORLD_SERVICE_BUNDLE2)
    .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
    .attribute("Import-Package", "org.apache.aries.sample")
    .attribute(Constants.BUNDLE_VERSION, "1.0.0").end()
    .binary("org/apache/aries/sample/impl/HelloWorldImpl.class", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("org/apache/aries/sample/impl/HelloWorldImpl.class"))
    .binary("OSGI-INF/blueprint/sample-blueprint.xml", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("basic/sample-blueprint.xml"))
    .end();

    fout = new FileOutputStream(HELLO_WORLD_SERVICE_BUNDLE2 + ".jar");
    bundle.writeOut(fout);
    fout.close();
    
    //Create a helloworld eba with the client included
    ZipFixture multiServiceHelloEba = ArchiveFixture.newZip()
     .binary(HELLO_WORLD_CLIENT_BUNDLE + ".jar", new FileInputStream(HELLO_WORLD_CLIENT_BUNDLE + ".jar")).end();
     
   fout = new FileOutputStream("hello.eba");
   multiServiceHelloEba.writeOut(fout);
   fout.close();

  }

  @Test(expected=ResolverException.class)
  public void testDemoAppResolveFail() throws ResolverException, Exception
  {
    startApplicationRuntimeBundle();

    generateOBRRepoXML(false, TRANSITIVE_BUNDLE_BY_REFERENCE + ".jar", CORE_BUNDLE_BY_REFERENCE + "_0.0.0.jar",  USE_BUNDLE_BY_REFERENCE+".jar");
    
    RepositoryAdmin repositoryAdmin = getOsgiService(RepositoryAdmin.class);
    
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository repo : repos) {
      repositoryAdmin.removeRepository(repo.getURI());
    }
    
    repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("demo.eba")));
    
    app = manager.resolve(app);
    
    
  }
  
  @Test
  public void testDemoApp() throws Exception 
  {
    startApplicationRuntimeBundle();

    generateOBRRepoXML(false, TRANSITIVE_BUNDLE_BY_REFERENCE + ".jar", CORE_BUNDLE_BY_REFERENCE + ".jar", USE_BUNDLE_BY_REFERENCE+".jar");
    
    RepositoryAdmin repositoryAdmin = getOsgiService(RepositoryAdmin.class);
    
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository repo : repos) {
      repositoryAdmin.removeRepository(repo.getURI());
    }
    
    repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("demo.eba")));
    //installing requires a valid url for the bundle in repository.xml.
    
    app = manager.resolve(app);
    
    DeploymentMetadata depMeta = app.getDeploymentMetadata();
    
    List<DeploymentContent> provision = depMeta.getApplicationProvisionBundles();
    Collection<DeploymentContent> useBundles = depMeta.getDeployedUseBundle();
    Collection<Content> importPackages = depMeta.getImportPackage();
    assertEquals(provision.toString(), 2, provision.size());
    assertEquals(useBundles.toString(), 1, useBundles.size());
    assertEquals(importPackages.toString(), 4, importPackages.size());
    
    List<String> bundleSymbolicNames = new ArrayList<String>();
    
    for (DeploymentContent dep : provision) {
      bundleSymbolicNames.add(dep.getContentName());
    }
    
    assertTrue("Bundle " + TRANSITIVE_BUNDLE_BY_REFERENCE + " not found.", bundleSymbolicNames.contains(TRANSITIVE_BUNDLE_BY_REFERENCE));
    assertTrue("Bundle " + TRANSITIVE_BUNDLE_BY_VALUE + " not found.", bundleSymbolicNames.contains(TRANSITIVE_BUNDLE_BY_VALUE));
    bundleSymbolicNames.clear();
    for (DeploymentContent dep : useBundles) {
      bundleSymbolicNames.add(dep.getContentName());
    }
    assertTrue("Bundle " + USE_BUNDLE_BY_REFERENCE + " not found.", bundleSymbolicNames.contains(USE_BUNDLE_BY_REFERENCE));
    Collection<String> packages = new ArrayList<String>();
    NameValueMap<String, String> maps = new NameValueMap<String, String>();
    maps.put("version", "0.0.0");
    maps.put("bundle-symbolic-name", "use.bundle.by.reference");
    maps.put("bundle-version", "[1.0.0,1.0.0]");
    Content useContent = ManifestHeaderProcessor.parseContent("a.b.c", maps);
    assertTrue("Use Bundle not found in import packags", importPackages.contains(useContent));
    
    for (Content c : importPackages) {
      packages.add(c.getContentName());
    }
    
    assertTrue("package javax.naming not found", packages.contains("javax.naming"));
    assertTrue("package p.q.r not found", packages.contains("p.q.r"));
    assertTrue("package x.y.z not found", packages.contains("x.y.z"));
    assertTrue("package a.b.c not found", packages.contains("a.b.c"));
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();

    Set<Bundle> bundles = ctx.getApplicationContent();
    
    assertEquals("Number of bundles provisioned in the app", 5, bundles.size());
    
    ctx.stop();
    manager.uninstall(ctx);
    
  }

  /* MN: This test generates a new repository.xml and compares it with one we made earlier. 
   * The problem is, the one we made earlier used an IBM JRE, which results in the elements
   * of the repository.xml coming out in a completely different order to those seen on a Sun
   * JRE. The test needs rework if it is going to work correctly on both JREs. 
   */
  @Ignore
  @Test
  public void testRepo() throws Exception {
    startApplicationRuntimeBundle();

    generateOBRRepoXML(true, REPO_BUNDLE+".jar");
    
    // compare the generated with the expected file
    BufferedReader expectedFileReader = new BufferedReader(new InputStreamReader(OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("/obr/aries.bundle1/expectedRepository.xml")));

    // read out the temp file
    BufferedReader reader = new BufferedReader(new FileReader(new File("repository.xml")));

    try {
      String tempFileline, expectedFileLine;
      
      while (((tempFileline = reader.readLine()) != null)
          && ((expectedFileLine = expectedFileReader.readLine()) != null)) {
        if (!(tempFileline.contains("lastmodified"))) {
          assertEquals("The result is not expected.", expectedFileLine, tempFileline);
        }
      }
    } finally {
      expectedFileReader.close();
      reader.close();
    }
  
  }
  
  @Test
  public void testMutlipleServices() throws Exception{
    startApplicationRuntimeBundle();
    generateOBRRepoXML(false, HELLO_WORLD_SERVICE_BUNDLE1 + ".jar", HELLO_WORLD_SERVICE_BUNDLE2 + ".jar");
    
    RepositoryAdmin repositoryAdmin = getOsgiService(RepositoryAdmin.class);
    
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository repo : repos) {
      repositoryAdmin.removeRepository(repo.getURI());
    }
    
    repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("hello.eba")));
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();
    
    // Wait 5 seconds just to give the blueprint-managed beans a chance to come up
    try { 
    	Thread.sleep(5000);
    } catch (InterruptedException ix) {}
    
    HelloWorld hw = getOsgiService(HelloWorld.class);
    String result = hw.getMessage();
    assertEquals (result, "hello world");
    
    
    // Uncomment the block below after https://issues.apache.org/jira/browse/FELIX-2546, 
    // "Only one service is provisioned even when specifying for mulitple services"
    // is fixed. This tracks the problem of provisioning only one service even when we 
    // specify multiple services.
    /* 
     * HelloWorldManager hwm = getOsgiService(HelloWorldManager.class);
     * int numberOfServices = hwm.getNumOfHelloServices();
     * assertEquals(numberOfServices, 2); 
     */
    ctx.stop();
    manager.uninstall(ctx);
   
  }

  private void generateOBRRepoXML(boolean nullURI, String ... bundleFiles) throws Exception
  {
    Set<ModelledResource> mrs = new HashSet<ModelledResource>();
    FileOutputStream fout = new FileOutputStream("repository.xml");
    RepositoryGenerator repositoryGenerator = getOsgiService(RepositoryGenerator.class);
    ModelledResourceManager modelledResourceManager = getOsgiService(ModelledResourceManager.class);
    for (String fileName : bundleFiles) {
      File bundleFile = new File(fileName);
      IDirectory jarDir = FileSystem.getFSRoot(bundleFile);
      String uri = "";
      if (!!!nullURI) {
        uri = bundleFile.toURI().toString();
      }
      mrs.add(modelledResourceManager.getModelledResource(uri, jarDir));
    }
    repositoryGenerator.generateRepository("Test repo description", mrs, fout);
    fout.close();
  }

  @After
  public void clearRepository() {
	  RepositoryAdmin repositoryAdmin = getOsgiService(RepositoryAdmin.class);
	  Repository[] repos = repositoryAdmin.listRepositories();
	  if ((repos != null) && (repos.length >0)) {
		  for (Repository repo : repos) {
			  repositoryAdmin.removeRepository(repo.getURI());
		  }
	  }
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
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime").noStart(),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),

        /* For debugging, uncomment the next two lines  
        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5010"),
        waitForFrameworkStartup(), */ 

        /* For debugging, add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
        */

        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}