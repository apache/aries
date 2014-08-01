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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.context.transaction.impl.DestroyCallback;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing all of the persistence contexts at a defined scope,
 * i.e. for a single framework or composite. It will automatically manage the lifecycle of all
 * registered persistence contexts.
 */
public class PersistenceContextManager extends ServiceTracker{
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container.context");
  
  /** The filter this tracker uses to select Persistence Units. */
  private static final Filter filter; 
  static {
    Filter f = null;
    String filterString = "(&(" + Constants.OBJECTCLASS + "=" + "javax.persistence.EntityManagerFactory" + ")(" + 
                      PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)(!(" + 
                      PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*)))";
    try {
      //Create a filter to select container managed persistence units that 
      //are not proxies for managed persistence contexts 
      f = FrameworkUtil.createFilter(filterString);
    } catch (InvalidSyntaxException e) {
      _logger.error(NLS.MESSAGES.getMessage("emf.filter.invalid", filterString), e);
      throw new RuntimeException(e);
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
  public void close() {
    super.close();
    for (ServiceRegistration reg : entityManagerRegistrations.values()) {
      AriesFrameworkUtil.safeUnregisterService(reg);
    }
  }

  @Override
  public Object addingService(ServiceReference reference) {

    if(_logger.isDebugEnabled()) {
      _logger.debug("A new managed persistence unit, {}, has been detected.", new Object[] {reference});
    }
    
    String unitName = (String) reference.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME);
    if(unitName == null)
      unitName = "";
    boolean register;
    //Use a synchronized block to ensure that we get an atomic view of the persistenceUnits
    //and the persistenceContextDefinitions
    synchronized (this) {
      //If we already track a unit with the same name then we are in trouble!
      //only one unit with a given name should exist at a single scope
      if(persistenceUnits.containsKey(unitName)) {
        _logger.warn(NLS.MESSAGES.getMessage("pu.registered.multiple.times", reference));
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
    if(_logger.isDebugEnabled()) {
      _logger.debug("A managed persistence unit, {}, has been unregistered.", new Object[] {ref});
    }
    
    String unitName = (String) ref.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME);
    if(unitName == null)
      unitName = "";
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
    if (_logger.isDebugEnabled()) {
      _logger.debug("Registering bundle {} as a client of persistence unit {} with properties {}.", 
          new Object[] {client.getSymbolicName() + "_" + client.getVersion(), name, properties});
    }
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
          _logger.warn(NLS.MESSAGES.getMessage("persistence.context.exists.multiple.times", client.getSymbolicName(), 
              client.getVersion(), name, properties, oldProps));
          persistenceContextDefinitions.put(name, oldProps);
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
    if (_logger.isDebugEnabled()) {
      _logger.debug("Unregistering the bundle {} as a client of persistence unit {}.", 
          new Object[] {client.getSymbolicName() + "_" + client.getVersion(), name});
    }
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
   * Register a {@link ManagedPersistenceContextFactory} for the named persistence context
   * 
   * This must <b>never</b> be called from a <code>synchronized</code> block.
   * @param name
   */
  private void registerEM(String name) {
    
    EntityManagerFactory entityManagerServiceFactory;
    ServiceReference unit;
    ServiceRegistration reg = null;
    boolean alreadyRegistered = false;
    try
    {
      //Synchronize for an atomic view
      synchronized (this) {
        //Not our job to register if someone is already doing it, or has already done it
        if(entityManagerRegistrations.containsKey(name)){
          alreadyRegistered = true;
          return;
        }
        if(_logger.isDebugEnabled()) {
          _logger.debug("Registering a managed persistence context for persistence unit {}", new Object[] {name});
        }
        //Block other threads from trying to register by adding the key
        entityManagerRegistrations.put(name, null);
        
        //Get hold of the information we need to create the context
        unit = persistenceUnits.get(name);
        Map<String, Object> props = persistenceContextDefinitions.get(name);
        
        //If either of these things is undefined then the context cannot be registered
        if(props == null || unit == null) {
          _logger.error(NLS.MESSAGES.getMessage("null.pu.or.props", name, unit, props));
          //The finally block will clear the entityManagerRegistrations key
          return;
        }

        //Create the service factory
        entityManagerServiceFactory = new ManagedPersistenceContextFactory(name, unit, props, persistenceContextRegistry);
      }
     
      //Always register from outside a synchronized 
      Hashtable<String, Object> props = new Hashtable<String, Object>();
      
      props.put(PersistenceUnitConstants.OSGI_UNIT_NAME, name);
      props.put(PersistenceUnitConstants.OSGI_UNIT_VERSION, unit.getProperty(PersistenceUnitConstants.OSGI_UNIT_VERSION));
      props.put(PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT, Boolean.TRUE);
      props.put(PersistenceUnitConstants.OSGI_UNIT_PROVIDER, unit.getProperty(PersistenceUnitConstants.OSGI_UNIT_PROVIDER));
      props.put(PersistenceUnitConstants.EMPTY_PERSISTENCE_UNIT_NAME, "".equals(name));
      props.put(PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE, "true");
      
      BundleContext persistenceBundleContext = unit.getBundle().getBundleContext();
      reg = persistenceBundleContext.registerService(
          EntityManagerFactory.class.getName(), entityManagerServiceFactory, props);
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
          if(entityManagerRegistrations.containsKey(name)) {
            entityManagerRegistrations.put(name, reg);
          } else {
            //Else we were in a potential live-lock and the service could not be unregistered
            //earlier. This means we have to do it (but outside the synchronized. Make sure we
            //also remove the registration key!
            _logger.warn(NLS.MESSAGES.getMessage("possible.livelock.recovery", name));
            entityManagerRegistrations.remove(name);
            recoverFromLiveLock = true;
          }
        }
        //There was no registration created. Remove the key if we were registering.
        else {
          if(!!!alreadyRegistered)
            entityManagerRegistrations.remove(name);
        }
        
        //Notify any waiting unregistrations that they can proceed
        this.notifyAll();
      }
      //If we were live-locked then unregister the registration here
      if(recoverFromLiveLock)
        AriesFrameworkUtil.safeUnregisterService(reg);
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
            _logger.warn(NLS.MESSAGES.getMessage("interruption.waiting.for.pu.unregister", unitName));
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
        _logger.warn(NLS.MESSAGES.getMessage("possible.livelock.detected", unitName));
      }
    }
    //If we found the registration then unregister it outside the synchronized.
    if (reg != null) {
      AriesFrameworkUtil.safeUnregisterService(reg);
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

  /**
   * Call this method to quiesce a given bundle
   * @param bundleToQuiesce The bundle to quiesce
   * @param callback A callback indicating that we have finished quiescing
   */
  public void quiesceUnits(Bundle bundleToQuiesce, DestroyCallback callback) {
    
    //Find the units supplied by this bundle
    List<String> units = new ArrayList<String>();
    synchronized (this) {
      Iterator<Entry<String, ServiceReference>> it = persistenceUnits.entrySet().iterator();
      while(it.hasNext()) {
        Entry<String, ServiceReference> entry = it.next();
        ServiceReference value = entry.getValue();
        //If the quiescing bundle supplies the unit then unget the unit and remove it
        if(value.getBundle().equals(bundleToQuiesce)) {
          units.add(entry.getKey());
          context.ungetService(entry.getValue());
          //remove it to prevent other units that start using it
          it.remove();
        }
      }
    }
    //If there are no units then our work is done!
    if(units.isEmpty()) {
      callback.callback();
    } else {
      //Find the ManagedFactories for the persistence unit
      List<ManagedPersistenceContextFactory> factoriesToQuiesce = new ArrayList<ManagedPersistenceContextFactory>();
      synchronized (this) {
        Iterator<String> it = units.iterator();
        while(it.hasNext()) {
          ServiceRegistration reg = entityManagerRegistrations.get(it.next());
          //If there's no managed factory then we don't need to quiesce this unit
          boolean needsQuiesce = false;
          if(reg != null) {
            ManagedPersistenceContextFactory fact = (ManagedPersistenceContextFactory) bundleToQuiesce.getBundleContext().getService(reg.getReference());
            if(fact != null) {
              factoriesToQuiesce.add(fact);
              needsQuiesce = true;
            }
          }
          //If the unit doesn't need quiescing then remove it from our check
          if(!!!needsQuiesce) {
            it.remove();
          }
        }
      }
      //If no factories are registered then we're done
      if(factoriesToQuiesce.isEmpty()) {
        callback.callback();
      } else { 
        //Create a new Tidy up helper and tell the factories to tidy up
        QuiesceTidyUp tidyUp = new QuiesceTidyUp(units, callback);
        
        for(ManagedPersistenceContextFactory fact : factoriesToQuiesce) {
          fact.quiesce(tidyUp);
        }
      }
    }
  }
  
  /**
   * Quiesce all the persistence units managed by this {@link PersistenceContextManager}
   * @param callback
   */
  public void quiesceAllUnits(final DestroyCallback callback) {
    
    Collection<String> names = new ArrayList<String>();
    Collection<ServiceRegistration> factoriesToQuiesce = new ArrayList<ServiceRegistration>();
   
    //Get all the resources
    synchronized (this) {
      names.addAll(entityManagerRegistrations.keySet());
      factoriesToQuiesce.addAll(entityManagerRegistrations.values());
    }
    //If there are no names or factories then we're done
    if(names.isEmpty() || factoriesToQuiesce.isEmpty()) {
      callback.callback();
    } else {
      
      //Register an async tidy up
      QuiesceTidyUp tidyUp = new QuiesceTidyUp(names, callback);
      
      for(ServiceRegistration reg : factoriesToQuiesce) {
        ManagedPersistenceContextFactory fact = (ManagedPersistenceContextFactory) reg.getReference().getBundle().getBundleContext().getService(reg.getReference());
        fact.quiesce(tidyUp);
      }
    }
  }
  
  /**
   * An asynchronous tidy up operation
   */
  class QuiesceTidyUp {
    //The units being tidied up
    private final Set<String> units;
    //The callback for when we're done
    private final DestroyCallback dc;
    
    public QuiesceTidyUp(Collection<String> units,
        DestroyCallback callback) {
      this.units = new HashSet<String>(units);
      dc = callback;
    }
    
    public void unitQuiesced(String name) {
      unregisterEM(name);
      boolean winner;
      synchronized(this) {
        winner = !!!units.isEmpty() && units.remove(name) && units.isEmpty();
      }
      
      if(winner) {
        dc.callback();
      }
    }
  }


  
}
