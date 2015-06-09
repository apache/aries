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
package org.apache.aries.jpa.support.impl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.supplier.EmSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread safe way to use an EntityManager.
 * 
 * Before the EMF is closed the close() method has to be called to make
 * sure all EMs are closed.
 */
public class EMSupplierImpl implements EmSupplier {
    private static final long DEFAULT_SHUTDOWN_WAIT_SECS = 10;
    private static Logger LOG = LoggerFactory.getLogger(EMSupplierImpl.class);
    private EntityManagerFactory emf;
    private AtomicBoolean shutdown;
    private long shutdownWaitTime = DEFAULT_SHUTDOWN_WAIT_SECS;
    private TimeUnit shutdownWaitTimeUnit = TimeUnit.SECONDS;

    private final ThreadLocal<EntityManager> localEm;

    // Counts how deeply nested the calls on this EM are
    private final ThreadLocal<AtomicInteger> usageCount;
    private Set<EntityManager> emSet;
    private CountDownLatch emsToShutDown;
    

    public EMSupplierImpl(final EntityManagerFactory emf) {
        this.emf = emf;
        this.shutdown = new AtomicBoolean(false);
        this.localEm = new ThreadLocal<EntityManager>();
        this.emSet = Collections.newSetFromMap(new ConcurrentHashMap<EntityManager, Boolean>());
        this.usageCount = new ThreadLocal<AtomicInteger>() {
            @Override
            protected AtomicInteger initialValue() {
                return new AtomicInteger(0);
            }
        };
    }

    private EntityManager createEm(EntityManagerFactory emf) {
        LOG.debug("Creating EntityManager");
        EntityManager em = emf.createEntityManager();
        emSet.add(em);
        return em;
    }

    /**
     * Allows to retrieve one EntityManager per thread. Creates the EntityManager if none is present for the
     * thread. If the EM on the thread is closed it will be replaced by a fresh one.
     */
    @Override
    public EntityManager get() {
        EntityManager em = this.localEm.get();
        if (em == null) {
            LOG.warn("No EntityManager present on this thread. Remember to call preCall() first");
        }
        return em;
    }


    @Override
    public void preCall() {
        if (shutdown.get()) {
            throw new IllegalStateException("This EntityManagerFactory is being shut down. Can not enter a new EM enabled method");
        }
        int count = this.usageCount.get().incrementAndGet();
        if (count == 1) {
            EntityManager em = createEm(emf);
            emSet.add(em);
            localEm.set(em);
        }
    }

    @Override
    public void postCall() {
        int count = this.usageCount.get().decrementAndGet();
        if (count == 0) {
            // Outermost call finished
            closeAndRemoveLocalEm();
        } else if (count < 0) {
            throw new IllegalStateException("postCall() called without corresponding preCall()");
        }
    }

    private synchronized void closeAndRemoveLocalEm() {
        EntityManager em = localEm.get();
        em.close();
        emSet.remove(em);
        localEm.remove();
        if (shutdown.get()) {
            emsToShutDown.countDown();
        }
    }

    /**
     * Closes all EntityManagers that were opened by this Supplier.
     * It will first wait for the EMs to be closed by the running threads.
     * If this times out it will shutdown the remaining EMs itself.
     * @return true if clean close, false if timeout occured
     */
    public boolean close() {
        synchronized (this) {
            shutdown.set(true);
            emsToShutDown = new CountDownLatch(emSet.size());
        }
        try {
            emsToShutDown.await(shutdownWaitTime, shutdownWaitTimeUnit);
        } catch (InterruptedException e) {
        }
        return shutdownRemaining();
    }

    private synchronized boolean shutdownRemaining() {
        boolean clean = (emSet.size() == 0); 
        if  (!clean) {
            LOG.warn("{} EntityManagers still open after timeout. Shutting them down now", emSet.size());
        }
        for (EntityManager em : emSet) {
            closeEm(em);
        }
        emSet.clear();
        return clean;
    }

    private void closeEm(EntityManager em) {
        try {
            if (em.isOpen()) {
                em.close();
            }
        } catch (Exception e) {
            LOG.warn("Error closing EntityManager", e);
        }
    }

    public void setShutdownWait(long shutdownWaitTime, TimeUnit shutdownWaitTimeUnit) {
        this.shutdownWaitTime = shutdownWaitTime;
        this.shutdownWaitTimeUnit = shutdownWaitTimeUnit;
    }
}
