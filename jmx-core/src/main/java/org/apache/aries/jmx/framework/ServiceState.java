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

import static org.apache.aries.jmx.util.FrameworkUtils.getBundleIds;
import static org.apache.aries.jmx.util.FrameworkUtils.resolveService;
import static org.osgi.jmx.JmxConstants.PROPERTIES_TYPE;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.JMXThreadFactory;
import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.codec.PropertyData;
import org.apache.aries.jmx.codec.ServiceData;
import org.apache.aries.jmx.codec.ServiceEventData;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.osgi.service.log.LogService;

/**
 * Implementation of <code>ServiceStateMBean</code> which emits JMX <code>Notification</code> for framework
 * <code>ServiceEvent</code> events
 * 
 * @version $Rev$ $Date$
 */
public class ServiceState extends NotificationBroadcasterSupport implements ServiceStateMBean, MBeanRegistration {

    protected Logger logger;
    private BundleContext bundleContext;

    protected ExecutorService eventDispatcher;
    protected AllServiceListener serviceListener;
    private AtomicInteger notificationSequenceNumber = new AtomicInteger(1);
    private AtomicInteger registrations = new AtomicInteger(0);
    private Lock lock = new ReentrantLock();
    // notification type description
    public static String SERVICE_EVENT = "org.osgi.service.event";

    public ServiceState(BundleContext bundleContext, Logger logger) {
        if (bundleContext == null) {
            throw new IllegalArgumentException("Argument bundleContext cannot be null");
        }
        this.bundleContext = bundleContext;
        this.logger = logger;
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#getBundleIdentifier(long)
     */
    public long getBundleIdentifier(long serviceId) throws IOException {
        ServiceReference reference = resolveService(bundleContext, serviceId);
        return reference.getBundle().getBundleId();
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#getObjectClass(long)
     */
    public String[] getObjectClass(long serviceId) throws IOException {
        ServiceReference reference = resolveService(bundleContext, serviceId);
        return (String[]) reference.getProperty(Constants.OBJECTCLASS);
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#getProperties(long)
     */
    public TabularData getProperties(long serviceId) throws IOException {
        ServiceReference reference = resolveService(bundleContext, serviceId);
        TabularData propertiesTable = new TabularDataSupport(PROPERTIES_TYPE);
        for (String propertyKey : reference.getPropertyKeys()) {
            propertiesTable.put(PropertyData.newInstance(propertyKey, reference.getProperty(propertyKey))
                    .toCompositeData());
        }
        return propertiesTable;
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#getUsingBundles(long)
     */
    public long[] getUsingBundles(long serviceId) throws IOException {
        ServiceReference reference = resolveService(bundleContext, serviceId);
        Bundle[] usingBundles = reference.getUsingBundles();
        return getBundleIds(usingBundles);
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#listServices()
     */
    public TabularData listServices() throws IOException {
        TabularData servicesTable = new TabularDataSupport(SERVICES_TYPE);
        ServiceReference[] allServiceReferences = null;
        try {
            allServiceReferences = bundleContext.getAllServiceReferences(null, null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Failed to retrieve all service references", e);
        }
        if (allServiceReferences != null) {
            for (ServiceReference reference : allServiceReferences) {
                servicesTable.put(new ServiceData(reference).toCompositeData());
            }
        }
        return servicesTable;
    }

    /**
     * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[] { SERVICE_EVENT };
        String name = Notification.class.getName();
        String description = "A ServiceEvent issued from the Framework describing a service lifecycle change";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[] { info };
    }

    /**
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
        if (registrations.decrementAndGet() < 1) {
            shutDownDispatcher();
        }
    }

    /**
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean registrationDone) {
        if (registrationDone && registrations.incrementAndGet() == 1) {
            eventDispatcher = Executors.newSingleThreadExecutor(new JMXThreadFactory("JMX OSGi Service State Event Dispatcher"));
            bundleContext.addServiceListener(serviceListener);
        }
    }

    public void preDeregister() throws Exception {
        // No action
    }

    /**
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        lock.lock();
        try {
            if (serviceListener == null) {
                serviceListener = new AllServiceListener() {
                    public void serviceChanged(ServiceEvent serviceevent) {
                        final Notification notification = new Notification(EVENT, OBJECTNAME,
                                notificationSequenceNumber.getAndIncrement());
                        try {
                            notification.setUserData(new ServiceEventData(serviceevent).toCompositeData());
                            eventDispatcher.submit(new Runnable() {
                                public void run() {
                                    sendNotification(notification);
                                }
                            });
                        } catch (RejectedExecutionException re) {
                            logger.log(LogService.LOG_WARNING, "Task rejected for JMX Notification dispatch of event ["
                                    + serviceevent + "] - Dispatcher may have been shutdown");
                        } catch (Exception e) {
                            logger.log(LogService.LOG_WARNING,
                                    "Exception occured on JMX Notification dispatch for event [" + serviceevent + "]",
                                    e);
                        }
                    }
                };
            }
        } finally {
            lock.unlock();
        }
        return name;
    }

    /*
     * Shuts down the notification dispatcher
     * [ARIES-259] MBeans not getting unregistered reliably
     */
    protected void shutDownDispatcher() {
        if (serviceListener != null) {
            try {
               bundleContext.removeServiceListener(serviceListener);
            }
            catch (Exception e) {
               // ignore
            }
        }
        if (eventDispatcher != null) {  
            eventDispatcher.shutdown();
        }
    }

    /*
     * Returns the ExecutorService used to dispatch Notifications
     */
    protected ExecutorService getEventDispatcher() {
        return eventDispatcher;
    }

}
