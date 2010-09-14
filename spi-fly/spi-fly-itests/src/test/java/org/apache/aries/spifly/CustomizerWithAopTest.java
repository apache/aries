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
package org.apache.aries.spifly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.rawPaxRunnerOption;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.aries.spifly.aop.sample.interf.TestInterface;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class CustomizerWithAopTest extends AbstractIntegrationTest {

    /**
     * This is needed because:
     * 
     * a) we need to have a single location that can be used from both the piece
     * of code that prepares the test before the container is started and the
     * cleanup method that is run inside the container
     * 
     * b) Depending on the way in which this test is run (whether it is run from
     * Eclipse or using Maven - from commandline), locations will be different.
     * That is why we cannot hardcode them.
     * 
     * Note: different locations will be returned depending on the location from
     * which this is run - from inside or outside of the container. That is why
     * location needs to be chosen outside of the container and then passed to
     * the test container using a system property.
     * 
     * @return
     */
    private static String getTempLocation() {
        return System.getProperty("user.dir");
    }

    @Before
    public void setItUp() throws Exception {
        // A bundle with an implementation of
        // javax.xml.parsers.DocumentBuilderFactory. This bundle contains a
        // SPI-Provider header:

        // SPI-Provider:
        // dom;provider-name="myimpl";service-ids="javax.xml.parsers.DocumentBuilderFactory"

        ZipFixture testImpl = ArchiveFixture
                .newJar()
                .manifest()
                .symbolicName("org.apache.aries.spifly.sample.impl")
                .attribute("Bundle-Version", "1.0.0")
                .attribute("Import-Package", "javax.xml.parsers")
                .attribute(
                        "SPI-Provider",
                        "dom;provider-name=\"myimpl\";service-ids=\"javax.xml.parsers.DocumentBuilderFactory\"")
                .end()
                .binary(
                        "org/apache/aries/spifly/sample/TestDomBuilderFactory.class",
                        org.apache.aries.spifly.sample.TestDomBuilderFactory.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/sample/TestDomBuilderFactory.class"))
                .file(
                        "META-INF/services/javax.xml.parsers.DocumentBuilderFactory",
                        "org.apache.aries.spifly.sample.TestDomBuilderFactory\n")
                .end();

        // Created in the current 'user.dir'. This is run inside the
        // container, so 'user.dir' will point to the Pax Exam working dir.
        FileOutputStream foutImpl = new FileOutputStream("impl.jar");
        testImpl.writeOut(foutImpl);
        foutImpl.close();
    }

    @After
    public void cleanUp() {
        // this one was generated during setup - inside the container
        // it will be located in the pax exam working dir
        new File("impl.jar").delete();
    }

    @BeforeClass
    public static void setupBeforeContainerIsLaunched()
            throws FileNotFoundException, IOException {
        // We cannot generate these bundles inside the setUp() method. This
        // would mean that we're inside the container. The issue is that we
        // removed all of the classes that are to be placed in this test jar
        // from the test probe (see the customizer below). Those classes would
        // not be available inside the container. We need to build these bundles
        // earlier.

        // A bundle with empty SPI-Consumer header; any impl will work with this
        // bundle
        generateBundleWithoutSPIConsumerHeader();

        // A bundle with non empty SPI-Consumer header; for DOM, only provider
        // named 'notmyimpl' will work; there is no such provider in our env, so
        // ...
        generateBundleWithUnsatisfiableSPIConsumerHeader();

        // A bundle with non empty SPI-Consumer header; for DOM, only provider
        // named 'myimpl' will work;
        generateBundleWithSatisfiableSPIConsumerHeader();
    }

    @AfterClass
    public static void cleanUpAfterContainerIsShutDown() {
        // This is run outside of the container. getTempLocation() will return
        // the same value as when called from the inside of the
        // setupBeforeContainerIsLaunched() method.
        String path = getTempLocation();
        new File(path + "/client1.jar").delete();
        new File(path + "/client2.jar").delete();
        new File(path + "/client3.jar").delete();
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() throws IOException {

        String tempLocation = getTempLocation();
        String projectRoot = getProjectRoot();

        Option[] options = options(

                // because of the weird way in which Pax JUnit Runner is being
                // initialized, this piece of code will be run twice; during the
                // first run those three client bundles will not be present;
                // they are created in one of the @BeforeClass methods;
                // fortunately, they will be in place when the container loads
                // those bundles;
                provision("file:///" + tempLocation + "/client1.jar"),

                provision("file:///" + tempLocation + "/client2.jar"),

                provision("file:///" + tempLocation + "/client3.jar"),

                rawPaxRunnerOption("--ee", "J2SE-1.6"),

                // The org.eclipse.equinox.weaving.aspectj bundle has too few
                // imports;

                // OSGi metadata for these Equinox (weaving) libraries is a
                // little bit malformed, so both system packages and boot
                // delegation packages need to be changed.
                bootDelegationPackage("org.xml.sax.helpers,org.xml.sax,"
                        + "org.eclipse.equinox.weaving.adaptors,org.eclipse.equinox.weaving.hooks,"
                        + "org.eclipse.equinox.service.weaving"),

                systemPackages("org.eclipse.equinox.service.weaving"),

                // Setup the Equinox hook
                // This needs to be on the boot classpath
                bootClasspathLibrary(projectRoot
                        + "/target/weaver/org.eclipse.equinox.weaving.hook.jar"),
                systemProperty("osgi.framework.extensions").value(
                        "org.eclipse.equinox.weaving.hook"),

                // Setup other Equinox Aspects bundles

                provision(wrappedBundle(
                        projectRoot
                                + "/target/weaver/org.eclipse.equinox.weaving.aspectj.jar")
                        .startLevel(1)),

                mavenBundle("org.aspectj",
                        "com.springsource.org.aspectj.runtime").startLevel(1),
                mavenBundle("org.aspectj",
                        "com.springsource.org.aspectj.weaver").startLevel(1),

                // Let's tell AspectJ to print verbose info
                systemProperty("org.aspectj.weaver.showWeaveInfo")
                        .value("true"),
                systemProperty("org.aspectj.osgi.verbose").value("true"),

                mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
                // Felix Config Admin
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                // Felix mvn url handler
                mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

                // this is how you set the default log level when using pax
                // logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
                        .value("DEBUG"),

                // Bundles
                mavenBundle("org.apache.aries.spifly",
                        "org.apache.aries.spifly.core").update().startLevel(2),

                mavenBundle("org.apache.aries", "org.apache.aries.util"),
                mavenBundle("org.osgi", "org.osgi.compendium"), mavenBundle(
                        "org.apache.aries.testsupport",
                        "org.apache.aries.testsupport.unit"),

                // vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5007"
                // ),
                waitForFrameworkStartup(),

                /*
                 * and add these imports: import static
                 * org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
                 * import static
                 * org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
                 */

                equinox().version("3.5.0"),

                // Remove resources that only need to be present in the test
                // bundle and not in the test probe. If they were present in the
                // test probe, they could mess things up.
                new Customizer() {

                    @Override
                    public InputStream customizeTestProbe(InputStream testProbe)
                            throws Exception {
                        TinyBundle bundle = TinyBundles.modifyBundle(testProbe);

                        bundle.removeResource("aop-resources/aop.xml");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/Activator.class");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/BundleAspect.class");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/TestImpl.class");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/HeaderParser.class");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/HeaderParser$PathElement.class");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/interf/TestInterface.class");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/interf/");
                        bundle
                                .removeResource("org/apache/aries/spifly/aop/sample/");

                        return bundle.build();
                    }
                }

        );
        options = updateOptions(options);
        return options;
    }

    private static void generateBundleWithoutSPIConsumerHeader()
            throws IOException, FileNotFoundException {

        ZipFixture testBundle = ArchiveFixture
                .newJar()
                .manifest()
                .symbolicName("org.apache.aries.spifly.sample.client")
                .attribute("Bundle-Version", "1.0.0")
                /*
                 * Symbolic name 'org.aspectj.runtime' should be used. This is
                 * the original name of the AspectJ artifact and also the
                 * symbolic name of the AspectJ bundle shipped with Equinox
                 * Aspects. However, we're now using AspectJ from SpringSource
                 * with a different name:
                 * 'com.springsource.org.aspectj.runtime'.
                 */
                .attribute("Require-Bundle",
                        "com.springsource.org.aspectj.runtime")
                .attribute(
                        "Import-Package",
                        "javax.xml.parsers,org.osgi.framework,"
                                + "org.aspectj.lang,org.aspectj.runtime.internal,"
                                + "org.apache.aries.spifly.api").attribute(
                        "Export-Package",
                        "org.apache.aries.spifly.aop.sample.interf").attribute(
                        "Bundle-Activator",
                        "org.apache.aries.spifly.aop.sample.Activator").end();

        addResourcesToGeneratedClientBundle(testBundle);

        // this bundle will also provide the interface - let's add the .class
        // file with the interface and define a proper export header
        testBundle
                .binary(
                        "org/apache/aries/spifly/aop/sample/interf/TestInterface.class",
                        org.apache.aries.spifly.aop.sample.Activator.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/aop/sample/interf/TestInterface.class"))
                .end();

        // Created in the current 'user.dir'. This is run outside of the
        // container, so 'user.dir' will point to one of the Maven projects and
        // not to Pax Exam working dir.
        File file = new File(getTempLocation(), "client1.jar");
        FileOutputStream fout = new FileOutputStream(file);
        testBundle.writeOut(fout);
        fout.close();
    }

    private static void generateBundleWithUnsatisfiableSPIConsumerHeader()
            throws IOException, FileNotFoundException {

        // this bundle does not provide the interface; it' imports it from the
        // bundle above

        ZipFixture testBundle = ArchiveFixture
                .newJar()
                .manifest()
                .symbolicName("org.apache.aries.spifly.sample.client")
                .attribute("Bundle-Version", "2.0.0")
                /*
                 * Symbolic name 'org.aspectj.runtime' should be used. This is
                 * the original name of the AspectJ artifact and also the
                 * symbolic name of the AspectJ bundle shipped with Equinox
                 * Aspects. However, we're now using AspectJ from SpringSource
                 * with a different name:
                 * 'com.springsource.org.aspectj.runtime'.
                 */
                .attribute("Require-Bundle",
                        "com.springsource.org.aspectj.runtime")
                .attribute(
                        "Import-Package",
                        "javax.xml.parsers,org.osgi.framework,"
                                + "org.aspectj.lang,org.aspectj.runtime.internal,"
                                + "org.apache.aries.spifly.api,"
                                + "org.apache.aries.spifly.aop.sample.interf")
                .attribute("Bundle-Activator",
                        "org.apache.aries.spifly.aop.sample.Activator")
                .attribute("SPI-Consumer", "dom;provider-name=\"notmyimpl\"")
                .end();

        addResourcesToGeneratedClientBundle(testBundle);
        testBundle.end();

        // Created in the current 'user.dir'. This is run outside of the
        // container, so 'user.dir' will point to one of the Maven projects and
        // not to Pax Exam working dir.
        File file = new File(getTempLocation(), "client2.jar");
        FileOutputStream fout = new FileOutputStream(file);
        testBundle.writeOut(fout);
        fout.close();
    }

    private static void generateBundleWithSatisfiableSPIConsumerHeader()
            throws IOException, FileNotFoundException {

        // this bundle does not provide the interface; it' imports it from the
        // bundle above

        ZipFixture testBundle = ArchiveFixture
                .newJar()
                .manifest()
                .symbolicName("org.apache.aries.spifly.sample.client")
                .attribute("Bundle-Version", "3.0.0")
                /*
                 * Symbolic name 'org.aspectj.runtime' should be used. This is
                 * the original name of the AspectJ artifact and also the
                 * symbolic name of the AspectJ bundle shipped with Equinox
                 * Aspects. However, we're now using AspectJ from SpringSource
                 * with a different name:
                 * 'com.springsource.org.aspectj.runtime'.
                 */
                .attribute("Require-Bundle",
                        "com.springsource.org.aspectj.runtime")
                .attribute(
                        "Import-Package",
                        "javax.xml.parsers,org.osgi.framework,"
                                + "org.aspectj.lang,org.aspectj.runtime.internal,"
                                + "org.apache.aries.spifly.api,"
                                + "org.apache.aries.spifly.aop.sample.interf")
                .attribute("Bundle-Activator",
                        "org.apache.aries.spifly.aop.sample.Activator")
                .attribute("SPI-Consumer", "dom;provider-name=\"myimpl\"")
                .end();

        addResourcesToGeneratedClientBundle(testBundle);
        testBundle.end();

        // Created in the current 'user.dir'. This is run outside of the
        // container, so 'user.dir' will point to one of the Maven projects and
        // not to Pax Exam working dir.
        File file = new File(getTempLocation(), "client3.jar");
        FileOutputStream fout = new FileOutputStream(file);
        testBundle.writeOut(fout);
        fout.close();
    }

    private static void addResourcesToGeneratedClientBundle(
            ZipFixture testBundle) throws IOException {
        // adds oridnary classes, the aspect, and aop.xml
        testBundle
                .binary(
                        "org/apache/aries/spifly/aop/sample/Activator.class",
                        org.apache.aries.spifly.aop.sample.Activator.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/aop/sample/Activator.class"))
                .binary(
                        "org/apache/aries/spifly/aop/sample/BundleAspect.class",
                        org.apache.aries.spifly.aop.sample.Activator.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/aop/sample/BundleAspect.class"))
                .binary(
                        "META-INF/aop.xml",
                        org.apache.aries.spifly.aop.sample.Activator.class
                                .getClassLoader().getResourceAsStream(
                                        "aop-resources/aop.xml"))
                .binary(
                        "org/apache/aries/spifly/aop/sample/TestImpl.class",
                        org.apache.aries.spifly.aop.sample.Activator.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/aop/sample/TestImpl.class"))

                .binary(
                        "org/apache/aries/spifly/aop/sample/HeaderParser.class",
                        org.apache.aries.spifly.aop.sample.Activator.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/aop/sample/HeaderParser.class"))
                .binary(
                        "org/apache/aries/spifly/aop/sample/HeaderParser$PathElement.class",
                        org.apache.aries.spifly.aop.sample.Activator.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/aop/sample/HeaderParser$PathElement.class"));
    }

    @Test
    public void testCustomizerWithEquinoxWeaving() throws Exception {

        // Load the impl, the bundle that contains the aspect is explicitly
        // defined in the Pax Exam config and doesn't need to be installed here.

        Bundle bundle = bundleContext.installBundle("file:impl.jar");
        bundle.start();

        // TODO find a way to verify that the SPI Fly customizer has
        // processed the impl bundle and AspectJ has processed our two client
        // bundles.
        Thread.sleep(5000);

        // Let's ask the test bundle for an impl. This test bundle will set
        // context classloader to its own classloader and then invoke
        // ServiceLoader. This will help the aspect do its job.

        // Client1 does not contain the SPI-Consumer header. Any impl will work
        // with this client.
        TestInterface service = getOsgiService(TestInterface.class,
                "(myversion=1.0.0)", 10000);
        assertNotNull(service);

        DocumentBuilderFactory impl = service.getImpl();
        assertNotNull(impl);
        assertEquals("org.apache.aries.spifly.sample.TestDomBuilderFactory",
                impl.getClass().getName());

        // Now a bundle with a SPI-Consumer header that will prevent the impl
        // that is available in this test from being used.
        TestInterface service2 = getOsgiService(TestInterface.class,
                "(myversion=2.0.0)", 10000);
        assertNotNull(service2);

        try {
            impl = service2.getImpl();
            fail("An exception should have been thrown");
        } catch (NoSuchElementException e) {
            // the iterator returned by ServiceLoader will be empty as for DOM
            // this bundle needs a provider with name notmyimpl, but only a
            // provider with name myimpl is available
        }

        // Now a bundle with a SPI-Consumer header that let's the impl that is
        // available to be used
        TestInterface service3 = getOsgiService(TestInterface.class,
                "(myversion=3.0.0)", 10000);
        assertNotNull(service3);
        impl = service.getImpl();
        assertNotNull(impl);
        assertEquals("org.apache.aries.spifly.sample.TestDomBuilderFactory",
                impl.getClass().getName());

    }

}
