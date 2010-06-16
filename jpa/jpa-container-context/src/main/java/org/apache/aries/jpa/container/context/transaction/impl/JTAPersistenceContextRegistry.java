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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TransactionRequiredException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to manage the lifecycle of JTA peristence contexts
 */
public final class JTAPersistenceContextRegistry {
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container.context");
  /** The unique key we use to find our Map */
  private static final TSRKey EMF_MAP_KEY = new TSRKey();
  
  /** 
   * A simple class to avoid key collisions in the TransactionSynchronizationRegistry. 
   * As recommended by {@link TransactionSynchronizationRegistry#putResource(Object, Object)}
   */
  private final static class TSRKey {

    @Override
    public final boolean equals(Object o) {
      return (this == o);
    }

    @Override
    public final int hashCode() {
      return 0xDEADBEEF;
    }
  }
  
  /** 
   * The transaction synchronization registry, used to determine the currently
   * active transaction, and to register for post-commit cleanup. 
   */
  private TransactionSynchronizationRegistry tranRegistry;
  
  /** 
   * A flag to indicate whether the {@link TransactionSynchronizationRegistry} is available. 
   * The initial value is false, as defined by {@link AtomicBoolean#AtomicBoolean()}.
   */
  private final AtomicBoolean registryAvailable = new AtomicBoolean();

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
  @SuppressWarnings("unchecked")
  public final EntityManager getCurrentPersistenceContext(EntityManagerFactory persistenceUnit, Map<?,?> properties) throws TransactionRequiredException
  {
    //There will only ever be one thread associated with a transaction at a given time
    //As a result, it is only the outer map that needs to be thread safe.
    
    //Throw the error on to the client
    if(!!!isTransactionActive()) {
      if(jtaIntegrationAvailable())
        throw new TransactionRequiredException("No transaction currently active");
      else {
        throw new TransactionRequiredException("No JTA transaction services implementation is currently available. As a result the" +
        		" JPA container cannot integrate with JTA transactions.");
      }
    }
    EntityManager toReturn = null;
    
    //Get hold of the Map. If there is no Map already registered then add one.
    //We don't need to worry about a race condition, as no other thread will
    //share our transaction and be able to access our Map
    Map<EntityManagerFactory, EntityManager> contextsForTransaction = (Map<EntityManagerFactory, EntityManager>) tranRegistry.getResource(EMF_MAP_KEY);
    
    //If we have a map then find an EntityManager, else create a new Map add it to the registry, and register for cleanup
    if(contextsForTransaction != null) {
      toReturn = contextsForTransaction.get(persistenceUnit);
    } else {
      contextsForTransaction = new IdentityHashMap<EntityManagerFactory, EntityManager>();
      try {
        tranRegistry.putResource(EMF_MAP_KEY, contextsForTransaction);
      } catch (IllegalStateException e) {
        _logger.warn("Unable to create a persistence context for the transaction {} because the is not active", new Object[] {tranRegistry.getTransactionKey()});
        throw new TransactionRequiredException("Unable to assiociate resources with transaction " + tranRegistry.getTransactionKey());
      }
    }
    
    //If we have no previously created EntityManager
    if(toReturn == null) {
      toReturn = (properties == null) ? persistenceUnit.createEntityManager() : persistenceUnit.createEntityManager(properties);
      if(_logger.isDebugEnabled())
        _logger.debug("Created a new persistence context {} for transaction {}.", new Object[] {toReturn, tranRegistry.getTransactionKey()});
      try {
        tranRegistry.registerInterposedSynchronization(new EntityManagerClearUp(toReturn));
      } catch (IllegalStateException e) {
        _logger.warn("No persistence context could be created as the JPA container could not register a synchronization with the transaction {}.", new Object[] {tranRegistry.getTransactionKey()});
        toReturn.close();
        throw new TransactionRequiredException("Unable to synchronize with transaction " + tranRegistry.getTransactionKey());
      }
      contextsForTransaction.put(persistenceUnit, toReturn);
    } else {
      if(_logger.isDebugEnabled())
        _logger.debug("Re-using a persistence context for transaction " + tranRegistry.getTransactionKey());
    }
    return toReturn;
  }
  
  /**
   * Determine whether there is an active transaction on the thread
   * @return
   */
  public final boolean isTransactionActive()
  {
    return registryAvailable.get() && tranRegistry.getTransactionKey() != null;
  }
  
  /**
   * Provide a {@link TransactionSynchronizationRegistry} to use
   * @param tranRegistry
   */
  public final void setTranRegistry(TransactionSynchronizationRegistry tranRegistry) {
    this.tranRegistry = tranRegistry;
  }

  /**
   * Returns true if we have access to a {@link TransactionSynchronizationRegistry} and
   * can manage persistence contexts
   * @return
   */
  public final boolean jtaIntegrationAvailable()
  {
    return registryAvailable.get();
  }
  
  /**
   * Called by the blueprint container to indicate that a new {@link TransactionSynchronizationRegistry}
   * will be used by the runtime
   * @param ref
   */
  public final void addRegistry(ServiceReference ref) {
    boolean oldValue = registryAvailable.getAndSet(true);
    if(oldValue) {
      _logger.warn("The TransactionSynchronizationRegistry used to manage persistence contexts has been replaced." +
      		" The new TransactionSynchronizationRegistry, {}, will now be used to manage persistence contexts." +
      		" Managed persistence contexts may not work correctly unless the runtime uses the new JTA Transaction services implementation" +
      		" to manage transactions.", new Object[] {ref});
    } else {
        _logger.info("A TransactionSynchronizationRegistry service is now available in the runtime. Managed persistence contexts will now" +
        		"integrate with JTA transactions using {}.", new Object[] {ref});
    }
  }
  
  public final void removeRegistry(ServiceReference ref) {
    registryAvailable.set(false);
    _logger.warn("The TransactionSynchronizationRegistry used to manage persistence contexts is no longer available." +
        " Managed persistence contexts will no longer be able to integrate with JTA transactions, and will behave as if" +
        " no there is no transaction context at all times until a new TransactionSynchronizationRegistry is available." +
        " Applications using managed persistence contexts may not work correctly until a new JTA Transaction services" +
        " implementation is available.");
  }
  
  /**
   * This class is used to close EntityManager instances once the transaction has committed,
   * and clear the persistenceContextRegistry of old persistence contexts.
   */
  private final static class EntityManagerClearUp implements Synchronization {

    private final EntityManager context;
    
    /**
     * Create a Synchronization to clear up our EntityManagers
     * @param em
     */
    public EntityManagerClearUp(EntityManager em)
    {
      context = em;
    }
    
    public final void beforeCompletion() {
      //This is a no-op;
    }

    @SuppressWarnings("unchecked")
    public final void afterCompletion(int arg0) {
      if(_logger.isDebugEnabled())
        _logger.debug("Clearing up EntityManager {} as the transaction has completed.", new Object[] {context});
      try {
        context.close();
      } catch (Exception e) {
        _logger.warn("There was an error when the container closed an EntityManager", context);
      }
    }
  }
}
