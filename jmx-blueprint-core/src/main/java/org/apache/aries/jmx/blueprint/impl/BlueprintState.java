/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.blueprint.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.blueprint.BlueprintStateMBean;
import org.apache.aries.jmx.blueprint.codec.OSGiBlueprintEvent;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintState extends NotificationBroadcasterSupport implements BlueprintStateMBean, MBeanRegistration {

    // notification type description
    public static String BLUEPRINT_EVENT = "org.osgi.blueprint.event";

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintState.class);

    private BundleContext context;

    private ServiceRegistration listenerReg;

    private Map<Long, CompositeData> dataMap = new HashMap<Long, CompositeData>();

    private ExecutorService eventDispatcher;

    private AtomicInteger notificationSequenceNumber = new AtomicInteger(1);

    private AtomicInteger registrations = new AtomicInteger(0);

    public BlueprintState(BundleContext context) {
        this.context = context;
    }

    public synchronized long[] getBlueprintBundleIds() throws IOException {
        Long[] bundleIdKeys = dataMap.keySet().toArray(new Long[dataMap.size()]);
        long[] bundleIds = new long[bundleIdKeys.length];
        for (int i = 0; i < bundleIdKeys.length; i++) {
            bundleIds[i] = bundleIdKeys[i];
        }
        return bundleIds;
    }

    public synchronized CompositeData getLastEvent(long bundleId) throws IOException {
        return dataMap.get(bundleId);
    }

    public synchronized TabularData getLastEvents() throws IOException {
        TabularDataSupport table = new TabularDataSupport(BlueprintStateMBean.OSGI_BLUEPRINT_EVENTS_TYPE);
        table.putAll(dataMap);
        return table;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        // no op
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        // reg listener
        if (registrationDone && registrations.incrementAndGet() == 1) {
            BlueprintListener listener = new BlueprintStateListener();
            eventDispatcher = Executors.newSingleThreadExecutor(new JMXThreadFactory("JMX OSGi Blueprint State Event Dispatcher"));
            listenerReg = context.registerService(BlueprintListener.class.getName(), listener, null);
        }
    }

    public void preDeregister() throws Exception {
        // no op
    }

    public void postDeregister() {
        if (registrations.decrementAndGet() < 1) {
            AriesFrameworkUtil.safeUnregisterService(listenerReg); 
          
            if (eventDispatcher != null) {
                eventDispatcher.shutdown(); 
            }
        }
    }

    protected synchronized void onEvent(BlueprintEvent event) {
        CompositeData data = new OSGiBlueprintEvent(event).asCompositeData();
        dataMap.put(event.getBundle().getBundleId(), data);

        if (!event.isReplay()) {
            final Notification notification = new Notification(EVENT_TYPE, OBJECTNAME,
                    notificationSequenceNumber.getAndIncrement());
            try {
                notification.setUserData(data);
                eventDispatcher.submit(new Runnable() {
                    public void run() {
                        sendNotification(notification);
                    }
                });
            } catch (RejectedExecutionException re) {
                LOGGER.warn("Task rejected for JMX Notification dispatch of event ["
                        + event + "] - Dispatcher may have been shutdown");
            } catch (Exception e) {
                LOGGER.warn("Exception occured on JMX Notification dispatch for event [" + event + "]", e);
            }
        }
    }

    /**
     * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[] { BLUEPRINT_EVENT };
        String name = Notification.class.getName();
        String description = "A BlueprintEvent issued from the Blueprint Extender describing a blueprint bundle lifecycle change";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[] { info };
    }

    private class BlueprintStateListener implements BlueprintListener {
        public void blueprintEvent(BlueprintEvent event) {
            onEvent(event);
        }

    }

    public static class JMXThreadFactory implements ThreadFactory {
        private final ThreadFactory factory = Executors.defaultThreadFactory();
        private final String name;

        public JMXThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            final Thread t = factory.newThread(r);
            t.setName(name);
            t.setDaemon(true);
            return t;
        }
    }

}
