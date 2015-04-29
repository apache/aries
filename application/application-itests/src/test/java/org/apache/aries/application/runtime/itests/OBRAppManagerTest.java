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
import static org.ops4j.pax.exam.CoreOptions.*;

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
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OBRAppManagerTest extends AbstractIntegrationTest {

    /* Use @Before not @BeforeClass so as to ensure that these resources
     * are created in the paxweb temp directory, and not in the svn tree
     */
    static boolean createdApplications = false;

    @Before
    public void createApplications() throws Exception {
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
        assertEquals(result, "hello world");

        ctx.stop();
        manager.uninstall(ctx);
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
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces").versionAsInProject());
    }

}