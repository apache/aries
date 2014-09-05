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
package org.apache.aries.jpa.container.context.transaction.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import org.apache.aries.jpa.container.sync.Synchronization;
import org.apache.aries.jpa.container.context.impl.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>PersistenceContextType.TRANSACTION</code> {@link EntityManager} instance
 */
public class JTAEntityManager implements EntityManager, Synchronization {

    /**
     * Logger
     */
    private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container.context");

    /**
     * The {@link EntityManagerFactory} that can create new {@link EntityManager} instances
     */
    private final EntityManagerFactory emf;
    /**
     * The map of properties to pass when creating EntityManagers
     */
    private final Map<String, Object> props;
    /**
     * A registry for creating new persistence contexts
     */
    private final JTAPersistenceContextRegistry reg;
    /**
     * The number of EntityManager instances that are open
     */
    private final AtomicLong instanceCount;
    /**
     * A callback for when we're quiescing
     */
    private final DestroyCallback callback;


    private final ThreadLocal<AtomicInteger> activeCalls = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    private final ThreadLocal<EntityManager> activeManager = new ThreadLocal<EntityManager>();

    private final ConcurrentLinkedQueue<EntityManager> pool = new ConcurrentLinkedQueue<EntityManager>();

    public JTAEntityManager(EntityManagerFactory factory,
                            Map<String, Object> properties, JTAPersistenceContextRegistry registry, AtomicLong activeCount,
                            DestroyCallback onDestroy) {
        emf = factory;
        props = properties;
        reg = registry;
        instanceCount = activeCount;
        callback = onDestroy;
    }

    public void preCall() {
        activeCalls.get().incrementAndGet();
    }

    public void postCall() {
        if (activeCalls.get().decrementAndGet() == 0) {
            EntityManager manager = activeManager.get();
            if (manager != null) {
                activeManager.set(null);
                manager.clear();
                pool.add(manager);
            }
        }
    }

    /**
     * Get the target persistence context
     *
     * @param forceTransaction Whether the returned entity manager needs to be bound to a transaction
     * @return
     * @throws TransactionRequiredException if forceTransaction is true and no transaction is available
     */
    private EntityManager getPersistenceContext(boolean forceTransaction) {
        if (forceTransaction) {
            EntityManager manager = activeManager.get();
            if (manager != null) {
                manager.clear();
            }
            return reg.getCurrentPersistenceContext(emf, props, instanceCount, callback);
        } else {
            if (reg.isTransactionActive()) {
                EntityManager manager = activeManager.get();
                if (manager != null) {
                    manager.clear();
                }
                return reg.getCurrentPersistenceContext(emf, props, instanceCount, callback);
            } else {
                if (!!!reg.jtaIntegrationAvailable() && _logger.isDebugEnabled())
                    _logger.debug("No integration with JTA transactions is available. No transaction context is active.");

                EntityManager manager = activeManager.get();
                if (manager == null) {
                    manager = pool.poll();
                    if (manager == null) {
                        manager = emf.createEntityManager(props);
                    }
                    activeManager.set(manager);
                }
                return manager;
            }
        }
    }

    /**
     * Called reflectively by blueprint
     */
    public void internalClose() {
        EntityManager temp;
        while ((temp = pool.poll()) != null) {
            temp.close();
        }
    }

    public void clear() {
        getPersistenceContext(false).clear();
    }

    public void close() {
        throw new IllegalStateException(NLS.MESSAGES.getMessage("close.called.on.container.manged.em"));
    }

    public boolean contains(Object arg0) {
        return getPersistenceContext(false).contains(arg0);
    }

    public Query createNamedQuery(String arg0) {
        return getPersistenceContext(false).createNamedQuery(arg0);
    }

    public Query createNativeQuery(String arg0) {
        return getPersistenceContext(false).createNativeQuery(arg0);
    }

    @SuppressWarnings("unchecked")
    public Query createNativeQuery(String arg0, Class arg1) {
        return getPersistenceContext(false).createNativeQuery(arg0, arg1);
    }

    public Query createNativeQuery(String arg0, String arg1) {
        return getPersistenceContext(false).createNativeQuery(arg0, arg1);
    }

    public Query createQuery(String arg0) {
        return getPersistenceContext(false).createQuery(arg0);
    }

    public <T> T find(Class<T> arg0, Object arg1) {
        return getPersistenceContext(false).find(arg0, arg1);
    }

    /**
     * @throws TransactionRequiredException
     */
    public void flush() {
        getPersistenceContext(true).flush();
    }

    public Object getDelegate() {
        return getPersistenceContext(false).getDelegate();
    }

    public FlushModeType getFlushMode() {
        return getPersistenceContext(false).getFlushMode();
    }

    public <T> T getReference(Class<T> arg0, Object arg1) {
        return getPersistenceContext(false).getReference(arg0, arg1);
    }

    public EntityTransaction getTransaction() {
        throw new IllegalStateException(NLS.MESSAGES.getMessage("getTransaction.called.on.container.managed.em"));
    }

    public boolean isOpen() {
        return true;
    }

    public void joinTransaction() {
        //This should be a no-op for a JTA entity manager
    }

    /**
     * @throws TransactionRequiredException
     */
    public void lock(Object arg0, LockModeType arg1) {
        getPersistenceContext(true).lock(arg0, arg1);
    }

    /**
     * @throws TransactionRequiredException
     */
    public <T> T merge(T arg0) {
        return getPersistenceContext(true).merge(arg0);
    }

    public boolean isJoinedToTransaction() {
        return getPersistenceContext(true).isJoinedToTransaction();
    }

    /**
     * @throws TransactionRequiredException
     */
    public void persist(Object arg0) {
        getPersistenceContext(true).persist(arg0);
    }

    /**
     * @throws TransactionRequiredException
     */
    public void refresh(Object arg0) {
        getPersistenceContext(true).refresh(arg0);
    }

    /**
     * @throws TransactionRequiredException
     */
    public void remove(Object arg0) {
        getPersistenceContext(true).remove(arg0);
    }

    public void setFlushMode(FlushModeType arg0) {
        getPersistenceContext(false).setFlushMode(arg0);
    }

    public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
        return getPersistenceContext(false).createNamedQuery(arg0, arg1);
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
        return getPersistenceContext(false).createQuery(arg0);
    }

    public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
        return getPersistenceContext(false).createQuery(arg0, arg1);
    }

    public void detach(Object arg0) {
        getPersistenceContext(false).detach(arg0);
    }

    public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
        return getPersistenceContext(false).find(arg0, arg1, arg2);
    }

    /**
     * @throws TransactionRequiredException if lock mode is not NONE
     */
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
        return getPersistenceContext(arg2 != LockModeType.NONE).find(arg0, arg1, arg2);
    }

    /**
     * @throws TransactionRequiredException if lock mode is not NONE
     */
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
        return getPersistenceContext(arg2 != LockModeType.NONE).find(arg0, arg1, arg2, arg3);
    }

    public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
        return getPersistenceContext(false).createStoredProcedureQuery(arg0);
    }

    public StoredProcedureQuery createStoredProcedureQuery(String arg0, Class ... arg1) {
        return getPersistenceContext(false).createStoredProcedureQuery(arg0, arg1);
    }

    public StoredProcedureQuery createStoredProcedureQuery(String arg0, String ... arg1) {
        return getPersistenceContext(false).createStoredProcedureQuery(arg0, arg1);
    }

    public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
        return getPersistenceContext(false).createNamedStoredProcedureQuery(arg0);
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return getPersistenceContext(false).getCriteriaBuilder();
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    /**
     * @throws TransactionRequiredException
     */
    public LockModeType getLockMode(Object arg0) {
        return getPersistenceContext(true).getLockMode(arg0);
    }

    public Metamodel getMetamodel() {
        return getPersistenceContext(false).getMetamodel();
    }

    public Map<String, Object> getProperties() {
        return getPersistenceContext(false).getProperties();
    }

    /**
     * @throws TransactionRequiredException
     */
    public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        getPersistenceContext(true).lock(arg0, arg1, arg2);
    }

    /**
     * @throws TransactionRequiredException
     */
    public void refresh(Object arg0, Map<String, Object> arg1) {
        getPersistenceContext(true).refresh(arg0, arg1);
    }

    /**
     * @throws TransactionRequiredException
     */
    public void refresh(Object arg0, LockModeType arg1) {
        getPersistenceContext(true).refresh(arg0, arg1);
    }

    /**
     * @throws TransactionRequiredException
     */
    public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        getPersistenceContext(true).refresh(arg0, arg1, arg2);
    }

    public void setProperty(String arg0, Object arg1) {
        getPersistenceContext(false).setProperty(arg0, arg1);
    }

    public <T> T unwrap(Class<T> arg0) {
        return getPersistenceContext(false).unwrap(arg0);
    }

    public Query createQuery(CriteriaUpdate arg0) {
        return getPersistenceContext(false).createQuery(arg0);
    }

    public Query createQuery(CriteriaDelete arg0) {
        return getPersistenceContext(false).createQuery(arg0);
    }

    public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
        return getPersistenceContext(false).createEntityGraph(arg0);
    }

    public EntityGraph<?> createEntityGraph(String arg0) {
        return getPersistenceContext(false).createEntityGraph(arg0);
    }

    public EntityGraph<?> getEntityGraph(String arg0) {
        return getPersistenceContext(false).getEntityGraph(arg0);
    }

    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
        return getPersistenceContext(false).getEntityGraphs(arg0);
    }

}
