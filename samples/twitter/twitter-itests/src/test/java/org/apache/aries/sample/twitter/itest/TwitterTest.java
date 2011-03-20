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
package org.apache.aries.sample.twitter.itest;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.utils.AppConstants;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
@RunWith(JUnit4TestRunner.class)
public class TwitterTest extends AbstractIntegrationTest 
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
  
  //Test for JIRA-461 which currently fails.
  @Test
  public void testTwitter() throws Exception
  {
    // provision against the local runtime
    System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "false");
    RepositoryAdmin repositoryAdmin = getOsgiService(RepositoryAdmin.class);
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository repo : repos) {
      repositoryAdmin.removeRepository(repo.getURI());
    }

    
    // Use the superclasses' getUrlToEba() method instead of the pax-exam mavenBundle() method because pax-exam is running in a
    // diffference bundle which doesn't have visibility to the META-INF/maven/dependencies.properties file used to figure out the
    // version of the maven artifact.
    URL twitterEbaUrl = getUrlToEba("org.apache.aries.samples.twitter",
        "org.apache.aries.samples.twitter.eba");
    URL twitterCommonLangJar_url = getUrlToBundle("commons-lang", "commons-lang");
    URL twitterJar_url = getUrlToBundle("org.apache.aries.samples.twitter", "org.apache.aries.samples.twitter.twitter4j");
   
    // add the repository xml to the repository admin
    StringBuilder repositoryXML = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/obr/twitter/TwitterRepository.xml")));
    String line;
    while ((line = reader.readLine()) != null) {
      repositoryXML.append(line);
      repositoryXML.append("\r\n");
    }
  //replace the jar file url with the real url related to the environment
    String repo = repositoryXML.toString().replaceAll("commons.lang.location", twitterCommonLangJar_url.toExternalForm());
    repo = repo.replaceAll("twitter4j.location", twitterJar_url.toExternalForm());
    
    FileWriter writer = new FileWriter("twitterRepo.xml");
    writer.write(repo);
    writer.close();
    repositoryAdmin.addRepository(new File("twitterRepo.xml").toURI().toURL());
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(twitterEbaUrl);
    app = manager.resolve(app);
    DeploymentMetadata depMeta = app.getDeploymentMetadata();
    List<DeploymentContent> provision = depMeta.getApplicationProvisionBundles();
    Collection<DeploymentContent> useBundles = depMeta.getDeployedUseBundle();
    Collection<DeploymentContent> appContent = depMeta.getApplicationDeploymentContents();
    // We cannot be sure whether there are two or three provision bundles pulled in by Felix OBR as there is an outstanding defect
    // https://issues.apache.org/jira/browse/FELIX-2672
    // The workaround is to check we get the two bundles we are looking for, instead of insisting on just having two bundles.
    
    List<String> provisionBundleSymbolicNames = new ArrayList<String>();
    for (DeploymentContent dep : provision) {
       provisionBundleSymbolicNames.add(dep.getContentName());
    }
    String provision_bundle1 = "org.apache.commons.lang";
    String provision_bundle2 = "twitter4j";
    assertTrue("Bundle " + provision_bundle1 + " not found.", provisionBundleSymbolicNames.contains(provision_bundle1));
    assertTrue("Bundle " + provision_bundle2 + " not found.", provisionBundleSymbolicNames.contains(provision_bundle2));
    assertEquals(useBundles.toString(), 0, useBundles.size());
    assertEquals(appContent.toString(), 1, appContent.size());
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();
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
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
        /* For debugging, uncomment the next two lines  */
        /*vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5010"),
        waitForFrameworkStartup(),  */
//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5010"),
        /* For debugging, add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
        */

        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}
