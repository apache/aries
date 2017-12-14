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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.aries.jmx.util.shared.RegistrableStandardEmitterMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MBeanHolder {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Object mbean;

    private final ObjectName requestedObjectName;

    private final Map<MBeanServer, ObjectName> registrations;

    static <T> MBeanHolder create(final T mbean,
            final ObjectName requestedObjectName) {
        if (mbean instanceof DynamicMBean) {
            return new MBeanHolder(mbean, requestedObjectName);
        } else if (mbean == null) {
            return null;
        }

        Class<?> mbeanClass = mbean.getClass();
        @SuppressWarnings("unchecked") // This is all in aid of getting new StandardMBean to work.
        Class<T> mbeanInterface = (Class<T>) getMBeanInterface(mbeanClass);
        if (mbeanInterface == null) {
            return null;
        }

        if (mbeanInterface.getName().equals(
            mbeanClass.getName().concat("MBean")) || mbeanInterface.getName().equals(mbeanClass.getName().concat("MXBean")) ) {
            return new MBeanHolder(mbean, requestedObjectName);
        }

        try {
            StandardMBean stdMbean = new RegistrableStandardEmitterMBean(mbean, mbeanInterface);
            return new MBeanHolder(stdMbean, requestedObjectName);
        } catch (NotCompliantMBeanException e) {
            LoggerFactory.getLogger(MBeanHolder.class).error(
                "create: Cannot create StandardMBean for " + mbean
                    + " of type " + mbeanClass + " for interface "
                    + mbeanInterface, e);
            return null;
        }
    }

    private static Class<?> getMBeanInterface(final Class<?> mbeanClass) {
        if (mbeanClass == null) {
            return null;
        }

        for (Class<?> i : mbeanClass.getInterfaces()) {
            if (i.getName().endsWith("MBean") || i.getName().endsWith("MXBean")) {
                return i;
            }

            Class<?> mbeanInterface = getMBeanInterface(i);
            if (mbeanInterface != null) {
                return mbeanInterface;
            }
        }

        if (mbeanClass.getSuperclass() != null) {
            return getMBeanInterface(mbeanClass.getSuperclass());
        }

        return null;
    }

    MBeanHolder(final Object mbean, final ObjectName requestedObjectName) {
        this.mbean = mbean;
        this.requestedObjectName = requestedObjectName;
        this.registrations = new IdentityHashMap<MBeanServer, ObjectName>();
    }

    void register(final MBeanServer server, String[] warnExceptions) {
        ObjectInstance instance;
        try {
            instance = server.registerMBean(mbean, requestedObjectName);
            registrations.put(server, instance.getObjectName());
        } catch (Exception e) {
            String exClass = e.getClass().getName();
            if (warnExceptions == null)
                warnExceptions = new String[] {};

            for (String exCls : warnExceptions) {
                if (exClass.equals(exCls)) {
                    log.warn("register: problem registering MBean " + mbean, e);
                    return;
                }
            }

            if (e instanceof InstanceAlreadyExistsException ||
                    e instanceof MBeanRegistrationException ||
                    e instanceof NotCompliantMBeanException) {
                log.error("register: Failure registering MBean " + mbean, e);
            } else if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            }
        }
    }

    void unregister(final MBeanServer server) {
        final ObjectName registeredName = registrations.remove(server);
        if (registeredName != null) {
            unregister(server, registeredName);
        }
    }

    void unregister() {
        for (Entry<MBeanServer, ObjectName> entry : registrations.entrySet()) {
            unregister(entry.getKey(), entry.getValue());
        }
        registrations.clear();
    }

    private void unregister(final MBeanServer server, final ObjectName name) {
        try {
            server.unregisterMBean(name);
        } catch (MBeanRegistrationException e) {
            log.error("unregister: preDeregister of " + name
                + " threw an exception", e);
        } catch (InstanceNotFoundException e) {
            // not really expected !
            log.error("unregister: Unexpected unregistration problem of MBean "
                + name, e);
        }
    }
}
