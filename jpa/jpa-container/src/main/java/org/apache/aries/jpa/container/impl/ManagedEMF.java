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

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;
import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER;
import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_VERSION;

import java.io.Closeable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an EntityManagerFactory(EMF) for a persistence unit and publishes it as a service.
 * Custom properties can be configured by supplying a config admin configuriation named like
 * the JPA_CONFIGURATION_PREFIX.<persistence unit name>.
 */
public class ManagedEMF implements Closeable, ManagedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedEMF.class);
    private static final String JPA_CONFIGURATION_PREFIX = "org.apache.aries.jpa.";

    private EntityManagerFactory emf;
    private ServiceRegistration<EntityManagerFactory> reg;
    private PersistenceProvider provider;
    private PersistenceUnitInfo persistenceUnit;
    private Bundle bundle;
    private ServiceRegistration<?> configReg;

    private boolean closed;

    public ManagedEMF(BundleContext containerContext, Bundle bundle, PersistenceProvider provider, PersistenceUnitInfo persistenceUnit) {
        this.provider = provider;
        this.persistenceUnit = persistenceUnit;
        this.bundle = bundle;
        registerManagedService(containerContext, persistenceUnit);
        closed = false;
    }

    private void registerManagedService(BundleContext containerContext, PersistenceUnitInfo persistenceUnit) {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>(); // NOSONAR
        configuration.put(Constants.SERVICE_PID,
                          JPA_CONFIGURATION_PREFIX + persistenceUnit.getPersistenceUnitName());
        configReg = containerContext.registerService(ManagedService.class.getName(), this, configuration);
    }

    public void closeEMF() {
        if (reg != null) {
            try {
                reg.unregister();
            } catch (Exception e) {
                LOGGER.debug("Exception on unregister", e);
            }
        }
        if (emf != null && emf.isOpen()) {
            try {
                emf.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing EntityManagerFactory for " + persistenceUnit.getPersistenceUnitName(), e);
            }
        }
        reg = null;
        emf = null;
    }
    
    @Override
    public void close() {
        closed = true;
        closeEMF();
        if (configReg != null) {
            configReg.unregister();
        }
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (closed) {
            return;
        }
        if (emf != null) {
            closeEMF();
        }
        if (bundle.getState() == Bundle.UNINSTALLED || bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.STOPPING) {
            // Not sure why but during the TCK tests updated sometimes was called
            // for uninstalled bundles
            return;
        }
        Map<String, Object> overrides = (properties != null) ? asMap(properties) : null;
        LOGGER.info("Registering EntityManagerFactory for persistence unit " + persistenceUnit.getPersistenceUnitName());
        if (LOGGER.isDebugEnabled()) {
           LOGGER.debug("Using properties override " + overrides); 
        }
        createAndPublishEMF(overrides);
    }

    private void createAndPublishEMF(Map<String, Object> overrides) {
        emf = provider.createContainerEntityManagerFactory(persistenceUnit, overrides);
        Dictionary<String, String> props = createProperties(persistenceUnit, bundle);
        BundleContext uctx = bundle.getBundleContext();
        reg = uctx.registerService(EntityManagerFactory.class, emf, props);
    }

    public static Dictionary<String, String> createProperties(PersistenceUnitInfo persistenceUnit, Bundle puBundle) {
        Dictionary<String, String> props = new Hashtable<String, String>(); // NOSONAR
        props.put(JPA_UNIT_NAME, persistenceUnit.getPersistenceUnitName());
        if (persistenceUnit.getPersistenceProviderClassName() != null) {
            props.put(JPA_UNIT_PROVIDER, persistenceUnit.getPersistenceProviderClassName());
        }
        props.put(JPA_UNIT_VERSION, puBundle.getVersion().toString());
        return props;
    }

    private Map<String, Object> asMap(Dictionary<String, ?> dict) {
        Map<String, Object> map = new HashMap<String, Object>(); // NOSONAR
        map.put(PersistenceUnitTransactionType.class.getName(), persistenceUnit.getTransactionType());
        for (Enumeration<String> e = dict.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            map.put(key, dict.get(key));
        }
        return map;
    }

}
