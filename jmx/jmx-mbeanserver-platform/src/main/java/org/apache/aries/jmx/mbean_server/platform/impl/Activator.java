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
package org.apache.aries.jmx.mbean_server.platform.impl;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private final Object registrationLock = new Object();
    private ServiceRegistration<MBeanServer> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        Hashtable<String, Object> mbeanProps = new Hashtable<String, Object>();
        ObjectName beanName = ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");
        AttributeList attrs = platformMBeanServer.getAttributes(beanName, new String[] {
            "MBeanServerId",
            "SpecificationName",
            "SpecificationVersion",
            "SpecificationVendor",
            "ImplementationName",
            "ImplementationVersion",
            "ImplementationVendor"
        });
        for (Object object : attrs) {
            Attribute attr = (Attribute) object;
            if (attr.getValue() != null) {
                mbeanProps.put(attr.getName(), attr.getValue().toString());
            }
        }
        synchronized (registrationLock) {
            registration = context.registerService(MBeanServer.class, platformMBeanServer, mbeanProps);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        synchronized (registrationLock) {
            if (registration != null) {
                registration.unregister();
                registration = null;
            }
        }
    }
}
