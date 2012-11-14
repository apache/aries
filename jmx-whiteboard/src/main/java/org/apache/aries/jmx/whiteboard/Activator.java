/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.whiteboard;

import javax.management.MBeanServer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
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
        mbeanTracker.open(true);
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

        /**
         * Listens for any services registered with a "jmx.objectname" service
         * property. If the property is not a non-empty String object the service
         * is expected to implement the MBeanRegistration interface to create
         * the name dynamically.
         */
        private static final String SIMPLE_MBEAN_FILTER = "("
            + JmxWhiteboardSupport.PROP_OBJECT_NAME+ "=*)";

        public MBeanTracker(BundleContext context)
                throws InvalidSyntaxException {
            super(context, context.createFilter(SIMPLE_MBEAN_FILTER), null);
        }

        @Override
        public Object addingService(ServiceReference reference) {
            Object mbean = super.addingService(reference);
            jmxWhiteBoard.registerMBean(mbean, reference);
            return mbean;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            jmxWhiteBoard.unregisterMBean(service);
            super.removedService(reference, service);
        }
    }
}
