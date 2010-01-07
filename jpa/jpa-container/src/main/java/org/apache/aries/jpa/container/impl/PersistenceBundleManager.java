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

import java.util.Collection;

import org.apache.aries.jpa.container.parsing.PersistenceDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;


/**
 * This class locates, parses and manages persistence units defined in OSGi bundles.
 */
public class PersistenceBundleManager extends BundleTracker
{
  /** The bundle context for this bundle */
  private BundleContext ctx = null;
  /** A map of bundles to generated fragments */
//  private final ConcurrentMap<Bundle, Bundle> hostToFragmentMap = new ConcurrentHashMap<Bundle, Bundle>();
//  /** A map of persistence bundles to sets of persistence metadata */
//  private final ConcurrentMap<Bundle, Set<ServiceRegistration>> hostToPersistenceUnitMap = new ConcurrentHashMap<Bundle, Set<ServiceRegistration>>();
  //TODO pull this from config
  /** The default JPA provider to use */
  public static final String DEFAULT_JPA_PROVIDER ="org.apache.openjpa.persistence.PersistenceProviderImpl";

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

    Collection <PersistenceDescriptor> persistenceXmls = PersistenceBundleHelper.findPersistenceXmlFiles(bundle);

    //If we have no persistence units then our job is done
    if (!!!persistenceXmls.isEmpty()) {
      //TODO parse these
    }
    return null;
  }
//      //Get the persistence units defined, and a provider for them to use
//      Collection<PersistenceUnitImpl> parsedPersistenceUnits = parseXmlFiles(persistenceXmls, b);
//      ServiceReference providerRef = getProviderServiceReference(parsedPersistenceUnits);
//      
//      //If we can't find a provider then bomb out
//      if (providerRef != null)
//      {
//        try {
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
      // TODO Auto-generated method stub
      
  }

  public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
      // TODO Auto-generated method stub
      
  }
  

  /**
   * Get a persistence provider from the service registry described by the
   * persistence units defined
   * @param parsedPersistenceUnits
   * @return A service reference or null if no suitable reference is available
   */
//  private ServiceReference getProviderServiceReference(Collection<PersistenceUnitImpl> parsedPersistenceUnits)
//  {
//    Set<String> ppClassNames = new HashSet<String>();
//    Set<Filter> versionFilters = new HashSet<Filter>();
//    //Fill the set of class names and version Filters
//    for(PersistenceUnitImpl unit : parsedPersistenceUnits)
//    {
//      Map<String, Object> metadata = unit.getPersistenceXmlMetadata();
//      String provider = (String) metadata.get(PersistenceUnitInfoService.PROVIDER_CLASSNAME);
//      //get providers specified in the persistence units
//      if(provider != null && !!!provider.equals(""))
//      {
//        ppClassNames.add(provider);
//        
//        Properties props = (Properties) metadata.get(PersistenceUnitInfoService.PROPERTIES);
//        
//        if(props != null && props.containsKey(PersistenceUnitInfoService.JPA_PROVIDER_VERSION)) {
//         
//          try {
//            Filter f = getFilter(props.getProperty(PersistenceUnitInfoService.JPA_PROVIDER_VERSION, "0.0.0"));
//            versionFilters.add(f);
//          } catch (InvalidSyntaxException e) {
//            // TODO Log error and ignore, This should never happen
//            e.printStackTrace();
//          }
//        }
//      }
//    }
//    
//    //If we have too many provider class names specified then blow up
//    if(ppClassNames.size() > 1)
//    {
//      //TODO log this error (too many persistence providers specified)
//    } else {
//      //Get the best provider for the given filters
//      String provider = (ppClassNames.isEmpty()) ?
//          DEFAULT_JPA_PROVIDER : ppClassNames.iterator().next();
//          return getBestProvider(provider, versionFilters);
//    }
//    return null;
//  }
 
//  /**
//   * Locate the best provider for the given criteria
//   * @param providerClass
//   * @param matchingCriteria
//   * @return
//   */
//  private ServiceReference getBestProvider(String providerClass, Set<Filter> matchingCriteria)
//  {
//    ServiceReference[] array = null;
//    try {
//      array = ctx.getAllServiceReferences(providerClass, null);
//    } catch (InvalidSyntaxException e) {
//      //TODO this can never happen
//    }
//    
//    if(array != null) {
//      //A linked list is faster for large numbers of ServiceReferences
//      //Note we cannot use Arrays.asList() as we need to remove items
//      //via an iterator, and this would throw UnsupportedOperationException.
//      List<ServiceReference> refs = new LinkedList<ServiceReference>();
//      
//      for(ServiceReference reference : array)
//        refs.add(reference);
//      
//      Iterator<ServiceReference> it = refs.iterator();
//      
//      //Remove anything that doesn't match the filter
//      while(it.hasNext())
//      {
//        ServiceReference ref = it.next();
//        for(Filter f : matchingCriteria)
//        {
//          if(!!!f.match(ref)) {
//            it.remove();
//            break;
//          }
//        }
//      }
//      
//      if(!!!refs.isEmpty()) {
//        //Sort the list in DESCENDING ORDER
//        Collections.sort(refs, new Comparator<ServiceReference>() {
//
//          //TODO we may wish to use Ranking, then versions for equal ranks
//          public int compare(ServiceReference object1, ServiceReference object2)
//          {
//            Version v1 = object1.getBundle().getVersion();
//            Version v2 = object2.getBundle().getVersion();
//            return v2.compareTo(v1);
//          }
//        });
//        return refs.get(0);
//      } else {
//        //TODO no matching providers for matching criteria
//      }
//    } else {
//      //TODO log no matching Providers for impl class
//    }
//    
//    return null;
//  }
//  
//  /**
//   * Create a filter for the supplied version range string
//   * @param providerVersion
//   * @return
//   * @throws InvalidSyntaxException
//   */
//  private Filter getFilter(String providerVersion)
//      throws InvalidSyntaxException
//  {
//    String toReturn = null;
//    
//    //TODO NLS enable the messages in the exceptions below (Invalid version range specified...)
//    //Create a filter to match the required provider version range
//    if(providerVersion != null) {
//      if(!!!providerVersion.contains(","))
//        toReturn = ("(osgi.jpa.provider.version>=" + providerVersion + ")");
//      else {
//        String[] versionArray = providerVersion.split(",");
//        
//        if(versionArray.length == 2) {
//          
//          versionArray[0] = versionArray[0].trim();
//          versionArray[1] = versionArray[1].trim();
//          
//          char bracket1 = versionArray[0].charAt(0);
//          char bracket2 = versionArray[1].charAt(versionArray[1].length() - 1);
//          
//          String version1 = versionArray[0].substring(1);
//          String version2 = versionArray[1].substring(0, versionArray[1].length() -1);
//
//          if(version1.compareTo(version2) > 0)
//            throw new InvalidSyntaxException("Invalid version range specified. " + providerVersion, providerVersion);
//          
//          String compare1 = "(osgi.jpa.provider.version>=" + version1 + ")";
//          String compare2 = "(osgi.jpa.provider.version<=" + version2 + ")";
//          
//          if('(' == bracket1)
//             compare1 = compare1 + "(!(osgi.jpa.provider.version=" + version1 + "))";
//          else if('[' != bracket1) throw new InvalidSyntaxException("Invalid version range specified. " + providerVersion, providerVersion);
//          
//
//          if(')' == bracket2)
//            compare2 = compare2 + "(!(osgi.jpa.provider.version=" + version2 + "))";
//          else if(']' != bracket2) throw new InvalidSyntaxException("Invalid version range specified. " + providerVersion, providerVersion);
//         
//         
//          toReturn = "(&" + compare1 + compare2 + ")";
//        } else throw new InvalidSyntaxException("Invalid version range specified. " + providerVersion, providerVersion);
//        
//      }
//    }
//    return FrameworkUtil.createFilter(toReturn);
//  }
}
