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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.impl;

import java.sql.SQLException;
import java.util.Properties;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.aries.jpa.container.parser.impl.PersistenceUnit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DSFTracker extends ServiceTracker<DataSourceFactory, ManagedEMF>{
    private static final String JDBC_DRIVER = "javax.persistence.jdbc.driver";
    private static final String JDBC_URL = "javax.persistence.jdbc.url";
    private static final String JDBC_USER = "javax.persistence.jdbc.user";
    private static final String JDBC_PASSWORD = "javax.persistence.jdbc.password";

    private static final Logger LOGGER = LoggerFactory.getLogger(DSFTracker.class);


    private PersistenceProvider provider;
    private PersistenceUnit punit;

    public DSFTracker(BundleContext context, PersistenceProvider provider, PersistenceUnit punit) {
        super(context, createFilter(context, punit), null);
        this.provider = provider;
        this.punit = punit;
    }

    static Filter createFilter(BundleContext context, PersistenceUnit punit) {
        String driverName = getDriverName(punit);
        if (driverName == null) {
            throw new IllegalArgumentException("No javax.persistence.jdbc.driver supplied in persistence.xml");
        }
        String filter = String.format("(&(objectClass=%s)(%s=%s))",
                                      DataSourceFactory.class.getName(),
                                      DataSourceFactory.OSGI_JDBC_DRIVER_CLASS,
                                      driverName);
        LOGGER.info("Tracking DataSourceFactory for punit " + punit.getPersistenceUnitName() + " with filter " + filter);
        try {
            return context.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getDriverName(PersistenceUnit punit) {
        return (String)punit.getProperties().get(JDBC_DRIVER);
    }

    @Override
    public ManagedEMF addingService(ServiceReference<DataSourceFactory> reference) {
        LOGGER.info("Found DataSourceFactory for " + punit.getPersistenceUnitName() + " "
                    + getDriverName(punit));
        try {
            DataSourceFactory dsf = context.getService(reference);
            DataSource ds = createDataSource(dsf);
            if (punit.getTransactionType() == PersistenceUnitTransactionType.JTA) {
                punit.setJtaDataSource(ds);
            } else {
                punit.setNonJtaDataSource(ds);
            }
            BundleContext containerContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            return new ManagedEMF(containerContext, punit.getBundle(), provider, punit);
        } catch (Exception e) {
            LOGGER.error("Error creating DataSource for punit " + punit.getPersistenceUnitName(), e);
            return null;
        }
    }

    private DataSource createDataSource(DataSourceFactory dsf) {
        try {
            Properties props = new Properties();
            put(props, DataSourceFactory.JDBC_URL, punit, JDBC_URL);
            put(props, DataSourceFactory.JDBC_USER, punit, JDBC_USER);
            put(props, DataSourceFactory.JDBC_PASSWORD, punit, JDBC_PASSWORD);
            DataSource ds = dsf.createDataSource(props);
            return ds;
        } catch (SQLException e) {
            throw new RuntimeException("Error creating DataSource for persistence unit " + punit + "."
                                       + e.getMessage(), e);
        }
    }

    private static void put(Properties props, String destKey, PersistenceUnit punit, String sourceKey) {
        Object value = punit.getProperties().get(sourceKey);
        if (value != null) {
            props.put(destKey, value);
        }
    }

    @Override
    public void removedService(ServiceReference<DataSourceFactory> reference, ManagedEMF managedEMF) {
        LOGGER.info("Lost DataSourceFactory for " + punit.getPersistenceUnitName() + " " + getDriverName(punit));
        managedEMF.close();
        super.removedService(reference, managedEMF);
    }
}
