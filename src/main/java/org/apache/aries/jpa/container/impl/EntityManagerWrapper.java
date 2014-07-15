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

import java.util.List;
import java.util.Map;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

/**
 * Wrapper an EntityManager so that we know when it has been closed
 */
public class EntityManagerWrapper implements EntityManager {

    private final EntityManager delegate;
    /**
     * Call this when the EntityManager is closed
     */
    private final DestroyCallback callback;

    public EntityManagerWrapper(EntityManager em, DestroyCallback callback) {
        delegate = em;
        this.callback = callback;
    }

    public void clear() {
        delegate.clear();
    }

    public void close() {
        delegate.close();
        //This will only ever be called once, the second time there
        //will be an IllegalStateException from the line above
        callback.callback();
    }

    public boolean contains(Object arg0) {
        return delegate.contains(arg0);
    }

    public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
        return delegate.createNamedQuery(arg0, arg1);
    }

    public Query createNamedQuery(String arg0) {
        return delegate.createNamedQuery(arg0);
    }

    public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
        return delegate.createNamedStoredProcedureQuery(arg0);
    }

    public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
        return delegate.createStoredProcedureQuery(arg0);
    }

    public StoredProcedureQuery createStoredProcedureQuery(String arg0, Class ... arg1) {
        return delegate.createStoredProcedureQuery(arg0, arg1);
    }

    public StoredProcedureQuery createStoredProcedureQuery(String arg0, String ... arg1) {
        return delegate.createStoredProcedureQuery(arg0, arg1);
    }

    public Query createNativeQuery(String arg0, Class arg1) {
        return delegate.createNativeQuery(arg0, arg1);
    }

    public Query createNativeQuery(String arg0, String arg1) {
        return delegate.createNativeQuery(arg0, arg1);
    }

    public Query createNativeQuery(String arg0) {
        return delegate.createNativeQuery(arg0);
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
        return delegate.createQuery(arg0);
    }

    public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
        return delegate.createQuery(arg0, arg1);
    }

    public Query createQuery(String arg0) {
        return delegate.createQuery(arg0);
    }

    public Query createQuery(CriteriaUpdate arg0) {
        return delegate.createQuery(arg0);
    }

    public Query createQuery(CriteriaDelete arg0) {
        return delegate.createQuery(arg0);
    }

    public void detach(Object arg0) {
        delegate.detach(arg0);
    }

    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2,
                      Map<String, Object> arg3) {
        return delegate.find(arg0, arg1, arg2, arg3);
    }

    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
        return delegate.find(arg0, arg1, arg2);
    }

    public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
        return delegate.find(arg0, arg1, arg2);
    }

    public <T> T find(Class<T> arg0, Object arg1) {
        return delegate.find(arg0, arg1);
    }

    public void flush() {
        delegate.flush();
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return delegate.getCriteriaBuilder();
    }

    public Object getDelegate() {
        return delegate.getDelegate();
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return delegate.getEntityManagerFactory();
    }

    public FlushModeType getFlushMode() {
        return delegate.getFlushMode();
    }

    public LockModeType getLockMode(Object arg0) {
        return delegate.getLockMode(arg0);
    }

    public Metamodel getMetamodel() {
        return delegate.getMetamodel();
    }

    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    public <T> T getReference(Class<T> arg0, Object arg1) {
        return delegate.getReference(arg0, arg1);
    }

    public EntityTransaction getTransaction() {
        return delegate.getTransaction();
    }

    public boolean isOpen() {
        return delegate.isOpen();
    }

    public void joinTransaction() {
        delegate.joinTransaction();
    }

    public boolean isJoinedToTransaction() {
        return delegate.isJoinedToTransaction();
    }

    public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        delegate.lock(arg0, arg1, arg2);
    }

    public void lock(Object arg0, LockModeType arg1) {
        delegate.lock(arg0, arg1);
    }

    public <T> T merge(T arg0) {
        return delegate.merge(arg0);
    }

    public void persist(Object arg0) {
        delegate.persist(arg0);
    }

    public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        delegate.refresh(arg0, arg1, arg2);
    }

    public void refresh(Object arg0, LockModeType arg1) {
        delegate.refresh(arg0, arg1);
    }

    public void refresh(Object arg0, Map<String, Object> arg1) {
        delegate.refresh(arg0, arg1);
    }

    public void refresh(Object arg0) {
        delegate.refresh(arg0);
    }

    public void remove(Object arg0) {
        delegate.remove(arg0);
    }

    public void setFlushMode(FlushModeType arg0) {
        delegate.setFlushMode(arg0);
    }

    public void setProperty(String arg0, Object arg1) {
        delegate.setProperty(arg0, arg1);
    }

    public <T> T unwrap(Class<T> arg0) {
        return delegate.unwrap(arg0);
    }

    public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
        return delegate.createEntityGraph(arg0);
    }

    public EntityGraph<?> createEntityGraph(String arg0) {
        return delegate.createEntityGraph(arg0);
    }

    public EntityGraph<?> getEntityGraph(String arg0) {
        return delegate.getEntityGraph(arg0);
    }

    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
        return delegate.getEntityGraphs(arg0);
    }

}
