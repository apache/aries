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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.itests.cm.handler.Aries1682NamespaceHandler;
import org.apache.aries.blueprint.services.ParserService;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

import static org.apache.aries.blueprint.itests.Helper.blueprintBundles;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

public class ParserServiceImportAndIncludeXSDsTest extends AbstractBlueprintIntegrationTest {

    private static final String NS_HANDLER_BUNDLE = "org.apache.aries.blueprint.aries1682";
    private static final String TEST_BUNDLE = "org.apache.aries.blueprint.aries1682.test";

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return new Option[] {
                baseOptions(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
                blueprintBundles(),
                keepCaches(),
                streamBundle(createAries1682NamespaceHandlerBundle()),
                streamBundle(createTestBundle())
        };
    }

    private InputStream createAries1682NamespaceHandlerBundle() {
        return TinyBundles.bundle()
                .add(Aries1682NamespaceHandler.class)
                .add("OSGI-INF/blueprint/blueprint-aries-1682.xml", getResource("blueprint-aries-1682.xml"))
                .add("blueprint-aries-1682.xsd", getResource("blueprint-aries-1682.xsd"))
                .add("blueprint-aries-1682-common.xsd", getResource("blueprint-aries-1682-common.xsd"))
                .set(Constants.BUNDLE_SYMBOLICNAME, NS_HANDLER_BUNDLE)
                .set(Constants.EXPORT_PACKAGE, Aries1682NamespaceHandler.class.getPackage().getName())
                .set(Constants.IMPORT_PACKAGE, "org.apache.aries.blueprint," +
                        "org.apache.aries.blueprint.ext," +
                        "org.apache.aries.blueprint.mutable," +
                        FrameworkUtil.class.getPackage().getName() + "," +
                        "org.osgi.service.blueprint.reflect," +
                        "org.w3c.dom")
                .build(TinyBundles.withClassicBuilder());
    }

    private InputStream createTestBundle() {
        return TinyBundles.bundle()
                .add("OSGI-INF/blueprint/ImportNamespacesWithXSDIncludeTest.xml", getResource("ImportNamespacesWithXSDIncludeTest.xml"))
                .set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE)
                .set(Constants.IMPORT_PACKAGE, Aries1682NamespaceHandler.class.getPackage().getName()
                        + ",org.apache.aries.blueprint,org.apache.aries.blueprint.ext")
                .build(TinyBundles.withBnd());
    }

    @Test
    public void testXSDImports() throws Exception {
        assertTrue(TEST_BUNDLE + " should correctly register blueprint container", waitForConfig());
        ParserService parserService = context().getService(ParserService.class);
        URL blueprintXML = context().getBundleByName(TEST_BUNDLE).getEntry("OSGI-INF/blueprint/ImportNamespacesWithXSDIncludeTest.xml");
        ComponentDefinitionRegistry cdr = parserService.parse(blueprintXML, context().getBundleByName(TEST_BUNDLE));
        assertNotNull(cdr.getComponentDefinition("aries-1682"));
    }

    private boolean waitForConfig() throws InterruptedException {
        final AtomicBoolean ready = new AtomicBoolean();
        @SuppressWarnings("rawtypes")
        ServiceRegistration reg = context().registerService(
                BlueprintListener.class,
                new BlueprintListener() {
                    @Override
                    public void blueprintEvent(BlueprintEvent event) {
                        if (TEST_BUNDLE.equals(event.getBundle().getSymbolicName())
                                && BlueprintEvent.CREATED == event.getType()) {
                            synchronized (ready) {
                                ready.set(true);
                                ready.notify();
                            }
                        }
                    }
                },
                null);
        try {
            synchronized (ready) {
                if (!ready.get()) {
                    ready.wait(3000);
                }
            }
            return ready.get();
        } finally {
            reg.unregister();
        }
    }

}
