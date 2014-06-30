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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.spi.framework.BundleFramework;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.apache.aries.application.management.spi.repository.RepositoryGenerator;
import org.apache.aries.application.management.spi.update.UpdateStrategy;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.runtime.itests.util.IsolationTestUtils;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.isolated.sample.HelloWorld;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.VersionRange;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class UpdateAppTest extends AbstractIntegrationTest {

    private static final String SAMPLE_APP_NAME = "org.apache.aries.sample2";
    /* Use @Before not @BeforeClass so as to ensure that these resources
     * are created in the paxweb temp directory, and not in the svn tree
     */
    static boolean createdApplications = false;

    @Before
    public void createApplications() throws Exception {

        if (createdApplications) {
            return;
        }

        ZipFixture testEba = ArchiveFixture.newZip()
                .binary("META-INF/APPLICATION.MF",
                        UpdateAppTest.class.getClassLoader().getResourceAsStream("isolated/APPLICATION.MF"))
                .jar("sample.jar")
                .manifest().symbolicName("org.apache.aries.isolated.sample")
                .attribute("Bundle-Version", "1.0.0")
                .end()
                .binary("org/apache/aries/isolated/sample/HelloWorld.class",
                        UpdateAppTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorld.class"))
                .binary("org/apache/aries/isolated/sample/HelloWorldImpl.class",
                        UpdateAppTest.class.getClassLoader().getResourceAsStream("org/apache/aries/isolated/sample/HelloWorldImpl.class"))
                .binary("OSGI-INF/blueprint/aries.xml",
                        UpdateAppTest.class.getClassLoader().getResourceAsStream("isolated/sample-blueprint.xml"))
                .end();

        FileOutputStream fout = new FileOutputStream("test.eba");
        testEba.writeOut(fout);
        fout.close();

        ZipFixture sample2 = ArchiveFixture.newJar()
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
        sample2.writeOut(fout);
        fout.close();

        createdApplications = true;
    }

    @Test
    @Ignore
    public void testFullUpdate() throws Exception {
        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
        AriesApplication app = setupApp();

        updateApp(manager, app);

        assertAppMessage("hello brave new world");
    }

    @Test
    @Ignore
    public void testFineUpdate() throws Exception {
        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
        AriesApplication app = setupApp();

        BundleContext oldCtx = IsolationTestUtils.findIsolatedAppBundleContext(bundleContext, SAMPLE_APP_NAME);

        installMockUpdateStrategy();
        updateApp(manager, app);

        BundleContext newCtx = IsolationTestUtils.findIsolatedAppBundleContext(bundleContext, SAMPLE_APP_NAME);
        assertAppMessage("hello brave new world");

        assertTrue("We bounced the app where the update was supposed to do an update in place", oldCtx == newCtx);
    }

    @Test
    @Ignore
    public void testUpdateThenStart() throws Exception {
        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
        AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test.eba")));
        AriesApplicationContext ctx = manager.install(app);
        app = ctx.getApplication();

        BundleContext oldCtx = IsolationTestUtils.findIsolatedAppBundleContext(bundleContext, SAMPLE_APP_NAME);

        installMockUpdateStrategy();
        ctx = updateApp(manager, app);

        BundleContext newCtx = IsolationTestUtils.findIsolatedAppBundleContext(bundleContext, SAMPLE_APP_NAME);

        assertNull("App is not started yet but HelloWorld service is already there",
                IsolationTestUtils.findHelloWorldService(bundleContext, SAMPLE_APP_NAME));

        ctx.start();

        assertAppMessage("hello brave new world");

        assertTrue("We bounced the app where the update was supposed to do an update in place", oldCtx == newCtx);
    }

    private void installMockUpdateStrategy() {
        bundleContext.registerService(UpdateStrategy.class.getName(), new UpdateStrategy() {

            public boolean allowsUpdate(DeploymentMetadata newMetadata, DeploymentMetadata oldMetadata) {
                return true;
            }

            public void update(UpdateInfo info) throws UpdateException {
                BundleFramework fwk = info.getAppFramework();

                Bundle old = null;
                for (Bundle b : fwk.getBundles()) {
                    if (b.getSymbolicName().equals("org.apache.aries.isolated.sample")) {
                        old = b;
                        break;
                    }
                }

                if (old == null) throw new RuntimeException("Could not find old bundle");

                try {
                    info.unregister(old);
                    fwk.uninstall(old);

                    // only contains one element at most
                    Map<DeploymentContent, BundleSuggestion> suggestions =
                            info.suggestBundle(info.getNewMetadata().getApplicationDeploymentContents());

                    BundleSuggestion toInstall = suggestions.values().iterator().next();

                    Bundle newBundle = fwk.install(toInstall, info.getApplication());
                    info.register(newBundle);
                    if (info.startBundles()) fwk.start(newBundle);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }, new Hashtable<String, String>());
    }

    private AriesApplication setupApp() throws Exception {
        AriesApplicationManager manager = context().getService(AriesApplicationManager.class);
        AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test.eba")));
        AriesApplicationContext ctx = manager.install(app);
        app = ctx.getApplication();

        ctx.start();
        assertAppMessage("hello world");

        return app;
    }

    private AriesApplicationContext updateApp(AriesApplicationManager manager, AriesApplication app) throws Exception {
        IsolationTestUtils.prepareSampleBundleV2(bundleContext,
                context().getService(RepositoryGenerator.class),
                context().getService(RepositoryAdmin.class),
                context().getService(ModellingManager.class));

        AriesApplication newApp = manager.resolve(app, new ResolveConstraint() {
            public String getBundleName() {
                return "org.apache.aries.isolated.sample";
            }

            public VersionRange getVersionRange() {
                return ManifestHeaderProcessor.parseVersionRange("[2.0.0,2.0.0]", true);
            }
        });

        return manager.update(app, newApp.getDeploymentMetadata());
    }

    private void assertAppMessage(String message) throws Exception {
        HelloWorld hw = IsolationTestUtils.findHelloWorldService(bundleContext, SAMPLE_APP_NAME);
        assertNotNull(hw);
        assertEquals(message, hw.getMessage());
    }

    @Configuration
    public static Option[] configuration() {
        return options(

                // framework / core bundles
                mavenBundle("org.osgi", "org.osgi.core").versionAsInProject(),
                mavenBundle("org.osgi", "org.osgi.compendium").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject(),

                // Logging
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                // do not provision against the local runtime
                systemProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP).value("true"),

                // Bundles
                junitBundles(),
                mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.repository").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.isolated").versionAsInProject(),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject());
    }

}
