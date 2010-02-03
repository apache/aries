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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TransactionRequiredException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * This class is used to manage the lifecycle of JTA peristence contexts
 */
public class JTAPersistenceContextRegistry {

  /** 
   * The transaction synchronization registry, used to determine the currently
   * active transaction, and to register for post-commit cleanup. 
   */
  private TransactionSynchronizationRegistry tranRegistry;

  /**
   * The registry of active persistence contexts. The outer map must be thread safe, as
   * multiple threads can request persistence contexts. The inner Map does not need to
   * be thread safe as only one thread can be in a transaction. As a result the inner
   * Map will never be accessed concurrently.
   */
  private final ConcurrentMap<Object, Map<EntityManagerFactory, EntityManager>> persistenceContextRegistry = new ConcurrentHashMap<Object, Map<EntityManagerFactory,EntityManager>>();
  
  /**
   * Get a PersistenceContext for the current transaction. The persistence context will 
   * automatically be closed when the transaction completes.
   * 
   * @param persistenceUnit The peristence unit to create the persitence context from
   * @param properties  Any properties that should be passed on the call to {@code createEntityManager()}. 
   * The properties are NOT used for retrieving an already created persistence context.
   * 
   * @return A persistence context associated with the current transaction. Note that this will
   *         need to be wrappered to obey the JPA spec by throwing the correct exceptions
   * @throws {@link TransactionRequiredException} if there is no active transaction.
   */
  public EntityManager getCurrentPersistenceContext(EntityManagerFactory persistenceUnit, Map<?,?> properties) throws TransactionRequiredException
  {
    //There will only ever be one thread associated with a transaction at a given time
    //As a result, it is only the outer map that needs to be thread safe.
    
    Object transactionKey = tranRegistry.getTransactionKey();
    
    //TODO Globalize and log this problem
    //Throw the error on to the client
    if(transactionKey == null) {
      throw new TransactionRequiredException();
    }
    
    //Get hold of the Map. If there is no Map already registered then add one.
    //We don't need to worry about a race condition, as no other thread will
    //share our transaction
    Map<EntityManagerFactory, EntityManager> contextsForTransaction = persistenceContextRegistry.get(transactionKey);
    
    //If we need to, create a new Map add it to the registry, and register it for cleanup
    if(contextsForTransaction == null) {
      contextsForTransaction = new IdentityHashMap<EntityManagerFactory, EntityManager>();
      persistenceContextRegistry.put(transactionKey, contextsForTransaction);
      try {
        tranRegistry.registerInterposedSynchronization(new EntityManagerClearUp(transactionKey));
      } catch (IllegalStateException e) {
        persistenceContextRegistry.remove(transactionKey);
        //TODO add a message
        throw new TransactionRequiredException();
      }
    }
    
    //Still only one thread for this transaction, so don't worry about any race conditions
    EntityManager toReturn = contextsForTransaction.get(persistenceUnit);
    
    if(toReturn == null) {
      toReturn = (properties == null) ? persistenceUnit.createEntityManager() : persistenceUnit.createEntityManager(properties);
      contextsForTransaction.put(persistenceUnit, toReturn);
    } else {
      //TODO maybe add debug
    }
    
    return toReturn;
  }
  
  /**
   * Determine whether there is an active transaction on the thread
   * @return
   */
  public boolean isTransactionActive()
  {
    return tranRegistry.getTransactionKey() != null;
  }
  
  /**
   * Provide a {@link TransactionSynchronizationRegistry} to use
   * @param tranRegistry
   */
  public void setTranRegistry(TransactionSynchronizationRegistry tranRegistry) {
    this.tranRegistry = tranRegistry;
  }

  /**
   * This class is used to close EntityManager instances once the transaction has committed,
   * and clear the persistenceContextRegistry of old persistence contexts.
   */
  private class EntityManagerClearUp implements Synchronization {

    private final Object key;
    
    public EntityManagerClearUp(Object transactionKey) {
      key = transactionKey;
    }
    
    public void afterCompletion(int arg0) {
      //This is a no-op;
    }

    public void beforeCompletion() {
      Map<EntityManagerFactory, EntityManager> tidyUp = persistenceContextRegistry.remove(key);
      if(tidyUp != null) {
        for(EntityManager em : tidyUp.values()) {
          try {
            em.close();
          } catch (Exception e) {
            //TODO Log this, but continue
          }
        }
      }
    }
  }
}
