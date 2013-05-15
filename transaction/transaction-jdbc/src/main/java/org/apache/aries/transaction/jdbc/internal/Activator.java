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
package org.apache.aries.transaction.jdbc.internal;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.transaction.AriesTransactionManager;
import org.apache.xbean.blueprint.context.impl.XBeanNamespaceHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.TransactionManager;
import java.util.Hashtable;

public class Activator implements BundleActivator, ServiceTrackerCustomizer, ServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private AriesTransactionManager tm;
    private ServiceTracker t;
    private ServiceReference ref;
    private BundleContext context;
    private ServiceRegistration nshReg;

    public void start(BundleContext ctx) {
        context = ctx;

        // Expose blueprint namespace handler if xbean is present
        try {
            nshReg = JdbcNamespaceHandler.register(ctx);
        } catch (NoClassDefFoundError e) {
            LOGGER.warn("Unable to register JDBC blueprint namespace handler (xbean-blueprint not available).");
        } catch (Exception e) {
            LOGGER.error("Unable to register JDBC blueprint namespace handler", e);
        }

        t = new ServiceTracker(ctx, javax.sql.XADataSource.class.getName(), this);

        try {
            ctx.addServiceListener(this, "(objectClass=" + AriesTransactionManager.class.getName() + ")");
        } catch (InvalidSyntaxException e) {
        }
        ref = ctx.getServiceReference(TransactionManager.class.getName());
        if (ref != null) {
            tm = (AriesTransactionManager) ctx.getService(ref);
        }

        if (tm != null) {
            t.open();
        }
    }

    public void stop(BundleContext ctx) {
        // it is possible these are not cleaned by serviceChanged method when the
        // tm service is still active
        if (t != null) {
            t.close();
        }
        if (ref != null) {
            context.ungetService(ref);
        }
        if (nshReg != null) {
            nshReg.unregister();
        }
    }

    public Object addingService(ServiceReference ref) {
        try {
            LOGGER.info("Wrapping XADataSource " + ref);
            ManagedDataSourceFactory mdsf = new ManagedDataSourceFactory(ref, tm);
            return mdsf.register();
        } catch (Exception e) {
            LOGGER.warn("Error wrapping XADataSource " + ref, e);
            return null;
        }
    }

    public void modifiedService(ServiceReference ref, Object service) {
        ServiceRegistration reg = (ServiceRegistration) service;

        Hashtable<String, Object> map = new Hashtable<String, Object>();
        for (String key : ref.getPropertyKeys()) {
            map.put(key, ref.getProperty(key));
        }
        map.put("aries.xa.aware", "true");

        reg.setProperties(map);
    }

    public void removedService(ServiceReference ref, Object service) {
        safeUnregisterService((ServiceRegistration) service);
    }

    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED && tm == null) {
            ref = event.getServiceReference();
            tm = (AriesTransactionManager) context.getService(ref);

            if (tm == null) ref = null;
            else t.open();
        } else if (event.getType() == ServiceEvent.UNREGISTERING && tm != null &&
                ref.getProperty("service.id").equals(event.getServiceReference().getProperty("service.id"))) {
            t.close();
            context.ungetService(ref);
            ref = null;
            tm = null;
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

    static class JdbcNamespaceHandler {

        public static ServiceRegistration register(BundleContext context) throws Exception {
            XBeanNamespaceHandler nsh = new XBeanNamespaceHandler(
                    "http://aries.apache.org/xmlns/transaction-jdbc/2.0",
                    "org.apache.aries.transaction.jdbc.xsd",
                    context.getBundle(),
                    "META-INF/services/org/apache/xbean/spring/http/aries.apache.org/xmlns/transaction-jdbc/2.0"
            );
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("osgi.service.blueprint.namespace", "http://aries.apache.org/xmlns/transaction-jdbc/2.0");
            return context.registerService(NamespaceHandler.class.getName(), nsh, props);
        }

    }

}
