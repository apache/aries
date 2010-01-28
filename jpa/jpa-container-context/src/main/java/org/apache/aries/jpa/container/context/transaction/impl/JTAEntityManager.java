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
 * A <code>PersistenceContextType.TRANSACTION</code> {@link EntityManager} instance
 */
public class JTAEntityManager implements EntityManager {

  private final EntityManagerFactory emf;
  private final Map<String, Object> props;
  private final JTAPersistenceContextRegistry reg;
  
  public JTAEntityManager(EntityManagerFactory factory,
      Map<String, Object> properties, JTAPersistenceContextRegistry registry) {
    emf = factory;
    props = properties;
    reg = registry;
  }

  public void clear()
  {
    EntityManager em = reg.getCurrentOrNoPersistenceContext(emf, props);
    if(em != null)
      em.clear();
  }

  public void close()
  {
    //TODO add a message here
    throw new IllegalStateException();
  }

  public boolean contains(Object arg0)
  {
    EntityManager em = reg.getCurrentOrNoPersistenceContext(emf, props);
    if(em != null)
     return em.contains(arg0);
    else
      return false;
  }

  public Query createNamedQuery(String arg0)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createNamedQuery(arg0);
  }

  public Query createNativeQuery(String arg0)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createNativeQuery(arg0);
  }

  @SuppressWarnings("unchecked")
  public Query createNativeQuery(String arg0, Class arg1)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createNativeQuery(arg0, arg1);
  }

  public Query createNativeQuery(String arg0, String arg1)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createNativeQuery(arg0, arg1);
  }

  public Query createQuery(String arg0)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createQuery(arg0);
  }

  public <T> T find(Class<T> arg0, Object arg1)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).find(arg0, arg1);
  }

  public void flush()
  {
    reg.getCurrentPersistenceContext(emf, props).flush();
  }

  public Object getDelegate()
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).getDelegate();
  }

  public FlushModeType getFlushMode()
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).getFlushMode();
  }

  public <T> T getReference(Class<T> arg0, Object arg1)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).getReference(arg0, arg1);
  }

  public EntityTransaction getTransaction()
  {
    //TODO add a message here
    throw new IllegalStateException();
  }

  public boolean isOpen()
  {
    return true;
  }

  public void joinTransaction()
  {
    //This should be a no-op for a JTA entity manager
  }

  public void lock(Object arg0, LockModeType arg1)
  {
    reg.getCurrentPersistenceContext(emf, props).lock(arg0, arg1);
  }

  public <T> T merge(T arg0)
  {
    return reg.getCurrentPersistenceContext(emf, props).merge(arg0);
  }

  public void persist(Object arg0)
  {
    reg.getCurrentPersistenceContext(emf, props).persist(arg0);
  }

  public void refresh(Object arg0)
  {
    reg.getCurrentPersistenceContext(emf, props).refresh(arg0);
  }

  public void remove(Object arg0)
  {
    reg.getCurrentPersistenceContext(emf, props).remove(arg0);
  }

  public void setFlushMode(FlushModeType arg0)
  {
    reg.getCurrentOrDetachedPersistenceContext(emf, props).setFlushMode(arg0);
  }

  public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createNamedQuery(arg0, arg1);
  }

  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createQuery(arg0);
  }

  public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).createQuery(arg0, arg1);
  }

  public void detach(Object arg0)
  {
    reg.getCurrentOrDetachedPersistenceContext(emf, props).detach(arg0);
  }

  public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).find(arg0, arg1, arg2);
  }

  public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).find(arg0, arg1, arg2);
  }

  public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).find(arg0, arg1, arg2, arg3);
  }

  public CriteriaBuilder getCriteriaBuilder()
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).getCriteriaBuilder();
  }

  public EntityManagerFactory getEntityManagerFactory()
  {
    return emf;
  }

  public LockModeType getLockMode(Object arg0)
  {
    return reg.getCurrentPersistenceContext(emf, props).getLockMode(arg0);
  }

  public Metamodel getMetamodel()
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).getMetamodel();
  }

  public Map<String, Object> getProperties()
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).getProperties();
  }

  public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2)
  {
    reg.getCurrentPersistenceContext(emf, props).lock(arg0, arg1, arg2);
  }

  public void refresh(Object arg0, Map<String, Object> arg1)
  {
    reg.getCurrentPersistenceContext(emf, props).refresh(arg0, arg1);
  }

  public void refresh(Object arg0, LockModeType arg1)
  {
    reg.getCurrentPersistenceContext(emf, props).refresh(arg0, arg1);
  }

  public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2)
  {
    reg.getCurrentPersistenceContext(emf, props).refresh(arg0, arg1, arg2);
  }

  public void setProperty(String arg0, Object arg1)
  {    
    /*
     * TODO: check this
     * We don't update props because the changed property should only be visible to the 
     * EntityManager of the current transaction !?
     */
    EntityManager em = reg.getCurrentOrNoPersistenceContext(emf, props);
    if (em != null) {
      em.setProperty(arg0, arg1);
    }
  }

  public <T> T unwrap(Class<T> arg0)
  {
    return reg.getCurrentOrDetachedPersistenceContext(emf, props).unwrap(arg0);
  }
}
