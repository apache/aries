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

import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.runtime.itests.util.IsolationTestUtils;
import org.apache.aries.isolated.sample.HelloWorld;
import org.apache.aries.isolated.sample.HelloWorldImpl;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.itest.RichBundleContext;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This test suite is responsible for validating that an application can package and use the
 * isolated configuration admin deployed. This includes both from Blueprint and manually.
 * <p/>
 * Blueprint Specific:
 * <p/>
 * Note that, the CmNamespaceHandler has been rewired to create a service reference to the configuration
 * admin that resides within the application framework. This will allow the configuration admin bundle
 * activator sufficient time to register a config admin service before the blueprint container for the bundle
 * requiring it is started (i.e. no configuration admin race condition).
 * <p/>
 * Other notes:
 * <p/>
 * In order to avoid boundary issues (i.e. class casting exceptions etc), the actual configuration admin bundle
 * classes are loaded from the shared framework. This is necessary as the blueprint-cm bundle refers to the
 * configuration admin classes directly, as we register a configuration admin and managed service in the
 * application scope, any attempt to use those services from the CM bundle would end up in a class cast exception.
 * This is why we use the classes already loaded in the root container, which are imported into the shared framework.
 * Behind the scenes a manifest transformer is used to make sure that the DEPLOYMENT.MF is augmented with the
 * necessary org.osgi.service.cm package import to make the class space consistent everywhere. From the developers
 * perspective, it appears as if they are deploying into a flat container as the wiring magic is hidden when the
 * application is created and installed. Note that the config package import only includes the CM API, nothing else.
 *
 * @version $Rev$ $Date$
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class IsolatedCfgAdminRuntimeTest extends AbstractIntegrationTest {

    private static final String APP_HWBP = "helloworld-bp.eba";
    private static final String APP_HWMN = "helloworld-mn.eba";
    private static final List<String> APPLICATIONS = Arrays.asList(APP_HWBP, APP_HWMN);

    /**
     * Creates two applications, as follows:
     * <p/>
     * - helloworld-bp.eba ------
     * |
     * | This application contains a helloworld bundle which contains an interface and impl for HelloWorld. Upon being started
     * | blueprint will create a new container for this bundle and register the HelloWorld service. The service will be injected
     * | with a message coming from the ConfigurationAdmin service using the PID: helloworld-bp. As a CM property placeholder is
     * | used, a ManagedService will also be registered on the bundles behalf so that further updates can be captured. Note that
     * | the blueprint configuration is wired to reload the container on a configuration update (to allow easier tracking of when
     * | to test service contents etc).
     * |
     * | The application also contains a configuration admin bundle (pulled from Maven).
     * ---------------------------
     * <p/>
     * - helloworld-mn.eba -------
     * |
     * | This application contains a helloworld bundle containing an activator that will register itself as a ManagedService for the
     * | PID: helloworld-mn. The activator will also expose out a HelloWorld service. Upon recieving an update from the packaged
     * | Configuration Admin service, the HelloWorld service will be re-registered using the latest configuration, namely the "message".
     * |
     * | The application also contains a configuration admin bundle (pulled from Maven).
     * ---------------------------
     *
     * @throws Exception
     */
    @Before
    public void constructApplications() throws Exception {

        Assert.assertNotNull("Could not find Maven URL handler", (new RichBundleContext(context())).getService(URLStreamHandlerService.class, "url.handler.protocol=mvn", 300000));
        MavenArtifactProvisionOption configAdminProvisionOption = mavenBundleInTest(getClass().getClassLoader(), "org.apache.felix", "org.apache.felix.configadmin");
        Assert.assertNotNull("Unable to lookup config admin maven bundle", configAdminProvisionOption);
        URL configAdminUrl = new URL(configAdminProvisionOption.getURL());

        ZipFixture helloWorldBluePrintEba = ArchiveFixture
                .newZip()
                .binary("META-INF/APPLICATION.MF",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader()
                                .getResourceAsStream("isolated/config/APPLICATION-BP.MF")
                )
                .binary("org.apache.felix.configadmin.jar", configAdminUrl.openStream())
                .jar("helloworld-bundle.jar")
                .manifest()
                .symbolicName("org.apache.aries.isolated.helloworldbp")
                .attribute("Bundle-Version", "1.0.0")
                .attribute("Import-Package", "org.osgi.service.cm")
                .end()
                .binary("org/apache/aries/isolated/sample/HelloWorld.class",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader().getResourceAsStream(
                                "org/apache/aries/isolated/sample/HelloWorld.class")
                )
                .binary("org/apache/aries/isolated/sample/HelloWorldImpl.class",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader().getResourceAsStream(
                                "org/apache/aries/isolated/sample/HelloWorldImpl.class")
                )
                .binary("OSGI-INF/blueprint/blueprint.xml",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader()
                                .getResourceAsStream("isolated/config/blueprint.xml")
                ).end();

        ZipFixture helloWorldManualEba = ArchiveFixture
                .newZip()
                .binary("META-INF/APPLICATION.MF",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader()
                                .getResourceAsStream("isolated/config/APPLICATION-MN.MF")
                )
                .binary("org.apache.felix.configadmin.jar", configAdminUrl.openStream())
                .jar("helloworld-bundle.jar")
                .manifest()
                .symbolicName("org.apache.aries.isolated.helloworldmn")
                .attribute("Bundle-Version", "1.0.0")
                .attribute("Bundle-Activator", "org.apache.aries.isolated.config.HelloWorldManagedServiceImpl")
                .attribute("Import-Package", "org.osgi.framework,org.osgi.service.cm")
                .end()
                .binary("org/apache/aries/isolated/sample/HelloWorld.class",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader().getResourceAsStream(
                                "org/apache/aries/isolated/sample/HelloWorld.class")
                )
                .binary("org/apache/aries/isolated/sample/HelloWorldImpl.class",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader().getResourceAsStream(
                                "org/apache/aries/isolated/sample/HelloWorldImpl.class")
                )
                .binary("org/apache/aries/isolated/config/HelloWorldManagedServiceImpl.class",
                        IsolatedCfgAdminRuntimeTest.class.getClassLoader().getResourceAsStream(
                                "org/apache/aries/isolated/config/HelloWorldManagedServiceImpl.class")
                ).end();

        FileOutputStream fout = new FileOutputStream(APP_HWBP);
        helloWorldBluePrintEba.writeOut(fout);
        fout.close();

        fout = new FileOutputStream(APP_HWMN);
        helloWorldManualEba.writeOut(fout);
        fout.close();
    }

    /**
     * Try and clean up the applications created by {@link #constructApplications()}
     */
    @After
    public void deleteApplications() {
        for (String application : APPLICATIONS) {
            File eba = new File(application);

            if (eba.exists()) {
                eba.delete();
            }
        }
    }

    /**
     * The purpose of this test is to make sure an application that contains an config admin bundle
     * can be used by Blueprint. The following steps are performed:
     * <p/>
     * - install the application
     * - start the application
     * - assert we have the following services in the isolated service registry (ConfigurationAdmin, ManagedService, HelloWorld)
     * - assert no configuration existed when the CM-PPH was invoked by BP (default message will be the token i.e. ${message})
     * - update the configuration (the message) for the PID (using a primitive boundary proxy), this will cause the blueprint container to reload
     * - check that the re-registered HelloWorld service contains the updated message
     * - clean up
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testIsolatedCfgAdminBPReload() throws Exception {
        validateApplicationConfiguration(
                APP_HWBP,
                "org.apache.aries.helloworldbpapp",
                "helloworld-bp",
                "${message}",
                "blueprint");
    }

    /**
     * The purpose of this test is to make sure an application that contains an config admin bundle
     * can be used by manually. The following steps are performed:
     * <p/>
     * - install the application
     * - start the application
     * - assert we have the following services in the isolated service registry (ConfigurationAdmin, ManagedService, HelloWorld)
     * - assert no configuration existed when the CM-PPH was invoked by BP (default message will be the token i.e. ${message})
     * - update the configuration (the message) for the PID (using a primitive boundary proxy), this will cause the HW service to be re-registered ({@link org.apache.aries.isolated.config.HelloWorldManagedServiceImpl}
     * - check that the re-registered HelloWorld service contains the updated message
     * - clean up
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testIsolatedCfgAdminManualReload() throws Exception {
        validateApplicationConfiguration(
                APP_HWMN,
                "org.apache.aries.helloworldmnapp",
                "helloworld-mn",
                (new HelloWorldImpl()).getMessage(),
                "manual");
    }

    /**
     * Central validation method for verifying configuration can be published and consumed correctly within
     * an isolated scope.
     *
     * @param application     the application file name
     * @param applicationName the application name
     * @param pid             the service.pid
     * @param defaultMessage  the default message for the HelloWorld service (checked before any configuration updates occur)
     * @param newMessage      the new message to set during a configuration update
     * @throws Exception
     */
    private void validateApplicationConfiguration(String application, String applicationName, String pid, String defaultMessage, String newMessage) throws Exception {

        //install and start the application
        Context ctx = installApplication(FileSystem.getFSRoot(new File(application)), applicationName);

        //assert we have the services that we're expecting
        assertExpectedServices(ctx.getBundleContext(), pid);

        //make sure we have the defaults set
        Assert.assertEquals("Invalid message set on the HW service", defaultMessage, IsolationTestUtils.findHelloWorldService(ctx.getBundleContext()).getMessage());

        //cause a configuration update to occur which should reload our HW service
        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("message", newMessage);
        Assert.assertTrue("Configuration update failed", executeConfigurationUpdate(ctx.getBundleContext(), pid, dictionary));

        //now make sure we have our new message set in the HW service
        Assert.assertEquals("Invalid message set on the HW service", newMessage, IsolationTestUtils.findHelloWorldService(ctx.getBundleContext()).getMessage());

        //clean up
        uninstallApplication(ctx);
    }

    /**
     * Executes a configuration update using the given dictionary. A HelloWorld service will be tracked
     * to ensure the configuration was successful (listening for add/remove tracker events).
     *
     * @param ctx        the application bundle context
     * @param pid        the service-pid to track
     * @param dictionary the dictionary containing updated properties
     * @return if the configuration update was successful
     * @throws Exception
     */
    private boolean executeConfigurationUpdate(BundleContext ctx, String pid, Dictionary<String, String> dictionary) throws Exception {
        boolean result = true;
        MonitorTask monitor = new MonitorTask(ctx, "(" + Constants.OBJECTCLASS + "=" + HelloWorld.class.getName() + ")", 2, 1);

        try {
            monitor.beginTracking();
            result &= (new ConfigurationTask(ctx, pid, dictionary)).execute();
            result &= monitor.execute();
        } finally {
            monitor.endTracking();
        }

        return result;
    }

    /**
     * Assert that the following services are present in the service registry:
     * <p/>
     * - ConfigurationAdmin
     * - ManagedService
     * - HelloWorld
     *
     * @param ctx the bundle context
     * @param pid the service pid used to register the underlying ManagedService
     * @throws Exception
     */
    private void assertExpectedServices(RichBundleContext ctx, String pid) throws Exception {
        //assert the CfgAdmin service was registered
        Assert.assertNotNull("Missing the ConfigurationAdmin service", ctx.getService(ConfigurationAdmin.class));

        //assert we have the ManagedService exposed
        Assert.assertNotNull("Missing the Managed service", ctx.getService(ManagedService.class, "(" + Constants.SERVICE_PID + "=" + pid + ")"));
        //now just make sure we can see it through the context of our config admin bundle context (should be in the same scope)
        ServiceReference ref = ctx.getServiceReference(ConfigurationAdmin.class.getName());
        Assert.assertNotNull("Couldn't find the ManagedService using the ConfigAdmin bundle context", new RichBundleContext(ref.getBundle().getBundleContext()).getService(ManagedService.class, "(" + Constants.SERVICE_PID + "=" + pid + ")"));

        //make sure we have the helloworld service registered
        HelloWorld helloWorldBluePrint = IsolationTestUtils.findHelloWorldService(ctx);
        Assert.assertNotNull("Missing the HelloWorld service", helloWorldBluePrint);
    }

    private Context installApplication(IDirectory application, String applicationName) throws Exception {
        //install the application and start it
        AriesApplicationManager appManager = context().getService(AriesApplicationManager.class);
        AriesApplication app = appManager.createApplication(application);
        AriesApplicationContext appCtx = appManager.install(app);
        appCtx.start();

        return new Context(appCtx, IsolationTestUtils.findIsolatedAppBundleContext(context(), applicationName));
    }

    private void uninstallApplication(Context ctx) throws Exception {
        AriesApplicationManager appManager = context().getService(AriesApplicationManager.class);
        appManager.uninstall(ctx.getApplicationContext());
    }

    /**
     * Create the configuration for the PAX container
     *
     * @return the various required options
     * @throws Exception
     */
    @org.ops4j.pax.exam.Configuration
    public static Option[] configuration() throws Exception {
        return options(

                // framework / core bundles
                mavenBundle("org.osgi", "org.osgi.core").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject(),

                // Repository
                repository("http://repository.ops4j.org/maven2"),

                // Logging
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                // Bundles
                junitBundles(),
                mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),

                // Bundles
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolve.transform.cm").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.isolated").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.framework.management").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.repository").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
                mavenBundle("org.ops4j.pax.url", "pax-url-aether").versionAsInProject());
    }

    /**
     * High level interface for executing a unit of work
     *
     * @version $Rev$ $Date$
     */
    private static interface Task {
        /**
         * Executes the task logic
         *
         * @return if the task was successful
         * @throws Exception
         */
        public boolean execute() throws Exception;
    }

    /**
     * Base class for a task implementation
     *
     * @version $Rev$ $Date$
     */
    private static abstract class BaseTask implements Task {
        private BundleContext ctx;

        public BaseTask(BundleContext ctx) {
            this.ctx = ctx;
        }

        protected BundleContext getBundleContext() {
            return ctx;
        }
    }

    /**
     * Trackable task that allows a service tracker to pickup service registration/un-registration events using the
     * supplied filter. Remember that if a service exists matching a filter while opening the underlying tracker
     * it will cause a addedService event to be fired, this must be taken into account when instantiating this type
     * task. For example if you had a ManagedService present in the container matching the given filter, you should
     * set the expected registerCount as 2 if you expect a re-register to occur due to a container reload etc.
     *
     * @version $Rev$ $Date$
     */
    public static abstract class TrackableTask extends BaseTask implements Task, ServiceTrackerCustomizer {
        private static final long DEFAULT_TIMEOUT = 5000;

        private String filter;
        private ServiceTracker tracker;
        private CountDownLatch addedLatch;
        private CountDownLatch removedLatch;

        public TrackableTask(BundleContext ctx, String filter) {
            this(ctx, filter, 1, 1);
        }

        public TrackableTask(BundleContext ctx, String filter, int registerCount) {
            this(ctx, filter, registerCount, 0);
        }

        public TrackableTask(BundleContext ctx, String filter, int registerCount, int unregisterCount) {
            super(ctx);

            this.filter = filter;
            this.addedLatch = new CountDownLatch(registerCount);
            this.removedLatch = new CountDownLatch(unregisterCount);
            this.tracker = null;
        }

        /**
         * Initiates the underlying service tracker
         *
         * @throws InvalidSyntaxException
         */
        protected synchronized void beginTracking() throws InvalidSyntaxException {
            if (tracker == null) {
                tracker = new ServiceTracker(getBundleContext(), getBundleContext().createFilter(filter), this);
                tracker.open();
            }
        }

        /**
         * Stops and clears the underlying service tracker
         */
        protected synchronized void endTracking() {
            if (tracker != null) {
                tracker.close();
                tracker = null;
            }
        }

        /*
         * (non-Javadoc)
         * @see org.apache.aries.application.runtime.itests.IsolatedCfgAdminRuntimeTest.Task#execute()
         */
        public boolean execute() throws Exception {
            boolean result = true;

            try {
                beginTracking();
                doExecute();

                result &= addedLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
                result &= removedLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            } finally {
                endTracking();
            }

            return result;
        }

        /*
         * (non-Javadoc)
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
         */
        public synchronized Object addingService(ServiceReference serviceRef) {
            addedLatch.countDown();
            return serviceRef;
        }

        /*
         * (non-Javadoc)
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        public void modifiedService(ServiceReference serviceRef, Object service) {
        }

        /*
         * (non-Javadoc)
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        public synchronized void removedService(ServiceReference serviceRef, Object service) {
            removedLatch.countDown();
        }

        /**
         * Performs the task logic
         *
         * @throws Exception
         */
        protected abstract void doExecute() throws Exception;
    }

    /**
     * The configuration task is responsible for executing a configuration update within the scope
     * of the given context. Note, this task assumes the class space is inconsistent will not try
     * casting to classes that may exist in the test world and also in the application.
     *
     * @version $Rev$ $Date$
     */
    private static class ConfigurationTask extends BaseTask {
        private String pid;
        private Dictionary<String, String> dictionary;

        public ConfigurationTask(BundleContext ctx, String pid, Dictionary<String, String> dictionary) {
            super(ctx);

            this.pid = pid;
            this.dictionary = dictionary;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.aries.application.runtime.itests.IsolatedCfgAdminRuntimeTest.Task#execute()
         */
        public boolean execute() throws Exception {
            boolean result = false;
            ServiceTracker tracker = new ServiceTracker(getBundleContext(), getBundleContext().createFilter("(" + Constants.OBJECTCLASS + "=" + ConfigurationAdmin.class.getName() + ")"), null);

            try {
                tracker.open();
                Object cfgAdminService = tracker.waitForService(5000);

                if (cfgAdminService != null) {

                    ConfigurationAdmin proxy = Skeleton.newMock(cfgAdminService, ConfigurationAdmin.class);
                    Configuration configuration = proxy.getConfiguration(pid);
                    configuration.setBundleLocation(null);
                    configuration.update(dictionary);
                    result = true;
                }
            } finally {
                tracker.close();
            }

            return result;
        }
    }

    /**
     * Simple monitor class to keep track of services using the supplied filter, acts as a wrapper
     * so that it can be placed in side a composite task.
     *
     * @version $Rev$ $Date$
     */
    private static final class MonitorTask extends TrackableTask {
        public MonitorTask(BundleContext ctx, String filter, int registerCount, int unregisterCount) {
            super(ctx, filter, registerCount, unregisterCount);
        }

        /*
         * (non-Javadoc)
         * @see org.apache.aries.application.runtime.itests.IsolatedCfgAdminRuntimeTest.TrackableTask#doExecute()
         */
        @Override
        protected void doExecute() throws Exception {
            //do nothing, we just care about tracking
        }

    }

    /**
     * Simple wrapper for the various contexts required in this test suite
     *
     * @version $Rev$ $Date$
     */
    private static class Context {
        private AriesApplicationContext applicationContext;
        private RichBundleContext bundleContext;

        public Context(AriesApplicationContext applicationContext, BundleContext bundleContext) {
            this.applicationContext = applicationContext;
            this.bundleContext = new RichBundleContext(bundleContext);
        }

        public AriesApplicationContext getApplicationContext() {
            return applicationContext;
        }

        public RichBundleContext getBundleContext() {
            return bundleContext;
        }
    }

    public static MavenArtifactProvisionOption mavenBundleInTest(ClassLoader loader, String groupId, String artifactId) {
        return mavenBundle().groupId(groupId).artifactId(artifactId)
                .version(getArtifactVersion(loader, groupId, artifactId));
    }

    //TODO getArtifactVersion and getFileFromClasspath are borrowed and modified from pax-exam.  They should be moved back ASAP.
    private static String getArtifactVersion(ClassLoader loader, final String groupId, final String artifactId) {
        final Properties dependencies = new Properties();
        try {
            InputStream in = getFileFromClasspath(loader, "META-INF/maven/dependencies.properties");
            try {
                dependencies.load(in);
            } finally {
                in.close();
            }
            final String version = dependencies.getProperty(groupId + "/" + artifactId + "/version");
            if (version == null) {
                throw new RuntimeException(
                        "Could not resolve version. Do you have a dependency for " + groupId + "/" + artifactId
                                + " in your maven project?"
                );
            }
            return version;
        } catch (IOException e) {
            // TODO throw a better exception
            throw new RuntimeException(
                    "Could not resolve version. Did you configured the plugin in your maven project?"
                            + "Or maybe you did not run the maven build and you are using an IDE?"
            );
        }
    }

    private static InputStream getFileFromClasspath(ClassLoader loader, final String filePath)
            throws FileNotFoundException {
        try {
            URL fileURL = loader.getResource(filePath);
            if (fileURL == null) {
                throw new FileNotFoundException("File [" + filePath + "] could not be found in classpath");
            }
            return fileURL.openStream();
        } catch (IOException e) {
            throw new FileNotFoundException("File [" + filePath + "] could not be found: " + e.getMessage());
        }
    }
}
