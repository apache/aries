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
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import junit.framework.Assert;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.PropertyData;
import org.apache.aries.jmx.test.bundlea.api.InterfaceA;
import org.apache.aries.jmx.test.bundleb.api.InterfaceB;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.jmx.JmxConstants;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 *
 *
 * @version $Rev$ $Date$
 */
@ExamReactorStrategy(PerMethod.class)
public class ServiceStateMBeanTest extends AbstractIntegrationTest {
	
    private ObjectName objectName;
	private ServiceStateMBean mbean;

    @Configuration
    public Option[] configuration() {
		return options(
		        // new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
		        // new TimeoutOption( 0 ),
				jmxRuntime(), 
				bundlea(), 
				bundleb()
				);
    }
    
    @Before
    public void doSetUp() {
        waitForMBean(ServiceStateMBean.OBJECTNAME);
        objectName = waitForMBean(ServiceStateMBean.OBJECTNAME);
        mbean = getMBean(objectName, ServiceStateMBean.class);
        assertNotNull(mbean);
    }

    @Test
    public void testObjectName() throws Exception {
        Set<ObjectName> names = mbeanServer.queryNames(new ObjectName(ServiceStateMBean.OBJECTNAME + ",*"), null);
        assertEquals(1, names.size());
        ObjectName name = names.iterator().next();
        Hashtable<String, String> props = name.getKeyPropertyList();
        assertEquals(context().getProperty(Constants.FRAMEWORK_UUID), props.get("uuid"));
        assertEquals(context().getBundle(0).getSymbolicName(), props.get("framework"));
    }

    @Test
    public void testMBeanInterface() throws Exception {
        //get bundles
        Bundle a = getBundleByName("org.apache.aries.jmx.test.bundlea");
        assertBundleStarted(a);
        Bundle b = getBundleByName("org.apache.aries.jmx.test.bundleb");
        assertBundleStarted(b);

        // get services

        ServiceReference<?> refA = bundleContext.getServiceReference(InterfaceA.class.getName());
        assertNotNull(refA);
        long serviceAId = (Long) refA.getProperty(Constants.SERVICE_ID);
        assertTrue(serviceAId > -1);

        ServiceReference<?> refB = bundleContext.getServiceReference(InterfaceB.class.getName());
        assertNotNull(refB);
        long serviceBId = (Long) refB.getProperty(Constants.SERVICE_ID);
        assertTrue(serviceBId > -1);

        ServiceReference<?>[] refs = bundleContext.getServiceReferences(ManagedServiceFactory.class.getName(), "(" + Constants.SERVICE_PID + "=jmx.test.B.factory)");
        assertNotNull(refs);
        assertEquals(1, refs.length);
        ServiceReference<?> msf = refs[0];


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

        ServiceReference<?>[] allSvsRefs = bundleContext.getAllServiceReferences(null, null);
        TabularData allServices = mbean.listServices();
        assertNotNull(allServices);
        assertEquals(allSvsRefs.length, allServices.values().size());

        // notifications

        final List<Notification> received = new ArrayList<Notification>();
        final List<AttributeChangeNotification> attributeChanges = new ArrayList<AttributeChangeNotification>();

        mbeanServer.addNotificationListener(objectName, new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
                if (notification instanceof AttributeChangeNotification) {
                    attributeChanges.add((AttributeChangeNotification) notification);
                } else {
                    received.add(notification);
                }
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

        waitForListToReachSize(received, 4);

        assertEquals(4, received.size());
        assertEquals(4, attributeChanges.size());
    }

	private void assertBundleStarted(Bundle bundle) {
		Assert.assertEquals("Bundle " + bundle.getSymbolicName() + " should be started but is in state " + bundle.getState(),
				Bundle.ACTIVE, bundle.getState());
	}

	@Test
    public void testAttributeChangeNotifications() throws Exception {
        final List<AttributeChangeNotification> attributeChanges = new ArrayList<AttributeChangeNotification>();
        mbeanServer.addNotificationListener(objectName, new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
                if (notification instanceof AttributeChangeNotification) {
                    attributeChanges.add((AttributeChangeNotification) notification);
                }
            }
        }, null, null);

        assertEquals("Precondition", 0, attributeChanges.size());

        long[] idsWithout = mbean.getServiceIds();

        String svc = "A String Service";
        ServiceRegistration<?> reg = bundleContext.registerService(String.class.getName(), svc, null);
        long id = (Long) reg.getReference().getProperty(Constants.SERVICE_ID);

        long[] idsWith = new long[idsWithout.length + 1];
        System.arraycopy(idsWithout, 0, idsWith, 0, idsWithout.length);
        idsWith[idsWith.length - 1] = id;
        Arrays.sort(idsWith);

        waitForListToReachSize(attributeChanges, 1);
        AttributeChangeNotification ac = attributeChanges.get(0);
        assertEquals("ServiceIds", ac.getAttributeName());
        long seq1 = ac.getSequenceNumber();
        assertTrue(Arrays.equals(idsWithout, (long []) ac.getOldValue()));
        assertTrue(Arrays.equals(idsWith, (long []) ac.getNewValue()));

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("somekey", "someval");
        reg.setProperties(props);

        // Setting the properties updates the service registration, however it should not cause the attribute notification
        Thread.sleep(500); // Give the system a bit of time to send potential notifications
        assertEquals("Changing the service registration should not cause an attribute notification",
                1, attributeChanges.size());

        reg.unregister();

        waitForListToReachSize(attributeChanges, 2);
        AttributeChangeNotification ac2 = attributeChanges.get(1);
        assertEquals("ServiceIds", ac2.getAttributeName());
        assertEquals(seq1 +1, ac2.getSequenceNumber());
        assertTrue(Arrays.equals(idsWith, (long []) ac2.getOldValue()));
        assertTrue(Arrays.equals(idsWithout, (long []) ac2.getNewValue()));
    }

    @Test
    public void testGetServiceIds() throws Exception {
        ServiceReference<?>[] allSvsRefs = bundleContext.getAllServiceReferences(null, null);
        long[] expectedServiceIds = new long[allSvsRefs.length];
        for (int i=0; i < allSvsRefs.length; i++) {
            expectedServiceIds[i] = (Long) allSvsRefs[i].getProperty(Constants.SERVICE_ID);
        }
        long[] actualServiceIds = mbean.getServiceIds();
        Arrays.sort(expectedServiceIds);
        Arrays.sort(actualServiceIds);
        assertTrue(Arrays.equals(expectedServiceIds, actualServiceIds));
    }

    @Test
    public void testGetServiceAndGetProperty() throws Exception {
    	
        ServiceReference<InterfaceA> sref = bundleContext.getServiceReference(InterfaceA.class);
        // Get service to increase service references
        context().getService(sref);
        Long serviceID = (Long) sref.getProperty(Constants.SERVICE_ID);

        CompositeData svcData  = mbean.getService(serviceID);
        assertEquals(serviceID, svcData.get(ServiceStateMBean.IDENTIFIER));
        assertEquals(sref.getBundle().getBundleId(), svcData.get(ServiceStateMBean.BUNDLE_IDENTIFIER));
        Set<String> expectedClasses = new HashSet<String>(Arrays.asList(InterfaceA.class.getName(), ManagedService.class.getName()));
        Set<String> actualClasses = new HashSet<String>(Arrays.asList((String []) svcData.get(ServiceStateMBean.OBJECT_CLASS)));
        assertEquals(expectedClasses, actualClasses);
        Bundle[] ub = sref.getUsingBundles();
        assertEquals("Precondition", 1, ub.length);
        assertTrue(Arrays.equals(new Long[] {ub[0].getBundleId()}, (Long[]) svcData.get("UsingBundles")));

        // Test mbean.getProperty()
        String pid = (String) sref.getProperty(Constants.SERVICE_PID);
        CompositeData pidData = mbean.getProperty(serviceID, Constants.SERVICE_PID);
        assertEquals(pid, pidData.get("Value"));
        assertEquals("String", pidData.get("Type"));

        CompositeData idData = mbean.getProperty(serviceID, Constants.SERVICE_ID);
        assertEquals("" + serviceID, idData.get("Value"));
        assertEquals("Long", idData.get("Type"));

        CompositeData ocData = mbean.getProperty(serviceID, Constants.OBJECTCLASS);
        String form1 = InterfaceA.class.getName() + "," + ManagedService.class.getName();
        String form2 = ManagedService.class.getName() + "," + InterfaceA.class.getName();
        assertTrue(ocData.get("Value").equals(form1) ||
                   ocData.get("Value").equals(form2));
        assertEquals("Array of String", ocData.get("Type"));
        context().ungetService(sref);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testServicePropertiesInListServices() throws Exception {
        ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(InterfaceA.class.getName(), null);
        assertEquals("Precondition", 1, refs.length);
        ServiceReference<?> ref = refs[0];

        TabularData svcTab = mbean.listServices();
        CompositeData svcData = svcTab.get(new Object [] {ref.getProperty(Constants.SERVICE_ID)});

        Set<String> expectedOCs = new HashSet<String>(Arrays.asList(
                InterfaceA.class.getName(), ManagedService.class.getName()));
        Set<String> actualOCs = new HashSet<String>(
                Arrays.asList((String [])svcData.get(Constants.OBJECTCLASS)));
        assertEquals(expectedOCs, actualOCs);

        Map<String, Object> expectedProperties = new HashMap<String, Object>();
        for (String key : ref.getPropertyKeys()) {
            Object value = ref.getProperty(key);
            if (value.getClass().isArray())
                continue;

            expectedProperties.put(key, value);
        }

        Map<String, Object> actualProperties = new HashMap<String, Object>();
        TabularData actualProps = (TabularData) svcData.get(ServiceStateMBean.PROPERTIES);
        for (CompositeData cd : (Collection<CompositeData>) actualProps.values()) {
            Object type = cd.get(JmxConstants.TYPE);
            if (JmxConstants.STRING.equals(type)) {
                actualProperties.put((String) cd.get(JmxConstants.KEY), cd.get(JmxConstants.VALUE));
            } else if (JmxConstants.LONG.equals(type)) {
                actualProperties.put((String) cd.get(JmxConstants.KEY), Long.valueOf(cd.get(JmxConstants.VALUE).toString()));
            }
        }

        assertEquals(expectedProperties, actualProperties);
    }

    @Test
    public void testListServices() throws Exception {
        String filter = "(" + Constants.SERVICE_PID + "=*)";
        ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(null, filter);
        TabularData svcData = mbean.listServices(null, filter);
        assertEquals(refs.length, svcData.size());

        ServiceReference<InterfaceA> sref = bundleContext.getServiceReference(InterfaceA.class);
        TabularData svcTab = mbean.listServices(InterfaceA.class.getName(), null);
        assertEquals(1, svcTab.size());
        CompositeData actualSvc = (CompositeData) svcTab.values().iterator().next();
        CompositeData expectedSvc = mbean.getService((Long) sref.getProperty(Constants.SERVICE_ID));
        assertEquals(expectedSvc, actualSvc);
    }

    @SuppressWarnings("unchecked")
	@Test
    public void testListServicesSelectiveItems() throws Exception {
        String filter = "(|(service.pid=org.apache.aries.jmx.test.ServiceB)(service.pid=jmx.test.B.factory))";
        ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(null, filter);
        TabularData svcData = mbean.listServices(null, filter, ServiceStateMBean.BUNDLE_IDENTIFIER);
        assertEquals(refs.length, svcData.size());

        long id = refs[0].getBundle().getBundleId();
        for (ServiceReference<?> ref : refs) {
            assertEquals("Precondition", id, ref.getBundle().getBundleId());
        }

        for (CompositeData cd : new ArrayList<CompositeData>((Collection<CompositeData>) svcData.values())) {
            assertEquals(id, cd.get(ServiceStateMBean.BUNDLE_IDENTIFIER));
            assertNotNull(cd.get(ServiceStateMBean.IDENTIFIER));
            assertNull(cd.get(ServiceStateMBean.OBJECT_CLASS));
            assertNull(cd.get(ServiceStateMBean.USING_BUNDLES));
        }
    }

    private void waitForListToReachSize(List<?> list, int targetSize) throws InterruptedException {
        int i = 0;
        while (list.size() < targetSize && i < 3) {
            Thread.sleep(1000);
            i++;
        }
    }
}
