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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.aries.application.VersionRange;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptor;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParser;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParserException;
import org.apache.aries.jpa.container.unit.impl.ManagedPersistenceUnitInfoFactoryImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.util.tracker.BundleTracker;


/**
 * This class locates, parses and manages persistence units defined in OSGi bundles.
 */
public class PersistenceBundleManager extends BundleTracker
{
  /** The bundle context for this bundle */
  private BundleContext ctx = null;
  /** 
   * A map of providers to persistence bundles this is used to guarantee that 
   * when a provider service is removed we can access all of the bundles that
   * might possibly be using it. The map should only ever be accessed when
   * synchronized on {@code this}.
   */
  private final Map<Bundle, EntityManagerFactoryManager> bundleToManagerMap = new HashMap<Bundle, EntityManagerFactoryManager>();
  /** The PersistenceProviders.  */
  private Set<ServiceReference> persistenceProviders = new HashSet<ServiceReference>();
  /** Plug-point for persistence unit providers */
  private ManagedPersistenceUnitInfoFactory persistenceUnitFactory; 
  /** Configuration for this extender */
  private Properties config;

  private static final String DEFAULT_PU_INFO_FACTORY = "";
  
  private static final String DEFAULT_PU_INFO_FACTORY_KEY = "org.apache.aries.jpa.container.PersistenceUnitInfoFactory";
  
  /**
   * Create the extender. Note that it will not start tracking 
   * until the {@code open()} method is called
   * @param ctx The extender bundle's context
   */
  public PersistenceBundleManager(BundleContext ctx) 
  {
	  super(ctx, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING |
			  Bundle.ACTIVE | Bundle.STOPPING, null);
    this.ctx = ctx;
  }
  
  @Override
  public void open() {
    String className = (String) config.get(DEFAULT_PU_INFO_FACTORY_KEY);
    Class<? extends ManagedPersistenceUnitInfoFactory> clazz = null;
    
    if(className != null) {
      try {
        clazz = ctx.getBundle().loadClass(className);
        persistenceUnitFactory = clazz.newInstance();
      } catch (Exception e) {
        // TODO Log the error
        e.printStackTrace();
        //clazz = default;
      }
    }
    
    if(persistenceUnitFactory == null)
      persistenceUnitFactory = new ManagedPersistenceUnitInfoFactoryImpl();
    
    super.open();
  }
  
  
  
//  /**
//   * If we have generated a resources for the supplied bundle, then
//   * tidy them  up.
//   * @param host
//   */
//  private void tidyUpPersistenceBundle(Bundle host)
//  {
//    
//    Bundle fragment = hostToFragmentMap.remove(host);
//    Set<ServiceRegistration> services = hostToPersistenceUnitMap.remove(host);
//    
//    if(services != null) {
//      for(ServiceRegistration reg : services)
//        reg.unregister();
//    }
//    
//    if(fragment != null){
//      try {
//        fragment.uninstall();
//      } catch (BundleException be) {
//        //TODO log this error, then hope that we don't try to
//        //recreate the fragment before restarting the framework!
//      }
//    }
//  }

  public Object addingBundle(Bundle bundle, BundleEvent event) 
  {
    
    if(bundle.getState() == Bundle.ACTIVE) {
      //TODO LOG WARNING HERE
    }
    EntityManagerFactoryManager mgr = null;
    mgr = setupManager(bundle, mgr);
    return mgr;
  }

  private EntityManagerFactoryManager setupManager(Bundle bundle,
    EntityManagerFactoryManager mgr) {
  Collection <PersistenceDescriptor> persistenceXmls = PersistenceBundleHelper.findPersistenceXmlFiles(bundle);

    //If we have no persistence units then our job is done
    if (!!!persistenceXmls.isEmpty()) {
      Collection<ParsedPersistenceUnit> pUnits = new ArrayList<ParsedPersistenceUnit>();
      
      for(PersistenceDescriptor descriptor : persistenceXmls) {
        try {
          pUnits.addAll(PersistenceDescriptorParser.parse(bundle, descriptor));
        } catch (PersistenceDescriptorParserException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      
      if(!!!pUnits.isEmpty()) {
        ServiceReference ref = getProviderServiceReference(pUnits);
          
        Collection<ManagedPersistenceUnitInfo> infos = persistenceUnitFactory.
            createManagedPersistenceUnitMetadata(ctx, bundle, ref, pUnits);
        if(mgr != null)
          mgr.manage(ref, infos);
        else {
          synchronized (this) {
            if(persistenceProviders.contains(ref)) {
                mgr = new EntityManagerFactoryManager(ctx, bundle, ref, infos);
                bundleToManagerMap.put(bundle, mgr);
            }
          }
        }
        if(mgr != null) {
          try {
            mgr.bundleStateChange();
          } catch (InvalidPersistenceUnitException e) {
            // TODO Log this error
            mgr.destroy();
            persistenceUnitFactory.destroyPersistenceBundle(bundle);
          }
        }
      }
    }
    return mgr;
  }
  
  public synchronized void addingProvider(ServiceReference ref)
  {
    persistenceProviders.add(ref);
  }
  
  public void removingProvider(ServiceReference ref)
  {
    Map<Bundle, EntityManagerFactoryManager> mgrs;
    synchronized (this) {
      persistenceProviders.remove(ref);
      mgrs = new HashMap<Bundle, EntityManagerFactoryManager>(bundleToManagerMap);
    }
    for(Entry<Bundle, EntityManagerFactoryManager> entry : mgrs.entrySet()) {
      if(entry.getValue().providerRemoved(ref))
        persistenceUnitFactory.destroyPersistenceBundle(entry.getKey());
    }
  }
  
  public void setConfig(Properties props) {
    config = props;
    URL u = ctx.getBundle().getResource("org.apache.aries.jpa.container.properties");
    
    if(u != null)
      try {
        config.load(u.openStream());
      } catch (IOException e) {
        // TODO Log this error
        e.printStackTrace();
      }
  }
     
//      //If we can't find a provider then bomb out
//      if (providerRef != null)
//      {
//        try 
//          FragmentBuilder builder = new FragmentBuilder(b, ".jpa.fragment");
//          builder.addImportsFromExports(providerRef.getBundle());
//          fragment = builder.install(ctx);
//        
//          
//          hostToFragmentMap.put(b, fragment);
//          // If we successfully got a fragment then
//          // set the provider reference and register the units
//          Set<ServiceRegistration> registrations = new HashSet<ServiceRegistration>();
//          Hashtable<String, Object> props = new Hashtable<String, Object>();
//          
//          props.put(PersistenceUnitInfoService.PERSISTENCE_BUNDLE_SYMBOLIC_NAME, b.getSymbolicName());
//          props.put(PersistenceUnitInfoService.PERSISTENCE_BUNDLE_VERSION, b.getVersion());
//          
//          for(PersistenceUnitImpl unit : parsedPersistenceUnits){
//            Hashtable<String, Object> serviceProps = new Hashtable<String, Object>(props);
//            
//            String unitName = (String) unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME);
//            if(unitName != null)
//              serviceProps.put(PersistenceUnitInfoService.PERSISTENCE_UNIT_NAME, unitName);
//            
//            unit.setProviderReference(providerRef);
//            registrations.add(ctx.registerService(PersistenceUnitInfoService.class.getName(), unit, serviceProps));
//          }
//          hostToPersistenceUnitMap.put(b, registrations);
//        }
//        catch (IOException e)
//        {
//          // TODO Fragment generation failed, log the error
//          // No clean up because we didn't register the bundle yet
//          e.printStackTrace();
//        }
//        catch (BundleException be) {
//          //TODO log the failure to install the fragment, but return null
//          // to show we didn't get a fragment installed
//          // No clean up because we didn't register the bundle yet
//        }
//      }
//    }
//  }


  public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {

    EntityManagerFactoryManager mgr = (EntityManagerFactoryManager) object;
    
    if(event != null && event.getType() == BundleEvent.UPDATED) {
      mgr.destroy();
      persistenceUnitFactory.destroyPersistenceBundle(bundle);
      setupManager(bundle, mgr);
    } else {
      try {
        mgr.bundleStateChange();
      } catch (InvalidPersistenceUnitException e) {
        // TODO log this
        mgr.destroy();
      }
    }
  }

  public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
    EntityManagerFactoryManager mgr = (EntityManagerFactoryManager) object;   
    mgr.destroy();
    persistenceUnitFactory.destroyPersistenceBundle(bundle);
    
    synchronized (this) {
      bundleToManagerMap.remove(bundle);
    }
  }
  

  /**
   * Get a persistence provider from the service registry described by the
   * persistence units defined
   * @param parsedPersistenceUnits
   * @return A service reference or null if no suitable reference is available
   */
  private ServiceReference getProviderServiceReference(Collection<ParsedPersistenceUnit> parsedPersistenceUnits)
  {
    Set<String> ppClassNames = new HashSet<String>();
    List<VersionRange> versionRanges = new ArrayList<VersionRange>();
    //Fill the set of class names and version Filters
    for(ParsedPersistenceUnit unit : parsedPersistenceUnits)
    {
      Map<String, Object> metadata = unit.getPersistenceXmlMetadata();
      String provider = (String) metadata.get(ParsedPersistenceUnit.PROVIDER_CLASSNAME);
      //get providers specified in the persistence units
      if(provider != null && !!!provider.equals(""))
      {
        ppClassNames.add(provider);
        
        Properties props = (Properties) metadata.get(ParsedPersistenceUnit.PROPERTIES);
        
        if(props != null && props.containsKey(ParsedPersistenceUnit.JPA_PROVIDER_VERSION)) {
         
          try {
            String versionRangeString = props.getProperty(ParsedPersistenceUnit.JPA_PROVIDER_VERSION, "0.0.0");
            versionRanges.add(ManifestHeaderProcessor.parseVersionRange(versionRangeString));
          } catch (IllegalArgumentException e) {
            // TODO Log error. This is an invalid range and will be ignored.
            e.printStackTrace();
          }
        }
      }
    }
    //If we have too many provider class names or incompatible version ranges specified then blow up
    
    VersionRange range;
    try {
      range = combineVersionRanges(versionRanges);
    } catch (InvalidRangeCombination e) {
      // TODO Log this error
      e.printStackTrace();
      return null;
    }
    
    if(ppClassNames.size() > 1)
    {
      //TODO log this error then(too many persistence providers specified)
      return null;
    } else {
      //Get the best provider for the given filters
      String provider = (ppClassNames.isEmpty()) ?
          persistenceUnitFactory.getDefaultProviderClassName() : ppClassNames.iterator().next();
          return getBestProvider(provider, range);
    }
  }
 
  private VersionRange combineVersionRanges(List<VersionRange> versionRanges) throws InvalidRangeCombination {

    Version minVersion = new Version(0,0,0);
    Version maxVersion = null;
    boolean minExclusive = false;
    boolean maxExclusive = false;
    
    for(VersionRange range : versionRanges) {
      int minComparison = minVersion.compareTo(range.getMinimumVersion());
      //If minVersion is smaller then we have a new, larger, minimum
      if(minComparison < 0) {
        minVersion = range.getMinimumVersion();
        minExclusive = range.isMinimumExclusive();
      }
      //Only update if it is the same version but more restrictive
      else if(minComparison == 0 && range.isMaximumExclusive())
        minExclusive = true;
    
      if(range.isMaximumUnbounded())
        continue;
      else if (maxVersion == null) {
        maxVersion = range.getMaximumVersion();
        maxExclusive = range.isMaximumExclusive();
      } else {
        int maxComparison = maxVersion.compareTo(range.getMaximumVersion());
        
        //We have a new, lower maximum
        if(maxComparison > 0) {
          maxVersion = range.getMaximumVersion();
          maxExclusive = range.isMaximumExclusive();
          //If the maximum is the same then make sure we set the exclusivity properly
        } else if (maxComparison == 0 && range.isMaximumExclusive())
          maxExclusive = true;
      }
    }
    
    //Now check that we have valid values
    int check = (maxVersion == null) ? -1 : minVersion.compareTo(maxVersion);
    //If min is greater than max, or min is equal to max and one of the exclusive
    //flags is set then we have a problem!
    if(check > 0 || (check == 0 && (minExclusive || maxExclusive))) {
      throw new InvalidRangeCombination(minVersion, minExclusive, maxVersion, maxExclusive);
    }
    
    StringBuilder rangeString = new StringBuilder();
    rangeString.append(minVersion);
    
    if(maxVersion != null) {
      rangeString.insert(0, minExclusive ? "(" : "[");
      rangeString.append(",");
      rangeString.append(maxVersion);
      rangeString.append(maxExclusive ? ")" : "[");
    }
    
    return ManifestHeaderProcessor.parseVersionRange(rangeString.toString());
  }

  /**
   * Locate the best provider for the given criteria
   * @param providerClass
   * @param matchingCriteria
   * @return
   */
  private synchronized ServiceReference getBestProvider(String providerClass, VersionRange matchingCriteria)
  {
    if(!!!persistenceProviders.isEmpty()) {

      List<ServiceReference> refs = new ArrayList<ServiceReference>();
      
      for(ServiceReference reference : persistenceProviders) {
        
        if(providerClass != null && !!!providerClass.equals(
            reference.getProperty("javax.persistence.provider")))
          continue;
          
        if(matchingCriteria.matches(reference.getBundle().getVersion()))
          refs.add(reference);
      }
      
      if(!!!refs.isEmpty()) {
        //Sort the list in DESCENDING ORDER
        Collections.sort(refs, new Comparator<ServiceReference>() {

          //TODO we may wish to use Ranking, then versions for equal ranks
          public int compare(ServiceReference object1, ServiceReference object2)
          {
            Version v1 = object1.getBundle().getVersion();
            Version v2 = object2.getBundle().getVersion();
            return v2.compareTo(v1);
          }
        });
        return refs.get(0);
      } else {
        //TODO no matching providers for matching criteria
      }
    } else {
      //TODO log no matching Providers for impl class
    }
    return null;
  }
}
