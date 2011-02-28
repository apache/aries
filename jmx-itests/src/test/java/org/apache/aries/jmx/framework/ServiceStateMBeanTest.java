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
package org.apache.aries.jmx.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.modifyBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.PropertyData;
import org.apache.aries.jmx.test.bundlea.api.InterfaceA;
import org.apache.aries.jmx.test.bundleb.api.InterfaceB;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.JmxConstants;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class ServiceStateMBeanTest extends AbstractIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        Option[] options = CoreOptions
                .options(
                        CoreOptions.equinox(),
                        mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                        mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
                        mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
                        mavenBundle("org.osgi", "org.osgi.compendium"),
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx"),
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
        options = updateOptions(options);
        return options;
    }
    
    @Before
    public void doSetUp() throws Exception {
        super.setUp();
        int i=0;
        while (true) {
            try {
                mbeanServer.getObjectInstance(new ObjectName(ServiceStateMBean.OBJECTNAME));
                break;
            } catch (InstanceNotFoundException e) {
                if (i == 5) {
                    throw new Exception("ServiceStateMBean not available after waiting 5 seconds");
                }
            }
            i++;
            Thread.sleep(1000);
        }
    }
    
    
    @Test
    public void testMBeanInterface() throws Exception {
        
        ServiceStateMBean mbean = getMBean(ServiceStateMBean.OBJECTNAME, ServiceStateMBean.class);
        assertNotNull(mbean);
        
        //get bundles
        
        Bundle a = getBundle("org.apache.aries.jmx.test.bundlea");
        assertNotNull(a);
        
        Bundle b = getBundle("org.apache.aries.jmx.test.bundleb");
        assertNotNull(b);
        
        // get services
        
        ServiceReference refA = bundleContext.getServiceReference(InterfaceA.class.getName());
        assertNotNull(refA);
        long serviceAId = (Long) refA.getProperty(Constants.SERVICE_ID);
        assertTrue(serviceAId > -1);
        
        ServiceReference refB = bundleContext.getServiceReference(InterfaceB.class.getName());
        assertNotNull(refB);
        long serviceBId = (Long) refB.getProperty(Constants.SERVICE_ID);
        assertTrue(serviceBId > -1);
        
        ServiceReference[] refs = bundleContext.getServiceReferences(ManagedServiceFactory.class.getName(), "(" + Constants.SERVICE_PID + "=jmx.test.B.factory)");
        assertNotNull(refs);
        assertEquals(1, refs.length);
        ServiceReference msf = refs[0];

        
        // getBundleIdentifier
        
        assertEquals(a.getBundleId(), mbean.getBundleIdentifier(serviceAId));
        
        //getObjectClass
        
        String[] objectClass = mbean.getObjectClass(serviceAId);
        assertEquals(2, objectClass.length);
        List<String> classNames = Arrays.asList(objectClass);
        assertTrue(classNames.contains(InterfaceA.class.getName()));
        assertTrue(classNames.contains(ManagedService.class.getName()));
        
        // getProperties
        
        TabularData serviceProperties = mbean.getProperties(serviceBId);
        assertNotNull(serviceProperties);
        assertEquals(JmxConstants.PROPERTIES_TYPE, serviceProperties.getTabularType());
        assertTrue(serviceProperties.values().size() > 1);
        assertEquals("org.apache.aries.jmx.test.ServiceB", 
                PropertyData.from(serviceProperties.get(new Object[] { Constants.SERVICE_PID })).getValue());
        
        // getUsingBundles
        
        long[] usingBundles = mbean.getUsingBundles(serviceBId);
        assertEquals(1, usingBundles.length);
        assertEquals(a.getBundleId(), usingBundles[0]);
        
        // listServices
        
        TabularData allServices = mbean.listServices();
        assertNotNull(allServices);
        assertEquals(bundleContext.getAllServiceReferences(null, null).length, allServices.values().size());
        
        // notifications
        
        final List<Notification> received = new ArrayList<Notification>();
      
        mbeanServer.addNotificationListener(new ObjectName(ServiceStateMBean.OBJECTNAME), new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
               received.add(notification);
            }
        }, null, null);
        
      
        assertNotNull(refB);
        assertNotNull(msf);
        b.stop();
        refB = bundleContext.getServiceReference(InterfaceB.class.getName()); 
        refs = bundleContext.getServiceReferences(ManagedServiceFactory.class.getName(), "(" + Constants.SERVICE_PID + "=jmx.test.B.factory)");
        assertNull(refs);
        assertNull(refB);
        b.start();
        refB = bundleContext.getServiceReference(InterfaceB.class.getName());
        refs = bundleContext.getServiceReferences(ManagedServiceFactory.class.getName(), "(" + Constants.SERVICE_PID + "=jmx.test.B.factory)");
        assertNotNull(refB);
        assertNotNull(refs);
        assertEquals(1, refs.length);
        
        int i = 0;
        while (received.size() < 4 && i < 3) {
            Thread.sleep(1000);
            i++;
        }
        
        assertEquals(4, received.size());
            
    }

}
