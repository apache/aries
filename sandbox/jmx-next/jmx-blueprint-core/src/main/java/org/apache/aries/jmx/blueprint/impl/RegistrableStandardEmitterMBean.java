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
package org.apache.aries.jmx.blueprint.impl;

import javax.management.*;

/**
 * The <code>StandardMBean</code> does not appear to delegate correctly to the underlying MBean implementation. Due to
 * issues surrounding the <code>MBeanRegistration</code> callback methods and <code>NotificationEmmitter</code> methods,
 * this subclass was introduced to force the delegation
 * 
 * @version $Rev$ $Date$
 */
public class RegistrableStandardEmitterMBean extends StandardMBean implements MBeanRegistration, NotificationEmitter {

    public <T> RegistrableStandardEmitterMBean(T impl, Class<T> intf) throws NotCompliantMBeanException {
        super(impl, intf);
    }

    /**
     * @see javax.management.StandardMBean#getMBeanInfo()
     */
    public MBeanInfo getMBeanInfo() {
        MBeanInfo mbeanInfo = super.getMBeanInfo();
        if (mbeanInfo != null) {
            MBeanNotificationInfo[] notificationInfo;
            Object impl = getImplementation();
            if (impl instanceof NotificationEmitter) {
                notificationInfo = ((NotificationEmitter) (impl)).getNotificationInfo();
            } else {
                notificationInfo = new MBeanNotificationInfo[0];
            }
            mbeanInfo = new MBeanInfo(mbeanInfo.getClassName(), mbeanInfo.getDescription(), mbeanInfo.getAttributes(),
                    mbeanInfo.getConstructors(), mbeanInfo.getOperations(), notificationInfo);
        }
        return mbeanInfo;
    }

    /**
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            ((MBeanRegistration) impl).postDeregister();
        }
    }

    /**
     * @see javax.management.MBeanRegistration#postRegister(Boolean)
     */
    public void postRegister(Boolean registrationDone) {
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            ((MBeanRegistration) impl).postRegister(registrationDone);
        }
    }

    /**
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception {
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            ((MBeanRegistration) impl).preDeregister();
        }
    }

    /**
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        ObjectName result = name;
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            result = ((MBeanRegistration) impl).preRegister(server, name);
        }
        return result;
    }

    /**
     * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, Object)
     */
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        Object impl = getImplementation();
        if (impl instanceof NotificationEmitter) {
            ((NotificationEmitter) (impl)).removeNotificationListener(listener, filter, handback);
        }
    }

    /**
     * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, Object)
     */
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
        Object impl = getImplementation();
        if (impl instanceof NotificationEmitter) {
            ((NotificationEmitter) (impl)).addNotificationListener(listener, filter, handback);
        }
    }

    /**
     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        MBeanNotificationInfo[] result;
        Object impl = getImplementation();
        if (impl instanceof NotificationEmitter) {
            result = ((NotificationEmitter) (impl)).getNotificationInfo();
        } else {
            result = new MBeanNotificationInfo[0];
        }
        return result;
    }

    /**
     * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
     */
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        Object impl = getImplementation();
        if (impl instanceof NotificationEmitter) {
            ((NotificationEmitter) (impl)).removeNotificationListener(listener);
        }
    }

}
