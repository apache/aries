/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.context.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that coordinates PersistenceContextManagers across multiple (nested) OSGi frameworks.
 */
public class GlobalPersistenceManager implements PersistenceContextProvider, SynchronousBundleListener {
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container.context");
  
  private JTAPersistenceContextRegistry registry;
  
  /** 
   *    The list of persistence context managers. Each is valid for exactly one framework
   *    as identified by the respective system bundle. This allows us to work properly in 
   *    a multi (nested) framework environment without using the (deprecated) CompositeBundle API.
   */
  private Map<Bundle, PersistenceContextManager> managers = 
    new HashMap<Bundle, PersistenceContextManager>();
  
  /**
   *  This Map relates persistence context clients to persistence contexts. A bundle may be
   *  a client of more than one persistence context. 
   */
  private Map<Bundle, Set<String>> persistenceContexts = 
    new HashMap<Bundle, Set<String>>();
  

  public void setRegistry(JTAPersistenceContextRegistry registry) {
    this.registry = registry;
  }
  
  public void registerContext(String unitName, Bundle client, HashMap<String, Object> properties) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Registering bundle {} as a client of persistence unit {} with properties {}.", 
          new Object[] {client.getSymbolicName() + "_" + client.getVersion(), unitName, properties});
    }
    
    if(!!!registry.jtaIntegrationAvailable())
      _logger.warn("No JTA integration is currently available. The managed persistence context {} used by the bundle {} will operate " +
      		"with no transaction context and be read only until a JTA Transaction Services implementation is available in" +
      		"the runtime", new Object[] {unitName, client.getSymbolicName() + "_" + client.getVersion()});
    
    //Find the framework for this bundle (we may be in a composite)
    Bundle frameworkBundle = client.getBundleContext().getBundle(0);
    PersistenceContextManager manager = null;
    boolean newManager = false;
    
    //Synchronize to update internal state atomically
    synchronized (this) {
      //If we already have a manager use it
      if (managers.containsKey(frameworkBundle)) {
        manager = managers.get(frameworkBundle);
      } else {
        if (_logger.isDebugEnabled()) {
          _logger.debug("No existing manager for the framework with identity hash code {}. Creating a new one.",
              new Object[] {System.identityHashCode(frameworkBundle)});
        }
        manager = new PersistenceContextManager(frameworkBundle.getBundleContext(), registry);
        managers.put(frameworkBundle, manager);
        //Remember to listen to this new framework so that we know when bundles are starting/stopping
        frameworkBundle.getBundleContext().addBundleListener(this);
        
        newManager = true;
      }
      
      //Register the new bundle as a client
      if (!persistenceContexts.containsKey(client)) {
        persistenceContexts.put(client, new HashSet<String>());
      }
      
      persistenceContexts.get(client).add(unitName);
    }
    //Remember to start the manager if it was new. This MUST occur outside the synchronized block.
    if (newManager)
      manager.open();
    
    manager.registerContext(unitName, client, properties);
  }

  /**
   * This method is used to track the lifecycle of bundles inside composites
   */
  public void bundleChanged(BundleEvent event) {
    Bundle bundle = event.getBundle();
    
    //We only care about bundles stopping
    if (event.getType() == BundleEvent.STOPPING) {
      Set<String> contextsToBeRemoved = Collections.emptySet();
      Bundle frameworkBundle = bundle.getBundleContext().getBundle(0);
      PersistenceContextManager manager = null;
      boolean removeManager = false;
      
      //Synchronize to update internal state atomically
      synchronized (this) {
        if (persistenceContexts.containsKey(bundle)) {
          //This is a client, find the contexts to remove
          contextsToBeRemoved = persistenceContexts.remove(bundle);
          
          if (_logger.isDebugEnabled()) {
            _logger.debug("The bundle {} in framework {}, which is a client of the persistence contexts {} is stopping.",
                new Object[] {bundle.getSymbolicName() + "_" + bundle.getVersion(), 
                System.identityHashCode(frameworkBundle), contextsToBeRemoved});
          }
          
          manager = managers.get(frameworkBundle);
          if (manager == null) {
              _logger.error("There was no context manager for framework {}. This should never happen");
            throw new IllegalStateException();
          }
        } else if (managers.containsKey(bundle)) {
          //The framework is stopping, tidy up the manager
          if (_logger.isDebugEnabled()) {
            _logger.debug("The framework {} is stopping.",
                new Object[] {bundle.getSymbolicName() + "_" + bundle.getVersion(), 
                System.identityHashCode(frameworkBundle)});
          }
          removeManager = true;
          managers.remove(bundle);
          bundle.getBundleContext().removeBundleListener(this);
        }
      }
      
      if (removeManager) {
        manager.close();
      } else {
        for (String context : contextsToBeRemoved) {
          manager.unregisterContext(context, bundle);
        }
      }
    }
  }

}
