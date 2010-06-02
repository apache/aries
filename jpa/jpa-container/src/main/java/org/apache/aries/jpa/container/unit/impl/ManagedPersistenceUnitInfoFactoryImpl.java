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
package org.apache.aries.jpa.container.unit.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory;
import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactoryListener;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.jpa.transformer.TransformerAgent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ManagedPersistenceUnitInfoFactoryImpl implements ManagedPersistenceUnitInfoFactory {

  private Map<Bundle, PersistenceBundleInfo> map = 
      Collections.synchronizedMap(new HashMap<Bundle, PersistenceBundleInfo>());
    
  public Collection<ManagedPersistenceUnitInfo> createManagedPersistenceUnitMetadata(
      BundleContext containerContext, Bundle persistenceBundle,
      ServiceReference providerReference,
      Collection<ParsedPersistenceUnit> persistenceMetadata) {
    
    // try to get TransformerAgent service
    TransformerAgent agent = null;
    ServiceReference agentReference = containerContext.getServiceReference(TransformerAgent.class.getName());   
    if (agentReference != null) {
        agent = (TransformerAgent) containerContext.getService(agentReference);
    }
    
    Collection<ManagedPersistenceUnitInfo> managedUnits = new ArrayList<ManagedPersistenceUnitInfo>();    
    for (ParsedPersistenceUnit unit : persistenceMetadata) {
      managedUnits.add(new ManagedPersistenceUnitInfoImpl(persistenceBundle, unit, providerReference, agent));
    }
            
    // try to get ManagedPersistenceUnitInfoFactoryListener service
    ManagedPersistenceUnitInfoFactoryListener listener = null;
    ServiceReference listenerReference = getManagedPersistenceUnitInfoFactoryListener(containerContext, providerReference);
    if (listenerReference != null) {
        listener = (ManagedPersistenceUnitInfoFactoryListener) containerContext.getService(listenerReference);
    }
        
    if (listener != null) {
        listener.persistenceUnitMetadataCreated(containerContext, persistenceBundle, providerReference, managedUnits);
    }
    
    PersistenceBundleInfo info = new PersistenceBundleInfo();
    info.managedUnits = managedUnits;
    info.listener = listener;
    info.references.add(agentReference);
    info.references.add(listenerReference);
    
    map.put(persistenceBundle, info);
    
    return managedUnits;
  }
  
  private ServiceReference getManagedPersistenceUnitInfoFactoryListener(BundleContext containerContext, 
                                                                        ServiceReference providerReference) {
     
      if (providerReference != null) {
          String providerName = (String) providerReference.getProperty("javax.persistence.provider");
          if (providerName != null) {
              String filter = "(javax.persistence.provider=" + providerName + ")";
              try {
                  ServiceReference[] refs = containerContext.getServiceReferences(ManagedPersistenceUnitInfoFactoryListener.class.getName(), filter);
                  if (refs != null && refs.length > 0) {
                      return refs[0];
                  }
              } catch (InvalidSyntaxException e) {
                  // should not happen
                  e.printStackTrace();
              }
          }
      }
      
      return containerContext.getServiceReference(ManagedPersistenceUnitInfoFactoryListener.class.getName());
  }
  
  public void destroyPersistenceBundle(BundleContext containerContext, Bundle persistenceBundle) {
      PersistenceBundleInfo info = map.remove(persistenceBundle);
      if (info != null) {
          // destroy units
          for (ManagedPersistenceUnitInfo unit : info.managedUnits) {
              ((ManagedPersistenceUnitInfoImpl) unit).destroy();
          }
          info.managedUnits.clear();
          if (info.listener != null) {
              info.listener.persistenceBundleDestroyed(containerContext, persistenceBundle);
          }
          // unget services
          for (ServiceReference ref : info.references) {
              containerContext.ungetService(ref);
          }
      }
  }

  public String getDefaultProviderClassName() {
      return null;
  }
    
  private static class PersistenceBundleInfo {
      private List<ServiceReference> references = new LinkedList<ServiceReference>();
      private Collection<ManagedPersistenceUnitInfo> managedUnits;
      private ManagedPersistenceUnitInfoFactoryListener listener;
  }
  
  //Code that can be used to attach a fragment for provider wiring
  
////If we can't find a provider then bomb out
//if (providerRef != null)
//{
//  try 
//    FragmentBuilder builder = new FragmentBuilder(b, ".jpa.fragment");
//    builder.addImportsFromExports(providerRef.getBundle());
//    fragment = builder.install(ctx);
//  
//    
//    hostToFragmentMap.put(b, fragment);
//    // If we successfully got a fragment then
//    // set the provider reference and register the units
//    Set<ServiceRegistration> registrations = new HashSet<ServiceRegistration>();
//    Hashtable<String, Object> props = new Hashtable<String, Object>();
//    
//    props.put(PersistenceUnitInfoService.PERSISTENCE_BUNDLE_SYMBOLIC_NAME, b.getSymbolicName());
//    props.put(PersistenceUnitInfoService.PERSISTENCE_BUNDLE_VERSION, b.getVersion());
//    
//    for(PersistenceUnitImpl unit : parsedPersistenceUnits){
//      Hashtable<String, Object> serviceProps = new Hashtable<String, Object>(props);
//      
//      String unitName = (String) unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME);
//      if(unitName != null)
//        serviceProps.put(PersistenceUnitInfoService.PERSISTENCE_UNIT_NAME, unitName);
//      
//      unit.setProviderReference(providerRef);
//      registrations.add(ctx.registerService(PersistenceUnitInfoService.class.getName(), unit, serviceProps));
//    }
//    hostToPersistenceUnitMap.put(b, registrations);
//  }
//  catch (IOException e)
//  {
//    // TODO Fragment generation failed, log the error
//    // No clean up because we didn't register the bundle yet
//    e.printStackTrace();
//  }
//  catch (BundleException be) {
//    //TODO log the failure to install the fragment, but return null
//    // to show we didn't get a fragment installed
//    // No clean up because we didn't register the bundle yet
//  }
//}
//}
//}
  
  //Code that can be used to clear up a persistence unit
  
///**
// * If we have generated a resources for the supplied bundle, then
// * tidy them  up.
// * @param host
// */
//private void tidyUpPersistenceBundle(Bundle host)
//{
//  
//  Bundle fragment = hostToFragmentMap.remove(host);
//  Set<ServiceRegistration> services = hostToPersistenceUnitMap.remove(host);
//  
//  if(services != null) {
//    for(ServiceRegistration reg : services)
//      reg.unregister();
//  }
//  
//  if(fragment != null){
//    try {
//      fragment.uninstall();
//    } catch (BundleException be) {
//      //TODO log this error, then hope that we don't try to
//      //recreate the fragment before restarting the framework!
//    }
//  }
//}
}
