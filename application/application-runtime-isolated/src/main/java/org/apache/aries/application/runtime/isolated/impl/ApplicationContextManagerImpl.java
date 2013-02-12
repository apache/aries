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
import static org.apache.aries.application.utils.AppConstants.LOG_EXCEPTION;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationContext.ApplicationState;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.spi.framework.BundleFrameworkManager;
import org.apache.aries.application.management.spi.repository.BundleRepositoryManager;
import org.apache.aries.application.management.spi.runtime.AriesApplicationContextManager;
import org.apache.aries.application.utils.AppConstants;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationContextManagerImpl implements AriesApplicationContextManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextManagerImpl.class);
  
  private ConcurrentMap<AriesApplication, AriesApplicationContext> _appToContextMap;
  private BundleFrameworkManager _bundleFrameworkManager;  
  private BundleRepositoryManager _bundleRepositoryManager;

  public ApplicationContextManagerImpl()
  {
    LOGGER.debug(LOG_ENTRY, "ApplicationContextImpl");
    
    _appToContextMap = new ConcurrentHashMap<AriesApplication, AriesApplicationContext>();
    
    // When doing isolated runtime support provisioning against the local repo is a really bad idea
    // it can result in trying to install things into the shared framework into the local framework
    // this doesn't work because we don't know how to install them into the shared framework and
    // we can't just use them because they are in the local framework, so if this class is constructed
    // we disable local provisioning.
    System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "true");
    
    LOGGER.debug(LOG_EXIT, "ApplicationContextImpl", this);
  }

  public void setBundleFrameworkManager(BundleFrameworkManager bfm)
  {
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
        
    AriesApplicationContext result;
    if (_appToContextMap.containsKey(app)) {
      result = _appToContextMap.get(app);
    } else {
      result = new ApplicationContextImpl(app, this);
      AriesApplicationContext previous = _appToContextMap.putIfAbsent(app, result);
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
    for (Map.Entry<AriesApplication, AriesApplicationContext> entry : _appToContextMap.entrySet()) {
      result.add(entry.getValue());
    }
    
    LOGGER.debug(LOG_EXIT, "getApplicationContexts", result);
    
    return result;
  }

  public void remove(AriesApplicationContext app) throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "remove", app);
    
    ApplicationContextImpl appToRemove = null;
    synchronized (_appToContextMap) { 
    	Iterator<Map.Entry<AriesApplication, AriesApplicationContext>> it = _appToContextMap.entrySet().iterator();
    	while (it.hasNext()) {
        Map.Entry<AriesApplication, AriesApplicationContext> entry = it.next();
        ApplicationContextImpl potentialMatch = (ApplicationContextImpl) entry.getValue();
        if (potentialMatch == app) {
          it.remove();
          appToRemove = potentialMatch;
          break;
        }
    	}
    }
    
    if (appToRemove != null) { 
    	appToRemove.uninstall();
    }

    LOGGER.debug(LOG_EXIT, "remove");
  }
  
  public void close()
  {
    LOGGER.debug(LOG_ENTRY, "close");
    
    List<ApplicationContextImpl> contextsToUninstall = new ArrayList<ApplicationContextImpl>();
    synchronized (_appToContextMap) { 
    	Iterator<AriesApplicationContext> it = _appToContextMap.values().iterator();
    	while (it.hasNext()) { 
    		ApplicationContextImpl ctx = (ApplicationContextImpl)it.next();
    		if (ctx.getApplicationState() != ApplicationState.UNINSTALLED) { 
    			contextsToUninstall.add(ctx);
    			it.remove();
    		}
    	}
    }
    for (ApplicationContextImpl c : contextsToUninstall) { 
    	try { 
    		c.uninstall();
    	} catch (BundleException e)
      {
        LOGGER.debug(LOG_EXCEPTION,e);
      }
    }
    
    LOGGER.debug(LOG_EXIT, "close");
  }
  
  protected BundleFrameworkManager getBundleFrameworkManager()
  {
    LOGGER.debug(LOG_ENTRY, "getBundleFrameworkManager");
    LOGGER.debug(LOG_EXIT, "getBundleFrameworkManager", _bundleFrameworkManager);
    
    return _bundleFrameworkManager;
  }

  public AriesApplicationContext update(AriesApplication app, DeploymentMetadata oldMetadata) throws UpdateException {
    ApplicationContextImpl ctx = (ApplicationContextImpl)_appToContextMap.get(app);
    
    if (ctx == null) {
      throw new IllegalArgumentException("AriesApplication "+
          app.getApplicationMetadata().getApplicationSymbolicName() + "/" + app.getApplicationMetadata().getApplicationVersion() + 
          " cannot be updated because it is not installed");
    }
    
    ctx.update(app.getDeploymentMetadata(), oldMetadata);
    
    return ctx;
  }

  public void bindBundleFrameworkManager(BundleFrameworkManager bfm)
  {
    LOGGER.debug(LOG_ENTRY, "bindBundleFrameworkManager", bfm);
    
    List<AriesApplicationContext> contexts = new ArrayList<AriesApplicationContext>();
    synchronized (_appToContextMap) { 
    	contexts.addAll (_appToContextMap.values());
    }
    
    for (AriesApplicationContext ctx : contexts) { 
    	try { 
    		((ApplicationContextImpl)ctx).open();
    	} catch (BundleException e) {
        LOGGER.debug(LOG_EXCEPTION,e);
      }
    }
    LOGGER.debug(LOG_EXIT, "bindBundleFrameworkManager");
  }

  public void unbindBundleFrameworkManager(BundleFrameworkManager bfm)
  {
    LOGGER.debug(LOG_ENTRY, "unbindBundleFrameworkManager", bfm);
    
    List<AriesApplicationContext> appContexts = new ArrayList<AriesApplicationContext>();
    synchronized (_appToContextMap) { 
    	appContexts.addAll(_appToContextMap.values());
    }
    for (AriesApplicationContext c : appContexts) { 
    	try { 
    		((ApplicationContextImpl)c).close();
    	} catch (BundleException e) { 
    		LOGGER.debug(LOG_EXCEPTION,e);
    	}
    }
   
    LOGGER.debug(LOG_EXIT, "unbindBundleFrameworkManager");
  }
}