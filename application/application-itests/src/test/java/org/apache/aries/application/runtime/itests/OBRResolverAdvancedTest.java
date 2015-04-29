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
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.spi.repository.RepositoryGenerator;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ContentFactory;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.sample.HelloWorld;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OBRResolverAdvancedTest extends AbstractIntegrationTest {

    public static final String CORE_BUNDLE_BY_VALUE = "core.bundle.by.value";
    public static final String CORE_BUNDLE_BY_REFERENCE = "core.bundle.by.reference";
    public static final String TRANSITIVE_BUNDLE_BY_VALUE = "transitive.bundle.by.value";
    public static final String TRANSITIVE_BUNDLE_BY_REFERENCE = "transitive.bundle.by.reference";
    public static final String USE_BUNDLE_BY_REFERENCE = "use.bundle.by.reference";
    public static final String REPO_BUNDLE = "aries.bundle1";
    public static final String HELLO_WORLD_CLIENT_BUNDLE = "hello.world.client.bundle";
    public static final String HELLO_WORLD_SERVICE_BUNDLE1 = "hello.world.service.bundle1";
    public static final String HELLO_WORLD_SERVICE_BUNDLE2 = "hello.world.service.bundle2";

    /* Use @Before not @BeforeClass so as to ensure that these resources
     * are created in the paxweb temp directory, and not in the svn tree
     */
    @Before
    public void createApplications() throws Exception {
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

        bundle = ArchiveFixture.newJar().manifest()
                .attribute(Constants.BUNDLE_SYMBOLICNAME, CORE_BUNDLE_BY_REFERENCE)
                .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                .attribute(Constants.EXPORT_PACKAGE, "d.e.f").end();

        fout = new FileOutputStream(CORE_BUNDLE_BY_REFERENCE + "_0.0.0.jar");
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
                .binary("OSGI-INF/blueprint/blueprint.xml", OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("obr/hello-world-client.xml")).end()
                .binary("OSGI-INF/blueprint/anotherBlueprint.xml", OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("obr/aries.bundle1/OSGI-INF/blueprint/sample-blueprint.xml")).end();
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

    @Test(expected = ResolverException.class)
    public void testDemoAppResolveFail() throws ResolverException, Exception {
        // do not provision against the local runtime
        System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "true");
        generateOBRRepoXML(false, TRANSITIVE_BUNDLE_BY_REFERENCE + ".jar", CORE_BUNDLE_BY_REFERENCE + "_0.0.0.jar", USE_BUNDLE_BY_REFERENCE + ".jar");

        RepositoryAdmin repositoryAdmin = context().getService(RepositoryAdmin.class);

        Repository[] repos = repositoryAdmin.listRepositories();
        for (Repository repo : repos) {
            repositoryAdmin.removeRepository(repo.getURI());
        }

        repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
        AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("demo.eba")));

        app = manager.resolve(app);


    }

    @Test(expected = ModellerException.class)
    public void testModellerException() throws Exception {

        ZipFixture bundle = ArchiveFixture.newJar().manifest()
                .attribute(Constants.BUNDLE_SYMBOLICNAME, CORE_BUNDLE_BY_VALUE)
                .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                .attribute(Constants.IMPORT_PACKAGE, "a.b.c, p.q.r, x.y.z, javax.naming")
                .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();
        FileOutputStream fout = new FileOutputStream("delete.jar");
        bundle.writeOut(fout);
        fout.close();
        generateOBRRepoXML(false, "delete.jar");
    }

    @Test
    public void testDemoApp() throws Exception {
        // do not provision against the local runtime
        System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "true");
        generateOBRRepoXML(false, TRANSITIVE_BUNDLE_BY_REFERENCE + ".jar", CORE_BUNDLE_BY_REFERENCE + ".jar", USE_BUNDLE_BY_REFERENCE + ".jar");

        RepositoryAdmin repositoryAdmin = context().getService(RepositoryAdmin.class);

        Repository[] repos = repositoryAdmin.listRepositories();
        for (Repository repo : repos) {
            repositoryAdmin.removeRepository(repo.getURI());
        }

        repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
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
        Map<String, String> maps = new HashMap<String, String>();
        maps.put("version", "0.0.0");
        maps.put("bundle-symbolic-name", "use.bundle.by.reference");
        maps.put("bundle-version", "[1.0.0,1.0.0]");
        Content useContent = ContentFactory.parseContent("a.b.c", maps);
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

    /**
     * This test just verifies whether every entry in the MANIFEST.MF was fed into the repository generator.
     * Since the IBM JRE generates a slightly different repository file from the Sun JRE as far as the order of xml elements is concerned. It is not feasible
     * to perform a file comparison.
     *
     * @throws Exception
     */
    @Test
    public void testRepo() throws Exception {
        // do not provision against the local runtime
        System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "true");
        generateOBRRepoXML(true, REPO_BUNDLE + ".jar");
        //print out the repository.xml
        BufferedReader reader = new BufferedReader(new FileReader(new File("repository.xml")));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        // compare the generated with the expected file
        Document real_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("repository.xml"));
        Document expected_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("/obr/aries.bundle1/expectedRepository.xml"));
        // parse two documents to make sure they have the same number of elements
        Element element_real = real_doc.getDocumentElement();
        Element element_expected = expected_doc.getDocumentElement();
        NodeList nodes_real = element_real.getElementsByTagName("capability");
        NodeList nodes_expected = element_expected.getElementsByTagName("capability");
        assertEquals("The number of capability is not expected. ", nodes_expected.getLength(), nodes_real.getLength());
        nodes_real = element_real.getElementsByTagName("require");
        nodes_expected = element_expected.getElementsByTagName("require");
        assertEquals("The number of require elements is not expected. ", nodes_expected.getLength(), nodes_real.getLength());
        nodes_real = element_real.getElementsByTagName("p");
        nodes_expected = element_expected.getElementsByTagName("p");
        assertEquals("The number of properties is not expected. ", nodes_expected.getLength(), nodes_real.getLength());
        // Let's verify all p elements are shown as expected.
        for (int index = 0; index < nodes_expected.getLength(); index++) {
            Node node = nodes_expected.item(index);
            boolean contains = false;
            // make sure the node exists in the real generated repository
            for (int i = 0; i < nodes_real.getLength(); i++) {
                Node real_node = nodes_real.item(i);
                if (node.isEqualNode(real_node)) {
                    contains = true;
                    break;
                }
            }
            assertTrue("The node " + node.toString() + "should exist.", contains);
        }
    }

    @Test
    public void testRepoAgain() throws Exception {
        // do not provision against the local runtime
        System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "true");

        RepositoryGenerator repositoryGenerator = context().getService(RepositoryGenerator.class);

        String fileURI = new File(REPO_BUNDLE + ".jar").toURI().toString();
        File repoXml = new File("repository.xml");
        if (repoXml.exists()) {
            repoXml.delete();
        }
        repositoryGenerator.generateRepository(new String[]{fileURI}, new FileOutputStream(repoXml));

        //print out the repository.xml
        BufferedReader reader = new BufferedReader(new FileReader(new File("repository.xml")));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        // compare the generated with the expected file
        Document real_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("repository.xml"));
        Document expected_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(OBRResolverAdvancedTest.class.getClassLoader().getResourceAsStream("/obr/aries.bundle1/expectedRepository.xml"));
        // parse two documents to make sure they have the same number of elements
        Element element_real = real_doc.getDocumentElement();
        Element element_expected = expected_doc.getDocumentElement();
        NodeList nodes_real = element_real.getElementsByTagName("capability");
        NodeList nodes_expected = element_expected.getElementsByTagName("capability");
        assertEquals("The number of capability is not expected. ", nodes_expected.getLength(), nodes_real.getLength());
        nodes_real = element_real.getElementsByTagName("require");
        nodes_expected = element_expected.getElementsByTagName("require");
        assertEquals("The number of require elements is not expected. ", nodes_expected.getLength(), nodes_real.getLength());
        nodes_real = element_real.getElementsByTagName("p");
        nodes_expected = element_expected.getElementsByTagName("p");
        assertEquals("The number of properties is not expected. ", nodes_expected.getLength(), nodes_real.getLength());
        // Let's verify all p elements are shown as expected.
        for (int index = 0; index < nodes_expected.getLength(); index++) {
            Node node = nodes_expected.item(index);
            boolean contains = false;
            // make sure the node exists in the real generated repository
            for (int i = 0; i < nodes_real.getLength(); i++) {
                Node real_node = nodes_real.item(i);
                if (node.isEqualNode(real_node)) {
                    contains = true;
                    break;
                }
            }
            assertTrue("The node " + node.toString() + "should exist.", contains);
        }
    }

    @Test
    public void testMutlipleServices() throws Exception {
        // provision against the local runtime
        System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "false");
        generateOBRRepoXML(false, HELLO_WORLD_SERVICE_BUNDLE1 + ".jar", HELLO_WORLD_SERVICE_BUNDLE2 + ".jar");

        RepositoryAdmin repositoryAdmin = context().getService(RepositoryAdmin.class);

        Repository[] repos = repositoryAdmin.listRepositories();
        for (Repository repo : repos) {
            repositoryAdmin.removeRepository(repo.getURI());
        }

        repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
        AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("hello.eba")));
        AriesApplicationContext ctx = manager.install(app);
        ctx.start();

        // Wait 5 seconds just to give the blueprint-managed beans a chance to come up
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ix) {
        }

        HelloWorld hw = context().getService(HelloWorld.class);
        String result = hw.getMessage();
        assertEquals(result, "hello world");


        // Uncomment the block below after https://issues.apache.org/jira/browse/FELIX-2546,
        // "Only one service is provisioned even when specifying for mulitple services"
        // is fixed. This tracks the problem of provisioning only one service even when we
        // specify multiple services.

        /** HelloWorldManager hwm = context().getService(HelloWorldManager.class);
         * int numberOfServices = hwm.getNumOfHelloServices();
         * assertEquals(2, numberOfServices);
         */
        ctx.stop();
        manager.uninstall(ctx);

    }


    private void generateOBRRepoXML(boolean nullURI, String... bundleFiles) throws Exception {
        Set<ModelledResource> mrs = new HashSet<ModelledResource>();
        FileOutputStream fout = new FileOutputStream("repository.xml");
        RepositoryGenerator repositoryGenerator = context().getService(RepositoryGenerator.class);
        ModelledResourceManager modelledResourceManager = context().getService(ModelledResourceManager.class);
        for (String fileName : bundleFiles) {
            File bundleFile = new File(fileName);
            IDirectory jarDir = FileSystem.getFSRoot(bundleFile);
            String uri = "";
            if (!!!nullURI) {
                uri = bundleFile.toURI().toString();
            }
            if ("delete.jar".equals(fileName)) {
                jarDir = null;
            }
            mrs.add(modelledResourceManager.getModelledResource(uri, jarDir));
        }
        repositoryGenerator.generateRepository("Test repo description", mrs, fout);
        fout.close();
    }

    @After
    public void clearRepository() {
        RepositoryAdmin repositoryAdmin = context().getService(RepositoryAdmin.class);
        Repository[] repos = repositoryAdmin.listRepositories();
        if ((repos != null) && (repos.length > 0)) {
            for (Repository repo : repos) {
                repositoryAdmin.removeRepository(repo.getURI());
            }
        }
    }

    @Configuration
    public static Option[] configuration() {
        return options(

                // framework / core bundles
                mavenBundle("org.osgi", "org.osgi.core").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject(),

                // Logging
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                // Bundles
                junitBundles(),
                mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),

                // Bundles
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject());
    }

}