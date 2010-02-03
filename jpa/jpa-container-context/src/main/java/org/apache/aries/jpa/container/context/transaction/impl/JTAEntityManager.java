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
import javax.persistence.TransactionRequiredException;
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
  private EntityManager detachedManager = null;
  
  public JTAEntityManager(EntityManagerFactory factory,
      Map<String, Object> properties, JTAPersistenceContextRegistry registry) {
    emf = factory;
    props = properties;
    reg = registry;
  }

  /**
   * Get the target persistence context
   * @param forceTransaction Whether the returned entity manager needs to be bound to a transaction
   * @throws TransactionRequiredException if forceTransaction is true and no transaction is available
   * @return
   */
  private EntityManager getPersistenceContext(boolean forceTransaction) 
  {
    if (forceTransaction) {
      return reg.getCurrentPersistenceContext(emf, props);
    } else {
      if (reg.isTransactionActive()) {
        return reg.getCurrentPersistenceContext(emf, props);
      } else {
        if (detachedManager == null) {
          EntityManager temp = emf.createEntityManager(props);
          
          synchronized (this) {
            if (detachedManager == null) {
              detachedManager = temp;
              temp = null;
            }
          }
          
          if (temp != null)
            temp.close();
        }
        
        return detachedManager;
      }
    }
  }
  
  public void clear()
  {
    getPersistenceContext(false).clear();
  }

  public void close()
  {
    //TODO add a message here
    throw new IllegalStateException();
  }

  public boolean contains(Object arg0)
  {
    return getPersistenceContext(false).contains(arg0);
  }

  public Query createNamedQuery(String arg0)
  {
    return getPersistenceContext(false).createNamedQuery(arg0);
  }

  public Query createNativeQuery(String arg0)
  {
    return getPersistenceContext(false).createNativeQuery(arg0);
  }

  @SuppressWarnings("unchecked")
  public Query createNativeQuery(String arg0, Class arg1)
  {
    return getPersistenceContext(false).createNativeQuery(arg0, arg1);
  }

  public Query createNativeQuery(String arg0, String arg1)
  {
    return getPersistenceContext(false).createNativeQuery(arg0, arg1);
  }

  public Query createQuery(String arg0)
  {
    return getPersistenceContext(false).createQuery(arg0);
  }

  public <T> T find(Class<T> arg0, Object arg1)
  {
    return getPersistenceContext(false).find(arg0, arg1);
  }

  /**
   * @throws TransactionRequiredException
   */
  public void flush()
  {
    getPersistenceContext(true).flush();
  }

  public Object getDelegate()
  {
    return getPersistenceContext(false).getDelegate();
  }

  public FlushModeType getFlushMode()
  {
    return getPersistenceContext(false).getFlushMode();
  }

  public <T> T getReference(Class<T> arg0, Object arg1)
  {
    return getPersistenceContext(false).getReference(arg0, arg1);
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

  /**
   * @throws TransactionRequiredException
   */
  public void lock(Object arg0, LockModeType arg1)
  {
    getPersistenceContext(true).lock(arg0, arg1);
  }

  /**
   * @throws TransactionRequiredException
   */
  public <T> T merge(T arg0)
  {
    return getPersistenceContext(true).merge(arg0);
  }

  /**
   * @throws TransactionRequiredException
   */
  public void persist(Object arg0)
  {
    getPersistenceContext(true).persist(arg0);
  }

  /**
   * @throws TransactionRequiredException
   */
  public void refresh(Object arg0)
  {
    getPersistenceContext(true).refresh(arg0);
  }

  /**
   * @throws TransactionRequiredException
   */
  public void remove(Object arg0)
  {
    getPersistenceContext(true).remove(arg0);
  }

  public void setFlushMode(FlushModeType arg0)
  {
    getPersistenceContext(false).setFlushMode(arg0);
  }

  public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1)
  {
    return getPersistenceContext(false).createNamedQuery(arg0, arg1);
  }

  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0)
  {
    return getPersistenceContext(false).createQuery(arg0);
  }

  public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1)
  {
    return getPersistenceContext(false).createQuery(arg0, arg1);
  }

  public void detach(Object arg0)
  {
    getPersistenceContext(false).detach(arg0);
  }

  public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2)
  {
    return getPersistenceContext(false).find(arg0, arg1, arg2);
  }

  /**
   * @throws TransactionRequiredException if lock mode is not NONE
   */
  public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2)
  {
    return getPersistenceContext(arg2 != LockModeType.NONE).find(arg0, arg1, arg2);
  }

  /**
   * @throws TransactionRequiredException if lock mode is not NONE
   */
  public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3)
  {
    return getPersistenceContext(arg2 != LockModeType.NONE).find(arg0, arg1, arg2, arg3);
  }

  public CriteriaBuilder getCriteriaBuilder()
  {
    return getPersistenceContext(false).getCriteriaBuilder();
  }

  public EntityManagerFactory getEntityManagerFactory()
  {
    return emf;
  }

  /**
   * @throws TransactionRequiredException
   */
  public LockModeType getLockMode(Object arg0)
  {
    return getPersistenceContext(true).getLockMode(arg0);
  }

  public Metamodel getMetamodel()
  {
    return getPersistenceContext(false).getMetamodel();
  }

  public Map<String, Object> getProperties()
  {
    return getPersistenceContext(false).getProperties();
  }

  /**
   * @throws TransactionRequiredException
   */
  public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2)
  {
    getPersistenceContext(true).lock(arg0, arg1, arg2);
  }

  /**
   * @throws TransactionRequiredException
   */
  public void refresh(Object arg0, Map<String, Object> arg1)
  {
    getPersistenceContext(true).refresh(arg0, arg1);
  }

  /**
   * @throws TransactionRequiredException
   */
  public void refresh(Object arg0, LockModeType arg1)
  {
    getPersistenceContext(true).refresh(arg0, arg1);
  }

  /**
   * @throws TransactionRequiredException
   */
  public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2)
  {
    getPersistenceContext(true).refresh(arg0, arg1, arg2);
  }

  public void setProperty(String arg0, Object arg1)
  {
    /*
     * TODO check whether we need to change the properies as well
     */
    getPersistenceContext(false).setProperty(arg0, arg1);
  }

  public <T> T unwrap(Class<T> arg0)
  {
    return getPersistenceContext(false).unwrap(arg0);
  }
}
