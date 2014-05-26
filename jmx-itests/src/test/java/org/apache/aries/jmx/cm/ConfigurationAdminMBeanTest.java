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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Dictionary;

import javax.inject.Inject;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.PropertyData;
import org.apache.aries.jmx.test.bundlea.api.InterfaceA;
import org.apache.aries.jmx.test.bundleb.api.InterfaceB;
import org.apache.aries.jmx.test.bundleb.api.MSF;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.jmx.service.cm.ConfigurationAdminMBean;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class ConfigurationAdminMBeanTest extends AbstractIntegrationTest {
	private ConfigurationAdminMBean mbean;
	
	@Inject
	InterfaceA managedServiceA; 
	
	@Inject
	@org.ops4j.pax.exam.util.Filter("(" + Constants.SERVICE_PID + "=jmx.test.B.factory)")
    MSF managedFactory;
	
	@Inject
    ConfigurationAdmin configAdmin;

    @Configuration
    public Option[] configuration() {
		return options(
				jmxRuntime(), 
				bundlea(),
				bundleb()
				);
    }
    
    @Before
    public void doSetUp() throws Exception {
        waitForMBean(ConfigurationAdminMBean.OBJECTNAME);
        mbean = getMBean(ConfigurationAdminMBean.OBJECTNAME, ConfigurationAdminMBean.class);
        assertNotNull(mbean);
    }
    
    @Ignore("ManagedServiceFactory tests failing.. " +
            "Some issues surrounding creating a factory configuration and then retrieving by pid to update.. Needs investigation")
    @Test
    @SuppressWarnings("unchecked")
    public void testMBeanInterface() throws Exception {
        // get bundles
        Bundle a = getBundleByName("org.apache.aries.jmx.test.bundlea");
        
        // ManagedService operations
        assertNull(managedServiceA.getConfig());
        
        // create a configuration for A
        TabularData data = mbean.getProperties("org.apache.aries.jmx.test.ServiceA");
        assertEquals(0, data.size());
        
        PropertyData<String> p1 = PropertyData.newInstance("A1", "first");
        data.put(p1.toCompositeData());
        PropertyData<Integer> p2 = PropertyData.newInstance("A2", 2);
        data.put(p2.toCompositeData());
        
        mbean.update("org.apache.aries.jmx.test.ServiceA", data);
        
        Thread.sleep(1000);
        Dictionary<String, Object> config = managedServiceA.getConfig();
        assertNotNull(config);
        assertEquals(3, config.size());
        assertEquals("org.apache.aries.jmx.test.ServiceA", config.get(Constants.SERVICE_PID));
        assertEquals("first", config.get("A1"));
        assertEquals(2, config.get("A2"));
        
        //delete
        mbean.deleteForLocation("org.apache.aries.jmx.test.ServiceA", a.getLocation());
        Thread.sleep(1000);
        assertNull(managedServiceA.getConfig());
        
        
        // ManagedServiceFactory operations
        String cpid = mbean.createFactoryConfiguration("jmx.test.B.factory");
        assertNotNull(cpid);
        assertTrue(cpid.contains("jmx.test.B.factory"));
        
        TabularData fConfig = mbean.getProperties(cpid);
        assertNotNull(fConfig);
        assertEquals(0, fConfig.values().size());
        
        PropertyData<String> prop1 = PropertyData.newInstance("B1", "value1");
        fConfig.put(prop1.toCompositeData());
        PropertyData<Boolean> prop2 = PropertyData.newInstance("B2", true);
        fConfig.put(prop2.toCompositeData());
        
        mbean.update(cpid, fConfig);
        
        Thread.sleep(1000);
        
        InterfaceB configured = managedFactory.getConfigured(cpid);
        assertNotNull(configured);
        config = configured.getConfig();
        assertNotNull(config);
        assertTrue(config.size() >= 4);
        assertEquals("jmx.test.B.factory", config.get(ConfigurationAdmin.SERVICE_FACTORYPID));
        assertEquals(cpid, config.get(Constants.SERVICE_PID));
        assertEquals("value1", config.get("B1"));
        assertEquals("true", config.get("B2"));
        
        assertEquals("jmx.test.B.factory", mbean.getFactoryPid(cpid));
        
        mbean.delete(cpid);
        
        Thread.sleep(1000);
        
        assertNull(managedFactory.getConfigured(cpid));
       
        // list operations
        
        data = mbean.getProperties("org.apache.aries.jmx.test.ServiceA");
        assertEquals(0, data.size());
        
        p1 = PropertyData.newInstance("A1", "a1Value");
        data.put(p1.toCompositeData());
        
        mbean.update("org.apache.aries.jmx.test.ServiceA", data);
        
        Thread.sleep(1000);
        
        config = managedServiceA.getConfig();
        assertNotNull(config);
        assertEquals(2, config.size());
        assertEquals("org.apache.aries.jmx.test.ServiceA", config.get(Constants.SERVICE_PID));
        assertEquals("a1Value", config.get("A1"));

        
        String[][] configurations = mbean.getConfigurations("(A1=a1Value)");
        assertNotNull(configurations);
        assertEquals(1, configurations.length);
        assertEquals("org.apache.aries.jmx.test.ServiceA", configurations[0][0]);
        assertEquals(a.getLocation(), configurations[0][1]);
        
        // delete with filter
        mbean.deleteConfigurations("(A1=a1Value)");
        
        Thread.sleep(1000);
        
        assertNull(managedServiceA.getConfig());
        
    }
}
