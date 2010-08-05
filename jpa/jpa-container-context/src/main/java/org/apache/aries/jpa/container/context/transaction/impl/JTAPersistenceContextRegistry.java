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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TransactionRequiredException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to manage the lifecycle of JTA peristence contexts
 */
public final class JTAPersistenceContextRegistry extends ServiceTracker {
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
  private final AtomicReference<TransactionSynchronizationRegistry> tranRegistry = new AtomicReference<TransactionSynchronizationRegistry>();
  
  /** The reference for our TSR service */
  private AtomicReference<ServiceReference> tranRegistryRef = new AtomicReference<ServiceReference>();

  public JTAPersistenceContextRegistry(BundleContext context) {
    super(context, TransactionSynchronizationRegistry.class.getName(), null);
    open();
  }

  /**
   * Get a PersistenceContext for the current transaction. The persistence context will 
   * automatically be closed when the transaction completes.
   * 
   * @param persistenceUnit The peristence unit to create the persitence context from
   * @param properties  Any properties that should be passed on the call to {@code createEntityManager()}. 
   * The properties are NOT used for retrieving an already created persistence context.
   * @param activeCount The AtomicLong for counting instances
   * @param cbk A callback called when the instance is destroyed
   * 
   * @return A persistence context associated with the current transaction. Note that this will
   *         need to be wrappered to obey the JPA spec by throwing the correct exceptions
   * @throws {@link TransactionRequiredException} if there is no active transaction.
   */
  @SuppressWarnings("unchecked")
  public final EntityManager getCurrentPersistenceContext(
      EntityManagerFactory persistenceUnit, Map<?,?> properties, AtomicLong activeCount,
      DestroyCallback cbk) throws TransactionRequiredException
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
    TransactionSynchronizationRegistry tsr = tranRegistry.get();
    //Get hold of the Map. If there is no Map already registered then add one.
    //We don't need to worry about a race condition, as no other thread will
    //share our transaction and be able to access our Map
    Map<EntityManagerFactory, EntityManager> contextsForTransaction = (Map<EntityManagerFactory, EntityManager>) tsr.getResource(EMF_MAP_KEY);
    
    //If we have a map then find an EntityManager, else create a new Map add it to the registry, and register for cleanup
    if(contextsForTransaction != null) {
      toReturn = contextsForTransaction.get(persistenceUnit);
    } else {
      contextsForTransaction = new IdentityHashMap<EntityManagerFactory, EntityManager>();
      try {
        tsr.putResource(EMF_MAP_KEY, contextsForTransaction);
      } catch (IllegalStateException e) {
        _logger.warn("Unable to create a persistence context for the transaction {} because the is not active", new Object[] {tsr.getTransactionKey()});
        throw new TransactionRequiredException("Unable to assiociate resources with transaction " + tsr.getTransactionKey());
      }
    }
    
    //If we have no previously created EntityManager
    if(toReturn == null) {
      toReturn = (properties == null) ? persistenceUnit.createEntityManager() : persistenceUnit.createEntityManager(properties);
      if(_logger.isDebugEnabled())
        _logger.debug("Created a new persistence context {} for transaction {}.", new Object[] {toReturn, tsr.getTransactionKey()});
      try {
        tsr.registerInterposedSynchronization(new EntityManagerClearUp(toReturn, activeCount, cbk));
      } catch (IllegalStateException e) {
        _logger.warn("No persistence context could be created as the JPA container could not register a synchronization with the transaction {}.", new Object[] {tsr.getTransactionKey()});
        toReturn.close();
        throw new TransactionRequiredException("Unable to synchronize with transaction " + tsr.getTransactionKey());
      }
      contextsForTransaction.put(persistenceUnit, toReturn);
      activeCount.incrementAndGet();
    } else {
      if(_logger.isDebugEnabled())
        _logger.debug("Re-using a persistence context for transaction " + tsr.getTransactionKey());
    }
    return toReturn;
  }
  
  /**
   * Determine whether there is an active transaction on the thread
   * @return
   */
  public final boolean isTransactionActive()
  {
    TransactionSynchronizationRegistry tsr = tranRegistry.get();
    return tsr != null && tsr.getTransactionKey() != null;
  }

  /**
   * Returns true if we have access to a {@link TransactionSynchronizationRegistry} and
   * can manage persistence contexts
   * @return
   */
  public final boolean jtaIntegrationAvailable()
  {
    return tranRegistry.get() != null;
  }
  
  /**
   * Called by service tracker to indicate that a new {@link TransactionSynchronizationRegistry}
   * is available in the runtime
   * @param ref
   */
  @Override
  public final Object addingService(ServiceReference ref) {
    
    if(tranRegistryRef.compareAndSet(null, ref)) 
    {
      TransactionSynchronizationRegistry tsr = (TransactionSynchronizationRegistry) context.getService(ref);
      if(tsr != null) {
        if(tranRegistry.compareAndSet(null, tsr)) {
          _logger.info("A TransactionSynchronizationRegistry service is now available in the runtime. Managed persistence contexts will now" +
              "integrate with JTA transactions using {}.", new Object[] {ref});
        }
        else
        {
          tranRegistry.set(tsr);
          _logger.warn("The TransactionSynchronizationRegistry used to manage persistence contexts has been replaced." +
              " The new TransactionSynchronizationRegistry, {}, will now be used to manage persistence contexts." +
              " Managed persistence contexts may not work correctly unless the runtime uses the new JTA Transaction services implementation" +
              " to manage transactions.", new Object[] {ref});
        }
      } else {
        tranRegistryRef.compareAndSet(ref, null);
      }
    }
    return ref;
  }
  
  @Override
  public final void removedService(ServiceReference reference, Object o) {
    if(tranRegistryRef.get() == reference) { 
      //Our reference is going away, find a new one
      ServiceReference[] refs = getServiceReferences();
      ServiceReference chosenRef = null;
      TransactionSynchronizationRegistry replacement = null;
      if(refs != null) {
        for(ServiceReference ref : refs) {
          if(ref != reference) {
            replacement = (TransactionSynchronizationRegistry) context.getService(ref);
            if(replacement != null) {
              chosenRef = ref;
              break;
            }
          }
        }
      }
      
      if(replacement == null) {
        TransactionSynchronizationRegistry old = tranRegistry.get();
        tranRegistryRef.set(null);
        tranRegistry.compareAndSet(old, null);
        
      _logger.warn("The TransactionSynchronizationRegistry used to manage persistence contexts is no longer available." +
          " Managed persistence contexts will no longer be able to integrate with JTA transactions, and will behave as if" +
          " no there is no transaction context at all times until a new TransactionSynchronizationRegistry is available." +
          " Applications using managed persistence contexts may not work correctly until a new JTA Transaction services" +
          " implementation is available.");
      } else {
        tranRegistryRef.set(chosenRef);
        tranRegistry.set(replacement);
      _logger.warn("The TransactionSynchronizationRegistry used to manage persistence contexts has been replaced." +
          " The new TransactionSynchronizationRegistry, {}, will now be used to manage persistence contexts." +
          " Managed persistence contexts may not work correctly unless the runtime uses the new JTA Transaction services implementation" +
          " to manage transactions.", new Object[] {chosenRef});
      }
      context.ungetService(reference);
      //If there was no replacement before, check again. This closes the short window if
      //add and remove happen simultaneously
      if(replacement == null) {
        ServiceReference[] retryRefs = getServiceReferences();
        if(refs != null) {
          for(ServiceReference r : retryRefs) {
            if(r != reference) {
              addingService(r);
              if(tranRegistryRef.get() != null)
                break;
            }
          }
        }
      }
    }
  }
  
  /**
   * This class is used to close EntityManager instances once the transaction has committed,
   * and clear the persistenceContextRegistry of old persistence contexts.
   */
  private final static class EntityManagerClearUp implements Synchronization {

    private final EntityManager context;
    
    private final AtomicLong activeCount;
    
    private final DestroyCallback callback;
    
    /**
     * Create a Synchronization to clear up our EntityManagers
     * @param em
     */
    public EntityManagerClearUp(EntityManager em, AtomicLong instanceCount, DestroyCallback cbk)
    {
      context = em;
      activeCount = instanceCount;
      callback = cbk;
    }
    
    public final void beforeCompletion() {
      //This is a no-op;
    }

    @SuppressWarnings("unchecked")
    public final void afterCompletion(int arg0) {
      if(_logger.isDebugEnabled())
        _logger.debug("Clearing up EntityManager {} as the transaction has completed.", new Object[] {context});
      try {
        activeCount.decrementAndGet();
        callback.callback();
      } finally {
			  try{
			    context.close();
        } catch (Exception e) {
          _logger.warn("There was an error when the container closed an EntityManager", context);
        }
      }
    }
  }

}
