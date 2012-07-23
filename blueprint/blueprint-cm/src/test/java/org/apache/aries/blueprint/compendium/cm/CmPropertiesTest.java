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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CmPropertiesTest extends BaseTest {

    @Override
    protected String getBlueprintDescriptor() {
        return "org/apache/aries/blueprint/compendium/cm/CmPropertiesTest.xml";
    }

    @Test
    public void test4() throws Exception {
        BundleContext context = getBundleContext();
        ServiceReference sr = Helper.getOsgiServiceReference(context, FooInterface.class, "(key=foo4)", Helper.DEFAULT_TIMEOUT);
        assertNotNull(sr);

        FooInterface foo = (FooInterface) context.getService(sr);
        assertNotNull(foo);
        assertNotNull(foo.getProps());
        assertTrue(foo.getProps().isEmpty());

        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-properties.pid", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);

        Thread.sleep(500);
        assertFalse(foo.getProps().isEmpty());
        assertEquals("5", foo.getProps().getProperty("a"));
    }

}
