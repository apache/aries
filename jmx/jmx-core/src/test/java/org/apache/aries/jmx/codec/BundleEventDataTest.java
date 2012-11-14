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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.osgi.jmx.framework.BundleStateMBean.BUNDLE_EVENT_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.EVENT;
import static org.osgi.jmx.framework.BundleStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.BundleStateMBean.LOCATION;
import static org.osgi.jmx.framework.BundleStateMBean.SYMBOLIC_NAME;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class BundleEventDataTest {

   
    @Test
    public void testToCompositeData() throws Exception {

        BundleEvent event = mock(BundleEvent.class);
        Bundle bundle = mock(Bundle.class);
        when(event.getBundle()).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn("test");
        when(bundle.getBundleId()).thenReturn(new Long(4));
        when(bundle.getLocation()).thenReturn("location");
        when(event.getType()).thenReturn(BundleEvent.INSTALLED);
        
        BundleEventData eventData = new BundleEventData(event);
        CompositeData eventCompositeData = eventData.toCompositeData();
        
        assertEquals(new Long(4), (Long) eventCompositeData.get(IDENTIFIER));
        assertEquals("test", (String) eventCompositeData.get(SYMBOLIC_NAME));
        assertEquals(new Integer(BundleEvent.INSTALLED),  (Integer) eventCompositeData.get(EVENT));
        assertEquals("location",  (String) eventCompositeData.get(LOCATION));
        
    }

    
    @Test
    public void testFrom() throws Exception {

        Map<String, Object> items = new HashMap<String, Object>();
        items.put(IDENTIFIER, new Long(7));
        items.put(SYMBOLIC_NAME, "t");
        items.put(LOCATION, "l");
        items.put(EVENT, BundleEvent.RESOLVED);

        CompositeData compositeData = new CompositeDataSupport(BUNDLE_EVENT_TYPE, items);
        BundleEventData event = BundleEventData.from(compositeData);
        
        assertEquals(7, event.getBundleId());
        assertEquals("t", event.getBundleSymbolicName());
        assertEquals("l", event.getLocation());
        assertEquals(BundleEvent.RESOLVED, event.getEventType());
            
    }

}
