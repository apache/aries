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
package org.apache.aries.blueprint.itests;

import static org.apache.aries.blueprint.itests.Helper.blueprintBundles;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

import java.io.InputStream;
import java.net.URL;

import org.apache.aries.blueprint.itests.cm.service.Foo;
import org.apache.aries.blueprint.itests.cm.service.FooFactory;
import org.apache.aries.blueprint.itests.cm.service.FooInterface;
import org.apache.aries.blueprint.services.ParserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


public class ParserServiceIgnoreUnknownNamespaceHandlerTest extends AbstractBlueprintIntegrationTest {
    private static final String CM_BUNDLE = "org.apache.aries.blueprint.cm";
    private static final String TEST_BUNDLE = "org.apache.aries.blueprint.cm.test.b1";

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.EXPORT_PACKAGE, Foo.class.getPackage().getName());
        probe.setHeader(Constants.IMPORT_PACKAGE, Foo.class.getPackage().getName());
        return probe;
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        InputStream testBundle = createTestBundle();
        return new Option[] {
            baseOptions(),
            frameworkProperty("org.apache.aries.blueprint.parser.service.ignore.unknown.namespace.handlers").value("true"),
            blueprintBundles(),
            keepCaches(),
            streamBundle(testBundle)
        };
    }

    private InputStream createTestBundle() {
        return TinyBundles.bundle()
    		.add(FooInterface.class)
    		.add(Foo.class)
    		.add(FooFactory.class)
    		.add("OSGI-INF/blueprint/context.xml", getResource("IgnoreUnknownNamespaceTest.xml"))
    		.set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE)
    		.set(Constants.EXPORT_PACKAGE, Foo.class.getPackage().getName())
    		.set(Constants.IMPORT_PACKAGE, Foo.class.getPackage().getName())
    		.build(TinyBundles.withBnd());
    }

    @Before
    public void stopCM() throws BundleException {
        context().getBundleByName(CM_BUNDLE).stop();
    }

    @After
    public void startCM() throws BundleException {
        context().getBundleByName(CM_BUNDLE).start();
    }

    @Test
    public void testIgnoreTrue() throws Exception {
        ParserService parserService = context().getService(ParserService.class);
        URL blueprintXML = context().getBundleByName(TEST_BUNDLE).getEntry("OSGI-INF/blueprint/context.xml");
        // ensure there is no error parsing while CM is stopped
        parserService.parse(blueprintXML, context().getBundleByName(TEST_BUNDLE));
    }
}
