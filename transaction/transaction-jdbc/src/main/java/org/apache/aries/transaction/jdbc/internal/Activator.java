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
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.xbean.blueprint.context.impl.XBeanNamespaceHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.CommonDataSource;
import java.util.Hashtable;


public class Activator implements BundleActivator,
                                  ServiceTrackerCustomizer<CommonDataSource, ManagedDataSourceFactory>,
                                  SingleServiceTracker.SingleServiceListener
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ServiceTracker<CommonDataSource, ManagedDataSourceFactory> t;
    private SingleServiceTracker<AriesTransactionManager> tm;
    private BundleContext context;
    private ServiceRegistration[] nshReg;

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

        Filter filter;
        String flt = "(&(|(objectClass=javax.sql.XADataSource)(objectClass=javax.sql.DataSource))(!(aries.managed=true)))";
        try {
            filter = context.createFilter(flt);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
        t = new ServiceTracker<CommonDataSource, ManagedDataSourceFactory>(ctx, filter, this);

        tm = new SingleServiceTracker<AriesTransactionManager>(ctx, AriesTransactionManager.class, this);
        tm.open();
    }

    public void stop(BundleContext ctx) {
        tm.close();
        t.close();
        if (nshReg != null) {
            for (ServiceRegistration reg : nshReg) {
                safeUnregisterService(reg);
            }
        }
    }

    public ManagedDataSourceFactory addingService(ServiceReference<CommonDataSource> ref) {
        try {
            LOGGER.info("Wrapping DataSource " + ref);
            ManagedDataSourceFactory mdsf = new ManagedDataSourceFactory(ref, tm.getService());
            mdsf.register();
            return mdsf;
        } catch (Exception e) {
            LOGGER.warn("Error wrapping DataSource " + ref, e);
            return null;
        }
    }

    public void modifiedService(ServiceReference<CommonDataSource> ref, ManagedDataSourceFactory service) {
        try {
            service.unregister();
        } catch (Exception e) {
            LOGGER.warn("Error closing DataSource " + ref, e);
        }
        try {
            service.register();
        } catch (Exception e) {
            LOGGER.warn("Error wrapping DataSource " + ref, e);
        }
    }

    public void removedService(ServiceReference<CommonDataSource> ref, ManagedDataSourceFactory service) {
        try {
            service.unregister();
        } catch (Exception e) {
            LOGGER.warn("Error closing DataSource " + ref, e);
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

    @Override
    public void serviceFound()
    {
        t.open();
    }

    @Override
    public void serviceLost()
    {
        t.close();
    }

    @Override
    public void serviceReplaced()
    {
        t.close();
        t.open();
    }

    static class JdbcNamespaceHandler {

        public static ServiceRegistration[] register(BundleContext context) throws Exception {
            XBeanNamespaceHandler nsh20 = new XBeanNamespaceHandler(
                    "http://aries.apache.org/xmlns/transaction-jdbc/2.0",
                    "org.apache.aries.transaction.jdbc-2.0.xsd",
                    context.getBundle(),
                    "META-INF/services/org/apache/xbean/spring/http/aries.apache.org/xmlns/transaction-jdbc/2.0"
            );
            Hashtable<String, Object> props20 = new Hashtable<String, Object>();
            props20.put("osgi.service.blueprint.namespace", "http://aries.apache.org/xmlns/transaction-jdbc/2.0");
            ServiceRegistration reg20 = context.registerService(NamespaceHandler.class.getName(), nsh20, props20);

            XBeanNamespaceHandler nsh21 = new XBeanNamespaceHandler(
                    "http://aries.apache.org/xmlns/transaction-jdbc/2.1",
                    "org.apache.aries.transaction.jdbc.xsd",
                    context.getBundle(),
                    "META-INF/services/org/apache/xbean/spring/http/aries.apache.org/xmlns/transaction-jdbc/2.1"
            );
            Hashtable<String, Object> props21 = new Hashtable<String, Object>();
            props21.put("osgi.service.blueprint.namespace", "http://aries.apache.org/xmlns/transaction-jdbc/2.1");
            ServiceRegistration reg21 = context.registerService(NamespaceHandler.class.getName(), nsh21, props21);

            return new ServiceRegistration[] { reg20, reg21 };
        }

    }

}
