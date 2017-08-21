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
package org.apache.aries.blueprint.itests.cm;

import java.io.InputStream;
import java.util.Hashtable;
import javax.inject.Inject;

import org.apache.aries.blueprint.itests.AbstractBlueprintIntegrationTest;
import org.apache.aries.blueprint.itests.Helper;
import org.apache.aries.blueprint.itests.cm.service.Foo;
import org.apache.aries.blueprint.itests.cm.service.FooFactory;
import org.apache.aries.blueprint.itests.cm.service.FooInterface;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

public class CmPropertiesTest extends AbstractBlueprintIntegrationTest {
    private static final String TEST_BUNDLE = "org.apache.aries.blueprint.cm.test.b1";

    @Inject
    ConfigurationAdmin ca;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.EXPORT_PACKAGE, Foo.class.getPackage().getName());
        probe.setHeader(Constants.IMPORT_PACKAGE, Foo.class.getPackage().getName());
        return probe;
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return new Option[] {
            baseOptions(), 
            Helper.blueprintBundles(), 
            keepCaches(),
            streamBundle(testBundle())
        };
    }

    protected InputStream testBundle() {
        return TinyBundles.bundle() //
            .add(FooInterface.class) //
            .add(Foo.class) //
            .add(FooFactory.class) //
            .add("OSGI-INF/blueprint/context.xml", getResource("CmPropertiesTest.xml"))
            .set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE) //
            .set(Constants.EXPORT_PACKAGE, Foo.class.getPackage().getName()) //
            .set(Constants.IMPORT_PACKAGE, Foo.class.getPackage().getName()) //
            .build(TinyBundles.withBnd());
    }

    @Test
    public void testProperties() throws Exception {
        ServiceReference sr = getServiceRef(FooInterface.class, "(key=foo4)");
        assertNotNull(sr);
        FooInterface foo = (FooInterface)context().getService(sr);
        assertNotNull(foo);
        assertNotNull(foo.getProps());
        assertTrue(foo.getProps().isEmpty());

        Configuration cf = ca.getConfiguration("blueprint-sample-properties.pid", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);
        Thread.sleep(500);
        assertFalse(foo.getProps().isEmpty());
        assertEquals("5", foo.getProps().getProperty("a"));

        props.put("a", "6");
        cf.update(props);
        Thread.sleep(500);
        assertFalse(foo.getProps().isEmpty());
        assertEquals("6", foo.getProps().getProperty("a"));

        cf.delete();
        Thread.sleep(500);
        assertNull(foo.getProps().getProperty("a"));
    }

    @SuppressWarnings("rawtypes")
    private ServiceReference getServiceRef(Class serviceInterface, String filter)
            throws InvalidSyntaxException {
        int tries = 0;
        do {
            ServiceReference[] srAr = bundleContext.getServiceReferences(serviceInterface.getName(), filter);
            if (srAr != null && srAr.length > 0) {
                return srAr[0];
            }
            tries++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        } while (tries < 100);
        throw new RuntimeException("Could not find service " + serviceInterface.getName() + ", " + filter);
    }

}
