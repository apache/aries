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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_LOCATION;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_SYMBOLIC_NAME;
import static org.osgi.jmx.framework.ServiceStateMBean.EVENT;
import static org.osgi.jmx.framework.ServiceStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECTNAME;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECT_CLASS;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.Logger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class ServiceStateTest {


    private void createService(StateConfig stateConfig, final List<Notification> received,
                               final List<AttributeChangeNotification> attributeChanges) throws Exception {
        BundleContext context = mock(BundleContext.class);
        Logger logger = mock(Logger.class);

        ServiceState serviceState = new ServiceState(context, stateConfig, logger);

        ServiceReference reference = mock(ServiceReference.class);
        Bundle b1 = mock(Bundle.class);

        when(b1.getBundleId()).thenReturn(new Long(9));
        when(b1.getSymbolicName()).thenReturn("bundle");
        when(b1.getLocation()).thenReturn("file:/location");
        when(reference.getBundle()).thenReturn(b1);
        when(reference.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(44));
        when(reference.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] {"org.apache.aries.jmx.Mock"});

        when(context.getAllServiceReferences(null, null)).thenReturn(new ServiceReference[] {reference});

        ServiceEvent registeredEvent = mock(ServiceEvent.class);
        when(registeredEvent.getServiceReference()).thenReturn(reference);
        when(registeredEvent.getType()).thenReturn(ServiceEvent.REGISTERED);

        ServiceEvent modifiedEvent = mock(ServiceEvent.class);
        when(modifiedEvent.getServiceReference()).thenReturn(reference);
        when(modifiedEvent.getType()).thenReturn(ServiceEvent.MODIFIED);

        MBeanServer server = mock(MBeanServer.class);

        //setup for notification
        ObjectName objectName = new ObjectName(OBJECTNAME);
        serviceState.preRegister(server, objectName);
        serviceState.postRegister(true);


        //add NotificationListener to receive the events
        serviceState.addNotificationListener(new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
                if (notification instanceof AttributeChangeNotification) {
                    attributeChanges.add((AttributeChangeNotification) notification);
                } else {
                    received.add(notification);
                }
            }
        }, null, null);

        // capture the ServiceListener registered with BundleContext to issue ServiceEvents
        ArgumentCaptor<AllServiceListener> argument = ArgumentCaptor.forClass(AllServiceListener.class);
        verify(context).addServiceListener(argument.capture());

        //send events
        AllServiceListener serviceListener = argument.getValue();
        serviceListener.serviceChanged(registeredEvent);
        serviceListener.serviceChanged(modifiedEvent);

        //shutdown dispatcher via unregister callback
        serviceState.postDeregister();
        //check the ServiceListener is cleaned up
        verify(context).removeServiceListener(serviceListener);

        ExecutorService dispatcher = serviceState.getEventDispatcher();
        assertTrue(dispatcher.isShutdown());
        dispatcher.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(dispatcher.isTerminated());
    }

    @Test
    public void testNotificationsForServiceEvents() throws Exception {
        StateConfig stateConfig = new StateConfig();

        //holders for Notifications captured
        List<Notification> received = new LinkedList<Notification>();
        List<AttributeChangeNotification> attributeChanges = new LinkedList<AttributeChangeNotification>();

        createService(stateConfig, received, attributeChanges);

        assertEquals(2, received.size());
        Notification registered = received.get(0);
        assertEquals(1, registered.getSequenceNumber());
        CompositeData data = (CompositeData) registered.getUserData();
        assertEquals(new Long(44), data.get(IDENTIFIER));
        assertEquals(new Long(9), data.get(BUNDLE_IDENTIFIER));
        assertEquals("file:/location", data.get(BUNDLE_LOCATION));
        assertEquals("bundle", data.get(BUNDLE_SYMBOLIC_NAME));
        assertArrayEquals(new String[] {"org.apache.aries.jmx.Mock" }, (String[]) data.get(OBJECT_CLASS));
        assertEquals(ServiceEvent.REGISTERED, data.get(EVENT));

        Notification modified = received.get(1);
        assertEquals(2, modified.getSequenceNumber());
        data = (CompositeData) modified.getUserData();
        assertEquals(new Long(44), data.get(IDENTIFIER));
        assertEquals(new Long(9), data.get(BUNDLE_IDENTIFIER));
        assertEquals("file:/location", data.get(BUNDLE_LOCATION));
        assertEquals("bundle", data.get(BUNDLE_SYMBOLIC_NAME));
        assertArrayEquals(new String[] {"org.apache.aries.jmx.Mock" }, (String[]) data.get(OBJECT_CLASS));
        assertEquals(ServiceEvent.MODIFIED, data.get(EVENT));

        assertEquals(1, attributeChanges.size());
        AttributeChangeNotification ac = attributeChanges.get(0);
        assertEquals("ServiceIds", ac.getAttributeName());
        assertEquals(0, ((long [])ac.getOldValue()).length);
        assertEquals(1, ((long [])ac.getNewValue()).length);
        assertEquals(44L, ((long [])ac.getNewValue())[0]);
    }

    @Test
    public void testLifeCycleOfNotificationSupport() throws Exception {

        BundleContext context = mock(BundleContext.class);
        Logger logger = mock(Logger.class);

        ServiceState serviceState = new ServiceState(context, new StateConfig(), logger);

        MBeanServer server1 = mock(MBeanServer.class);
        MBeanServer server2 = mock(MBeanServer.class);

        ObjectName objectName = new ObjectName(OBJECTNAME);
        serviceState.preRegister(server1, objectName);
        serviceState.postRegister(true);

        // capture the ServiceListener registered with BundleContext to issue ServiceEvents
        ArgumentCaptor<AllServiceListener> argument = ArgumentCaptor.forClass(AllServiceListener.class);
        verify(context).addServiceListener(argument.capture());

        AllServiceListener serviceListener = argument.getValue();
        assertNotNull(serviceListener);

        ExecutorService dispatcher = serviceState.getEventDispatcher();

        //do registration with another server
        serviceState.preRegister(server2, objectName);
        serviceState.postRegister(true);

        // check no more actions on BundleContext
        argument = ArgumentCaptor.forClass(AllServiceListener.class);
        verify(context, atMost(1)).addServiceListener(argument.capture());
        assertEquals(1, argument.getAllValues().size());

        //do one unregister
        serviceState.postDeregister();

        //verify bundleListener not invoked
        verify(context, never()).removeServiceListener(serviceListener);
        assertFalse(dispatcher.isShutdown());

        //do second unregister and check cleanup
        serviceState.postDeregister();
        verify(context).removeServiceListener(serviceListener);
        assertTrue(dispatcher.isShutdown());
        dispatcher.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(dispatcher.isTerminated());



    }

    @Test
    public void testAttributeNotificationDisabled() throws Exception {
        StateConfig stateConfig = new StateConfig(false);

        //holders for Notifications captured
        List<AttributeChangeNotification> attributeChanges = new LinkedList<AttributeChangeNotification>();
        createService(stateConfig, new LinkedList<Notification>(), attributeChanges);

        assertEquals(0, attributeChanges.size());
    }

}
