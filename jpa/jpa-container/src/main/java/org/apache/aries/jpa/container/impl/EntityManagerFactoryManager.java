package org.apache.aries.jpa.container.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class EntityManagerFactoryManager {

  private final BundleContext containerContext;
  
  private final Bundle bundle;
  
  private ServiceReference provider;
  
  private Collection<ManagedPersistenceUnitInfo> persistenceUnits;
  
  private Map<String, EntityManagerFactory> emfs = null;
  
  private Collection<ServiceRegistration> registrations = null;

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
   */
  public EntityManagerFactoryManager(BundleContext containerCtx, Bundle b, ServiceReference ref, Collection<ManagedPersistenceUnitInfo> infos) {
    containerContext = containerCtx;
    bundle = b;
    provider = ref;
    persistenceUnits = infos;
  }

  public synchronized boolean providerRemoved(ServiceReference ref) {
    
    boolean toReturn = ref == provider;
    
    if(toReturn)
      destroy();
    
    return toReturn;
  }

  public synchronized void bundleStateChange() throws InvalidPersistenceUnitException {
    
    switch(bundle.getState()) {
      case Bundle.RESOLVED :
        createEntityManagerFactories();
        break;
      case Bundle.STARTING :
      case Bundle.ACTIVE :
        registerEntityManagerFactories();
        break;
      case Bundle.STOPPING :
        unregisterEntityManagerFactories();
        break;
    }
  }

  private void unregisterEntityManagerFactories() {
    if(registrations != null) {
      for(ServiceRegistration reg : registrations) {
        try {
          reg.unregister();
        } catch (Exception e) {
          //TODO log this
        }
      }
      registrations = null;
    }
  }

  private void registerEntityManagerFactories() throws InvalidPersistenceUnitException {
    if(provider != null) {
      if(emfs == null)
        createEntityManagerFactories();
      
      registrations = new ArrayList<ServiceRegistration>();
      String providerName = (String) provider.getProperty("javax.persistence.provider");
      
      for(Entry<String, EntityManagerFactory> entry : emfs.entrySet())
      {
        Properties props = new Properties();
        String unitName = entry.getKey();
        props.put("osgi.unit.name", unitName);
        props.put("osgi.unit.provider", providerName);
        props.put("org.apache.aries.jpa.container.managed", Boolean.TRUE);
        try {
          registrations.add(bundle.getBundleContext().registerService(EntityManagerFactory.class.getCanonicalName(), entry.getValue(), props));
        } catch (Exception e) {
          //TODO log
          throw new InvalidPersistenceUnitException(e);
        }
      }
    }
  }

  private void createEntityManagerFactories() throws InvalidPersistenceUnitException {
    if(provider != null) {
      if(emfs == null) {
        try {
          emfs = new HashMap<String, EntityManagerFactory>();
        
          PersistenceProvider providerService = (PersistenceProvider) containerContext.getService(provider);

          if(providerService == null) throw new InvalidPersistenceUnitException();
      
          for(ManagedPersistenceUnitInfo info : persistenceUnits){
            PersistenceUnitInfo pUnitInfo = info.getPersistenceUnitInfo();
        
            emfs.put(pUnitInfo.getPersistenceUnitName(), 
                providerService.createContainerEntityManagerFactory(
                    pUnitInfo, info.getContainerProperties()));
          }
        } finally {
          containerContext.ungetService(provider);
        }
      }
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
      Collection<ManagedPersistenceUnitInfo> infos)  throws IllegalStateException{
    provider = ref;
    persistenceUnits = infos;
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
  }

  private void destroyEntityManagerFactories() {
    if(registrations != null)
      unregisterEntityManagerFactories();
    if(emfs != null) {
      for(Entry<String, EntityManagerFactory> entry : emfs.entrySet()) {
        try {
          entry.getValue().close();
        } catch (Exception e) {
          //TODO log this error
        }
      }
    }
    emfs = null;
  }


}
