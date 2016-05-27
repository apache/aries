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
package org.apache.aries.tx.control.jpa.common.impl;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

public abstract class EntityManagerWrapper implements EntityManager {

	public void persist(Object entity) {
		getRealEntityManager().persist(entity);
	}

	public <T> T merge(T entity) {
		return getRealEntityManager().merge(entity);
	}

	public void remove(Object entity) {
		getRealEntityManager().remove(entity);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return getRealEntityManager().find(entityClass, primaryKey);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return getRealEntityManager().find(entityClass, primaryKey, properties);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return getRealEntityManager().find(entityClass, primaryKey, lockMode);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return getRealEntityManager().find(entityClass, primaryKey, lockMode, properties);
	}

	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		return getRealEntityManager().getReference(entityClass, primaryKey);
	}

	public void flush() {
		getRealEntityManager().flush();
	}

	public void setFlushMode(FlushModeType flushMode) {
		getRealEntityManager().setFlushMode(flushMode);
	}

	public FlushModeType getFlushMode() {
		return getRealEntityManager().getFlushMode();
	}

	public void lock(Object entity, LockModeType lockMode) {
		getRealEntityManager().lock(entity, lockMode);
	}

	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		getRealEntityManager().lock(entity, lockMode, properties);
	}

	public void refresh(Object entity) {
		getRealEntityManager().refresh(entity);
	}

	public void refresh(Object entity, Map<String, Object> properties) {
		getRealEntityManager().refresh(entity, properties);
	}

	public void refresh(Object entity, LockModeType lockMode) {
		getRealEntityManager().refresh(entity, lockMode);
	}

	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		getRealEntityManager().refresh(entity, lockMode, properties);
	}

	public void clear() {
		getRealEntityManager().clear();
	}

	public void detach(Object entity) {
		getRealEntityManager().detach(entity);
	}

	public boolean contains(Object entity) {
		return getRealEntityManager().contains(entity);
	}

	public LockModeType getLockMode(Object entity) {
		return getRealEntityManager().getLockMode(entity);
	}

	public void setProperty(String propertyName, Object value) {
		getRealEntityManager().setProperty(propertyName, value);
	}

	public Map<String, Object> getProperties() {
		return getRealEntityManager().getProperties();
	}

	public Query createQuery(String qlString) {
		return getRealEntityManager().createQuery(qlString);
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return getRealEntityManager().createQuery(criteriaQuery);
	}

	public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery) {
		return getRealEntityManager().createQuery(updateQuery);
	}

	public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery) {
		return getRealEntityManager().createQuery(deleteQuery);
	}

	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		return getRealEntityManager().createQuery(qlString, resultClass);
	}

	public Query createNamedQuery(String name) {
		return getRealEntityManager().createNamedQuery(name);
	}

	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		return getRealEntityManager().createNamedQuery(name, resultClass);
	}

	public Query createNativeQuery(String sqlString) {
		return getRealEntityManager().createNativeQuery(sqlString);
	}

	public Query createNativeQuery(String sqlString, @SuppressWarnings("rawtypes") Class resultClass) {
		return getRealEntityManager().createNativeQuery(sqlString, resultClass);
	}

	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		return getRealEntityManager().createNativeQuery(sqlString, resultSetMapping);
	}

	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		return getRealEntityManager().createNamedStoredProcedureQuery(name);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		return getRealEntityManager().createStoredProcedureQuery(procedureName);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, @SuppressWarnings("rawtypes") Class... resultClasses) {
		return getRealEntityManager().createStoredProcedureQuery(procedureName, resultClasses);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return getRealEntityManager().createStoredProcedureQuery(procedureName, resultSetMappings);
	}

	public void joinTransaction() {
		getRealEntityManager().joinTransaction();
	}

	public boolean isJoinedToTransaction() {
		return getRealEntityManager().isJoinedToTransaction();
	}

	public <T> T unwrap(Class<T> cls) {
		return getRealEntityManager().unwrap(cls);
	}

	public Object getDelegate() {
		return getRealEntityManager().getDelegate();
	}

	public void close() {
		getRealEntityManager().close();
	}

	public boolean isOpen() {
		return getRealEntityManager().isOpen();
	}

	public EntityTransaction getTransaction() {
		return getRealEntityManager().getTransaction();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return getRealEntityManager().getEntityManagerFactory();
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return getRealEntityManager().getCriteriaBuilder();
	}

	public Metamodel getMetamodel() {
		return getRealEntityManager().getMetamodel();
	}

	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		return getRealEntityManager().createEntityGraph(rootType);
	}

	public EntityGraph<?> createEntityGraph(String graphName) {
		return getRealEntityManager().createEntityGraph(graphName);
	}

	public EntityGraph<?> getEntityGraph(String graphName) {
		return getRealEntityManager().getEntityGraph(graphName);
	}

	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return getRealEntityManager().getEntityGraphs(entityClass);
	}

	protected abstract EntityManager getRealEntityManager();

}
