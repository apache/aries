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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JmxWhiteboardSupport {

    static final String PROP_OBJECT_NAME = "jmx.objectname";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private MBeanServer[] mbeanServers = new MBeanServer[0];

    // mapping registered MBean services to their MBeanHolder objects
    private final IdentityHashMap<Object, MBeanHolder> mbeans = new IdentityHashMap<Object, MBeanHolder>();

    protected synchronized void addMBeanServer(final MBeanServer mbeanServer) {

        log.debug("addMBeanServer: Adding MBeanServer {}", mbeanServer);

        ArrayList<MBeanServer> serverList = new ArrayList<MBeanServer>(
            Arrays.asList(mbeanServers));
        serverList.add(mbeanServer);
        mbeanServers = serverList.toArray(new MBeanServer[serverList.size()]);

        // register all mbeans with the new server
        for (MBeanHolder mbean : mbeans.values()) {
            mbean.register(mbeanServer);
        }
    }

    protected synchronized void removeMBeanServer(final MBeanServer mbeanServer) {

        log.debug("removeMBeanServer: Removing MBeanServer {}", mbeanServer);

        // remove all dynamically registered mbeans from the server
        for (MBeanHolder mbean : mbeans.values()) {
            mbean.unregister(mbeanServer);
        }

        ArrayList<MBeanServer> serverList = new ArrayList<MBeanServer>(
            Arrays.asList(mbeanServers));
        serverList.remove(mbeanServer);
        mbeanServers = serverList.toArray(new MBeanServer[serverList.size()]);
    }

    protected synchronized void registerMBean(Object mbean, final ServiceReference props) {

        log.debug("registerMBean: Adding MBean {}", mbean);

        ObjectName objectName = getObjectName(props);
        if (objectName != null || mbean instanceof MBeanRegistration) {
            MBeanHolder holder = MBeanHolder.create(mbean, objectName);
            if (holder != null) {
                MBeanServer[] mbeanServers = this.mbeanServers;
                for (MBeanServer mbeanServer : mbeanServers) {
                    holder.register(mbeanServer);
                }
                mbeans.put(mbean, holder);
            } else {
                log.error(
                    "registerMBean: Cannot register MBean service {} with MBean servers: Not an instanceof DynamicMBean or not MBean spec compliant standard MBean",
                    mbean);
            }
        } else {
            log.error(
                "registerMBean: MBean service {} not registered with valid jmx.objectname propety and not implementing MBeanRegistration interface; not registering with MBean servers",
                mbean);
        }
    }

    protected synchronized void unregisterMBean(Object mbean) {

        log.debug("unregisterMBean: Removing MBean {}", mbean);

        final MBeanHolder holder = mbeans.remove(mbean);
        if (holder != null) {
            holder.unregister();
        }
    }

    private ObjectName getObjectName(final ServiceReference props) {
        Object oName = props.getProperty(PROP_OBJECT_NAME);
        if (oName instanceof ObjectName) {
            return (ObjectName) oName;
        } else if (oName instanceof String) {
            try {
                return new ObjectName((String) oName);
            } catch (MalformedObjectNameException e) {
                log.error("getObjectName: Provided ObjectName property "
                    + oName + " cannot be used as an ObjectName", e);
            }
        } else {
            log.info(
                "getObjectName: Missing {} service property (or wrong type); registering if MBean is MBeanRegistration implementation",
                PROP_OBJECT_NAME);
        }

        return null;
    }
}
