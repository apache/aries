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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.spi.framework.BundleFrameworkManager;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.apache.aries.application.management.spi.repository.BundleRepositoryManager;
import org.apache.aries.application.management.spi.repository.ContextException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationContextImpl implements AriesApplicationContext
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextImpl.class);

  private final AriesApplication _application;
  private final Set<Bundle> _bundles;
  private ApplicationState _state = ApplicationState.UNINSTALLED;
  private boolean _closed;
  private final BundleRepositoryManager _bundleRepositoryManager;
  private final BundleFrameworkManager _bundleFrameworkManager;

  /** deployment metadata associated with aries application */
  private DeploymentMetadata _deploymentMF;

  public ApplicationContextImpl(AriesApplication app, ApplicationContextManagerImpl acm)
      throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "ApplicationContextImpl", new Object[] { app, acm });

    _bundleFrameworkManager = acm.getBundleFrameworkManager();
    _bundleRepositoryManager = acm.getBundleRepositoryManager();
    _bundles = new LinkedHashSet<Bundle>();

    _application = app;
    _deploymentMF = _application.getDeploymentMetadata();

    if (_deploymentMF.getApplicationDeploymentContents() != null
        && !_deploymentMF.getApplicationDeploymentContents().isEmpty()) {
      install();
    }
    
    LOGGER.debug(LOG_EXIT, "ApplicationContextImpl", this);
  }

  /**
   * Called to install the application.
   * @return whether the installation is successful
   * 
   */
  private void install() throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "install");

    List<DeploymentContent> bundlesToFind = new ArrayList<DeploymentContent>(_deploymentMF
        .getApplicationDeploymentContents());
    List<DeploymentContent> useBundlesToFind = new ArrayList<DeploymentContent>(_deploymentMF
        .getDeployedUseBundle());
    List<DeploymentContent> provisionBundlesToFind = new ArrayList<DeploymentContent>(_deploymentMF
        .getApplicationProvisionBundles());

    try {
    	
      installBundles(provisionBundlesToFind, true);
      installBundles(useBundlesToFind, true);
      installBundles(bundlesToFind, false);
      
      _state = ApplicationState.INSTALLED;
      
      LOGGER.debug("Successfully installed application "
          + _application.getApplicationMetadata().getApplicationSymbolicName());
    } catch (BundleException e) {
      LOGGER.debug(LOG_EXCEPTION, "Failed to install application "
          + _application.getApplicationMetadata().getApplicationSymbolicName());
      
      uninstall();
      
      throw e;
    }

    LOGGER.debug(LOG_EXIT, "install");

  }

  /**
   * Called to remove the application, if called multiple times the subsequent
   * calls will be ignored.
   * @return whether the uninstallation is successful
   */
  protected synchronized void uninstall() throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "uninstall");
    
    if (_state != ApplicationState.UNINSTALLED) {
      // Iterate through all of the bundles that were started when this application was started, 
      // and attempt to stop and uninstall each of them. 
      for (Iterator<Bundle> bundleIter = _bundles.iterator(); bundleIter.hasNext();) {
        Bundle bundleToRemove = bundleIter.next();
  
        try {
          // If Bundle is active, stop it first.
          if (bundleToRemove.getState() == Bundle.ACTIVE) {
            _bundleFrameworkManager.stopBundle(bundleToRemove);
          }
  
          // Delegate the uninstall to the bundleFrameworkManager
          _bundleFrameworkManager.uninstallBundle(bundleToRemove);
  
        } catch (BundleException be) {
          LOGGER.debug(LOG_EXCEPTION, be);
          throw be;
        }
      }
      _bundles.clear();
      
      _state = ApplicationState.UNINSTALLED;
    }
    
    LOGGER.debug(LOG_EXIT, "uninstall");

  }

  /**
   * This method finds bundles matching the list of content passed in
   * @param bundlesToFind       bundles to find and start if the bundle is shared.  If isolated, install it.
   * @param shared                      whether the bundles will be shared or isolated
   * @return the result of execution
   */
  private void installBundles(List<DeploymentContent> bundlesToFind, boolean shared)
      throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "install", new Object[] { bundlesToFind, Boolean.valueOf(shared) });

    if (!bundlesToFind.isEmpty() || !shared) {

      Iterator<DeploymentContent> it = bundlesToFind.iterator();

      /**
       * Dont install any bundles from the list which are already
       * installed
       */
      Bundle[] sharedBundles = _bundleFrameworkManager.getSharedBundleFramework()
          .getIsolatedBundleContext().getBundles();
      if (shared) {
        if (sharedBundles.length > 0) {
          while (it.hasNext()) {
            DeploymentContent bundleToFind = it.next();

            for (Bundle b : sharedBundles) {
              if (bundleToFind.getContentName().equals(b.getSymbolicName())
                  && bundleToFind.getExactVersion().equals(b.getVersion())) {
                it.remove();
                _bundles.add(b);
                break;
              }
            }
          }
        }
      }

      /**
       * Ask the repository manager to find us a list of suggested bundles
       * to install based on our content list
       */
      Map<DeploymentContent, BundleSuggestion> bundlesToBeInstalled = 
        findBundleSuggestions(bundlesToFind);

      /**
       * Perform the install of the bundles
       */
      try {
        if (shared) _bundles.addAll(_bundleFrameworkManager.installSharedBundles(
            new ArrayList<BundleSuggestion>(bundlesToBeInstalled.values()), makeAppProxy()));
        else _bundles.add(_bundleFrameworkManager.installIsolatedBundles(
            new ArrayList<BundleSuggestion>(bundlesToBeInstalled.values()), makeAppProxy()));

      } catch (BundleException e) {
        LOGGER.debug(LOG_EXCEPTION, e);
        throw e;
      }
    }
    LOGGER.debug(LOG_EXIT, "install");
  }
  
  /**
   * Create a proxy for the AriesApplication we pass on so as to respect the correct current deployment metadata.
   */
  private AriesApplication makeAppProxy() {
    return new AriesApplication() {
      
      public void store(OutputStream out) throws FileNotFoundException, IOException {
        throw new UnsupportedOperationException();
      }
      
      public void store(File f) throws FileNotFoundException, IOException {
        throw new UnsupportedOperationException();
      }
      
      public boolean isResolved() {
        return true;
      }
      
      public DeploymentMetadata getDeploymentMetadata() {
        return _deploymentMF;
      }
      
      public Set<BundleInfo> getBundleInfo() {
        return _application.getBundleInfo();
      }
      
      public ApplicationMetadata getApplicationMetadata() {
        return _application.getApplicationMetadata();
      }
    };
  }

  private Map<DeploymentContent, BundleSuggestion> findBundleSuggestions(
      Collection<DeploymentContent> bundlesToFind) throws BundleException
  {
    Map<DeploymentContent, BundleSuggestion> suggestions = null;
    try {
      suggestions = _bundleRepositoryManager.getBundleSuggestions(_application
          .getApplicationMetadata().getApplicationSymbolicName(), _application
          .getApplicationMetadata().getApplicationVersion().toString(), bundlesToFind);
    } catch (ContextException e) {
      LOGGER.debug(LOG_EXCEPTION, e);
      throw new BundleException("Failed to locate bundle suggestions", e);
    }

    return suggestions;

  }

  public AriesApplication getApplication()
  {
    LOGGER.debug(LOG_ENTRY, "getApplication");
    LOGGER.debug(LOG_EXIT, "getApplication", new Object[] { _application });

    return _application;
  }

  public synchronized Set<Bundle> getApplicationContent()
  {
    LOGGER.debug(LOG_ENTRY, "getApplicationContent");
    LOGGER.debug(LOG_EXIT, "getApplicationContent", new Object[] { _bundles });

    return _bundles;
  }

  public synchronized ApplicationState getApplicationState()
  {
    LOGGER.debug(LOG_ENTRY, "getApplicationState");
    LOGGER.debug(LOG_EXIT, "getApplicationState", new Object[] { _state });

    return _state;
  }

  public synchronized void start() throws BundleException, IllegalStateException
  {
    LOGGER.debug(LOG_ENTRY, "start");

    if (!(_state == ApplicationState.INSTALLED || 
        _state == ApplicationState.RESOLVED))
      throw new IllegalStateException("Appication is in incorrect state " + _state + " expected " + ApplicationState.INSTALLED + " or " + ApplicationState.RESOLVED);
    
    List<Bundle> bundlesWeStarted = new ArrayList<Bundle>();
    try {
      for (Bundle b : _bundles) {
        _bundleFrameworkManager.startBundle(b);
        bundlesWeStarted.add(b);
      }
    } catch (BundleException be) {

      for (Bundle b : bundlesWeStarted) {
        try {
          _bundleFrameworkManager.stopBundle(b);
        } catch (BundleException be2) {
          // we are doing tidyup here, so we don't want to replace the bundle exception
          // that occurred during start with one from stop. We also want to try to stop
          // all the bundles we started even if some bundles wouldn't stop.
          LOGGER.debug(LOG_EXCEPTION, be2);
        }
      }

      LOGGER.debug(LOG_EXCEPTION, be);
      LOGGER.debug(LOG_EXIT, "start", new Object[] { be });
      throw be;
    }
    
    _state = ApplicationState.ACTIVE;

    LOGGER.debug(LOG_EXIT, "start");
  }

  public synchronized void stop() throws BundleException, IllegalStateException
  {
    LOGGER.debug(LOG_ENTRY, "stop");
    
    if (_state != ApplicationState.ACTIVE)
      throw new IllegalStateException("Appication is in incorrect state " + _state + " expected " + ApplicationState.ACTIVE);
        
    for (Bundle entry : _bundles) {
      Bundle b = entry;
      _bundleFrameworkManager.stopBundle(b);
    }
    
    _state = ApplicationState.RESOLVED;

    LOGGER.debug(LOG_EXIT, "stop");
  }

  public synchronized void update(final DeploymentMetadata newMetadata, final DeploymentMetadata oldMetadata)
      throws UpdateException
  {
    final boolean toStart = getApplicationState() == ApplicationState.ACTIVE;

    if (_bundleFrameworkManager.allowsUpdate(newMetadata, oldMetadata)) {
      _bundleFrameworkManager.updateBundles(newMetadata, oldMetadata, _application,
          new BundleFrameworkManager.BundleLocator() {
            public Map<DeploymentContent, BundleSuggestion> suggestBundle(
                Collection<DeploymentContent> bundles) throws BundleException
            {
              return findBundleSuggestions(bundles);
            }
          }, _bundles, toStart);

    } else {
      // fallback do a uninstall, followed by a reinstall
      try {        
        uninstall();
        _deploymentMF = newMetadata;        
        try {
          install();
          
          if (toStart)
            start();
        }
        catch (BundleException e)
        {
          try {
            uninstall();
            
            _deploymentMF = oldMetadata;
            install();

            if (toStart)
              start();
           
            throw new UpdateException("Could not install updated application", e,
                true, null);
          }
          catch (BundleException e2)
          {
            throw new UpdateException("Could not install updated application", e,
                false, e2);
          }          
        }       
      }
      catch (BundleException e)
      {
        try {          
          _deploymentMF = oldMetadata;
          install();

          if (toStart)
            start();
          
          throw new UpdateException("Could not install updated application", e,
              true, null);
        }
        catch (BundleException e2)
        {
          throw new UpdateException("Could not install updated application", e,
              false, e2);
        } 
      }
    }
  }

  public synchronized void close() throws BundleException
  {
    uninstall();
    _closed = true;
  }
  
  public synchronized void open() throws BundleException
  {
    if (_closed) {
      install();
      _closed = false;
    }
  }
}
