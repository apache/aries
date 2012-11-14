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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.apache.aries.jmx.blueprint.BlueprintStateMBean;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.apache.aries.jmx.blueprint");

    protected BundleContext bundleContext;

    protected StandardMBean blueprintState;
    protected ObjectName blueprintStateName;

    protected StandardMBean blueprintMetadata;
    protected ObjectName blueprintMetadataName;

    protected List<MBeanServer> mbeanServers = new CopyOnWriteArrayList<MBeanServer>();
    protected ServiceTracker mbeanServiceTracker;

    protected AtomicBoolean servicesRegistered = new AtomicBoolean(false);

    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        this.blueprintStateName = new ObjectName(BlueprintStateMBean.OBJECTNAME);
        this.blueprintMetadataName = new ObjectName(BlueprintMetadataMBean.OBJECTNAME);

        // create MBeanServerServiceTracker
        // if there has been already a MBeanServer Service in place, this MBeanServerServiceTracker won't miss it
        mbeanServiceTracker = new ServiceTracker(bundleContext, MBeanServer.class.getCanonicalName(),
                new MBeanServerServiceTracker());
        LOGGER.debug("Awaiting MBeanServer service registration");
        mbeanServiceTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        for (MBeanServer mbeanServer : mbeanServers) {
            this.deregisterMBeans(mbeanServer);
        }
        mbeanServiceTracker.close();
        mbeanServers.clear();
    }

    class MBeanServerServiceTracker implements ServiceTrackerCustomizer {

        public Object addingService(ServiceReference servicereference) {
            try {
                LOGGER.debug("Adding MBeanServer: {}", servicereference);
                final MBeanServer mbeanServer = (MBeanServer) bundleContext.getService(servicereference);
                Activator.this.mbeanServers.add(mbeanServer);
                Activator.this.processRegister(mbeanServer);
                return mbeanServer;
            } catch (RuntimeException e) {
                LOGGER.error("uncaught exception in addingService", e);
                throw e;
            }
        }

        public void removedService(ServiceReference servicereference, Object obj) {
            try {
                LOGGER.debug("Removing MBeanServer: {}", servicereference);
                final MBeanServer mbeanServer = (MBeanServer) bundleContext.getService(servicereference);
                Activator.this.mbeanServers.remove(mbeanServer);
                Activator.this.processDeregister(mbeanServer);
            } catch (Throwable e) {
                LOGGER.debug("uncaught exception in removedService", e);
            }
        }

        public void modifiedService(ServiceReference servicereference, Object obj) {
            // no op
        }

    }

    private void processRegister(final MBeanServer mbeanServer) {
        Runnable registration = new Runnable() {
            public void run() {
                Activator.this.registerMBeans(mbeanServer);
            }
        };
        Thread registrationThread = new Thread(registration, "Blueprint MBeans Registration");
        registrationThread.setDaemon(true);
        registrationThread.start();

    }

    private void processDeregister(final MBeanServer mbeanServer) {
        Runnable deregister = new Runnable() {
            public void run() {
                Activator.this.deregisterMBeans(mbeanServer);
            }
        };

        Thread deregisterThread = new Thread(deregister, "Blueprint MBeans Deregistration");
        deregisterThread.setDaemon(true);
        deregisterThread.start();
    }

    protected synchronized void registerMBeans(MBeanServer mbeanServer) {
        // create BlueprintStateMBean
        /* the StardardMBean does not implement the MBeanRegistration in jdk1.5 */
        try {
            blueprintState = new RegistrableStandardEmitterMBean(new BlueprintState(bundleContext), BlueprintStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            LOGGER.error("Unable to create StandardMBean for BlueprintState", e);
            return;
        }

        // register BlueprintStateMBean to MBean server
        LOGGER.debug("Registering bundle state monitor with MBeanServer: {} with name: {}",
                        mbeanServer, blueprintStateName);
        try {
            mbeanServer.registerMBean(blueprintState, blueprintStateName);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.debug("Cannot register BlueprintStateMBean");
        } catch (MBeanRegistrationException e) {
            LOGGER.error("Cannot register BlueprintStateMBean", e);
        } catch (NotCompliantMBeanException e) {
            LOGGER.error("Cannot register BlueprintStateMBean", e);
        }

        // create BlueprintMetadataMBean
        try {
            blueprintMetadata = new StandardMBean(new BlueprintMetadata(bundleContext), BlueprintMetadataMBean.class);
        } catch (NotCompliantMBeanException e) {
            LOGGER.error("Unable to create StandardMBean for BlueprintMetadata", e);
            return;
        }
        // register BlueprintMetadataMBean to MBean server
        LOGGER.debug("Registering bundle metadata monitor with MBeanServer: {} with name: {}",
                    mbeanServer, blueprintMetadataName);
        try {
            mbeanServer.registerMBean(blueprintMetadata, blueprintMetadataName);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.debug("Cannot register BlueprintMetadataMBean");
        } catch (MBeanRegistrationException e) {
            LOGGER.error("Cannot register BlueprintMetadataMBean", e);
        } catch (NotCompliantMBeanException e) {
            LOGGER.error("Cannot register BlueprintMetadataMBean", e);
        }

        servicesRegistered.set(true);
    }

    protected synchronized void deregisterMBeans(MBeanServer mbeanServer) {
        if (!servicesRegistered.get()) {
            return;
        }
        // unregister BlueprintStateMBean from MBean server
        try {
            mbeanServer.unregisterMBean(blueprintStateName);
        } catch (InstanceNotFoundException e) {
            LOGGER.debug("BlueprintStateMBean not found on deregistration");
        } catch (MBeanRegistrationException e) {
            LOGGER.error("BlueprintStateMBean deregistration problem");
        }
        blueprintState = null;

        // unregister BlueprintMetadataMBean from MBean server
        try {
            mbeanServer.unregisterMBean(blueprintMetadataName);
        } catch (InstanceNotFoundException e) {
            LOGGER.debug("BlueprintMetadataMBean not found on deregistration");
        } catch (MBeanRegistrationException e) {
            LOGGER.error("BlueprintMetadataMBean deregistration problem");
        }
        blueprintMetadata = null;

        servicesRegistered.set(false);
    }

}
