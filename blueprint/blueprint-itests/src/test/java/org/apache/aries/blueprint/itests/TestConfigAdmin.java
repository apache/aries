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

import java.util.Currency;
import java.util.Hashtable;

import javax.inject.Inject;

import org.apache.aries.blueprint.sample.Foo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class TestConfigAdmin extends AbstractBlueprintIntegrationTest {
    @Inject
    ConfigurationAdmin ca;

    @Test
    public void testStrategyNone() throws Exception {
        ca.getConfiguration("blueprint-sample-managed.none", null).update(getConfig1());
        startTestBundle();

        // foo should receive initial configuration
        Foo foo = getComponent("none-managed");
        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());

        // foo should not reflect changes in config
        ca.getConfiguration("blueprint-sample-managed.none", null).update(getConfig2());
        Thread.sleep(100);
        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
    }



    @Test
    public void testStrategyContainer() throws Exception {
        // foo should have received initial configuration
        ca.getConfiguration("blueprint-sample-managed.container", null).update(getConfig1());
        startTestBundle();
        Foo foo = getComponent("container-managed");
        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());

        // foo bean properties should have been updated 
        ca.getConfiguration("blueprint-sample-managed.container", null).update(getConfig2());
        Thread.sleep(100);
        assertEquals(10, foo.getA());
        assertEquals(Currency.getInstance("USD"), foo.getCurrency());
    }

    @Test
    public void testStrategyComponent() throws Exception {
        // foo should receive initial configuration
        ca.getConfiguration("blueprint-sample-managed.component", null).update(getConfig1());
        startTestBundle();
        Foo foo = getComponent("component-managed");
        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());

        // Foo.update() should have been called but the bean properties should not have been updated
        ca.getConfiguration("blueprint-sample-managed.component", null).update(getConfig2());
        Thread.sleep(100);
        assertEquals(5, foo.getA());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertNotNull(foo.getProps());
        assertEquals("10", foo.getProps().get("a"));
        assertEquals("USD", foo.getProps().get("currency"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testManagedServiceFactory() throws Exception {
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory", null);
        cf.update(getConfig1());
        startTestBundle();
        
        // Make sure only one service is registered
        // Ask the service registry, not the container, since the container might have got it wrong :)
        Foo foo = context().getService(Foo.class, "(service.pid=blueprint-sample-managed-service-factory.*)");
        ServiceReference[] refs = context().getAllServiceReferences(Foo.class.getName(), "(service.pid=blueprint-sample-managed-service-factory.*)");
        assertNotNull("No services were registered for the managed service factory", refs);
        assertEquals("Multiple services were registered for the same pid.", 1, refs.length);
    }

    @Test
    public void testPlaceholder() throws Exception {
        Configuration cf = ca.getConfiguration("blueprint-sample-placeholder", null);
        cf.update(getConfig3());
        startTestBundle();
    }

    private Hashtable<String, String> getConfig1() {
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("currency", "PLN");
        return props;
    }

    private Hashtable<String, String> getConfig2() {
        Hashtable<String, String> props;
        props = new Hashtable<String,String>();
        props.put("a", "10");
        props.put("currency", "USD");
        return props;
    }

    private Hashtable<String, String> getConfig3() {
        Hashtable<String, String> props;
        props = new Hashtable<String,String>();
        props.put("key.b", "10");
        return props;
    }

    private <T>T getComponent(String componentId) {
        BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        @SuppressWarnings("unchecked")
        T component = (T)blueprintContainer.getComponentInstance(componentId);
        assertNotNull(component);
        return component;
    }
    
    private void startTestBundle() throws BundleException {
        Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
        assertNotNull(bundle);
        bundle.start();
    }

    @org.ops4j.pax.exam.Configuration
    public static Option[] configuration() {
        return new Option[] {
            junitBundles(),
            Helper.blueprintBundles(),
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample", false)
        };
    }

}
