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

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MBeanHolder {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Object mbean;

    private final ObjectName requestedObjectName;

    private final Map<MBeanServer, ObjectName> registrations;

    MBeanHolder(final Object mbean, final ObjectName requestedObjectName) {
        this.mbean = mbean;
        this.requestedObjectName = requestedObjectName;
        this.registrations = new IdentityHashMap<MBeanServer, ObjectName>();
    }

    void register(final MBeanServer server) {
        ObjectInstance instance;
        try {
            instance = server.registerMBean(mbean, requestedObjectName);
            registrations.put(server, instance.getObjectName());
        } catch (InstanceAlreadyExistsException e) {
            log.error("register: Failure registering MBean " + mbean, e);
        } catch (MBeanRegistrationException e) {
            log.error("register: Failure registering MBean " + mbean, e);
        } catch (NotCompliantMBeanException e) {
            log.error("register: Failure registering MBean " + mbean, e);
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

    // ---------- Object Overwrite

    @Override
    public int hashCode() {
        return mbean.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof MBeanHolder) {
            return mbean == ((MBeanHolder) obj).mbean;
        }

        return false;
    }
}
