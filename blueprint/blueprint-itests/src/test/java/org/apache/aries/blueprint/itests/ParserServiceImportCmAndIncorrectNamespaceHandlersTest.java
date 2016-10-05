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

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.itests.cm.handler.IncorrectNamespaceHandler;
import org.apache.aries.blueprint.services.ParserService;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

import static org.apache.aries.blueprint.itests.Helper.blueprintBundles;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

public class ParserServiceImportCmAndIncorrectNamespaceHandlersTest extends AbstractBlueprintIntegrationTest {

    private static final String NS_HANDLER_BUNDLE = "org.apache.aries.blueprint.incorrect";
    private static final String TEST_BUNDLE = "org.apache.aries.blueprint.aries1503.test";

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return new Option[] {
                baseOptions(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                blueprintBundles(),
                keepCaches(),
                streamBundle(createIncorrectNamespaceHandlerBundle()),
                streamBundle(createTestBundle())
        };
    }

    private InputStream createIncorrectNamespaceHandlerBundle() {
        return TinyBundles.bundle()
                .add(IncorrectNamespaceHandler.class)
                .add("OSGI-INF/blueprint/incorrect.xml", getResource("incorrect.xml"))
                .add("incorrect-1.0.0.xsd", getResource("incorrect-1.0.0.xsd"))
                .add("incorrect-1.1.0.xsd", getResource("incorrect-1.1.0.xsd"))
                .set(Constants.BUNDLE_SYMBOLICNAME, NS_HANDLER_BUNDLE)
                .set(Constants.EXPORT_PACKAGE, IncorrectNamespaceHandler.class.getPackage().getName())
                .set(Constants.IMPORT_PACKAGE, "org.apache.aries.blueprint,org.apache.aries.blueprint.ext," +
                        "org.apache.aries.blueprint.mutable," +
                        "org.apache.aries.blueprint.compendium.cm," +
                        "org.osgi.service.blueprint.reflect,org.w3c.dom")
                .build(TinyBundles.withBnd());
    }

    private InputStream createTestBundle() {
        return TinyBundles.bundle()
                .add("OSGI-INF/blueprint/ImportIncorrectAndCmNamespacesTest.xml", getResource("ImportIncorrectAndCmNamespacesTest.xml"))
                .set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE)
                .set(Constants.IMPORT_PACKAGE, IncorrectNamespaceHandler.class.getPackage().getName()
                        + ",org.apache.aries.blueprint,org.apache.aries.blueprint.ext," +
                        "org.apache.aries.blueprint.mutable," +
                        "org.osgi.service.blueprint.reflect,org.w3c.dom")
                .build(TinyBundles.withBnd());
    }

    @Test
    public void testXSDImports() throws Exception {
        waitForConfig();
        ParserService parserService = context().getService(ParserService.class);
        URL blueprintXML = context().getBundleByName(TEST_BUNDLE).getEntry("OSGI-INF/blueprint/ImportIncorrectAndCmNamespacesTest.xml");
        ComponentDefinitionRegistry cdr = parserService.parse(blueprintXML, context().getBundleByName(TEST_BUNDLE));
        assertNotNull(cdr.getComponentDefinition("aries-1503"));
    }

    private void waitForConfig() throws InterruptedException {
        final CountDownLatch ready = new CountDownLatch(2);
        final AtomicBoolean failure = new AtomicBoolean(false);
        @SuppressWarnings("rawtypes")
        ServiceRegistration reg = context().registerService(
                BlueprintListener.class,
                new BlueprintListener() {
                    @Override
                    public void blueprintEvent(BlueprintEvent event) {
                        if (NS_HANDLER_BUNDLE.equals(event.getBundle().getSymbolicName())
                                && BlueprintEvent.CREATED == event.getType()) {
                            ready.countDown();
                        } else if (TEST_BUNDLE.equals(event.getBundle().getSymbolicName())
                                && (BlueprintEvent.CREATED == event.getType() || BlueprintEvent.FAILURE == event.getType())) {
                            ready.countDown();
                            if (BlueprintEvent.FAILURE == event.getType()) {
                                failure.set(true);
                            }
                        }
                    }
                },
                null);
        try {
            assertTrue(ready.await(3000, TimeUnit.MILLISECONDS));
            assertFalse("org.apache.aries.blueprint.aries1503.test bundle should successfully start Blueprint container",
                    failure.get());
        } finally {
            reg.unregister();
        }
    }

}
