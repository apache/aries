/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx.codec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osgi.jmx.JmxConstants.BOOLEAN;
import static org.osgi.jmx.JmxConstants.KEY;
import static org.osgi.jmx.JmxConstants.LONG;
import static org.osgi.jmx.JmxConstants.P_BOOLEAN;
import static org.osgi.jmx.JmxConstants.STRING;
import static org.osgi.jmx.JmxConstants.TYPE;
import static org.osgi.jmx.JmxConstants.VALUE;
import static org.osgi.jmx.framework.BundleStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECT_CLASS;
import static org.osgi.jmx.framework.ServiceStateMBean.PROPERTIES;
import static org.osgi.jmx.framework.ServiceStateMBean.SERVICE_TYPE;
import static org.osgi.jmx.framework.ServiceStateMBean.USING_BUNDLES;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.JmxConstants;
import org.osgi.jmx.framework.ServiceStateMBean;

/**
 * @version $Rev$ $Date$
 */
public class ServiceDataTest {
    @Test
    public void testToCompositeData() throws Exception {
        ServiceReference<?> reference = mock(ServiceReference.class);
        Bundle bundle = mock(Bundle.class);
        String[] interfaces = new String[] { "org.apache.aries.jmx.Test", "org.apache.aries.jmx.Mock" };

        Bundle b1 = mock(Bundle.class);
        when(b1.getBundleId()).thenReturn(new Long(6));
        Bundle b2 = mock(Bundle.class);
        when(b2.getBundleId()).thenReturn(new Long(9));


        when(reference.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(98));
        when(reference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(new Long(34));
        when(reference.getProperty(Constants.OBJECTCLASS)).thenReturn(interfaces);
        when(reference.getUsingBundles()).thenReturn(new Bundle[] { b1, b2 });
        when(reference.getPropertyKeys()).thenReturn( new String[] {"x.vendor", "x.domain", "x.index", "x.optimized" } );
        when(reference.getProperty("x.vendor")).thenReturn("aries");
        when(reference.getProperty("x.domain")).thenReturn("test");
        when(reference.getProperty("x.index")).thenReturn(new Long(67));
        when(reference.getProperty("x.optimized")).thenReturn(true);


        ServiceData serviceData = new ServiceData(reference);

        CompositeData compositeData = serviceData.toCompositeData();
        assertEquals(new Long(98), compositeData.get(IDENTIFIER));
        assertEquals(new Long(34), compositeData.get(BUNDLE_IDENTIFIER));
        assertArrayEquals( new Long[] {new Long(6), new Long(9)}, (Long[]) compositeData.get(USING_BUNDLES));
        assertArrayEquals(interfaces, (String[]) compositeData.get(OBJECT_CLASS));

        TabularData propertiesTable = (TabularData) compositeData.get(PROPERTIES);

        @SuppressWarnings("unchecked")
        Collection<CompositeData> propertyData = (Collection<CompositeData>) propertiesTable.values();

        assertEquals(4, propertyData.size());
        for (CompositeData propertyRow: propertyData) {
            String key = (String) propertyRow.get(KEY);
            if (key.equals("x.vendor")) {
                assertEquals("aries", propertyRow.get(VALUE));
                assertEquals(STRING, propertyRow.get(TYPE));
            } else if (key.equals("x.domain")) {
                assertEquals("test", propertyRow.get(VALUE));
                assertEquals(STRING, propertyRow.get(TYPE));
            } else if (key.equals("x.index")) {
                assertEquals("67", propertyRow.get(VALUE));
                assertEquals(LONG, propertyRow.get(TYPE));
            } else if (key.equals("x.optimized")) {
                assertEquals("true", propertyRow.get(VALUE));
                assertEquals(BOOLEAN, propertyRow.get(TYPE));
            } else {
                fail("unknown key parsed from properties");
            }
        }
    }

    @Test
    public void testFromCompositeData() throws Exception {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put(IDENTIFIER, new Long(99));
        items.put(BUNDLE_IDENTIFIER, new Long(5));
        items.put(USING_BUNDLES, new Long[] { new Long(10), new Long(11) });
        items.put(OBJECT_CLASS, new String[] { "org.apache.aries.jmx.Test", "org.apache.aries.jmx.Mock" });
        TabularData propertyTable = new TabularDataSupport(JmxConstants.PROPERTIES_TYPE);
        propertyTable.put(PropertyData.newInstance("a", true).toCompositeData());
        propertyTable.put(PropertyData.newInstance("b", "value").toCompositeData());
        propertyTable.put(PropertyData.newInstance("c", new int[] {1, 2}).toCompositeData());
        propertyTable.put(PropertyData.newInstance("d", new Long[] {new Long(3), new Long(4)}).toCompositeData());
        items.put(ServiceStateMBean.PROPERTIES, propertyTable);
        CompositeData compositeData = new CompositeDataSupport(SERVICE_TYPE, items);

        ServiceData data = ServiceData.from(compositeData);
        assertEquals(99, data.getServiceId());
        assertEquals(5, data.getBundleId());
        assertArrayEquals(new long[] {10, 11}, data.getUsingBundles());
        assertArrayEquals(new String[] { "org.apache.aries.jmx.Test", "org.apache.aries.jmx.Mock" }, data.getServiceInterfaces());

        List<PropertyData<? extends Object>> properties = data.getProperties();
        assertEquals(4, properties.size());

        for (PropertyData<? extends Object> property: properties) {
            if (property.getKey().equals("a")) {
                assertTrue((Boolean) property.getValue());
                assertEquals(P_BOOLEAN, property.getEncodedType());
            } else if (property.getKey().equals("b")) {
                assertEquals("value", property.getValue());
                assertEquals(STRING, property.getEncodedType());
            } else if (property.getKey().equals("c")) {
                assertArrayEquals(new int[] { 1, 2 }, (int[]) property.getValue());
                assertEquals("Array of int", property.getEncodedType());
                assertEquals("1,2", property.getEncodedValue());
            } else if (property.getKey().equals("d")) {
                assertArrayEquals(new Long[] {new Long(3), new Long(4) }, (Long[]) property.getValue());
                assertEquals("Array of Long", property.getEncodedType());
                assertEquals("3,4", property.getEncodedValue());
            } else {
                fail("unknown key parsed from properties");
            }
        }
    }
}
