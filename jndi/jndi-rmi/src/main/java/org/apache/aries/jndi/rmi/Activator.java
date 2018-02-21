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
package org.apache.aries.jndi.rmi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class.getName());
    private ServiceRegistration reg;

    public void start(BundleContext context) {

        LOGGER.debug("Registering RMI url handler");

        try {
            Hashtable<String, Object> props = new Hashtable<>();
            props.put(JNDIConstants.JNDI_URLSCHEME, new String[]{"rmi"});
            reg = context.registerService(
                    ObjectFactory.class.getName(),
                    ClassLoader.getSystemClassLoader().loadClass("com.sun.jndi.url.rmi.rmiURLContextFactory").newInstance(),
                    props);
        } catch (Exception e) {
            LOGGER.info("A failure occurred while attempting to create the handler for the rmi JNDI URL scheme.", e);
        }
    }

    public void stop(BundleContext context) {
        safeUnregisterService(reg);
    }

    private static void safeUnregisterService(ServiceRegistration reg) {
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                //This can be safely ignored
            }
        }
    }

}
