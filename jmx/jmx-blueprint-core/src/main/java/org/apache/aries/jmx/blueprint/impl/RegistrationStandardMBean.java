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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

public class RegistrationStandardMBean extends StandardMBean implements MBeanRegistration {
    
    // in java 6, StandardMBean implements MBeanRegistration, but it does not in java 5.

    public <T> RegistrationStandardMBean(T implementation, Class<T> mbeanInterface) 
        throws NotCompliantMBeanException {
        super(implementation, mbeanInterface);
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        //name = super.preRegister(server, name);
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            return ((MBeanRegistration) impl).preRegister(server, name);
        }
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        //super.postRegister(registrationDone);
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            ((MBeanRegistration) impl).postRegister(registrationDone);
        }
    }

    public void preDeregister() throws Exception {
        //super.preDeregister();
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            ((MBeanRegistration) impl).preDeregister();
        }
    }

    public void postDeregister() {
        //super.postDeregister();
        Object impl = getImplementation();
        if (impl instanceof MBeanRegistration) {
            ((MBeanRegistration) impl).postDeregister();
        }
    }
    
}
