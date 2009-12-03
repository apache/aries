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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_LOCATION;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_SYMBOLIC_NAME;
import static org.osgi.jmx.framework.ServiceStateMBean.EVENT;
import static org.osgi.jmx.framework.ServiceStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECT_CLASS;
import static org.osgi.jmx.framework.ServiceStateMBean.SERVICE_EVENT_TYPE;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class ServiceEventDataTest {

    
    @Test
    public void testToCompositeData() throws Exception {

        ServiceEvent event = mock(ServiceEvent.class);
        ServiceReference reference = mock(ServiceReference.class);
        Bundle bundle = mock(Bundle.class);
        
        when(event.getType()).thenReturn(ServiceEvent.REGISTERED);
        when(event.getServiceReference()).thenReturn(reference);
        when(reference.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(44));
        when(reference.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] {"org.apache.aries.jmx.Mock"});
        when(reference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(new Long(1));
        when(bundle.getLocation()).thenReturn("string");
        when(bundle.getSymbolicName()).thenReturn("org.apache.aries.jmx.core");
        
        ServiceEventData eventData = new ServiceEventData(event);
        CompositeData data = eventData.toCompositeData();
        
        assertEquals(new Long(44), data.get(IDENTIFIER));
        assertEquals(new Long(1), data.get(BUNDLE_IDENTIFIER));
        assertEquals("string", data.get(BUNDLE_LOCATION));
        assertEquals("org.apache.aries.jmx.core", data.get(BUNDLE_SYMBOLIC_NAME));
        assertArrayEquals(new String[] {"org.apache.aries.jmx.Mock" }, (String[]) data.get(OBJECT_CLASS));
        assertEquals(ServiceEvent.REGISTERED, data.get(EVENT));
        
    }

    
    @Test
    public void testFrom() throws Exception {
        
        Map<String, Object> items = new HashMap<String, Object>();
        items.put(IDENTIFIER, new Long(7));
        items.put(BUNDLE_IDENTIFIER, new Long(67));
        items.put(BUNDLE_LOCATION, "string");
        items.put(BUNDLE_SYMBOLIC_NAME, "test");
        items.put(OBJECT_CLASS, new String[] {"org.apache.aries.jmx.Mock" });
        items.put(EVENT, ServiceEvent.MODIFIED);

        CompositeData compositeData = new CompositeDataSupport(SERVICE_EVENT_TYPE, items);
        ServiceEventData event = ServiceEventData.from(compositeData);
        
        assertEquals(7, event.getServiceId());
        assertEquals(67, event.getBundleId());
        assertArrayEquals(new String[] {"org.apache.aries.jmx.Mock" }, event.getServiceInterfaces());
        assertEquals("test", event.getBundleSymbolicName());
        assertEquals("string", event.getBundleLocation());
        assertEquals(ServiceEvent.MODIFIED, event.getEventType());
        
    }

}
