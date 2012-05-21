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
package org.apache.aries.jmx.cm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.jmx.JmxConstants.PROPERTIES_TYPE;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.codec.PropertyData;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class ConfigurationAdminTest {

   
    @Test
    public void testCreateFactoryConfiguration() throws Exception {
        
        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String fpid = "org.apache.aries.jmx.mock.factory";
        Configuration config = mock(Configuration.class);
        
        when(admin.createFactoryConfiguration(eq(fpid))).thenReturn(config);
        when(admin.createFactoryConfiguration(eq(fpid), anyString())).thenReturn(config);
        when(config.getPid()).thenReturn(fpid + "-1260133982371-0");
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        assertEquals(fpid + "-1260133982371-0", mbean.createFactoryConfiguration(fpid));
        assertEquals(fpid + "-1260133982371-0", mbean.createFactoryConfigurationForLocation(fpid, "/bundlex"));
        
    }

   
    @Test
    public void testDelete() throws Exception {
        
        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String pid = "org.apache.aries.jmx.mock";
        Configuration config = mock(Configuration.class);
        
        when(admin.getConfiguration(pid, null)).thenReturn(config);
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        mbean.delete(pid);
        verify(config).delete();
        
        reset(config);
        
        when(admin.getConfiguration(pid, "location")).thenReturn(config);
        mbean.deleteForLocation(pid, "location");
        verify(config).delete();
        
    }

  
    @Test
    public void testDeleteConfigurations() throws Exception {

        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String filter = "(" + Constants.SERVICE_PID + "=org.apache.aries.jmx.mock)";
        
        Configuration a = mock(Configuration.class);
        Configuration b = mock(Configuration.class);
        
        when(admin.listConfigurations(filter)).thenReturn(new Configuration[] { a, b });
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        mbean.deleteConfigurations(filter);

        verify(a).delete();
        verify(b).delete();
        
    }

   
    @Test
    public void testGetBundleLocation() throws Exception {

        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String pid = "org.apache.aries.jmx.mock";
        Configuration config = mock(Configuration.class);
        
        when(admin.getConfiguration(pid, null)).thenReturn(config);
        when(config.getBundleLocation()).thenReturn("/location");
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        
        assertEquals("/location", mbean.getBundleLocation(pid));
        
    }

 
    @Test
    public void testGetConfigurations() throws Exception {
     
        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String factoryPid = "org.apache.aries.jmx.factory.mock";
        String filter = "(" + org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID + "=org.apache.aries.jmx.factory.mock)";
        String location = "../location";
        
        Configuration a = mock(Configuration.class);
        when(a.getPid()).thenReturn(factoryPid + "-2160133952674-0");
        when(a.getBundleLocation()).thenReturn(location);
        Configuration b = mock(Configuration.class);
        when(b.getPid()).thenReturn(factoryPid + "-1260133982371-1");
        when(b.getBundleLocation()).thenReturn(location);
        
        when(admin.listConfigurations(filter)).thenReturn(new Configuration[] { a, b});
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        String[][] result = mbean.getConfigurations(filter);
        assertEquals(2, result.length);
        assertArrayEquals(new String[]{ factoryPid + "-2160133952674-0", location }, result[0] );
        assertArrayEquals(new String[]{ factoryPid + "-1260133982371-1", location }, result[1] );
        
    }

   
    @Test
    public void testGetFactoryPid() throws Exception {

        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String factoryPid = "org.apache.aries.jmx.factory.mock";
        
        Configuration config = mock(Configuration.class);
        when(admin.getConfiguration(eq(factoryPid  + "-1260133982371-0"), anyString())).thenReturn(config);
        when(config.getFactoryPid()).thenReturn(factoryPid);
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        assertEquals(factoryPid, mbean.getFactoryPid(factoryPid  + "-1260133982371-0"));
        assertEquals(factoryPid, mbean.getFactoryPidForLocation(factoryPid  + "-1260133982371-0", "location"));
        
    }

    
    @Test
    public void testGetProperties() throws Exception {

        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String pid = "org.apache.aries.jmx.mock";
        Configuration config = mock(Configuration.class);
        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("one", "value");
        props.put("two", 2);
        when(admin.getConfiguration(eq(pid), anyString())).thenReturn(config);
        when(config.getProperties()).thenReturn(props);
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        
        TabularData properties = mbean.getPropertiesForLocation(pid, null);
        assertNotNull(properties);
        assertEquals(PROPERTIES_TYPE, properties.getTabularType());
        assertEquals(2, properties.values().size());
        PropertyData<? extends Object> oneData = PropertyData.from(properties.get(new Object[]{ "one"}));
        assertEquals("value", oneData.getValue());
        PropertyData<? extends Object> twoData = PropertyData.from(properties.get(new Object[]{ "two"}));
        assertEquals(2, twoData.getValue());
        assertEquals("2", twoData.getEncodedValue());
        
    }

   

    @Test
    public void testSetBundleLocation() throws Exception {

        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String pid = "org.apache.aries.jmx.mock";
        
        Configuration config = mock(Configuration.class);
        when(admin.getConfiguration(pid, null)).thenReturn(config);
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        mbean.setBundleLocation(pid, "file:/newlocation");
        
        ArgumentCaptor<String> locationArgument = ArgumentCaptor.forClass(String.class);
        verify(config).setBundleLocation(locationArgument.capture());
        
        assertEquals("file:/newlocation", locationArgument.getValue());
        
    }

   
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateTabularData() throws Exception {
       
        TabularData data = new TabularDataSupport(PROPERTIES_TYPE);
        PropertyData<String> p1 = PropertyData.newInstance("one", "first");
        data.put(p1.toCompositeData());
        PropertyData<Integer> p2 = PropertyData.newInstance("two", 3);
        data.put(p2.toCompositeData());
        
        org.osgi.service.cm.ConfigurationAdmin admin = mock(org.osgi.service.cm.ConfigurationAdmin.class);
        String pid = "org.apache.aries.jmx.mock";
        
        Configuration config = mock(Configuration.class);
        when(admin.getConfiguration(pid, null)).thenReturn(config);
        
        ConfigurationAdmin mbean = new ConfigurationAdmin(admin);
        mbean.updateForLocation(pid, null, data);
        
        ArgumentCaptor<Dictionary> props = ArgumentCaptor.forClass(Dictionary.class);
        verify(config).update(props.capture());
        
        Dictionary configProperties = props.getValue();
        assertEquals("first", configProperties.get("one"));
        assertEquals(3, configProperties.get("two"));
        
    }

  

}
