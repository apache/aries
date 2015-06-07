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

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.aries.jpa.supplier.EmSupplier;
import org.apache.aries.jpa.support.impl.EMSupplierImpl;
import org.apache.aries.jpa.support.impl.ResourceLocalJpaTemplate;
import org.apache.aries.jpa.template.JpaTemplate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks EntityManagerFactory services and publishes a Supplier<EntityManager> for each.
 * IF the persistence unit uses JTA a TMTracker is created. If it uses RESOURCE_LOCAL as 
 * ResourceLocalJpaTemplate is created.
 */
@SuppressWarnings("rawtypes")
public class EMFTracker extends ServiceTracker {

    @SuppressWarnings("unchecked")
    public EMFTracker(BundleContext context) {
        super(context, EntityManagerFactory.class, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object addingService(ServiceReference reference) {
        String unitName = (String)reference.getProperty(JPA_UNIT_NAME);
        if (unitName == null) {
            return null;
        }
        BundleContext bContext = reference.getBundle().getBundleContext();
        TrackedEmf tracked = new TrackedEmf();
        tracked.emf = (EntityManagerFactory)bContext.getService(reference);
        tracked.emSupplier = new EMSupplierImpl(tracked.emf);
        tracked.emSupplierReg = bContext.registerService(EmSupplier.class, tracked.emSupplier,
                                                         getEmSupplierProps(unitName));

        if (getTransactionType(tracked.emf) == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
            JpaTemplate txManager = new ResourceLocalJpaTemplate(tracked.emSupplier);
            tracked.rlTxManagerReg = bContext.registerService(JpaTemplate.class, txManager,
                                                          rlTxManProps(unitName));
        } else {
            tracked.tmTracker = new TMTracker(bContext, tracked.emSupplier, unitName);
            tracked.tmTracker.open();
        }
        return tracked;
    }

    /**
     * 
     * @param emf
     * @return
     */
    private PersistenceUnitTransactionType getTransactionType(EntityManagerFactory emf) {
        PersistenceUnitTransactionType transactionType = (PersistenceUnitTransactionType) emf.getProperties()
        		.get(PersistenceUnitTransactionType.class.getName());
        if(transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
        	return PersistenceUnitTransactionType.RESOURCE_LOCAL;
        } else {
        	return PersistenceUnitTransactionType.JTA;
        }
    }

    private Dictionary<String, String> getEmSupplierProps(String unitName) {
        Dictionary<String, String> props = new Hashtable<>();
        props.put(JPA_UNIT_NAME, unitName);
        return props;
    }

    private Dictionary<String, String> rlTxManProps(String unitName) {
        Dictionary<String, String> props = new Hashtable<>();
        props.put(JPA_UNIT_NAME, unitName);
        props.put(TMTracker.TRANSACTION_TYPE, "RESOURCE_LOCAL");
        return props;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removedService(ServiceReference reference, Object trackedO) {
        TrackedEmf tracked = (TrackedEmf)trackedO;
        if (tracked.tmTracker != null) {
            tracked.tmTracker.close();
        }
        if (tracked.rlTxManagerReg != null) {
            tracked.rlTxManagerReg.unregister();
        }
        tracked.emSupplier.close();
        tracked.emSupplierReg.unregister();
        super.removedService(reference, tracked.emf);
    }

    static class TrackedEmf {
        ServiceRegistration emSupplierReg;
        EMSupplierImpl emSupplier;
        ServiceRegistration rlTxManagerReg;
        EntityManagerFactory emf;
        TMTracker tmTracker;
    }
}
