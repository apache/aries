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
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.itests.cm.handler.Aries1503aNamespaceHandler;
import org.apache.aries.blueprint.itests.cm.handler.Aries1503bNamespaceHandler;
import org.apache.aries.blueprint.services.ParserService;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

public class ParserServiceImportXSDsBetweenNamespaceHandlersTest extends AbstractBlueprintIntegrationTest {

    private static final String NS_HANDLER_BUNDLE = "org.apache.aries.blueprint.aries1503";
    private static final String NS_HANDLER2_BUNDLE = "org.apache.aries.blueprint.aries1503b";
    private static final String TEST_BUNDLE = "org.apache.aries.blueprint.aries1503.test";

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return new Option[] {
                baseOptions(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                blueprintBundles(),
                keepCaches(),
                streamBundle(createAries1503aNamespaceHandlerBundle()).noStart(),
                streamBundle(createAries1503bNamespaceHandlerBundle()),
                streamBundle(createTestBundle())
        };
    }

    private InputStream createAries1503aNamespaceHandlerBundle() {
        return TinyBundles.bundle()
                .add(Aries1503aNamespaceHandler.class)
                .add("OSGI-INF/blueprint/blueprint-aries-1503.xml", getResource("blueprint-aries-1503.xml"))
                .add("blueprint-aries-1503.xsd", getResource("blueprint-aries-1503.xsd"))
                .set(Constants.BUNDLE_SYMBOLICNAME, NS_HANDLER_BUNDLE)
                .set(Constants.EXPORT_PACKAGE, Aries1503aNamespaceHandler.class.getPackage().getName())
                .set(Constants.IMPORT_PACKAGE, "org.apache.aries.blueprint,org.apache.aries.blueprint.ext," +
                        "org.apache.aries.blueprint.mutable," +
                        "org.osgi.service.blueprint.reflect,org.w3c.dom")
                .build(TinyBundles.withBnd());
    }

    private InputStream createAries1503bNamespaceHandlerBundle() {
        return TinyBundles.bundle()
                .add(Aries1503bNamespaceHandler.class)
                // add this class too - we don't want to play with split packages, etc.
                .add(Aries1503aNamespaceHandler.class)
                .add("OSGI-INF/blueprint/blueprint-aries-1503-2.xml", getResource("blueprint-aries-1503-2.xml"))
                .add("blueprint-aries-1503-2.xsd", getResource("blueprint-aries-1503-2.xsd"))
                .add("blueprint-aries-1503.xsd", getResource("blueprint-aries-1503.xsd"))
                .set(Constants.BUNDLE_SYMBOLICNAME, NS_HANDLER2_BUNDLE)
                .set(Constants.EXPORT_PACKAGE, Aries1503bNamespaceHandler.class.getPackage().getName())
                .set(Constants.IMPORT_PACKAGE, "org.apache.aries.blueprint,org.apache.aries.blueprint.ext," +
                        "org.apache.aries.blueprint.mutable," +
                        "org.osgi.service.blueprint.reflect,org.w3c.dom," +
                        Aries1503bNamespaceHandler.class.getPackage().getName())
                .build(TinyBundles.withBnd());
    }

    private InputStream createTestBundle() {
        return TinyBundles.bundle()
                .add("OSGI-INF/blueprint/ImportNamespacesTest.xml", getResource("ImportNamespacesTest.xml"))
                .set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE)
                .set(Constants.IMPORT_PACKAGE, Aries1503bNamespaceHandler.class.getPackage().getName()
                        + ",org.apache.aries.blueprint,org.apache.aries.blueprint.ext")
                .build(TinyBundles.withBnd());
    }

    @Test
    public void testXSDImports() throws Exception {
    	waitForConfig();
        ParserService parserService = context().getService(ParserService.class);
        URL blueprintXML = context().getBundleByName(TEST_BUNDLE).getEntry("OSGI-INF/blueprint/ImportNamespacesTest.xml");
        ComponentDefinitionRegistry cdr = parserService.parse(blueprintXML, context().getBundleByName(TEST_BUNDLE));
        assertNotNull(cdr.getComponentDefinition("aries-1503"));
    }
    
    private void waitForConfig() throws InterruptedException {
    	final AtomicBoolean ready = new AtomicBoolean();
    	@SuppressWarnings("rawtypes")
		ServiceRegistration reg = context().registerService(
    			BlueprintListener.class, 
    			new BlueprintListener() {
    				@Override
    				public void blueprintEvent(BlueprintEvent event) {
    					if ("org.apache.aries.blueprint.aries1503b".equals(event.getBundle().getSymbolicName())
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
    	}
    	finally {
    		reg.unregister();
    	}
    }

}
