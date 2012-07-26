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

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

/**
 * A synchronized wrapper for the EntityManager
 */
public class SynchronizedEntityManagerWrapper implements EntityManager {
    
    private final EntityManager entityManager;

    public SynchronizedEntityManagerWrapper(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public synchronized void persist(Object entity) {
        entityManager.persist(entity);
    }

    public synchronized <T> T merge(T entity) {
        return entityManager.merge(entity);
    }

    public synchronized void remove(Object entity) {
        entityManager.remove(entity);
    }

    public synchronized <T> T find(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey);
    }

    public synchronized <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        return entityManager.find(entityClass, primaryKey, properties);
    }

    public synchronized <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        return entityManager.find(entityClass, primaryKey, lockMode);
    }

    public synchronized <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        return entityManager.find(entityClass, primaryKey, lockMode, properties);
    }

    public synchronized <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return entityManager.getReference(entityClass, primaryKey);
    }

    public synchronized void flush() {
        entityManager.flush();
    }

    public synchronized void setFlushMode(FlushModeType flushMode) {
        entityManager.setFlushMode(flushMode);
    }

    public synchronized FlushModeType getFlushMode() {
        return entityManager.getFlushMode();
    }

    public synchronized void lock(Object entity, LockModeType lockMode) {
        entityManager.lock(entity, lockMode);
    }

    public synchronized void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        entityManager.lock(entity, lockMode, properties);
    }

    public synchronized void refresh(Object entity) {
        entityManager.refresh(entity);
    }

    public synchronized void refresh(Object entity, Map<String, Object> properties) {
        entityManager.refresh(entity, properties);
    }

    public synchronized void refresh(Object entity, LockModeType lockMode) {
        entityManager.refresh(entity, lockMode);
    }

    public synchronized void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        entityManager.refresh(entity, lockMode, properties);
    }

    public synchronized void clear() {
        entityManager.clear();
    }

    public synchronized void detach(Object entity) {
        entityManager.detach(entity);
    }

    public synchronized boolean contains(Object entity) {
        return entityManager.contains(entity);
    }

    public synchronized LockModeType getLockMode(Object entity) {
        return entityManager.getLockMode(entity);
    }

    public synchronized void setProperty(String propertyName, Object value) {
        entityManager.setProperty(propertyName, value);
    }

    public synchronized Map<String, Object> getProperties() {
        return entityManager.getProperties();
    }

    public synchronized Query createQuery(String qlString) {
        return entityManager.createQuery(qlString);
    }

    public synchronized <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return entityManager.createQuery(criteriaQuery);
    }

    public synchronized <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        return entityManager.createQuery(qlString, resultClass);
    }

    public synchronized Query createNamedQuery(String name) {
        return entityManager.createNamedQuery(name);
    }

    public synchronized <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        return entityManager.createNamedQuery(name, resultClass);
    }

    public synchronized Query createNativeQuery(String sqlString) {
        return entityManager.createNativeQuery(sqlString);
    }

    public synchronized Query createNativeQuery(String sqlString, Class resultClass) {
        return entityManager.createNativeQuery(sqlString, resultClass);
    }

    public synchronized Query createNativeQuery(String sqlString, String resultSetMapping) {
        return entityManager.createNativeQuery(sqlString, resultSetMapping);
    }

    public synchronized void joinTransaction() {
        entityManager.joinTransaction();
    }

    public synchronized <T> T unwrap(Class<T> cls) {
        return entityManager.unwrap(cls);
    }

    public synchronized Object getDelegate() {
        return entityManager.getDelegate();
    }

    public synchronized void close() {
        entityManager.close();
    }

    public synchronized boolean isOpen() {
        return entityManager.isOpen();
    }

    public synchronized EntityTransaction getTransaction() {
        return entityManager.getTransaction();
    }

    public synchronized EntityManagerFactory getEntityManagerFactory() {
        return entityManager.getEntityManagerFactory();
    }

    public synchronized CriteriaBuilder getCriteriaBuilder() {
        return entityManager.getCriteriaBuilder();
    }

    public synchronized Metamodel getMetamodel() {
        return entityManager.getMetamodel();
    }
}
