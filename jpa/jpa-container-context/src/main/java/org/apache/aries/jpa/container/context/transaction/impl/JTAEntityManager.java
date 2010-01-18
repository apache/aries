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
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
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
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em != null)
     return em.contains(arg0);
    else
      return false;
  }

  public Query createNamedQuery(String arg0)
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.createNamedQuery(arg0);
  }

  public Query createNativeQuery(String arg0)
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.createNativeQuery(arg0);
  }

  @SuppressWarnings("unchecked")
  public Query createNativeQuery(String arg0, Class arg1)
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.createNativeQuery(arg0, arg1);
  }

  public Query createNativeQuery(String arg0, String arg1)
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.createNativeQuery(arg0, arg1);
  }

  public Query createQuery(String arg0)
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.createQuery(arg0);
  }

  public <T> T find(Class<T> arg0, Object arg1)
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.find(arg0, arg1);
  }

  public void flush()
  {
    reg.getCurrentPersistenceContext(emf, props).flush();
  }

  public Object getDelegate()
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.getDelegate();
  }

  public FlushModeType getFlushMode()
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.getFlushMode();
  }

  public <T> T getReference(Class<T> arg0, Object arg1)
  {
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    return em.getReference(arg0, arg1);
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
    EntityManager em = reg.getCurrentPersistenceContext(emf, props);
    if(em == null)
      em = emf.createEntityManager(props);
    
    em.setFlushMode(arg0);
  }

  public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public void detach(Object arg0)
  {
    // TODO Auto-generated method stub
    
  }

  public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public CriteriaBuilder getCriteriaBuilder()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public EntityManagerFactory getEntityManagerFactory()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public LockModeType getLockMode(Object arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public Metamodel getMetamodel()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public Map<String, Object> getProperties()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2)
  {
    // TODO Auto-generated method stub
    
  }

  public void refresh(Object arg0, Map<String, Object> arg1)
  {
    // TODO Auto-generated method stub
    
  }

  public void refresh(Object arg0, LockModeType arg1)
  {
    // TODO Auto-generated method stub
    
  }

  public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2)
  {
    // TODO Auto-generated method stub
    
  }

  public void setProperty(String arg0, Object arg1)
  {
    // TODO Auto-generated method stub
    
  }

  public <T> T unwrap(Class<T> arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
