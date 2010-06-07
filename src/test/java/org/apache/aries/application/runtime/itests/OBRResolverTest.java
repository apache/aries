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
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.resolver.obr.generator.RepositoryDescriptorGenerator;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.w3c.dom.Document;

@RunWith(JUnit4TestRunner.class)
public class OBRResolverTest extends AbstractIntegrationTest 
{
  public static final String CORE_BUNDLE_BY_VALUE = "core.bundle.by.value";
  public static final String CORE_BUNDLE_BY_REFERENCE = "core.bundle.by.reference";
  public static final String TRANSITIVE_BUNDLE_BY_VALUE = "transitive.bundle.by.value";
  public static final String TRANSITIVE_BUNDLE_BY_REFERENCE = "transitive.bundle.by.reference";
  
  
  /* Use @Before not @BeforeClass so as to ensure that these resources
   * are created in the paxweb temp directory, and not in the svn tree
   */
  @Before
  public static void createApplications() throws Exception 
  {
    ZipFixture bundle = ArchiveFixture.newJar().manifest()
                            .attribute(Constants.BUNDLE_SYMBOLICNAME, CORE_BUNDLE_BY_VALUE)
                            .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                            .attribute(Constants.IMPORT_PACKAGE, "p.q.r, x.y.z")
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

    bundle = ArchiveFixture.newJar().manifest()
                            .attribute(Constants.BUNDLE_SYMBOLICNAME, CORE_BUNDLE_BY_REFERENCE)
                            .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                            .attribute(Constants.EXPORT_PACKAGE, "d.e.f").end();

    fout = new FileOutputStream(CORE_BUNDLE_BY_REFERENCE + "_0.0.0.jar");
    bundle.writeOut(fout);
    fout.close();
    
    ZipFixture testEba = ArchiveFixture.newZip()
     .binary("META-INF/APPLICATION.MF",
        OBRResolverTest.class.getClassLoader().getResourceAsStream("obr/APPLICATION.MF"))
        .end()
      .binary(CORE_BUNDLE_BY_VALUE + ".jar", new FileInputStream(CORE_BUNDLE_BY_VALUE + ".jar")).end()
      .binary(TRANSITIVE_BUNDLE_BY_VALUE + ".jar", new FileInputStream(TRANSITIVE_BUNDLE_BY_VALUE + ".jar")).end();

    fout = new FileOutputStream("blog.eba");
    testEba.writeOut(fout);
    fout.close();
  }

  @Test(expected=ResolverException.class)
  public void testBlogAppResolveFail() throws ResolverException, Exception
  {
    startApplicationRuntimeBundle();

    generateOBRRepoXML(TRANSITIVE_BUNDLE_BY_REFERENCE + ".jar", CORE_BUNDLE_BY_REFERENCE + "_0.0.0.jar");
    
    RepositoryAdmin repositoryAdmin = getOsgiService(RepositoryAdmin.class);
    
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository repo : repos) {
      repositoryAdmin.removeRepository(repo.getURL());
    }
    
    repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("blog.eba")));
    //installing requires a valid url for the bundle in repository.xml.
    
    app = manager.resolve(app);
  }
  
  @Test
  public void testBlogApp() throws Exception 
  {
    startApplicationRuntimeBundle();

    generateOBRRepoXML(TRANSITIVE_BUNDLE_BY_REFERENCE + ".jar", CORE_BUNDLE_BY_REFERENCE + ".jar");
    
    RepositoryAdmin repositoryAdmin = getOsgiService(RepositoryAdmin.class);
    
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository repo : repos) {
      repositoryAdmin.removeRepository(repo.getURL());
    }
    
    repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("blog.eba")));
    //installing requires a valid url for the bundle in repository.xml.
    
    app = manager.resolve(app);
    
    DeploymentMetadata depMeta = app.getDeploymentMetadata();
    
    List<DeploymentContent> provision = depMeta.getApplicationProvisionBundles();
    
    assertEquals(provision.toString(), 2, provision.size());
    
    List<String> bundleSymbolicNames = new ArrayList<String>();
    
    for (DeploymentContent dep : provision) {
      bundleSymbolicNames.add(dep.getContentName());
    }
    
    assertTrue("Bundle " + TRANSITIVE_BUNDLE_BY_REFERENCE + " not found.", bundleSymbolicNames.contains(TRANSITIVE_BUNDLE_BY_REFERENCE));
    assertTrue("Bundle " + TRANSITIVE_BUNDLE_BY_VALUE + " not found.", bundleSymbolicNames.contains(TRANSITIVE_BUNDLE_BY_VALUE));
    
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();

    Set<Bundle> bundles = ctx.getApplicationContent();
    
    assertEquals("Number of bundles provisioned in the app", 4, bundles.size());
    
    ctx.stop();
    manager.uninstall(ctx);
  }


  private void generateOBRRepoXML(String ... bundleFiles) throws Exception
  {
    Set<BundleInfo> bundles = new HashSet<BundleInfo>();
    
    for (String file : bundleFiles) {
      bundles.add(createBundleInfo(new File(file).toURI().toURL().toExternalForm()));
    }
    
    Document doc = RepositoryDescriptorGenerator.generateRepositoryDescriptor("Test repo description", bundles);
    
    FileOutputStream fout = new FileOutputStream("repository.xml");
    
    TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(fout));
    
    fout.close();
    
    TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(System.out));
  }

  private BundleInfo createBundleInfo(String urlToBundle) throws Exception
  {
    ApplicationMetadataFactory factory = getOsgiService(ApplicationMetadataFactory.class);
    
    Bundle b = getBundle("org.apache.aries.application.management");
    @SuppressWarnings("unchecked")
    Class<BundleInfo> clazz = b.loadClass("org.apache.aries.application.utils.management.SimpleBundleInfo");
    Constructor<BundleInfo> c = clazz.getConstructor(ApplicationMetadataFactory.class, BundleManifest.class, String.class);
    
    return c.newInstance(factory, BundleManifest.fromBundle(new URL(urlToBundle).openStream()), urlToBundle);
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
        mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),

        /* For debugging, uncomment the next two lines */
//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777"),
//        waitForFrameworkStartup(),

        /* For debugging, uncomment the next two lines
        and add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
        */

        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}