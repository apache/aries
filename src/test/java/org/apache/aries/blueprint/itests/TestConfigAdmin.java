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
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import java.util.Currency;
import java.util.Hashtable;

import org.apache.aries.blueprint.sample.Foo;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.apache.aries.itest.ExtraOptions.*;

@RunWith(JUnit4TestRunner.class)
public class TestConfigAdmin extends AbstractIntegrationTest {

    @Test
    public void testStrategyNone() throws Exception {
        ConfigurationAdmin ca = context().getService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-managed.none", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("currency", "PLN");
        cf.update(props);

        Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
        assertNotNull(bundle);
        bundle.start();

        BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        Foo foo = (Foo) blueprintContainer.getComponentInstance("none-managed");
        assertNotNull(foo);

        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());

        props = new Hashtable<String,String>();
        props.put("a", "10");
        props.put("currency", "USD");
        cf = ca.getConfiguration("blueprint-sample-managed.none", null);
        cf.update(props);

        Thread.sleep(100);

        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
    }

    @Test
    public void testStrategyContainer() throws Exception {
        ConfigurationAdmin ca = context().getService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-managed.container", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("currency", "PLN");
        cf.update(props);

        Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
        assertNotNull(bundle);
        bundle.start();

        BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        Foo foo = (Foo) blueprintContainer.getComponentInstance("container-managed");
        assertNotNull(foo);

        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());

        props = new Hashtable<String,String>();
        props.put("a", "10");
        props.put("currency", "USD");
        cf.update(props);

        Thread.sleep(100);

        assertEquals(10, foo.getA());
        assertEquals(Currency.getInstance("USD"), foo.getCurrency());
    }

    @Test
    public void testStrategyComponent() throws Exception {
        ConfigurationAdmin ca = context().getService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-managed.component", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("currency", "PLN");
        cf.update(props);

        Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
        assertNotNull(bundle);
        bundle.start();

        BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        Foo foo = (Foo) blueprintContainer.getComponentInstance("component-managed");
        assertNotNull(foo);

        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());

        props = new Hashtable<String,String>();
        props.put("a", "10");
        props.put("currency", "USD");
        cf.update(props);

        Thread.sleep(100);

        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertNotNull(foo.getProps());
        assertEquals("10", foo.getProps().get("a"));
        assertEquals("USD", foo.getProps().get("currency"));
    }

    @Test
    public void testManagedServiceFactory() throws Exception {

        ConfigurationAdmin ca = context().getService(ConfigurationAdmin.class);
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("currency", "PLN");
        cf.update(props);
        
        Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
        assertNotNull(bundle);
        bundle.start();
        
        BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);
        
        // Make sure only one service is registered
        // Ask the service registry, not the container, since the container might have got it wrong :)
        ServiceReference[] refs = context().getAllServiceReferences(Foo.class.getName(), "(service.pid=blueprint-sample-managed-service-factory.*)");
        
        assertNotNull("No services were registered for the managed service factory", refs);
        assertEquals("Multiple services were registered for the same pid.", 1, refs.length);
        

    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        return testOptions(
            Helper.blueprintBundles(),
            paxLogging("DEBUG"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").noStart(),

            equinox().version("3.5.0")
        );
    }

}
