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

package org.apache.aries.application.runtime.isolated.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.AriesApplicationContext.ApplicationState;
import org.apache.aries.application.management.spi.framework.BundleFrameworkManager;
import org.apache.aries.application.management.spi.repository.BundleRepositoryManager;
import org.apache.aries.application.management.spi.runtime.AriesApplicationContextManager;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationContextManagerImpl implements AriesApplicationContextManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextManagerImpl.class);
  
  private ConcurrentMap<AriesApplication, ApplicationContextImpl> _appToContextMap;
  private BundleFrameworkManager _bundleFrameworkManager;  
  private BundleRepositoryManager _bundleRepositoryManager;

  public ApplicationContextManagerImpl()
  {
    LOGGER.debug(LOG_ENTRY, "ApplicationContextImpl");
    
    _appToContextMap = new ConcurrentHashMap<AriesApplication, ApplicationContextImpl>();
    
    LOGGER.debug(LOG_EXIT, "ApplicationContextImpl", this);
  }

  public void setBundleFrameworkManager(BundleFrameworkManager bfm)
  {
    LOGGER.debug(LOG_ENTRY, "setBundleFrameworkManager", bfm);
    LOGGER.debug(LOG_EXIT, "setBundleFrameworkManager");
    
    _bundleFrameworkManager = bfm;
  }
  
  public void setBundleRepositoryManager(BundleRepositoryManager brm)
  {
    LOGGER.debug(LOG_ENTRY, "setBundleRepositoryManager", brm);
    LOGGER.debug(LOG_EXIT, "setBundleRepositoryManager");
    
    this._bundleRepositoryManager = brm;
  }
  
  public BundleRepositoryManager getBundleRepositoryManager()
  {
    LOGGER.debug(LOG_ENTRY, "getBundleRepositoryManager");
    LOGGER.debug(LOG_EXIT, "getBundleRepositoryManager", _bundleRepositoryManager);
    
    return _bundleRepositoryManager;
  }
  
  public synchronized AriesApplicationContext getApplicationContext(AriesApplication app)
      throws BundleException, ManagementException
  {
    LOGGER.debug(LOG_ENTRY, "getApplicationContext", app);
        
    ApplicationContextImpl result;
    if (_appToContextMap.containsKey(app)) {
      result = _appToContextMap.get(app);
    } else {
      result = new ApplicationContextImpl(app, this);
      ApplicationContextImpl previous = _appToContextMap.putIfAbsent(app, result);
      if (previous != null) {
        result = previous;
      }
    }
    
    LOGGER.debug(LOG_EXIT, "getApplicationContext", result);
    
    return result;
  }

  public synchronized Set<AriesApplicationContext> getApplicationContexts()
  {
    LOGGER.debug(LOG_ENTRY, "getApplicationContexts");
    
    Set<AriesApplicationContext> result = new HashSet<AriesApplicationContext>();
    for (Map.Entry<AriesApplication, ApplicationContextImpl> entry : _appToContextMap.entrySet()) {
      result.add(entry.getValue());
    }
    
    LOGGER.debug(LOG_EXIT, "getApplicationContexts", result);
    
    return result;
  }

  public synchronized void remove(AriesApplicationContext app)
  {
    LOGGER.debug(LOG_ENTRY, "remove", app);
    
    Iterator<Map.Entry<AriesApplication, ApplicationContextImpl>> it = _appToContextMap.entrySet()
        .iterator();

    while (it.hasNext()) {
      Map.Entry<AriesApplication, ApplicationContextImpl> entry = it.next();

      ApplicationContextImpl potentialMatch = entry.getValue();

      if (potentialMatch == app) {
        it.remove();

        uninstall(potentialMatch);

        break;
      }
    }
    
    LOGGER.debug(LOG_EXIT, "remove");
  }

  private void uninstall(ApplicationContextImpl app)
  {
    LOGGER.debug(LOG_ENTRY, "uninstall", app);
    
    if (app.uninstall())
      app.setState(ApplicationState.UNINSTALLED);      
    
    LOGGER.debug(LOG_EXIT, "uninstall");
  }

  public synchronized void close()
  {
    LOGGER.debug(LOG_ENTRY, "close");
    
    for (ApplicationContextImpl ctx : _appToContextMap.values()) {
      uninstall(ctx);
    }

    _appToContextMap.clear();
    
    LOGGER.debug(LOG_EXIT, "close");
  }
  
  protected BundleFrameworkManager getBundleFrameworkManager()
  {
    LOGGER.debug(LOG_ENTRY, "getBundleFrameworkManager");
    LOGGER.debug(LOG_EXIT, "getBundleFrameworkManager", _bundleFrameworkManager);
    
    return _bundleFrameworkManager;
  }

  public AriesApplicationContext update(AriesApplication app, DeploymentMetadata oldMetadata) throws UpdateException {
    ApplicationContextImpl ctx = _appToContextMap.get(app);
    
    if (ctx == null) {
      throw new IllegalArgumentException("AriesApplication "+
          app.getApplicationMetadata().getApplicationSymbolicName() + "/" + app.getApplicationMetadata().getApplicationVersion() + 
          " cannot be updated because it is not installed");
    }
    
    ctx.update(app.getDeploymentMetadata(), oldMetadata);
    
    return ctx;
  }

}