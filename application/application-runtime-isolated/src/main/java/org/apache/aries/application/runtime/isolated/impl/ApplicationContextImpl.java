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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.BundleFramework;
import org.apache.aries.application.management.BundleFrameworkManager;
import org.apache.aries.application.management.BundleRepositoryManager;
import org.apache.aries.application.management.ContextException;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.BundleRepository.BundleSuggestion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;
import static org.apache.aries.application.utils.AppConstants.LOG_EXCEPTION;

public class ApplicationContextImpl implements AriesApplicationContext
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextImpl.class);

  private AriesApplication _application;
  private Set<Bundle> _bundles;
  private ApplicationState _state = ApplicationState.UNINSTALLED;
  private BundleContext _bundleContext;
  private BundleRepositoryManager _bundleRepositoryManager;
  private BundleFrameworkManager _bundleFrameworkManager;

  /** deployment metadata associated with aries application */
  private final DeploymentMetadata _deploymentMF;

  public ApplicationContextImpl(AriesApplication app, ApplicationContextManagerImpl acm)
      throws BundleException, ManagementException
  {
    LOGGER.debug(LOG_ENTRY, "ApplicationContextImpl", new Object[] { app, acm });

    _bundleFrameworkManager = acm.getBundleFrameworkManager();
    _bundleRepositoryManager = acm.getBundleRepositoryManager();
    _bundles = new HashSet<Bundle>();

    _application = app;
    _deploymentMF = _application.getDeploymentMetadata();

    if (_deploymentMF.getApplicationDeploymentContents() != null
        && !_deploymentMF.getApplicationDeploymentContents().isEmpty()) {
      if (processContent()) 
        _state = ApplicationState.INSTALLED;
    }

    LOGGER.debug(LOG_EXIT, "ApplicationContextImpl", this);
  }

  /**
   * Called to install the application, if called multiple times the subsequent
   * calls will be ignored.
   * @return whether the installation is successful
   * 
   */
  private boolean processContent()
  {
    LOGGER.debug(LOG_ENTRY, "install");

    boolean success = true;
    boolean provisionBundleInstall = false;
    boolean useBundleInstall = false;
    List<DeploymentContent> bundlesToFind = new ArrayList<DeploymentContent>(_deploymentMF
        .getApplicationDeploymentContents());
    List<DeploymentContent> useBundlesToFind = new ArrayList<DeploymentContent>(_deploymentMF
        .getDeployedUseBundle());
    List<DeploymentContent> provisionBundlesToFind = new ArrayList<DeploymentContent>(_deploymentMF
        .getApplicationProvisionBundles());

    // In release 1, we'll only support regular bundles in Deployed-Content or CompositeBundle-Content
    // let's process provision bundle first.  if we find it, good, if not, install it
    // please note that provision bundle may contain CBAs.
    provisionBundleInstall = install(provisionBundlesToFind, true);

    // note that useBundle may contains CBAs
    useBundleInstall = install(useBundlesToFind, true);

    // let's process application content/deployment content second.
    // for isolated env, this means, we need to install all and there is no need 
    // to find if it exists in the shared bundle space
    success = install(bundlesToFind, false);

    if (success && provisionBundleInstall && useBundleInstall) {
      LOGGER.debug("Successfully installed application "
          + _application.getApplicationMetadata().getApplicationSymbolicName());
    } else {
      LOGGER.debug(LOG_EXCEPTION, "Failed to install application "
          + _application.getApplicationMetadata().getApplicationSymbolicName());
      uninstall();
    }

    // let's calculate installed again as we only claim install success 
    // when provision bundle and use bundle are installed too.
    success = success && provisionBundleInstall && useBundleInstall;

    LOGGER.debug(LOG_EXIT, "install", new Object[] { Boolean.valueOf(success) });

    return success;

  }

  /**
   * Called to remove the application, if called multiple times the subsequent
   * calls will be ignored.
   * @return whether the uninstallation is successful
   */
  protected boolean uninstall()
  {
    LOGGER.debug(LOG_ENTRY, "uninstall");

    int numErrorBundle = 0;

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
        numErrorBundle++;
        LOGGER.debug(LOG_EXCEPTION, be);
      }
    }

    _bundles.clear();

    // return success only if all bundles were uninstalled successfully
    boolean result = (numErrorBundle == 0);

    LOGGER.debug(LOG_EXIT, "uninstall", new Object[] { Boolean.valueOf(result) });

    return result;

  }

  /**
   * This method finds bundles matching the list of content passed in
   * @param bundlesToFind       bundles to find and start if the bundle is shared.  If isolated, install it.
   * @param shared                      whether the bundles will be shared or isolated
   * @return the result of execution
   */
  private boolean install(List<DeploymentContent> bundlesToFind, boolean shared)
  {
    LOGGER.debug(LOG_ENTRY, "install", new Object[] { bundlesToFind, Boolean.valueOf(shared) });

    int numException = 0; //log the number of exceptions, only assert success if no exception

    if (!bundlesToFind.isEmpty() || !shared) {

      Iterator<DeploymentContent> it = bundlesToFind.iterator();

      /**
       * Dont install any bundles from the list which are already installed
       */
      Bundle[] sharedBundles = 
        _bundleFrameworkManager.getSharedBundleFramework().
                                getIsolatedBundleContext().
                                getBundles();
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
       * Ask the repository manager to find us a list of suggested bundles to install based on our
       * content list
       */
      Map<DeploymentContent, BundleSuggestion> bundlesToBeInstalled = new HashMap<DeploymentContent, BundleSuggestion>();
      try {
        bundlesToBeInstalled = _bundleRepositoryManager.getBundleSuggestions(_application
            .getApplicationMetadata().getApplicationSymbolicName(), _application
            .getApplicationMetadata().getApplicationVersion().toString(), bundlesToFind);
      } catch (ContextException e) {
        numException++;
        LOGGER.debug(LOG_EXCEPTION, e);
      }

      /**
       * Perform the install of the bundles
       */
      try {
        if (shared) 
          _bundles.addAll(_bundleFrameworkManager.installSharedBundles(
            new ArrayList<BundleSuggestion>(bundlesToBeInstalled.values()), _application));
        else 
          _bundles.add(_bundleFrameworkManager.installIsolatedBundles(
            new ArrayList<BundleSuggestion>(bundlesToBeInstalled.values()), _application));

      } catch (BundleException e) {
        numException++;
        LOGGER.debug(LOG_EXCEPTION, e);
      }
    }

    LOGGER.debug(LOG_EXIT, "install", new Object[] { Boolean.valueOf(numException == 0) });

    return (numException == 0);
  }

  public AriesApplication getApplication()
  {
    LOGGER.debug(LOG_ENTRY, "getApplication");
    LOGGER.debug(LOG_EXIT, "getApplication", new Object[] { _application });

    return _application;
  }

  public Set<Bundle> getApplicationContent()
  {
    LOGGER.debug(LOG_ENTRY, "getApplicationContent");
    LOGGER.debug(LOG_EXIT, "getApplicationContent", new Object[] { _bundles });

    return _bundles;
  }

  public ApplicationState getApplicationState()
  {
    LOGGER.debug(LOG_ENTRY, "getApplicationState");
    LOGGER.debug(LOG_EXIT, "getApplicationState", new Object[] { _state });

    return _state;
  }

  public void start() throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "start");

    ApplicationState oldState = _state;
    _state = ApplicationState.STARTING;

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
      _state = oldState;

      LOGGER.debug(LOG_EXCEPTION, be);
      LOGGER.debug(LOG_EXIT, "start", new Object[] { be });
      throw be;
    }
    _state = ApplicationState.ACTIVE;

    LOGGER.debug(LOG_EXIT, "start");
  }

  public void stop() throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "stop");

    for (Bundle entry : _bundles) {
      Bundle b = entry;
      _bundleFrameworkManager.stopBundle(b);
    }
    _state = ApplicationState.RESOLVED;

    LOGGER.debug(LOG_EXIT, "stop");
  }

  public void setState(ApplicationState state)
  {
    LOGGER.debug(LOG_ENTRY, "setState", new Object[] { _state, state });

    _state = state;

    LOGGER.debug(LOG_EXIT, "setState");
  }
}
