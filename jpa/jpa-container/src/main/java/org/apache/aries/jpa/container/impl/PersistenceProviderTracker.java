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

import java.util.Dictionary;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.apache.aries.jpa.container.parser.impl.PersistenceUnit;
import org.apache.aries.jpa.container.weaving.impl.DummyDataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks matching persistence providers for a persistence unit.
 * If a provider is found:
 * - an EntityManagerFactoryBuilder is installed
 * - A DataSourceTracker is installed if the JtaDataSource refers to an OSGi service 
 */
public class PersistenceProviderTracker extends ServiceTracker<PersistenceProvider, StoredPerProvider> {
    private static final String JAVAX_PERSISTENCE_PROVIDER = "javax.persistence.provider";

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceProviderTracker.class);

    private PersistenceUnit punit;

    public PersistenceProviderTracker(BundleContext context, PersistenceUnit punit) {
        super(context, createFilter(context, punit), null);
        this.punit = punit;
    }

    private static Filter createFilter(BundleContext context, PersistenceUnit punit) {
        String filter;
        if (punit.getPersistenceProviderClassName() != null) {
            filter = String.format("(&(objectClass=%s)(%s=%s))",
                                   PersistenceProvider.class.getName(),
                                   JAVAX_PERSISTENCE_PROVIDER,
                                   punit.getPersistenceProviderClassName());
        } else {
            filter = String.format("(objectClass=%s)", PersistenceProvider.class.getName());
        }

        try {
            return context.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public StoredPerProvider addingService(ServiceReference<PersistenceProvider> reference) {
        String providerName = (String)reference.getProperty(JAVAX_PERSISTENCE_PROVIDER);
        // FIXME should be set when creating the EMF was successful
        if (punit.getPersistenceProviderClassName() == null) {
            punit.setProviderClassName(providerName);
        }
        StoredPerProvider stored = new StoredPerProvider();
        LOGGER.info("Found provider for " + punit.getPersistenceUnitName() + " " + punit.getPersistenceProviderClassName());
        PersistenceProvider provider = context.getService(reference);

        createAndCloseDummyEMF(provider);

        stored.dsTracker = createDataSourceTracker(provider);
        EntityManagerFactoryBuilder emfBuilder = new AriesEntityManagerFactoryBuilder(provider, punit);
        Dictionary<String, ?> props = ManagedEMF.createProperties(punit, punit.getBundle());
        stored.reg = context.registerService(EntityManagerFactoryBuilder.class, emfBuilder , props);
        return stored;
    }

    /**
     * Create and close a dummy EMF to give the PersistenceProvider a chance to call
     * punit.addTransformer(). This has to occur as early as possible as weaving needs
     * to be done before the first entity class is loaded. So we can not wait till the
     * real DataSource is found.
     */
    private void createAndCloseDummyEMF(PersistenceProvider provider) {
        DataSource dummyDataSource = new DummyDataSource();
        punit.setJtaDataSource(dummyDataSource);
        punit.setNonJtaDataSource(dummyDataSource);
        try {
            EntityManagerFactory emf = provider.createContainerEntityManagerFactory(punit, null);
            emf.close();
        } catch (Exception e) {
            LOGGER.debug("Error while creating the Dummy EntityManagerFactory to allow weaving.", e);
        }
        punit.setJtaDataSource(null);
        punit.setNonJtaDataSource(null);
    }

    private ServiceTracker<?, ?> createDataSourceTracker(PersistenceProvider provider) {
        if (usesDataSource()) {
            if (!usesDataSourceService()) {
                LOGGER.warn("Persistence unit " + punit.getPersistenceUnitName() + " refers to a non OSGi service DataSource");
                return null;
            }
            DataSourceTracker dsTracker = new DataSourceTracker(context, provider, punit);
            dsTracker.open();
            return dsTracker;
        } else if (usesDSF()) {
            DSFTracker dsfTracker = new DSFTracker(context, provider, punit);
            dsfTracker.open();
            return dsfTracker;
        } else {
            LOGGER.debug("Persistence unit " + punit.getPersistenceUnitName() + " does not refer a DataSource. "
                         +"It can only be used with EntityManagerFactoryBuilder.");
            return null;
        }
    }

    private boolean usesDataSource() {
        return punit.getJtaDataSourceName() != null || punit.getNonJtaDataSourceName() != null;
    }

    private boolean usesDSF() {
        return DSFTracker.getDriverName(punit) != null;
    }

    private boolean usesDataSourceService() {
        return punit.getJtaDataSourceName() != null && punit.getJtaDataSourceName().startsWith(DataSourceTracker.DS_PREFIX)
            || punit.getNonJtaDataSourceName() != null && punit.getNonJtaDataSourceName().startsWith(DataSourceTracker.DS_PREFIX);
    }

    @Override
    public void removedService(ServiceReference<PersistenceProvider> reference, StoredPerProvider stored) {
        LOGGER.info("Lost provider for " + punit.getPersistenceUnitName() + " " + punit.getPersistenceProviderClassName());
        if (stored.dsTracker != null) {
            stored.dsTracker.close();
        }
        stored.reg.unregister();
        super.removedService(reference, stored);
    }
}
