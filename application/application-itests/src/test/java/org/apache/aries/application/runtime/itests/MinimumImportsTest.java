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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MinimumImportsTest extends AbstractIntegrationTest {

    /* Use @Before not @BeforeClass so as to ensure that these resources
     * are created in the paxweb temp directory, and not in the svn tree
     */
    static boolean createdApplications = false;
    static String fake_app_management = "application.management.fake";

    @Before
    public void createApplications() throws Exception {

        if (createdApplications) {
            return;
        }

        // need to fake a application manager to export the service in order to pass the resolving for the client
        // In the real situation, we don't allow customers' bundles to explicitly import the runtime services.
        ZipFixture bundle = ArchiveFixture.newJar().manifest()
                .attribute(Constants.BUNDLE_SYMBOLICNAME, fake_app_management)
                .attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
                .attribute(Constants.BUNDLE_VERSION, "1.0.0").end();

        OutputStream out = new FileOutputStream(fake_app_management + ".jar");
        bundle.writeOut(out);
        out.close();


        ZipFixture testEba = ArchiveFixture.newZip()
                .jar("org.apache.aries.application.itests.minimports.jar")
                .manifest().symbolicName("org.apache.aries.application.itests.minimports")
                .attribute("Bundle-Version", "1.0.0")
                .attribute("Import-Package", "org.apache.aries.application.management")
                .end()
                .binary("org/apache/aries/application/sample/appmgrclient/AppMgrClient.class",
                        MinimumImportsTest.class.getClassLoader().getResourceAsStream("org/apache/aries/application/sample/appmgrclient/AppMgrClient.class"))
                .binary("OSGI-INF/blueprint/app-mgr-client.xml",
                        MinimumImportsTest.class.getClassLoader().getResourceAsStream("app-mgr-client.xml"))
                .end();

        FileOutputStream fout = new FileOutputStream("appmgrclienttest.eba");
        testEba.writeOut(fout);
        fout.close();

        StringBuilder repositoryXML = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(MinimumImportsTest.class.getResourceAsStream("/basic/fakeAppMgrServiceRepo.xml")));
        String line;

        while ((line = reader.readLine()) != null) {
            repositoryXML.append(line);
            repositoryXML.append("\r\n");
        }

        String repo = repositoryXML.toString().replaceAll("bundle_location", new File(fake_app_management + ".jar").toURI().toString());

        System.out.println(repo);

        FileWriter writer = new FileWriter("repository.xml");
        writer.write(repo);
        writer.close();
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

        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
        AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("appmgrclienttest.eba")));
        RepositoryAdmin repositoryAdmin = context().getService(RepositoryAdmin.class);

        Repository[] repos = repositoryAdmin.listRepositories();
        for (Repository repo : repos) {
            repositoryAdmin.removeRepository(repo.getURI());
        }

        repositoryAdmin.addRepository(new File("repository.xml").toURI().toURL());

        AriesApplicationContext ctx = manager.install(app);
        ctx.start();

        int sleepfor = 3000;
        while ((acbl.success == null || acbl.success == false) && sleepfor > 0) {
            Thread.sleep(100);
            sleepfor -= 100;
        }
        assertNotNull("Timed out - didn't receive Blueprint CREATED or FAILURE event", acbl.success);
        assertTrue("Received Blueprint FAILURE event", acbl.success);

        ctx.stop();
        manager.uninstall(ctx);
        sr.unregister();
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
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management").versionAsInProject(),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject());
    }

}
