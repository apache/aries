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
package org.apache.aries.jpa.container.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.aries.jpa.container.context.impl.PersistenceContextManager;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

public class GlobalPersistenceManager implements PersistenceManager, SynchronousBundleListener {

  private JTAPersistenceContextRegistry registry;
  
  private Map<Bundle, PersistenceContextManager> managers = 
    new WeakHashMap<Bundle, PersistenceContextManager>();
  
  private Map<Bundle, Set<String>> persistenceContexts = 
    new HashMap<Bundle, Set<String>>();
  

  public void setRegistry(JTAPersistenceContextRegistry registry) {
    this.registry = registry;
  }
  
  public void registerContext(String unitName, Bundle client, HashMap<String, Object> properties) {
    Bundle frameworkBundle = client.getBundleContext().getBundle(0);
    PersistenceContextManager manager = null;
    
    synchronized (this) {
      if (managers.containsKey(frameworkBundle)) {
        manager = managers.get(frameworkBundle);
      } else {
        manager = new PersistenceContextManager(frameworkBundle.getBundleContext(), registry);
        manager.open();
        managers.put(frameworkBundle, manager);
        frameworkBundle.getBundleContext().addBundleListener(this);
      }
      
      if (!persistenceContexts.containsKey(client)) {
        persistenceContexts.put(client, new HashSet<String>());
      }
      
      persistenceContexts.get(client).add(unitName);
    }
    
    manager.registerContext(unitName, client, properties);
  }

  public void bundleChanged(BundleEvent event) {
    Bundle bundle = event.getBundle();
    
    if (event.getType() == event.STOPPING) {
      Set<String> contextsToBeRemoved = Collections.emptySet();
      Bundle frameworkBundle = bundle.getBundleContext().getBundle(0);
      PersistenceContextManager manager = null;
      
      synchronized (this) {
        if (persistenceContexts.containsKey(bundle)) {
          contextsToBeRemoved = persistenceContexts.get(bundle);
          persistenceContexts.remove(bundle);
          
          manager = managers.get(frameworkBundle);
          if (manager == null)
            throw new IllegalStateException();
        }        
      }
      
      for (String context : contextsToBeRemoved) {
        manager.unregisterContext(context, bundle);
      }
    }
  }

}
