/*
 * Copyright 1997-2010 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.whiteboard.fmeschbe.jmx.whiteboard;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private JmxWhiteboardSupport jmxWhiteBoard;

    private ServiceTracker mbeanServerTracker;

    private ServiceTracker mbeanTracker;

    public void start(BundleContext context) throws Exception {
        jmxWhiteBoard = new JmxWhiteboardSupport();

        mbeanServerTracker = new MBeanServerTracker(context);
        mbeanServerTracker.open();

        mbeanTracker = new MBeanTracker(context);
        mbeanTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        if (mbeanTracker != null) {
            mbeanTracker.close();
            mbeanTracker = null;
        }

        if (mbeanServerTracker != null) {
            mbeanServerTracker.close();
            mbeanServerTracker = null;
        }

        jmxWhiteBoard = null;
    }

    private class MBeanServerTracker extends ServiceTracker {

        public MBeanServerTracker(BundleContext context) {
            super(context, MBeanServer.class.getName(), null);
        }

        @Override
        public Object addingService(ServiceReference reference) {
            MBeanServer mbeanServer = (MBeanServer) super.addingService(reference);
            jmxWhiteBoard.addMBeanServer(mbeanServer);
            return mbeanServer;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (service instanceof MBeanServer) {
                jmxWhiteBoard.removeMBeanServer((MBeanServer) service);
            }
            super.removedService(reference, service);
        }
    }

    private class MBeanTracker extends ServiceTracker {

        public MBeanTracker(BundleContext context) {
            super(context, DynamicMBean.class.getName(), null);
        }

        @Override
        public Object addingService(ServiceReference reference) {
            DynamicMBean mbean = (DynamicMBean) super.addingService(reference);
            jmxWhiteBoard.registerMBean(mbean, reference);
            return mbean;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (service instanceof DynamicMBean) {
                jmxWhiteBoard.unregisterMBean((DynamicMBean) service);
            }
            super.removedService(reference, service);
        }
    }
}
