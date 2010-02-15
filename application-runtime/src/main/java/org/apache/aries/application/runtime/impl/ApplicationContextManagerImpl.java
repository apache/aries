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

package org.apache.aries.application.runtime.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.application.management.ApplicationContext;
import org.apache.aries.application.management.ApplicationContextManager;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.ApplicationContext.ApplicationState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class ApplicationContextManagerImpl implements ApplicationContextManager {

  private ConcurrentMap<AriesApplication, ApplicationContextImpl> _appToContextMap;
  private BundleContext _bundleContext;
  
  public ApplicationContextManagerImpl () { 
    _appToContextMap = new ConcurrentHashMap<AriesApplication, ApplicationContextImpl>();
  }
  
  public void setBundleContext (BundleContext b) { 
    _bundleContext = b;
  }
  
  public ApplicationContext getApplicationContext(AriesApplication app) throws BundleException, ManagementException {
    ApplicationContextImpl result;
    if (_appToContextMap.containsKey(app)) { 
      result = _appToContextMap.get(app);
    } else { 
      result = new ApplicationContextImpl (_bundleContext, app);
      ApplicationContextImpl previous = _appToContextMap.putIfAbsent(app, result);
      if (previous != null) { 
        result = previous;
      }
    }
    return result;
  }

  public Set<ApplicationContext> getApplicationContexts() {
    Set<ApplicationContext> result = new HashSet<ApplicationContext>();
    for (Map.Entry<AriesApplication, ApplicationContextImpl> entry: _appToContextMap.entrySet()) {
      result.add (entry.getValue());
    }
    return result;
  }

  public void remove(ApplicationContext app)
  {
    Iterator<Map.Entry<AriesApplication, ApplicationContextImpl>> it = _appToContextMap.entrySet().iterator();
    
    while (it.hasNext()) {
      Map.Entry<AriesApplication, ApplicationContextImpl> entry = it.next();
      
      ApplicationContextImpl potentialMatch = entry.getValue();
      
      if (potentialMatch == app) {
        it.remove();
        
        potentialMatch.setState(ApplicationState.UNINSTALLED);
        
        break;
      }
    }
  }
}