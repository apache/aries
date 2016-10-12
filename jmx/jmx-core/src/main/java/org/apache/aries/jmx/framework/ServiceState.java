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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
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
 * <code>ServiceEvent</code> events and changes to the <code>ServiceIds</code> attribute.
 *
 * @version $Rev$ $Date$
 */
public class ServiceState extends NotificationBroadcasterSupport implements ServiceStateMBean, MBeanRegistration {

    protected Logger logger;
    private BundleContext bundleContext;
    private StateConfig stateConfig;

    protected ExecutorService eventDispatcher;
    protected AllServiceListener serviceListener;
    private AtomicInteger notificationSequenceNumber = new AtomicInteger(1);
    private AtomicInteger attributeChangeNotificationSequenceNumber = new AtomicInteger(1);
    private AtomicInteger registrations = new AtomicInteger(0);
    private Lock lock = new ReentrantLock();

    // notification type description
    public static String SERVICE_EVENT = "org.osgi.service.event";

    public ServiceState(BundleContext bundleContext, StateConfig stateConfig, Logger logger) {
        if (bundleContext == null) {
            throw new IllegalArgumentException("Argument bundleContext cannot be null");
        }
        this.bundleContext = bundleContext;
        this.stateConfig = stateConfig;
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
     * @see org.osgi.jmx.framework.ServiceStateMBean#getProperty(long, java.lang.String)
     */
    public CompositeData getProperty(long serviceId, String key) throws IOException {
        ServiceReference reference = resolveService(bundleContext, serviceId);
            return PropertyData.newInstance(key, reference.getProperty(key)).toCompositeData();
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
     * @see org.osgi.jmx.framework.ServiceStateMBean#getService(long)
     */
    public CompositeData getService(long serviceId) throws IOException {
        return new ServiceData(resolveService(bundleContext, serviceId)).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#listServices()
     */
    public TabularData listServices() throws IOException {
        return listServices(null, null);
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#listServices(java.lang.String, java.lang.String)
     */
    public TabularData listServices(String clazz, String filter) throws IOException {
        return listServices(clazz, filter, ServiceStateMBean.SERVICE_TYPE.keySet());
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#listServices(java.lang.String, java.lang.String, java.lang.String...)
     */
    public TabularData listServices(String clazz, String filter, String ... serviceTypeItems) throws IOException {
        return listServices(clazz, filter, Arrays.asList(serviceTypeItems));
    }

    private TabularData listServices(String clazz, String filter, Collection<String> serviceTypeItems) throws IOException {
        TabularData servicesTable = new TabularDataSupport(SERVICES_TYPE);
        ServiceReference[] allServiceReferences = null;
        try {
            allServiceReferences = bundleContext.getAllServiceReferences(clazz, filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Failed to retrieve all service references", e);
        }
        if (allServiceReferences != null) {
            for (ServiceReference reference : allServiceReferences) {
                servicesTable.put(new ServiceData(reference).toCompositeData(serviceTypeItems));
            }
        }
        return servicesTable;
    }

    /**
     * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        MBeanNotificationInfo eventInfo = new MBeanNotificationInfo(
                new String[] { SERVICE_EVENT },
                Notification.class.getName(),
                "A ServiceEvent issued from the Framework describing a service lifecycle change");

        MBeanNotificationInfo attributeChangeInfo = new MBeanNotificationInfo(
                new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                AttributeChangeNotification.class.getName(),
                "An attribute of this MBean has changed");

        return new MBeanNotificationInfo[] { eventInfo, attributeChangeInfo };
    }

    /**
     * @see org.osgi.jmx.framework.ServiceStateMBean#getServiceIds()
     */
    public long[] getServiceIds() throws IOException {
        try {
            ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(null, null);
            long[] ids = new long[refs.length];
            for (int i=0; i < refs.length; i++) {
                ServiceReference<?> ref = refs[i];
                long id = (Long) ref.getProperty(Constants.SERVICE_ID);
                ids[i] = id;
            }

            // The IDs are sorted here. It's not required by the spec but it's nice
            // to have an ordered list returned.
            Arrays.sort(ids);

            return ids;
        } catch (InvalidSyntaxException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
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
                        if (stateConfig != null && !stateConfig.isServiceChangeNotificationEnabled()) {
                            return;
                        }
                        try {
                            // Create a notification for the event
                            final Notification notification = new Notification(EVENT, OBJECTNAME,
                                    notificationSequenceNumber.getAndIncrement());
                            notification.setUserData(new ServiceEventData(serviceevent).toCompositeData());

                            // also send notifications to the serviceIDs attribute listeners, if a service was added or removed
                            final AttributeChangeNotification attributeChangeNotification =
                                    getAttributeChangeNotification(serviceevent);

                            eventDispatcher.submit(new Runnable() {
                                public void run() {
                                    sendNotification(notification);
                                    if (attributeChangeNotification != null)
                                        sendNotification(attributeChangeNotification);
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

    protected AttributeChangeNotification getAttributeChangeNotification(ServiceEvent serviceevent) throws IOException {
        if (stateConfig != null && !stateConfig.isAttributeChangeNotificationEnabled()) {
            return null;
        }

        int eventType = serviceevent.getType();
        switch (eventType) {
        case ServiceEvent.REGISTERED:
        case ServiceEvent.UNREGISTERING:
            long serviceID = (Long) serviceevent.getServiceReference().getProperty(Constants.SERVICE_ID);
            long[] ids = getServiceIds();

            List<Long> without = new ArrayList<Long>(ids.length);
            for (long id : ids) {
                if (id != serviceID)
                    without.add(id);
            }
            List<Long> with = new ArrayList<Long>(without);
            with.add(serviceID);

            // Sorting is not mandatory, but its nice for the user, note that getServiceIds() also returns a sorted array
            Collections.sort(with);

            List<Long> oldList = eventType == ServiceEvent.REGISTERED ? without : with;
            List<Long> newList = eventType == ServiceEvent.REGISTERED ? with : without;

            long[] oldIDs = new long[oldList.size()];
            for (int i = 0; i < oldIDs.length; i++) {
                oldIDs[i] = oldList.get(i);
            }

            long[] newIDs = new long[newList.size()];
            for (int i = 0; i < newIDs.length; i++) {
                newIDs[i] = newList.get(i);
            }

            return new AttributeChangeNotification(OBJECTNAME, attributeChangeNotificationSequenceNumber.getAndIncrement(),
                    System.currentTimeMillis(), "ServiceIds changed", "ServiceIds", "Array of long", oldIDs, newIDs);
        default:
            return null;
        }
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
