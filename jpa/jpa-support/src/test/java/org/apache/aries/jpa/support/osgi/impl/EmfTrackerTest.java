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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.util.Dictionary;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.aries.jpa.support.osgi.impl.EMFTracker;
import org.apache.aries.jpa.support.osgi.impl.EMFTracker.TrackedEmf;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public class EmfTrackerTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testLifecycle() {
        BundleContext context = mock(BundleContext.class);
        Coordinator coordinator = mock(Coordinator.class);
        EMFTracker tracker = new EMFTracker(context, coordinator);
        ServiceReference<EntityManagerFactory> ref = mock(ServiceReference.class);
        Mockito.when(ref.getProperty(EntityManagerFactoryBuilder.JPA_UNIT_NAME)).thenReturn("testunit");
        Mockito.when(ref.getProperty(PersistenceUnitTransactionType.class.getName())).thenReturn("JTA");
        
        Bundle puBundle = mock(Bundle.class);
        BundleContext puContext = mock(BundleContext.class);
        when(puBundle.getBundleContext()).thenReturn(puContext);
        when(ref.getBundle()).thenReturn(puBundle);
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        when(puContext.getService(ref)).thenReturn(emf);
        ServiceRegistration<?> emSupplierReg = mock(ServiceRegistration.class, "emSupplierReg");
        ServiceRegistration<?> emProxyReg = mock(ServiceRegistration.class, "emProxyReg");
        when(puContext.registerService(any(Class.class), any(), any(Dictionary.class)))
            .thenReturn(emSupplierReg, emProxyReg);

        EMFTracker.TrackedEmf tracked = (TrackedEmf)tracker.addingService(ref);
        Assert.assertEquals(emf, tracked.emf);
        Assert.assertEquals(emSupplierReg, tracked.emSupplierReg);
        Assert.assertEquals(emProxyReg, tracked.emProxyReg);
        Assert.assertNotNull(tracked.tmTracker);
        Assert.assertNull(tracked.rlTxManagerReg);
        
        tracker.removedService(ref, tracked);
        verify(emSupplierReg, times(1)).unregister();
        verify(emProxyReg, times(1)).unregister();
        verify(puContext, times(1)).ungetService(ref);
    }
}
