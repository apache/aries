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
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.modifyBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import static org.apache.aries.itest.ExtraOptions.*;

import java.io.InputStream;
import java.util.Dictionary;

import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.PropertyData;
import org.apache.aries.jmx.test.bundlea.api.InterfaceA;
import org.apache.aries.jmx.test.bundleb.api.InterfaceB;
import org.apache.aries.jmx.test.bundleb.api.MSF;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.jmx.service.cm.ConfigurationAdminMBean;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class ConfigurationAdminMBeanTest extends AbstractIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        return testOptions(
                        CoreOptions.equinox(),
                        paxLogging("INFO"),
                        
                        mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                        mavenBundle("org.osgi", "org.osgi.compendium"),
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx"),
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.whiteboard"),
                        new Customizer() {
                            public InputStream customizeTestProbe(InputStream testProbe) throws Exception {
                                return modifyBundle(testProbe)
                                           .removeHeader(Constants.DYNAMICIMPORT_PACKAGE)
                                           .set(Constants.REQUIRE_BUNDLE, "org.apache.aries.jmx.test.bundlea,org.apache.aries.jmx.test.bundleb")
                                           .build(withBnd());
                            }
                        },
                        provision(newBundle()
                                .add(org.apache.aries.jmx.test.bundlea.Activator.class)
                                .add(org.apache.aries.jmx.test.bundlea.api.InterfaceA.class)
                                .add(org.apache.aries.jmx.test.bundlea.impl.A.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea")
                                .set(Constants.BUNDLE_VERSION, "2.0.0")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api;version=2.0.0")
                                .set(Constants.IMPORT_PACKAGE,
                                        "org.osgi.framework;version=1.5.0,org.osgi.util.tracker,org.apache.aries.jmx.test.bundleb.api;version=1.1.0;resolution:=optional" +
                                        ",org.osgi.service.cm")
                                .set(Constants.BUNDLE_ACTIVATOR,
                                        org.apache.aries.jmx.test.bundlea.Activator.class.getName())
                                .build(withBnd())),
                        provision(newBundle()
                                .add(org.apache.aries.jmx.test.bundleb.Activator.class)
                                .add(org.apache.aries.jmx.test.bundleb.api.InterfaceB.class)
                                .add(org.apache.aries.jmx.test.bundleb.api.MSF.class)
                                .add(org.apache.aries.jmx.test.bundleb.impl.B.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME,"org.apache.aries.jmx.test.bundleb")
                                .set(Constants.BUNDLE_VERSION, "1.0.0")
                                .set(Constants.EXPORT_PACKAGE,"org.apache.aries.jmx.test.bundleb.api;version=1.1.0")
                                .set(Constants.IMPORT_PACKAGE,"org.osgi.framework;version=1.5.0,org.osgi.util.tracker" +
                                        ",org.osgi.service.cm")
                                .set(Constants.BUNDLE_ACTIVATOR,
                                        org.apache.aries.jmx.test.bundleb.Activator.class.getName())
                                .build(withBnd()))
                        );
    }
    
    @Override
    public void doSetUp() throws Exception {
        waitForMBean(new ObjectName(ConfigurationAdminMBean.OBJECTNAME));
    }
    
    @Ignore("ManagedServiceFactory tests failing.. " +
            "Some issues surrounding creating a factory configuration and then retrieving by pid to update.. Needs investigation")
    @Test
    @SuppressWarnings("unchecked")
    public void testMBeanInterface() throws Exception {
        
        ConfigurationAdminMBean mbean = getMBean(ConfigurationAdminMBean.OBJECTNAME, ConfigurationAdminMBean.class);
        assertNotNull(mbean);
       
        // get bundles
        
        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        assertNotNull(a);
        
        Bundle b = context().getBundleByName("org.apache.aries.jmx.test.bundleb");
        assertNotNull(b);
       
        
        // get services
        
        ServiceTracker trackerA = new ServiceTracker(bundleContext, InterfaceA.class.getName(), null);
        trackerA.open();
        InterfaceA managedServiceA = (InterfaceA) trackerA.getService();
        assertNotNull(managedServiceA);
        
        Filter filter = bundleContext.createFilter("(" + Constants.SERVICE_PID + "=jmx.test.B.factory)");
        ServiceTracker trackerMSF = new ServiceTracker(bundleContext, filter, null);
        trackerMSF.open();
        MSF managedFactory = (MSF) trackerMSF.getService();
        assertNotNull(managedFactory);
        
        ServiceTracker tracker = new ServiceTracker(bundleContext, ConfigurationAdmin.class.getName(), null);
        tracker.open();
        ConfigurationAdmin configAdmin = (ConfigurationAdmin) tracker.getService();
        assertNotNull(configAdmin);
        
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
        
        //clean up
        
        trackerA.close();
        trackerMSF.close();
        tracker.close();
        
    }
}
