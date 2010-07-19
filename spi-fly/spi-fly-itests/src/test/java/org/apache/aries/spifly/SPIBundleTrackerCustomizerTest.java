/**
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class SPIBundleTrackerCustomizerTest extends AbstractIntegrationTest {

    @Before
    public void setItUp() throws Exception {

        // with the SPI-Provider header
        // javax.xml.parsers is a system package
        ZipFixture testBundle = ArchiveFixture
                .newJar()
                .manifest()
                .symbolicName("org.apache.aries.spifly.sample")
                .attribute("Bundle-Version", "1.0.0")
                .attribute("Import-Package", "javax.xml.parsers")
                .attribute("SPI-Provider", "")
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

        FileOutputStream fout = new FileOutputStream("TestSPIBundle.jar");
        testBundle.writeOut(fout);
        fout.close();

        // without the SPI-Provider header
        // javax.xml.parsers and org.xml.sax are system packages
        ZipFixture testBundle2 = ArchiveFixture
                .newJar()
                .manifest()
                .symbolicName("org.apache.aries.spifly.sample2")
                .attribute("Bundle-Version", "1.0.0")
                .attribute("Import-Package", "javax.xml.parsers,org.xml.sax")
                .end()
                .binary(
                        "org/apache/aries/spifly/sample/TestSaxParserFactory.class",
                        org.apache.aries.spifly.sample.TestSaxParserFactory.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "org/apache/aries/spifly/sample/TestSaxParserFactory.class"))
                .file("META-INF/services/javax.xml.parsers.SAXParserFactory",
                        "org.apache.aries.spifly.sample.TestSaxParserFactory\n")
                .end();

        FileOutputStream fout2 = new FileOutputStream("TestSPIBundle2.jar");
        testBundle2.writeOut(fout2);
        fout2.close();
    }

    @After
    public void cleanUp() {
        new File("TestSPIBundle.jar").delete();
        new File("TestSPIBundle2.jar").delete();
    }

    @Test
    public void testProvidersWithandWithoutSpiHeader() throws Exception {

        Bundle bundle = bundleContext.installBundle("file:TestSPIBundle.jar");
        bundle.start();

        Bundle bundle2 = bundleContext.installBundle("file:TestSPIBundle2.jar");
        bundle2.start();

        DocumentBuilderFactory documentBuilderFactory = getOsgiService(
                DocumentBuilderFactory.class, "(spi.provider.url=*)", 10000);
        // this service will be there as the first bundle had the SPI-Provider
        // header defined
        assertNotNull(documentBuilderFactory);

        // we don't need to wait long - DocumentBuilderFactory has been found
        // and the second bundle would also
        // be processed around the time the first one was analyzed
        try {
            getOsgiService(SAXParserFactory.class, "(spi.provider.url=*)", 1000);
        } catch (RuntimeException re) {
            // expected - timeout
            return;
        }
        fail("An exception should have been thrown.");
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() throws IOException {

        Option[] options = options(

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
                mavenBundle("org.osgi", "org.osgi.compendium"),

                mavenBundle("org.apache.aries.testsupport",
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

                equinox().version("3.5.0")

        );
        options = updateOptions(options);
        return options;
    }
}
