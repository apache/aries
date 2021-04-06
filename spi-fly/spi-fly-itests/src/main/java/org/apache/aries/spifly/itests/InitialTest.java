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
package org.apache.aries.spifly.itests;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.test.assertj.bundle.BundleAssert;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit5.context.BundleContextExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.aries.spifly.itests.util.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BundleContextExtension.class)
public class InitialTest {

    static URI testBase;
    static Path testBasePath;
    static Path examplesBasePath;

    @InjectBundleContext
    BundleContext bundleContext;

    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut = System.out;

    @BeforeAll
    static void beforeAll(@InjectBundleContext BundleContext bundleContext) {
        testBase = URI.create(bundleContext.getProperty("test.base"));
        testBasePath = Paths.get(testBase);
        examplesBasePath = testBasePath.getParent().resolve(
            "spi-fly-examples"
        );
    }

    @BeforeEach
    public void beforeEachTest() {
        System.setOut(new PrintStream(new TeeOutputStream(outContent, originalOut)));
    }

    @AfterEach
    public void afterEachTest() {
        System.setOut(originalOut);
    }

    @Test
    public void example1() throws Exception {
        assertBundleInstallation(getExampleJar("spi-fly-example-provider1-bundle"));
        assertBundleInstallation(getExampleJar("spi-fly-example-client1-bundle"));

        assertThat(
            outContent.toString()
        ).contains(
            "*** Result from invoking the SPI consume via library:",
            "Doing it!"
        ).doesNotContain("Doing it too!", "Doing it as well!");
    }

    @Test
    public void example2() throws Exception {
        assertBundleInstallation(getExampleJar("spi-fly-example-provider2-bundle"));
        assertBundleInstallation(getExampleJar("spi-fly-example-client2-bundle"));

        assertThat(
            outContent.toString()
        ).contains(
            "*** Result from invoking the SPI directly:",
            "Doing it too!"
        ).doesNotContain("Doing it!", "Doing it as well!");
    }

    @Test
    public void example3() throws Exception {
        Bundle provider3fragment = assertBundleInstallation(getExampleJar("spi-fly-example-provider3-fragment"), true);
        Bundle provider3Bundle = assertBundleInstallation(getExampleJar("spi-fly-example-provider3-bundle"));
        BundleAssert.assertThat(provider3fragment).isFragment().isInState(Bundle.RESOLVED);
        assertFragmentAttached(provider3Bundle, provider3fragment);

        Bundle client3fragment = assertBundleInstallation(getExampleJar("spi-fly-example-client3-fragment"), true);
        Bundle client3Bundle = assertBundleInstallation(getExampleJar("spi-fly-example-client3-bundle"));
        BundleAssert.assertThat(client3fragment).isFragment().isInState(Bundle.RESOLVED);
        assertFragmentAttached(client3Bundle, client3fragment);

        assertThat(
            outContent.toString()
        ).contains(
            "*** Result from invoking the SPI from untreated bundle:",
            "Doing it as well!"
        ).doesNotContain("Doing it!", "Doing it too!");
    }

    @Test
    public void example4() throws Exception {
        assertBundleInstallation(getExampleJar("spi-fly-example-resource-provider-bundle"));
        assertBundleInstallation(getExampleJar("spi-fly-example-resource-client-bundle"));

        assertThat(
            outContent.toString()
        ).contains(
            "*** First line of content:",
            "This is a test resource."
        );
    }

    Bundle assertBundleInstallation(Path bundleJar) throws Exception {
        return assertBundleInstallation(bundleJar, false);
    }

    Bundle assertBundleInstallation(Path bundleJar, boolean fragment) throws Exception {
        assertThat(bundleJar).exists();
        Bundle bundle = bundleContext.installBundle(
            bundleJar.toString(),
            bundleJar.toUri().toURL().openConnection().getInputStream());
        BundleAssert.assertThat(bundle).isInState(Bundle.INSTALLED);
        if (!fragment) {
            bundle.start();
            BundleAssert.assertThat(bundle).isInState(Bundle.ACTIVE);
        }
        return bundle;
    }

    void assertFragmentAttached(Bundle bundle, Bundle fragment) throws Exception {
        BundleAssert.assertThat(
            bundle.adapt(
                BundleWiring.class
            ).getProvidedWires(
                HostNamespace.HOST_NAMESPACE
            ).stream().map(
                BundleWire::getRequirerWiring
            ).map(
                BundleWiring::getBundle
            ).findFirst().orElseThrow(
                () -> new Exception("Fragment did not attach")
            )
        ).hasBundleId(
            fragment.getBundleId()
        );
    }

    Path getExampleJar(String mavenModuleName) throws IOException {
        Path bundle1TargetDirPath = examplesBasePath.resolve(
            mavenModuleName
        ).resolve(
            "target"
        );

        assertThat(bundle1TargetDirPath).exists();

        return Files.list(bundle1TargetDirPath).filter(
            p -> {
                String name = p.getFileName().toString();
                return name.endsWith(".jar") && !name.endsWith("-javadoc.jar") && !name.endsWith("-sources.jar");
            }
        ).findFirst().orElseThrow(
            () -> new FileNotFoundException("Could not find jar for " + mavenModuleName)
        );
    }

}
