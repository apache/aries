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
package org.apache.aries.jpa.container.context.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class is responsible for managing all of the persistence contexts at a defined scope
 * It will automatically manage the lifecycle of all registered persistence contexts
 */
public class PersistenceContextManager extends ServiceTracker{
  /** The key to use when storing the {@link PersistenceContextType} for this context */
  public static final String PERSISTENCE_CONTEXT_TYPE = "org.apache.aries.jpa.context.type";
  /** The filter this tracker uses to select Persistence Units. */
  private static final Filter filter; 
  static {
    Filter f = null;
    try {
      f = FrameworkUtil.createFilter("(&(" + Constants.OBJECTCLASS
        + "=" + "javax.persistence.EntityManagerFactory" + ")(" + 
        PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))" );
    } catch (InvalidSyntaxException e) {
      // TODO This should never ever happen!
      e.printStackTrace();
    }
    filter = f;
  }
  /** A Map of bundles that reference a particular persistence unit. Only access when synchronized on <code>this</code>.*/ 
  private final Map<String, Set<Bundle>> persistenceContextConsumers = new HashMap<String, Set<Bundle>>();
  /** 
   * A Map persistence unit names to persistenceContextDefinitions. We use HashMap in the generic definition to ensure that 
   * we get the correct deep <code>equals()</code> behaviour. Only access when synchronized on <code>this</code>.
   */
  private final Map<String, HashMap<String, Object>> persistenceContextDefinitions = new HashMap<String, HashMap<String,Object>>();
  /** A Map of persistence units tracked by the tracker. Only access when synchronized on <code>this</code>. */
  private final Map<String, ServiceReference> persistenceUnits = new HashMap<String, ServiceReference>();
  /** A Map for storing the registered ManagedPersistenceContextServiceFactory. Only access when synchronized on <code>this</code>. */
  private final Map<String, ServiceRegistration> entityManagerRegistrations = new HashMap<String, ServiceRegistration>();
  private final JTAPersistenceContextRegistry persistenceContextRegistry;
  
  /**
   * Create a new PersistenceContextManager at a scope defined by the supplied {@link BundleContext}
   * @param ctx the bundle context to use for tracking services. In order to prevent this
   *            object becoming prematurely invalid it is best to use the {@link BundleContext} of
   *            the system bundle (Bundle 0).
   */
  public PersistenceContextManager(BundleContext ctx, JTAPersistenceContextRegistry registry) {

    super(ctx, filter, null);
    persistenceContextRegistry = registry;
  }

  @Override
  public Object addingService(ServiceReference reference) {

    String unitName = (String) reference.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME);
    boolean register;
    //Use a synchronized block to ensure that we get an atomic view of the persistenceUnits
    //and the persistenceContextDefinitions
    synchronized (this) {
      //If we already track a unit with the same name then we are in trouble!
      //only one unit with a given name should exist at a single scope
      if(!!!persistenceUnits.containsKey(unitName)) {
        //TODO log a big warning here!
        //Stop tracking the duplicate unit.
        return null;
      }
      //If this is a new unit, then add it, and check whether we have any waiting
      //persistenceContextDefinitions
      persistenceUnits.put(unitName, reference);
      register = persistenceContextDefinitions.containsKey(unitName);
    }
    //If there are persistenceContexts then register them
    if(register){
      registerEM(unitName);
    }
    return reference;
  }

  public void removedService(ServiceReference ref, Object o)
  {
    String unitName = (String) ref.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME);;
    //Remove the persistence Unit service to prevent other people from trying to use it
    synchronized (this) {
      persistenceUnits.remove(unitName);
    }
    //Unregister any dependent contexts
    unregisterEM(unitName);
  }

  /**
   * Register a persistence context definition with this manager
   * @param name  The name of the persistence unit for this context
   * @param client The {@link Bundle} that uses this persistence context
   * @param properties The Map of properties for this persistence context
   *                   This must contain the {@link PersistenceContextType}
   */
  public void registerContext(String name, Bundle client, HashMap<String, Object> properties) {
    
    HashMap<String, Object> oldProps;
    boolean register;
    //Use a synchronized to get an atomic view
    synchronized (this) {
      //Add a new consumer for the context, including the Set if necessary
      Set<Bundle> bundles = persistenceContextConsumers.get(name);
      if(bundles == null) {
        bundles = new HashSet<Bundle>();
        persistenceContextConsumers.put(name, bundles);
      }
      bundles.add(client);
      
      //Check that we don't have different properties to other clients.
      //This would not make sense, as all clients should share the same
      //context!
      oldProps = persistenceContextDefinitions.put(name, properties);
      if(oldProps != null) {
        if(!!!oldProps.equals(properties)) {
          //TODO log an error and use the old properties
        }
      }
      //We should only register if our persistence unit exists
      register = persistenceUnits.containsKey(name);
    }
      
    if(register) {
      registerEM(name);
    }
  }

  /**
   * Unregister the supplied bundle as a client of the supplied persistence context
   * @param name  The name of the context
   * @param client The bundle that is using the persistence context
   */
  public void unregisterContext(String name, Bundle client)
  {
    boolean unregister = false;
    //Keep an atomic view of our state
    synchronized (this) {
      //Remove the client
      Set<Bundle> clients = persistenceContextConsumers.get(name);
      clients.remove(client);
      //If no clients remain then tidy up the context
      if(clients.isEmpty()) {
        persistenceContextDefinitions.remove(name);
        persistenceContextConsumers.remove(name);
        unregister = true;
      }
    }
    //Unregister the context if it is no longer used
    if(unregister)
      unregisterEM(name);
  }

  /**
   * Register a {@link ManagedPersistenceContextServiceFactory} for the named persistence context
   * 
   * This must <b>never</b> be called from a <code>synchronized</code> block.
   * @param name
   */
  private void registerEM(String name) {
    
    ServiceFactory entityManagerServiceFactory;
    ServiceReference unit;
    ServiceRegistration reg = null;
    try
    {
      //Synchronize for an atomic view
      synchronized (this) {
        //Not our job to register if someone is already doing it, or has already done it
        if(entityManagerRegistrations.containsKey(name))
          return;
        //Block other threads from trying to register by adding the key
        entityManagerRegistrations.put(name, null);
        
        //Get hold of the information we need to create the context
        unit = persistenceUnits.get(name);
        Map<String, Object> props = persistenceContextDefinitions.get(name);
        
        //If either of these things is undefined then the context cannot be registered
        if(props == null || unit == null)
          return;

        //Create the service factory
        entityManagerServiceFactory = new ManagedPersistenceContextServiceFactory(unit, props, persistenceContextRegistry);
      }
     
      //Always register from outside a synchronized 
      Hashtable<String, Object> props = new Hashtable<String, Object>();
      
      props.put(PersistenceUnitConstants.OSGI_UNIT_NAME, name);
      props.put(PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT, Boolean.TRUE);
      props.put(PersistenceUnitConstants.OSGI_UNIT_PROVIDER, unit.getProperty(PersistenceUnitConstants.OSGI_UNIT_PROVIDER));
      props.put(PersistenceUnitConstants.EMPTY_PERSISTENCE_UNIT_NAME, "".equals(name));
      
      BundleContext persistenceBundleContext = unit.getBundle().getBundleContext();
      reg = persistenceBundleContext.registerService(
          EntityManager.class.getName(), entityManagerServiceFactory, props);
    } finally {
      //As we have to register from outside a synchronized then someone may be trying to
      //unregister us. They will try to wait for us to finish, but in order to prevent 
      //live-lock they may flag to us that we need to do the unregistration by removing
      //the persistence context key.
      boolean recoverFromLiveLock = false;
    
      //Synchronize to get an atomic view
      synchronized (this) {
        //If we created a registration
        if(reg != null) {
          //If the key still exists then all is well
          if(entityManagerRegistrations.containsKey(name))
            entityManagerRegistrations.put(name, reg);
          //Else we were in a potential live-lock and the service could not be unregistered
          //earlier. This means we have to do it (but outside the synchronized. Make sure we
          //also remove the registration key!
          else {
            entityManagerRegistrations.remove(name);
            recoverFromLiveLock = true;
          }
        }
        //There was no registration created. Remove the key.
        else
          entityManagerRegistrations.remove(name);
        
        //Notify any waiting unregistrations that they can proceed
        this.notifyAll();
      }
      //If we were live-locked then unregister the registration here
      if(recoverFromLiveLock)
        reg.unregister();
    }
  }
  
  /**
   * Unregister the named persistence context. This code attempts to ensure that
   * the calling thread performs the unregistration to ensure the calls are
   * synchronous. If a potential live-lock is detected then the unregistration may
   * occur on a different thread or at a different time.
   * 
   * This must <b>never</b> be called from a <code>synchronized</code> block.
   * @param unitName
   * 
   */
  private void unregisterEM(String unitName) {
    ServiceRegistration reg = null;
    //Look for the registration
    synchronized (this) {
      //We use a loop to prevent live-locking, we
      //loop a maximum of 4 times before
      boolean found = false;
      int tries = 0;
      while (!!!found && tries < 4) {
        //If we contain the key then get the registration,
        //If not, then we have nothing to unregister
        if(entityManagerRegistrations.containsKey(unitName))
          reg = entityManagerRegistrations.get(unitName);
        else
          return;
        
        //It is possible that the registration is null. This means we are in
        //the transient case where a registration is being created. If we wait
        //then everything should be fine. If the registration is not null, then
        //remove it from the map and escape the loop
        if(reg != null) {
          found = true;
          entityManagerRegistrations.remove(unitName);
        } else
          //We didn't find it, wait and try again
          try {
            this.wait(500);
          } catch (InterruptedException e) {
            // TODO Log this properly
            e.printStackTrace();
          }
        //Increment the loop to prevent us from live-locking
        tries++;
      }
      //If we didn't find the registration in the loop then still
      //remove the key to warn the registering thread to unregister
      //immediately.
      if(!found) {
        //Possible Live lock, just remove the key
        entityManagerRegistrations.remove(unitName);
        //TODO log the potential issue
      }
    }
    //If we found the registration then unregister it outside the synchronized.
    if (reg != null) {
      reg.unregister();
    }
  }

  @Override
  /**
   * Open this <code>PersistenceContextManager</code> and begin tracking services.
   * 
   * <p>
   * This implementation calls <code>open(true)</code>.
   * 
   * @throws java.lang.IllegalStateException If the <code>BundleContext</code>
   *         with which this <code>ServiceTracker</code> was created is no
   *         longer valid.
   * @see #open(boolean)
   */
  public void open() {
    super.open(true);
  }
}
