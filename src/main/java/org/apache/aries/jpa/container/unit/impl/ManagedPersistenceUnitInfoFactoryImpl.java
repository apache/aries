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

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ManagedPersistenceUnitInfoFactoryImpl implements
    ManagedPersistenceUnitInfoFactory {

  public Collection<ManagedPersistenceUnitInfo> createManagedPersistenceUnitMetadata(
      BundleContext containerContext, Bundle persistenceBundle,
      ServiceReference providerReference,
      Collection<ParsedPersistenceUnit> persistenceMetadata) {
    
    //TODO add support for provider bundle imports (e.g. for weaving) here
    
    Collection<ManagedPersistenceUnitInfo> managedUnits = new ArrayList<ManagedPersistenceUnitInfo>();
    
    for(ParsedPersistenceUnit unit : persistenceMetadata)
      managedUnits.add(new ManagedPersistenceUnitInfoImpl(persistenceBundle, unit, providerReference));
    
    return managedUnits;
  }

  public void destroyPersistenceBundle(BundleContext containerContext, Bundle bundle) {

  }

  public String getDefaultProviderClassName() {
    return null;
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
