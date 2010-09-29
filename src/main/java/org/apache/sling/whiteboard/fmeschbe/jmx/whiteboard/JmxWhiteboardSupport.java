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
package org.apache.sling.whiteboard.fmeschbe.jmx.whiteboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = false)
public class JmxWhiteboardSupport {

    private static final String PROP_OBJECT_NAME = "jmx.objectname";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(referenceInterface = MBeanServer.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "addMBeanServer", unbind = "removeMBeanServer")
    private MBeanServer[] mbeanServers = new MBeanServer[0];

    @Reference(referenceInterface = DynamicMBean.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "registerMBean", unbind = "unregisterMBean")
    private final Map<DynamicMBean, Map<MBeanServer, ObjectName>> mbeans = new IdentityHashMap<DynamicMBean, Map<MBeanServer, ObjectName>>();

    protected void addMBeanServer(final MBeanServer mbeanServer) {
        List<MBeanServer> serverList = Arrays.asList(mbeanServers);
        serverList.add(mbeanServer);
        mbeanServers = serverList.toArray(new MBeanServer[serverList.size()]);

        // TODO: register all mbeans with the new server
        for (Entry<DynamicMBean, Map<MBeanServer, ObjectName>> mbean : mbeans.entrySet()) {
        }
    }

    protected void removeMBeanServer(final MBeanServer mbeanServer) {
        for (Map<MBeanServer, ObjectName> registration : mbeans.values()) {
            ObjectName name = registration.remove(mbeanServer);
            if (name != null) {
                try {
                    mbeanServer.unregisterMBean(name);
                } catch (MBeanRegistrationException e) {
                    log.error("unregisterMBean: Failure unregistering", e);
                } catch (InstanceNotFoundException e) {
                    log.error("unregisterMBean: Failure unregistering", e);
                }
            }
        }

        List<MBeanServer> serverList = Arrays.asList(mbeanServers);
        serverList.remove(mbeanServer);
        mbeanServers = serverList.toArray(new MBeanServer[serverList.size()]);
    }

    protected void registerMBean(DynamicMBean mbean, Map<String, Object> props) {
        ObjectName objectName = getObjectName(props);
        if (objectName != null || mbean instanceof MBeanRegistration) {
            Map<MBeanServer, ObjectName> registration = new HashMap<MBeanServer, ObjectName>();
            MBeanServer[] mbeanServers = this.mbeanServers;
            for (MBeanServer mbeanServer : mbeanServers) {
                try {
                    ObjectInstance registeredObject = mbeanServer.registerMBean(
                        mbean, objectName);
                    registration.put(mbeanServer,
                        registeredObject.getObjectName());
                } catch (InstanceAlreadyExistsException e) {
                    log.error("registerMBean: Failure registering MBean "
                        + mbean, e);
                } catch (MBeanRegistrationException e) {
                    log.error("registerMBean: Failure registering MBean "
                        + mbean, e);
                } catch (NotCompliantMBeanException e) {
                    log.error("registerMBean: Failure registering MBean "
                        + mbean, e);
                }
            }
            mbeans.put(mbean, registration);
        }
    }

    protected void unregisterMBean(DynamicMBean mbean) {
        Map<MBeanServer, ObjectName> registration = mbeans.remove(mbean);
        for (Entry<MBeanServer, ObjectName> reg : registration.entrySet()) {
            try {
                reg.getKey().unregisterMBean(reg.getValue());
            } catch (MBeanRegistrationException e) {
                log.error("unregisterMBean: Failure unregistering MBean "
                    + mbean, e);
            } catch (InstanceNotFoundException e) {
                log.error("unregisterMBean: Failure unregistering MBean "
                    + mbean, e);
            }
        }
        registration.clear();
    }

    private ObjectName getObjectName(final Map<String, Object> props) {
        Object oName = props.get(PROP_OBJECT_NAME);
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
