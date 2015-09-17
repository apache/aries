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
package org.apache.aries.jpa.support.osgi.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.transaction.TransactionManager;

import org.apache.aries.jpa.supplier.EmSupplier;
import org.apache.aries.jpa.support.impl.XAJpaTemplate;
import org.apache.aries.jpa.template.JpaTemplate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Is created for an EntityManagerFactory with JTA transactions and creates
 * an XaJpaTemplate for it as soon as the TransactionManager service is present.
 */
@SuppressWarnings("rawtypes")
public class TMTracker extends ServiceTracker<TransactionManager, ServiceRegistration> {
    static final String TRANSACTION_TYPE = "transaction.type";

    private final EmSupplier emSupplier;
    private final String unitName;

    private Coordinator coordinator;

    public TMTracker(BundleContext context, EmSupplier emSupplier, String unitName, Coordinator coordinator) {
        super(context, TransactionManager.class, null);
        this.emSupplier = emSupplier;
        this.unitName = unitName;
        this.coordinator = coordinator;
    }

    @Override
    public ServiceRegistration addingService(ServiceReference<TransactionManager> ref) {
        TransactionManager tm = context.getService(ref);
        XAJpaTemplate txManager = new XAJpaTemplate(emSupplier, tm, coordinator);
        return context.registerService(JpaTemplate.class, txManager, xaTxManProps(unitName));
    }

    private Dictionary<String, String> xaTxManProps(String unitName) {
        Dictionary<String, String> txmanProperties = new Hashtable<String, String>();
        txmanProperties.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, unitName);
        txmanProperties.put(TRANSACTION_TYPE, "JTA");
        return txmanProperties;
    }

    @Override
    public void removedService(ServiceReference<TransactionManager> reference, ServiceRegistration reg) {
        try {
            reg.unregister();
            context.ungetService(reference);
        } catch (Exception e) {
            // Ignore. May happen if persistence unit bundle is unloaded / updated
        }
    }

}
