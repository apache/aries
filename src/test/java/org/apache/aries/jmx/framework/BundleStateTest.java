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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.jmx.framework.BundleStateMBean.OBJECTNAME;

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
import org.apache.aries.jmx.codec.BundleEventData;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


public class BundleStateTest {

    private void createBundle(StateConfig stateConfig, final List<Notification> received,
                              final List<AttributeChangeNotification> attributeChanges) throws Exception {
        BundleContext context = mock(BundleContext.class);
        when(context.getBundles()).thenReturn(new Bundle [] {});
        PackageAdmin admin = mock(PackageAdmin.class);
        StartLevel startLevel = mock(StartLevel.class);
        Logger logger = mock(Logger.class);

        BundleState bundleState = new BundleState(context, admin, startLevel, stateConfig, logger);

        Bundle b1 = mock(Bundle.class);
        when(b1.getBundleId()).thenReturn(new Long(9));
        when(b1.getSymbolicName()).thenReturn("bundle");
        when(b1.getLocation()).thenReturn("file:/location");

        BundleEvent installedEvent = mock(BundleEvent.class);
        when(installedEvent.getBundle()).thenReturn(b1);
        when(installedEvent.getType()).thenReturn(BundleEvent.INSTALLED);

        BundleEvent resolvedEvent = mock(BundleEvent.class);
        when(resolvedEvent.getBundle()).thenReturn(b1);
        when(resolvedEvent.getType()).thenReturn(BundleEvent.RESOLVED);

        MBeanServer server = mock(MBeanServer.class);

        //setup for notification
        ObjectName objectName = new ObjectName(OBJECTNAME);
        bundleState.preRegister(server, objectName);
        bundleState.postRegister(true);

        //add NotificationListener to receive the events
        bundleState.addNotificationListener(new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
                if (notification instanceof AttributeChangeNotification) {
                    attributeChanges.add((AttributeChangeNotification) notification);
                } else {
                    received.add(notification);
                }
            }
        }, null, null);

        // capture the BundleListener registered with BundleContext to issue BundleEvents
        ArgumentCaptor<BundleListener> argument = ArgumentCaptor.forClass(BundleListener.class);
        verify(context).addBundleListener(argument.capture());

        //send events
        BundleListener listener = argument.getValue();
        listener.bundleChanged(installedEvent);
        listener.bundleChanged(resolvedEvent);

        //shutdown dispatcher via unregister callback
        bundleState.postDeregister();
        //check the BundleListener is cleaned up
        verify(context).removeBundleListener(listener);

        ExecutorService dispatcher = bundleState.getEventDispatcher();
        assertTrue(dispatcher.isShutdown());
        dispatcher.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(dispatcher.isTerminated());
    }

    @Test
    public void testNotificationsForBundleEvents() throws Exception {
        StateConfig stateConfig = new StateConfig();

        //holders for Notifications captured
        List<Notification> received = new LinkedList<Notification>();
        List<AttributeChangeNotification> attributeChanges = new LinkedList<AttributeChangeNotification>();

        createBundle(stateConfig, received, attributeChanges);

        assertEquals(2, received.size());
        Notification installed = received.get(0);
        assertEquals(1, installed.getSequenceNumber());
        CompositeData installedCompositeData = (CompositeData) installed.getUserData();
        BundleEventData installedData = BundleEventData.from(installedCompositeData);
        assertEquals("bundle", installedData.getBundleSymbolicName());
        assertEquals(9, installedData.getBundleId());
        assertEquals("file:/location", installedData.getLocation());
        assertEquals(BundleEvent.INSTALLED, installedData.getEventType());

        Notification resolved = received.get(1);
        assertEquals(2, resolved.getSequenceNumber());
        CompositeData resolvedCompositeData = (CompositeData) resolved.getUserData();
        BundleEventData resolvedData = BundleEventData.from(resolvedCompositeData);
        assertEquals("bundle", resolvedData.getBundleSymbolicName());
        assertEquals(9, resolvedData.getBundleId());
        assertEquals("file:/location", resolvedData.getLocation());
        assertEquals(BundleEvent.RESOLVED, resolvedData.getEventType());

        assertEquals(1, attributeChanges.size());
        AttributeChangeNotification ac = attributeChanges.get(0);
        assertEquals("BundleIds", ac.getAttributeName());
        assertEquals(0, ((long [])ac.getOldValue()).length);
        assertEquals(1, ((long [])ac.getNewValue()).length);
        assertEquals(9L, ((long [])ac.getNewValue())[0]);
    }

    @Test
    public void testNotificationsForBundleEventsDisabled() throws Exception {
        StateConfig stateConfig = new StateConfig();
        stateConfig.setBundleChangeNotificationEnabled(false);

        //holders for Notifications captured
        List<Notification> received = new LinkedList<Notification>();
        List<AttributeChangeNotification> attributeChanges = new LinkedList<AttributeChangeNotification>();

        createBundle(stateConfig, received, attributeChanges);

        assertEquals(0, received.size());
    }

    @Test
    public void testLifeCycleOfNotificationSupport() throws Exception {

        BundleContext context = mock(BundleContext.class);
        PackageAdmin admin = mock(PackageAdmin.class);
        StartLevel startLevel = mock(StartLevel.class);
        Logger logger = mock(Logger.class);

        BundleState bundleState = new BundleState(context, admin, startLevel, new StateConfig(), logger);

        MBeanServer server1 = mock(MBeanServer.class);
        MBeanServer server2 = mock(MBeanServer.class);

        ObjectName objectName = new ObjectName(OBJECTNAME);
        bundleState.preRegister(server1, objectName);
        bundleState.postRegister(true);

        // capture the BundleListener registered with BundleContext
        ArgumentCaptor<BundleListener> argument = ArgumentCaptor.forClass(BundleListener.class);
        verify(context).addBundleListener(argument.capture());
        assertEquals(1, argument.getAllValues().size());

        BundleListener listener = argument.getValue();
        assertNotNull(listener);

        ExecutorService dispatcher = bundleState.getEventDispatcher();

        //do registration with another server
        bundleState.preRegister(server2, objectName);
        bundleState.postRegister(true);

        // check no more actions on BundleContext
        argument = ArgumentCaptor.forClass(BundleListener.class);
        verify(context, atMost(1)).addBundleListener(argument.capture());
        assertEquals(1, argument.getAllValues().size());

        //do one unregister
        bundleState.postDeregister();

        //verify bundleListener not invoked
        verify(context, never()).removeBundleListener(listener);
        assertFalse(dispatcher.isShutdown());

        //do second unregister and check cleanup
        bundleState.postDeregister();
        verify(context).removeBundleListener(listener);
        assertTrue(dispatcher.isShutdown());
        dispatcher.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(dispatcher.isTerminated());



    }

    @Test
    public void testAttributeNotificationDisabled() throws Exception {
        StateConfig stateConfig = new StateConfig();
        stateConfig.setAttributeChangeNotificationEnabled(false);

        //holders for Notifications captured
        List<AttributeChangeNotification> attributeChanges = new LinkedList<AttributeChangeNotification>();
        createBundle(stateConfig, new LinkedList<Notification>(), attributeChanges);

        assertEquals(0, attributeChanges.size());
    }


}
