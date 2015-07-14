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
package org.apache.aries.jpa.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.support.impl.EMSupplierImpl;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.coordinator.Coordinator;

public class EmSupplierTest {

    @Test
    public void lifeCycleTest() {
        EntityManagerFactory emf = mockEmf();
        Coordinator coordinator = new DummyCoordinator();
        EMSupplierImpl emSupplier = new EMSupplierImpl(emf, coordinator );
        assertIllegalState(emSupplier);

        emSupplier.preCall();
        EntityManager em = emSupplier.get();
        Assert.assertNotNull("EM should be present after preCall", em);
        emSupplier.preCall();
        Assert.assertSame("Same EM for inner preCall", em, emSupplier.get());
        
        emSupplier.postCall();
        Assert.assertSame("EM must still be the same after inner postCall", em, emSupplier.get());
        
        emSupplier.postCall();
        assertIllegalState(emSupplier);
        
        boolean clean = emSupplier.close();
        Assert.assertTrue("Shutdown should be clean", clean);
    }


    private void assertIllegalState(EMSupplierImpl emSupplier) {
        try {
            emSupplier.get();
            Assert.fail(IllegalStateException.class + " expected");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    
    @Test
    public void uncleanLifeCycleTest() {
        EntityManagerFactory emf = mockEmf();
        Coordinator coordinator = new DummyCoordinator();
        EMSupplierImpl emSupplier = new EMSupplierImpl(emf, coordinator);
        emSupplier.setShutdownWait(100, MILLISECONDS);
        emSupplier.preCall();
        emSupplier.get();
        boolean clean = emSupplier.close();
        Assert.assertFalse("Shutdown should be unclean", clean);
    }
    
    @Test
    public void asyncCleanLifeCycleTest() throws InterruptedException {
        EntityManagerFactory emf = mockEmf();
        Coordinator coordinator = new DummyCoordinator();
        final EMSupplierImpl emSupplier = new EMSupplierImpl(emf,coordinator);
        final Semaphore preCallSem = new Semaphore(0);
        Runnable command = new Runnable() {
            
            @Override
            public void run() {
                emSupplier.preCall();
                preCallSem.release();
                emSupplier.postCall();
            }
        };
        Executors.newSingleThreadExecutor().execute(command);
        preCallSem.acquire();
        // EMs not closed when close is called but are closed before timeout 
        boolean clean = emSupplier.close();
        Assert.assertTrue("Shutdown should be clean", clean);
    }

    private EntityManagerFactory mockEmf() {
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        EntityManager em = mock(EntityManager.class);
        when(emf.createEntityManager()).thenReturn(em);
        return emf;
    }
}
