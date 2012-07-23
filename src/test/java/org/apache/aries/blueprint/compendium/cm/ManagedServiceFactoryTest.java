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
package org.apache.aries.blueprint.compendium.cm;

import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ManagedServiceFactoryTest extends BaseTest {

    @Override
    protected String getBlueprintDescriptor() {
        return "org/apache/aries/blueprint/compendium/cm/ManagedServiceFactoryTest.xml";
    }

    @Test
    public void test1() throws Exception {
        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);

        BundleContext context = getBundleContext();
        ServiceReference sr = Helper.getOsgiServiceReference(context, Foo.class, "(key=foo1)", Helper.DEFAULT_TIMEOUT);
        assertNotNull(sr);
        Foo foo = (Foo) context.getService(sr);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());
        assertEquals("5", sr.getProperty("a"));
        assertNull(sr.getProperty("b"));

        props = new Hashtable<String,String>();
        props.put("b", "foo");
        cf.update(props);
        Thread.sleep(500);

        // No update of bean after creation
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());

        // Only initial update of service properties
        assertEquals("5", sr.getProperty("a"));
        assertNull(sr.getProperty("b"));
    }

    @Test
    public void test2() throws Exception {
        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory2", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);

        BundleContext context = getBundleContext();
        ServiceReference sr = Helper.getOsgiServiceReference(context, Foo.class, "(key=foo2)", Helper.DEFAULT_TIMEOUT);
        assertNotNull(sr);

        Foo foo = (Foo) context.getService(sr);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());
        assertNull(sr.getProperty("a"));
        assertNull(sr.getProperty("b"));

        props = new Hashtable<String,String>();
        props.put("b", "foo");
        cf.update(props);

        // Update after creation
        Thread.sleep(500);
        assertEquals(5, foo.getA());
        assertEquals("foo", foo.getB());

        // No update of service properties
        assertNull(sr.getProperty("a"));
        assertNull(sr.getProperty("b"));
    }

    @Test
    public void test3() throws Exception {
        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory3", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);

        BundleContext context = getBundleContext();
        ServiceReference sr = Helper.getOsgiServiceReference(context, Foo.class, "(key=foo3)", Helper.DEFAULT_TIMEOUT);
        assertNotNull(sr);

        Foo foo = (Foo) context.getService(sr);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());
        assertEquals("5", sr.getProperty("a"));
        assertNull(sr.getProperty("b"));

        props = new Hashtable<String,String>();
        props.put("b", "foo");
        cf.update(props);

        // Update after creation
        Thread.sleep(500);
        assertEquals(5, foo.getA());
        assertEquals("foo", foo.getB());

        // Update of service properties
        assertEquals("5", sr.getProperty("a"));
        assertEquals("foo", sr.getProperty("b"));
    }

}
