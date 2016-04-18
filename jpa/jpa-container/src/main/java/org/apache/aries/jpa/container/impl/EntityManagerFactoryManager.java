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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.jpa.container.quiesce.impl.DestroyCallback;
import org.apache.aries.jpa.container.quiesce.impl.EMFProxyFactory;
import org.apache.aries.jpa.container.quiesce.impl.NamedCallback;
import org.apache.aries.jpa.container.quiesce.impl.QuiesceEMF;
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
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class manages the lifecycle of Persistence Units and their associated
 * {@link EntityManagerFactory} objects.
 */
@SuppressWarnings({
    "unchecked", "rawtypes"
})
public class EntityManagerFactoryManager implements ServiceTrackerCustomizer {

  public static final String DATA_SOURCE_NAME = "org.apache.aries.jpa.data.source.name";

  public static final String DATA_SOURCE_NAME_JTA = "org.apache.aries.jpa.data.source.name.jta";

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
  private Map<String, EntityManagerFactory> emfs = null;
  /** The {@link ServiceRegistration} objects for the {@link EntityManagerFactory}s */
  private ConcurrentMap<String, ServiceRegistration> registrations = null;
  /** Quiesce this Manager */
  private boolean quiesce = false;
  
  private volatile ServiceTracker jndiTracker;
  private volatile ServiceTracker dataSourceFactoriesTracker;

  /** DataSourceFactories in use by persistence units in this bundle - class name key to collection of unit values */
  private final ConcurrentMap<String, Collection<String>> dataSourceFactories =
         new ConcurrentHashMap<String, Collection<String>>();

  private final ConcurrentMap<ServiceReference, Collection<String>> jndiServices =
         new ConcurrentHashMap<ServiceReference, Collection<String>>();

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
   * @param containerCtx
   * @param b
   */
  public EntityManagerFactoryManager(BundleContext containerCtx, Bundle b) {
    containerContext = containerCtx;
    bundle = b;
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
  public synchronized boolean providerRemoved(ServiceReference ref) 
  {
    boolean toReturn = false;
    if (provider != null) {
    	toReturn = provider.equals(ref);
    }
    
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
        break;
        //Starting and active both require EMFs to be registered
      case Bundle.STARTING :
      case Bundle.ACTIVE :
        if(dataSourceFactoriesTracker == null) {
          dataSourceFactoriesTracker = new ServiceTracker(bundle.getBundleContext(),
              "org.osgi.service.jdbc.DataSourceFactory", this);
          dataSourceFactoriesTracker.open();
        }
        if(jndiTracker == null) {
          try {
            jndiTracker = new ServiceTracker(bundle.getBundleContext(),
                    FrameworkUtil.createFilter("(osgi.jndi.service.name=*)"), this);
            jndiTracker.open();
          } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
          }
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> result = executor.submit(new Callable<Void>() {
            
            @Override
            public Void call() throws InvalidPersistenceUnitException {
                registerEntityManagerFactories();
                return null;
            }
        });
        executor.shutdown();
        handleCreationResult(result);
        break;
        //Stopping means the EMFs should
      case Bundle.STOPPING :
        //If we're stopping we no longer need to be quiescing
        quiesce = false;
        if(jndiTracker != null) {
          jndiTracker.close();
          jndiTracker = null;
        }
        if(dataSourceFactoriesTracker != null) {
          dataSourceFactoriesTracker.close();
          dataSourceFactoriesTracker = null;
        }
        unregisterEntityManagerFactories();
        break;
      case Bundle.INSTALLED :
        //Destroy everything
        destroyEntityManagerFactories();
    }
  }

    private void handleCreationResult(Future<Void> result) throws InvalidPersistenceUnitException {
        try {
            result.get(5000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            _logger.warn(e.getMessage(), e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InvalidPersistenceUnitException) {
                throw (InvalidPersistenceUnitException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
        } catch (TimeoutException e) {
            _logger.info("EntityManagerFactory creation takes long. Continuing in background", e);
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
        clearQuiesce(emfs.get(entry.getKey()));
        persistenceUnits.get(entry.getKey()).unregistered();
      }
      // remember to set registrations to be null
      registrations = null;
    }
  }


  private void unregisterEntityManagerFactory(String unit) {
    if(registrations != null) {
      AriesFrameworkUtil.safeUnregisterService(registrations.remove(unit));
      clearQuiesce(emfs.get(unit));
      persistenceUnits.get(unit).unregistered();
    }
  }
  
  private void clearQuiesce(EntityManagerFactory emf) {
      if (emf instanceof QuiesceEMF) {
          ((QuiesceEMF) emf).clearQuiesce();
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
      for(Entry<String, EntityManagerFactory> entry : emfs.entrySet())
      {
        
        Hashtable<String,Object> props = new Hashtable<String, Object>();
        String unitName = entry.getKey();
        
        if(registrations.containsKey(unitName) ||
                !!!availableDataSourceFactory(unitName) ||
                !!!availableJndiService(unitName))
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

  boolean availableJndiService(String unitName) {
    ManagedPersistenceUnitInfo mpui = persistenceUnits.get(unitName);

    String dsName = (String) mpui.getContainerProperties().get(DATA_SOURCE_NAME);
    if (dsName != null && dsName.startsWith("osgi:service/")) {
      return isJndiServiceAvailable(unitName, dsName);
    }

    String jtaDsName = (String) mpui.getContainerProperties().get(DATA_SOURCE_NAME_JTA);
    if (jtaDsName != null && jtaDsName.startsWith("osgi:service/")) {
      return isJndiServiceAvailable(unitName, jtaDsName);
    }

    return true;
  }

  private boolean isJndiServiceAvailable(String unitName, String jndi) {
    OsgiName osgi = new OsgiName(jndi);
    String filter;
    if (osgi.isInterfaceNameBased()) {
      String interfaceName = osgi.getInterface();
      if (osgi.hasFilter()) {
        filter = "(&(objectClass=" + interfaceName + ")" + osgi.getFilter() + ")";
      } else {
        filter = "(objectClass=" + interfaceName + ")";
      }
    } else {
      String serviceName = osgi.getServiceName();
      filter = "(osgi.jndi.service.name=" + serviceName + ")";
    }
    try {
      Filter flt = FrameworkUtil.createFilter(filter);
      for (ServiceReference ref : jndiServices.keySet()) {
        if (flt.match(ref)) {
          jndiServices.get(ref).add(unitName);
          if (_logger.isDebugEnabled())
            _logger.debug(NLS.MESSAGES.getMessage("jndiservice.found", unitName, bundle.getSymbolicName(),
                    bundle.getVersion(), jndi));
          return true;
        }
      }
    } catch (InvalidSyntaxException e) {
      // Ignore
    }
    _logger.debug(NLS.MESSAGES.getMessage("jndiservice.not.found", unitName, bundle.getSymbolicName(),
            bundle.getVersion(), jndi));
    return false;
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
      emfs = new HashMap<String, EntityManagerFactory>();
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
          EntityManagerFactory emfProxy = EMFProxyFactory.createProxy(emf, unitName);
          emfs.put(unitName, emfProxy);
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
    if(jndiTracker != null) {
      jndiTracker.close();
      jndiTracker = null;
    }
    if(dataSourceFactoriesTracker != null) {
      dataSourceFactoriesTracker.close();
      dataSourceFactoriesTracker = null;
    }
  }

  /**
   * S
   */
  private void destroyEntityManagerFactories() {
    if(registrations != null)
      unregisterEntityManagerFactories();
    if(emfs != null) {
      for(Entry<String, EntityManagerFactory> entry : emfs.entrySet()) {
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
    Map<EntityManagerFactory, ServiceRegistration> entries = new HashMap<EntityManagerFactory, ServiceRegistration>();
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
      for(Entry<EntityManagerFactory, ServiceRegistration> entry : entries.entrySet()) {
        quiesce(entry.getKey(), callback, entry.getValue());
      }
    }
  }
  
  private void quiesce(EntityManagerFactory emf, NamedCallback callback, ServiceRegistration reg) {
      if (emf instanceof QuiesceEMF) {
          ((QuiesceEMF) emf).quiesce(callback, reg);
      }
  }
  
  @Override
  public StringBuffer addingService(ServiceReference reference) {
    Object driverClass = reference.getProperty("osgi.jdbc.driver.class");
    if (driverClass != null) {
      //Use String.valueOf to save us from nulls
      StringBuffer sb = new StringBuffer(String.valueOf(reference.getProperty("osgi.jdbc.driver.class")));

      //Only notify of a potential change if a new data source class is available
      if (dataSourceFactories.putIfAbsent(sb.toString(), new ArrayList<String>()) == null) {
        if (_logger.isDebugEnabled())
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
    else
    {
      Object jndiName = reference.getProperty("osgi.jndi.service.name");
      if (jndiName != null) {
        StringBuffer sb = new StringBuffer(String.valueOf(jndiName));
        if (jndiServices.putIfAbsent(reference, new ArrayList<String>()) == null) {
          if (_logger.isDebugEnabled())
            _logger.debug(NLS.MESSAGES.getMessage("new.jndiservice.available", sb.toString(),
                  bundle.getSymbolicName(), bundle.getVersion()));
          try {
            bundleStateChange();
          } catch (InvalidPersistenceUnitException e) {
            //Not much we can do here unfortunately
            _logger.warn(NLS.MESSAGES.getMessage("new.jndiservice.error", sb.toString(),
                  bundle.getSymbolicName(), bundle.getVersion()), e);
          }
        }
        return sb;
      }
      else {
        throw new IllegalStateException();
      }
    }
  }

  @Override
  public void modifiedService(ServiceReference reference, Object service) {
    if (reference.getProperty("osgi.jdbc.driver.class") != null) {
      //Updates only matter if they change the value of the driver class
      if(!!!service.toString().equals(reference.getProperty("osgi.jdbc.driver.class"))) {

        if (_logger.isDebugEnabled())
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
    else if (reference.getProperty("osgi.jndi.service.name") != null) {
      //Updates only matter if they change the value of the jndi name
      if (!!!service.toString().equals(reference.getProperty("osgi.jndi.service.name"))) {

        if (_logger.isDebugEnabled())
          _logger.debug(NLS.MESSAGES.getMessage("changed.jndiservice.available", service.toString(),
                  reference.getProperty("osgi.jndi.service.name"), bundle.getSymbolicName(), bundle.getVersion()));

        //Remove the service
        removedService(reference, service);
        //Clear the old driver class
        StringBuffer sb = (StringBuffer) service;
        sb.delete(0, sb.length());
        //add the new one
        sb.append(addingService(reference));
      }
    }
 }

  @Override
  public void removedService(ServiceReference reference, Object service) {
    if (reference.getProperty("osgi.jdbc.driver.class") != null) {
      if (_logger.isDebugEnabled())
        _logger.debug(NLS.MESSAGES.getMessage("datasourcefactory.unavailable", service.toString(),
                bundle.getSymbolicName(), bundle.getVersion()));

      Object[] objects = dataSourceFactoriesTracker.getServices();

      boolean gone = true;
      if (objects != null) {
        for (Object o : objects) {
          if (service.equals(o)) {
            gone = false;
            break;
          }
        }
      }
      if (gone) {
        Collection<String> units = dataSourceFactories.remove(service.toString());
        if (units != null) {
          synchronized (this) {
            if (_logger.isInfoEnabled())
              _logger.info(NLS.MESSAGES.getMessage("in.use.datasourcefactory.unavailable", service.toString(),
                      bundle.getSymbolicName(), bundle.getVersion(), units));
            for (String unit : units) {
              unregisterEntityManagerFactory(unit);
            }
          }
        }
      }
    }
    else if (reference.getProperty("osgi.jndi.service.name") != null) {
      if (_logger.isDebugEnabled())
        _logger.debug(NLS.MESSAGES.getMessage("jndiservice.unavailable", service.toString(),
                bundle.getSymbolicName(), bundle.getVersion()));

      Object[] objects = jndiTracker.getServices();

      boolean gone = true;
      if (objects != null) {
        for (Object o : objects) {
          if (service.equals(o)) {
            gone = false;
            break;
          }
        }
      }
      if (gone) {
        Collection<String> units = jndiServices.remove(service.toString());
        if (units != null) {
          synchronized (this) {
            if (_logger.isInfoEnabled())
              _logger.info(NLS.MESSAGES.getMessage("in.use.jndiservice.unavailable", service.toString(),
                      bundle.getSymbolicName(), bundle.getVersion(), units));
            for (String unit : units) {
              unregisterEntityManagerFactory(unit);
            }
          }
        }
      }
    }
  }

  static class OsgiName extends CompositeName {
    public static final String OSGI_SCHEME = "osgi";
    public static final String ARIES_SCHEME = "aries";
    public static final String SERVICE_PATH = "service";
    public static final String SERVICES_PATH = "services";
    public static final String SERVICE_LIST_PATH = "servicelist";
    public static final String FRAMEWORK_PATH = "framework";

    public OsgiName(String name)
    {
      super(split(name));
    }

    public boolean hasFilter()
    {
      return size() == 3;
    }

    public boolean isServiceNameBased()
    {
      return !isInterfaceNameBased();
    }

    public boolean isInterfaceNameBased()
    {
      if (size() < 2 || size() > 3) {
        return false;
      }
      String itf = get(1);
      if (!itf.matches("[a-zA-Z_$0-9]+(\\.[a-zA-Z_$0-9]+)+")) {
        return false;
      }
      if (size() == 3) {

      }
      return true;
    }

    public String getInterface()
    {
      return get(1);
    }

    public String getFilter()
    {
      return hasFilter() ? get(2) : null;
    }

    public String getServiceName()
    {
      Enumeration<String> parts = getAll();
      parts.nextElement();

      StringBuilder builder = new StringBuilder();

      if (parts.hasMoreElements()) {

        while (parts.hasMoreElements()) {
          builder.append(parts.nextElement());
          builder.append('/');
        }

        builder.deleteCharAt(builder.length() - 1);
      }

      return builder.toString();
    }

    public boolean hasInterface()
    {
      return size() > 1;
    }

    protected static Enumeration<String> split(String name)
    {
      List<String> elements = new ArrayList<String>();

      StringBuilder builder = new StringBuilder();

      int len = name.length();
      int count = 0;

      for (int i = 0; i < len; i++) {
        char c = name.charAt(i);

        if (c == '/' && count == 0) {
          elements.add(builder.toString());
          builder = new StringBuilder();
          continue;
        } else if (c == '(') count++;
        else if (c == ')') count++;

        builder.append(c);
      }

      elements.add(builder.toString());

      return Collections.enumeration(elements);
    }

    public String getScheme()
    {
      String part0 = get(0);
      int index = part0.indexOf(':');
      if (index > 0) {
        return part0.substring(0, index);
      } else {
        return null;
      }
    }

    public String getSchemePath()
    {
      String part0 = get(0);
      int index = part0.indexOf(':');

      String result;

      if (index > 0) {
        result = part0.substring(index + 1);
      } else {
        result = null;
      }

      return result;
    }
  }

}
