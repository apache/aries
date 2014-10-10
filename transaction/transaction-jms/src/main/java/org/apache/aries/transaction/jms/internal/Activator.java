/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.transaction.jms.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.xbean.blueprint.context.impl.XBeanNamespaceHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ServiceRegistration nshReg;

    @Override
    public void start(BundleContext context) throws Exception {
        // Expose blueprint namespace handler if xbean is present
        try {
            nshReg = JmsNamespaceHandler.register(context);
        } catch (NoClassDefFoundError e) {
            LOGGER.warn("Unable to register JMS blueprint namespace handler (xbean-blueprint not available).");
        } catch (Exception e) {
            LOGGER.error("Unable to register JMS blueprint namespace handler", e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (nshReg != null) {
            safeUnregisterService(nshReg);
        }
    }

    static void safeUnregisterService(ServiceRegistration reg) {
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                //This can be safely ignored
            }
        }
    }

    static class JmsNamespaceHandler {

        public static ServiceRegistration register(BundleContext context) throws Exception {
            XBeanNamespaceHandler nsh = new XBeanNamespaceHandler(
                    "http://aries.apache.org/xmlns/transaction-jms/2.0",
                    "org.apache.aries.transaction.jms.xsd",
                    context.getBundle(),
                    "META-INF/services/org/apache/xbean/spring/http/aries.apache.org/xmlns/transaction-jms/2.0"
            );
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("osgi.service.blueprint.namespace", "http://aries.apache.org/xmlns/transaction-jms/2.0");
            return context.registerService(NamespaceHandler.class.getName(), nsh, props);
        }
    }
}
