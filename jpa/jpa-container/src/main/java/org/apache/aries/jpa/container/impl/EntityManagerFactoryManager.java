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
package org.apache.aries.jpa.container.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class manages the lifecycle of Persistence Units and their associated
 * {@link EntityManagerFactory} objects.
 */
public class EntityManagerFactoryManager implements ServiceTrackerCustomizer {

  /**
   * A callback for a named persistence units
   */
  class NamedCallback {
    private final Set<String> names;
    private final DestroyCallback callback;
    public NamedCallback(Collection<String> names, DestroyCallback countdown) {
      this.names = new HashSet<String>(names);
      callback = countdown;
    }

    public void callback(String name) {
      boolean winner;
      synchronized (this) {
        winner = !!!names.isEmpty() && names.remove(name) && names.isEmpty();
      }
      if(winner)
        callback.callback();
    }
  }

  /** The container's {@link BundleContext} */
  private final BundleContext containerContext;
  /** The persistence bundle */
  private final Bundle bundle;
  /** The {@link PersistenceProvider} to use */
  private ServiceReference provider;
  /** The named persistence units to manage */
  private Map<String, ? extends ManagedPersistenceUnitInfo> persistenceUnits;
  /** The original parsed data */
  private Collection<ParsedPersistenceUnit> parsedData;
  /** A Map of created {@link EntityManagerFactory}s */
  private Map<String, CountingEntityManagerFactory> emfs = null;
  /** The {@link ServiceRegistration} objects for the {@link EntityManagerFactory}s */
  private ConcurrentMap<String, ServiceRegistration> registrations = null;
  /** Quiesce this Manager */
  private boolean quiesce = false;
  
  private volatile ServiceTracker tracker; 
  
  /** DataSourceFactories in use by persistence units in this bundle - class name key to collection of unit values */
  private final ConcurrentMap<String, Collection<String>> dataSourceFactories = 
         new ConcurrentHashMap<String, Collection<String>>();

  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  
  /**
   * Create an {@link EntityManagerFactoryManager} for
   * the supplied persistence bundle.
   * 
   * This constructor should only be used by a 
   * {@link PersistenceBundleManager} that is synchronized
   * on itself, and the resulting manager should be immediately
   * stored in the bundleToManager Map
   * 
   * @param b
   * @param infos 
   * @param ref 
   * @param parsedUnits 
   */
  public EntityManagerFactoryManager(BundleContext containerCtx, Bundle b, Collection<ParsedPersistenceUnit> parsedUnits, ServiceReference ref, Collection<? extends ManagedPersistenceUnitInfo> infos) {
    containerContext = containerCtx;
    bundle = b;
    provider = ref;
    persistenceUnits = getInfoMap(infos);
    parsedData = parsedUnits;
  }

  private Map<String, ? extends ManagedPersistenceUnitInfo> getInfoMap(
      Collection<? extends ManagedPersistenceUnitInfo> infos) {
    Map<String, ManagedPersistenceUnitInfo> map = Collections.synchronizedMap(
        new HashMap<String, ManagedPersistenceUnitInfo>());
    if (infos != null) {
      for(ManagedPersistenceUnitInfo info : infos) {
        map.put(info.getPersistenceUnitInfo().getPersistenceUnitName(), info);
      }
    }
    return map;
  }

  /**
   * Notify the {@link EntityManagerFactoryManager} that a provider is being
   * removed from the service registry.
   * 
   * If the provider is used by this {@link EntityManagerFactoryManager} then
   * the manager should destroy the dependent persistence units.
   * 
   * <b>This method should only be called when not holding any locks</b>
   * 
   * @param ref  The provider service reference
   * @return true if the the provider is being used by this manager
   */
  public synchronized boolean providerRemoved(ServiceReference ref) {
    
    boolean toReturn = provider.equals(ref);
    
    if(toReturn)
      destroy();
    
    return toReturn;
  }

  /**
   * Notify the {@link EntityManagerFactoryManager} that the bundle it is
   * managing has changed state
   * 
   * <b>This method should only be called when not holding any locks</b>
   * 
   * @throws InvalidPersistenceUnitException if the manager is no longer valid and
   *                                         should be destroyed
   */
  public synchronized void bundleStateChange() throws InvalidPersistenceUnitException {
    
    switch(bundle.getState()) {
      case Bundle.RESOLVED :
        //If we are Resolved as a result of having stopped
        //and missed the STOPPING event we need to unregister
        unregisterEntityManagerFactories();
      //Create the EMF objects if necessary
        createEntityManagerFactories();
        break;
        //Starting and active both require EMFs to be registered
      case Bundle.STARTING :
      case Bundle.ACTIVE :
        if(tracker == null) {
          tracker = new ServiceTracker(bundle.getBundleContext(), 
              "org.osgi.service.jdbc.DataSourceFactory", this);
          tracker.open();
        }
        registerEntityManagerFactories();
        break;
        //Stopping means the EMFs should
      case Bundle.STOPPING :
        //If we're stopping we no longer need to be quiescing
        quiesce = false;
        if(tracker != null) {
          tracker.close();
          tracker = null;
        }
        unregisterEntityManagerFactories();
        break;
      case Bundle.INSTALLED :
        //Destroy everything
        destroyEntityManagerFactories();
    }
  }

  /**
   * Unregister all {@link EntityManagerFactory} services
   */
  private void unregisterEntityManagerFactories() {
    //If we have registrations then unregister them
    if(registrations != null) {
      for(Entry<String, ServiceRegistration> entry : registrations.entrySet()) {
        AriesFrameworkUtil.safeUnregisterService(entry.getValue());
        emfs.get(entry.getKey()).clearQuiesce();
        persistenceUnits.get(entry.getKey()).unregistered();
      }
      // remember to set registrations to be null
      registrations = null;
    }
  }

  private void unregisterEntityManagerFactory(String unit) {
    if(registrations != null) {
      AriesFrameworkUtil.safeUnregisterService(registrations.remove(unit));
      emfs.get(unit).clearQuiesce();
      persistenceUnits.get(unit).unregistered();
    }
  }

  /**
   * Register {@link EntityManagerFactory} services
   * 
   * @throws InvalidPersistenceUnitException if this {@link EntityManagerFactory} is no longer
   *  valid and should be destroyed
   */
  private void registerEntityManagerFactories() throws InvalidPersistenceUnitException {
    //Only register if there is a provider and we are not
    //quiescing
    if(registrations == null) {
      registrations = new ConcurrentHashMap<String, ServiceRegistration>();
    }
    
    if(provider != null && !quiesce) {
      //Make sure the EntityManagerFactories are instantiated
      createEntityManagerFactories();
      
      String providerName = (String) provider.getProperty("javax.persistence.provider");
      if(providerName == null) {
        _logger.warn( NLS.MESSAGES.getMessage("no.provider.specified", 
                      bundle.getSymbolicName() + '/' + bundle.getVersion(), 
                      PersistenceUnitConstants.OSGI_UNIT_PROVIDER, provider));
      }
      //Register each EMF
      for(Entry<String, ? extends EntityManagerFactory> entry : emfs.entrySet())
      {
        
        Hashtable<String,Object> props = new Hashtable<String, Object>();
        String unitName = entry.getKey();
        
        if(registrations.containsKey(unitName) || !!!availableDataSourceFactory(unitName))
          continue;
        
        props.put(PersistenceUnitConstants.OSGI_UNIT_NAME, unitName);
        if(providerName != null)
          props.put(PersistenceUnitConstants.OSGI_UNIT_PROVIDER, providerName);
        
        props.put(PersistenceUnitConstants.OSGI_UNIT_VERSION, bundle.getVersion());
        props.put(PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT, Boolean.TRUE);
        props.put(PersistenceUnitConstants.EMPTY_PERSISTENCE_UNIT_NAME, "".equals(unitName));
        try {
          registrations.put(unitName, bundle.getBundleContext().registerService(EntityManagerFactory.class.getCanonicalName(), entry.getValue(), props));
          persistenceUnits.get(unitName).registered();
        } catch (Exception e) {
          _logger.error(NLS.MESSAGES.getMessage("cannot.register.persistence.unit", unitName, bundle.getSymbolicName() + '/' + bundle.getVersion()));
          throw new InvalidPersistenceUnitException(e);
        }
      }
    }
  }

  private boolean availableDataSourceFactory(String unitName) {
    ManagedPersistenceUnitInfo mpui = persistenceUnits.get(unitName);
        
    String driver = (String) mpui.getPersistenceUnitInfo().getProperties().
    get(PersistenceUnitConstants.DATA_SOURCE_FACTORY_CLASS_NAME);
    
    //True if the property is not "true" and the jdbc driver is set
    if(Boolean.parseBoolean((String)mpui.getContainerProperties().
        get(PersistenceUnitConstants.USE_DATA_SOURCE_FACTORY)) &&
        driver != null) {
      
      if(dataSourceFactories.containsKey(driver)) {
        dataSourceFactories.get(driver).add(unitName);
        if(_logger.isDebugEnabled())
          _logger.debug(NLS.MESSAGES.getMessage("datasourcefactory.found", unitName, bundle.getSymbolicName(),
              bundle.getVersion(), driver));
        return true;
      }
      if(_logger.isDebugEnabled())
        _logger.debug(NLS.MESSAGES.getMessage("datasourcefactory.not.found", unitName, bundle.getSymbolicName(),
            bundle.getVersion(), driver));
      return false;
    } else {
      //We aren't checking (thanks to the property or a null jdbc driver name)
      return true;
    }
  }

  /**
   * Create {@link EntityManagerFactory} services for this peristence unit
   * throws InvalidPersistenceUnitException if this {@link EntityManagerFactory} is no longer
   *  valid and should be destroyed
   */
  private void createEntityManagerFactories() throws InvalidPersistenceUnitException {
    if (emfs == null) {  
      emfs = new HashMap<String, CountingEntityManagerFactory>();
    }
    //Only try if we have a provider and EMFs
    if(provider == null || !emfs.isEmpty() || quiesce) {
        return;
    }
    try {
      //Get hold of the provider
      PersistenceProvider providerService = (PersistenceProvider) containerContext.getService(provider);

      if(providerService == null) {
        _logger.warn(NLS.MESSAGES.getMessage("persistence.provider.gone.awol", bundle.getSymbolicName() + '/' + bundle.getVersion()));
        throw new InvalidPersistenceUnitException();
      }

      for(String unitName : persistenceUnits.keySet()){
        ManagedPersistenceUnitInfo mpui = persistenceUnits.get(unitName);
        try {
          EntityManagerFactory emf = providerService.createContainerEntityManagerFactory(mpui.getPersistenceUnitInfo(), mpui.getContainerProperties());
          emfs.put(unitName, new CountingEntityManagerFactory(emf, unitName));
        } catch (Exception e) {
          _logger.warn("Error creating EntityManagerFactory", e);
        }
      }
    } finally {
      //Remember to unget the provider
      containerContext.ungetService(provider);
    }
  }

  /**
   * Manage the EntityManagerFactories for the following
   * provider and {@link PersistenceUnitInfo}s
   * 
   * This method should only be called when not holding any locks
   * 
   * @param ref The {@link PersistenceProvider} {@link ServiceReference}
   * @param infos The {@link PersistenceUnitInfo}s defined by our bundle
   */
  public synchronized void manage(ServiceReference ref,
      Collection<? extends ManagedPersistenceUnitInfo> infos)  throws IllegalStateException{
    provider = ref;
    persistenceUnits = getInfoMap(infos);
  }
  
  /**
   * Manage the EntityManagerFactories for the following
   * provider, updated persistence xmls and {@link PersistenceUnitInfo}s
   * 
   * This method should only be called when not holding any locks
   * 
   * @param parsedUnits The updated {@link ParsedPersistenceUnit}s for this bundle 
   * @param ref The {@link PersistenceProvider} {@link ServiceReference}
   * @param infos The {@link PersistenceUnitInfo}s defined by our bundle
   */
  public synchronized void manage(Collection<ParsedPersistenceUnit> parsedUnits, ServiceReference ref,
      Collection<? extends ManagedPersistenceUnitInfo> infos)  throws IllegalStateException{
    parsedData = parsedUnits;
    provider = ref;
    persistenceUnits = getInfoMap(infos);
  }

  /**
   * Stop managing any {@link EntityManagerFactory}s 
   * 
   * This method should only be called when not holding any locks
   */
  public synchronized void destroy() {
    destroyEntityManagerFactories();
    
    provider = null;
    persistenceUnits = null;
    if(tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

  /**
   * S
   */
  private void destroyEntityManagerFactories() {
    if(registrations != null)
      unregisterEntityManagerFactories();
    if(emfs != null) {
      for(Entry<String, ? extends EntityManagerFactory> entry : emfs.entrySet()) {
        try {
          entry.getValue().close();
        } catch (Exception e) {
          _logger.error(NLS.MESSAGES.getMessage("could.not.close.persistence.unit", entry.getKey(), bundle.getSymbolicName() + '/' + bundle.getVersion()), e);
        }
      }
    }
    emfs = null;
  }

  public Bundle getBundle() {
    return bundle;
  }

  public Collection<ParsedPersistenceUnit> getParsedPersistenceUnits()
  {
    return parsedData;
  }
  /** Quiesce this Manager */
  public void quiesce(DestroyCallback countdown) {
    
    //Find the EMFs to quiesce, and their Service registrations
    Map<CountingEntityManagerFactory, ServiceRegistration> entries = new HashMap<CountingEntityManagerFactory, ServiceRegistration>();
    Collection<String> names = new ArrayList<String>();
    synchronized(this) {
      if((bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0)
        quiesce = true;
      if(emfs != null) {
        for(String key : emfs.keySet()) {
          entries.put(emfs.get(key), registrations != null ? registrations.get(key) : null);
          names.add(key);
        }
      }
    }
    //Quiesce as necessary
    if(entries.isEmpty())
      countdown.callback();
    else {
    NamedCallback callback = new NamedCallback(names, countdown);
      for(Entry<CountingEntityManagerFactory, ServiceRegistration> entry : entries.entrySet()) {
        CountingEntityManagerFactory emf = entry.getKey();
        emf.quiesce(callback, entry.getValue());
      }
    }
  }

  @Override
  public StringBuffer addingService(ServiceReference reference) {
    //Use String.valueOf to save us from nulls
    StringBuffer sb = new StringBuffer(String.valueOf(reference.getProperty("osgi.jdbc.driver.class")));
    
    //Only notify of a potential change if a new data source class is available
    if(dataSourceFactories.putIfAbsent(sb.toString(), new ArrayList<String>()) == null) {
      if(_logger.isDebugEnabled())
        _logger.debug(NLS.MESSAGES.getMessage("new.datasourcefactory.available", sb.toString(), 
            bundle.getSymbolicName(), bundle.getVersion()));
      try {
        bundleStateChange();
      } catch (InvalidPersistenceUnitException e) {
        //Not much we can do here unfortunately
        _logger.warn(NLS.MESSAGES.getMessage("new.datasourcefactory.error", sb.toString(), 
          bundle.getSymbolicName(), bundle.getVersion()), e);
      }
    }
    return sb;
  }

  @Override
  public void modifiedService(ServiceReference reference, Object service) {
    //Updates only matter if they change the value of the driver class
    if(!!!service.toString().equals(reference.getProperty("osgi.jdbc.driver.class"))) {
      
      if(_logger.isDebugEnabled())
        _logger.debug(NLS.MESSAGES.getMessage("changed.datasourcefactory.available", service.toString(), 
            reference.getProperty("osgi.jdbc.driver.class"), bundle.getSymbolicName(), bundle.getVersion()));
      
      //Remove the service
      removedService(reference, service);
      //Clear the old driver class
      StringBuffer sb = (StringBuffer) service;
      sb.delete(0, sb.length());
      //add the new one
      sb.append(addingService(reference));
    }
  }

  @Override
  public void removedService(ServiceReference reference, Object service) {
    
    if(_logger.isDebugEnabled())
      _logger.debug(NLS.MESSAGES.getMessage("datasourcefactory.unavailable", service.toString(), 
          bundle.getSymbolicName(), bundle.getVersion()));
    
    Object[] objects = tracker.getServices();

    boolean gone = true;
    if(objects != null) {
      for(Object o : objects) {
        if(service.equals(o)) {
          gone = false;
          break;
        }
      }
    }
    if(gone) {
      Collection<String> units = dataSourceFactories.remove(service.toString());
      if(units != null) {
        synchronized (this) {
          if(_logger.isInfoEnabled())
            _logger.info(NLS.MESSAGES.getMessage("in.use.datasourcefactory.unavailable", service.toString(), 
                bundle.getSymbolicName(), bundle.getVersion(), units));
          for(String unit : units) {
            unregisterEntityManagerFactory(unit);
          }
        }
      } 
    }
  }
}
