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

package org.apache.aries.application.runtime.framework.management;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXCEPTION;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.spi.framework.BundleFramework;
import org.apache.aries.application.management.spi.framework.BundleFrameworkConfiguration;
import org.apache.aries.application.management.spi.framework.BundleFrameworkConfigurationFactory;
import org.apache.aries.application.management.spi.framework.BundleFrameworkFactory;
import org.apache.aries.application.management.spi.framework.BundleFrameworkManager;
import org.apache.aries.application.management.spi.repository.ContextException;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.apache.aries.application.management.spi.update.UpdateStrategy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleFrameworkManagerImpl implements BundleFrameworkManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleFrameworkManagerImpl.class);

  BundleContext _ctx;
  BundleFramework _sharedBundleFramework;
  BundleFrameworkFactory _bundleFrameworkFactory;
  BundleFrameworkConfigurationFactory _bundleFrameworkConfigurationFactory;
  Map<Bundle, BundleFramework> _frameworks = new HashMap<Bundle, BundleFramework>();
  Map<String, BundleFramework> _frameworksByAppScope = new HashMap<String, BundleFramework>();
  private List<UpdateStrategy> _updateStrategies = Collections.emptyList();

  public void setUpdateStrategies(List<UpdateStrategy> updateStrategies)
  {
    _updateStrategies = updateStrategies;
  }

  public void setBundleContext(BundleContext ctx)
  {
    _ctx = ctx;
  }

  public void setBundleFrameworkFactory(BundleFrameworkFactory bff)
  {
    _bundleFrameworkFactory = bff;
  }

  public void setBundleFrameworkConfigurationFactory(BundleFrameworkConfigurationFactory bfcf)
  {
    _bundleFrameworkConfigurationFactory = bfcf;
  }

  public void init()
  {
    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      try {
        _sharedBundleFramework = SharedBundleFramework.getSharedBundleFramework(_ctx,
            _bundleFrameworkConfigurationFactory,
            _bundleFrameworkFactory);
        _frameworks.put(_sharedBundleFramework.getFrameworkBundle(), _sharedBundleFramework);
      } catch (ContextException e) {
        LOGGER.error(LOG_EXCEPTION, e);
      }
    }
  }

  public BundleFramework getBundleFramework(Bundle frameworkBundle)
  {
    BundleFramework framework = null;
    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      framework = _frameworks.get(frameworkBundle);
    }
    return framework;
  }

  public Bundle installIsolatedBundles(Collection<BundleSuggestion> bundlesToInstall,
      AriesApplication app) throws BundleException
  {
    Bundle frameworkBundle = null;

    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      // We need to create a new isolated framework for this content and install
      // the bundles to it
      BundleFramework isolatedFramework = isolatedInstall(bundlesToInstall, _sharedBundleFramework
          .getIsolatedBundleContext(), app);

      _frameworks.put(isolatedFramework.getFrameworkBundle(), isolatedFramework);
      _frameworksByAppScope.put(app.getApplicationMetadata().getApplicationScope(), isolatedFramework);

      frameworkBundle = isolatedFramework.getFrameworkBundle();
    }

    return frameworkBundle;
  }

  public Collection<Bundle> installSharedBundles(Collection<BundleSuggestion> bundlesToInstall,
      AriesApplication app) throws BundleException
  {
    Collection<Bundle> installedBundles = new ArrayList<Bundle>();

    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      // Shared bundle : Install to the shared bundle framework
      for (BundleSuggestion suggestion : bundlesToInstall)
        installedBundles.add(_sharedBundleFramework.install(suggestion, app));
    }

    return installedBundles;
  }

  private BundleFramework isolatedInstall(Collection<BundleSuggestion> bundlesToBeInstalled,
      BundleContext parentCtx, AriesApplication app) throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "isolatedInstall", new Object[] { bundlesToBeInstalled, app });

    /**
     * Build the configuration information for this application framework
     */
    BundleFrameworkConfiguration config = _bundleFrameworkConfigurationFactory
        .createBundleFrameworkConfig(app.getApplicationMetadata().getApplicationScope(), parentCtx,
            app);

    /**
     * Install and start the new isolated bundle framework
     */
    BundleFramework bundleFramework = _bundleFrameworkFactory.createBundleFramework(parentCtx,
        config);

    // We should now have a bundleFramework
    if (bundleFramework != null) {
      
      try {  
        boolean frameworkStarted = false;
        try {
          // Start the empty framework bundle
          bundleFramework.start();
          frameworkStarted = true;
        } catch (BundleException e) {
          // This may fail if the framework bundle has exports but we will retry later
        }

  
        /**
         * Install the bundles into the new framework
         */
        
        BundleContext frameworkBundleContext = bundleFramework.getIsolatedBundleContext();
        if (frameworkBundleContext != null) {
          for (BundleSuggestion suggestion : bundlesToBeInstalled)
            bundleFramework.install(suggestion, app);
        }   
        
        if (!frameworkStarted)
          bundleFramework.start();
        
      } catch (BundleException be) {
        bundleFramework.close();
        throw be;
      } catch (RuntimeException re) {
        bundleFramework.close();
        throw re;
      }
    }

    LOGGER.debug(LOG_EXIT, "isolatedInstall", bundleFramework);

    return bundleFramework;
  }

  public BundleFramework getSharedBundleFramework()
  {
    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      return _sharedBundleFramework;
    }
  }

  public void uninstallBundle(Bundle b) throws BundleException
  {
    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      BundleFramework framework = getBundleFramework(b);
      if (framework != null) {
        framework.close();
        
        // clean up our maps so we don't leak memory
        _frameworks.remove(b);
        Iterator<BundleFramework> it = _frameworksByAppScope.values().iterator();
        while (it.hasNext()) {
          if (it.next().equals(framework)) it.remove();
        }
      }
    }
  }

  public void startBundle(Bundle b) throws BundleException
  {
    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      BundleFramework framework = getBundleFramework(b);
            
      // Start all bundles inside the framework
      if (framework != null) // App Content
      {        
        for (Bundle bundle : framework.getBundles())
          framework.start(bundle);
        
      } else // Shared bundle
      _sharedBundleFramework.start(b);
    }
  }

  public void stopBundle(Bundle b) throws BundleException
  {
    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      BundleFramework framework = getBundleFramework(b);
      
      // Stop all bundles inside the framework
      if (framework != null) // App Content
      {
        for (Bundle bundle : framework.getBundles())
          framework.stop(bundle);
        
      }
      
      // Do not stop shared bundles
    }
  }

  public boolean allowsUpdate(DeploymentMetadata newMetadata, DeploymentMetadata oldMetadata)
  {
    for (UpdateStrategy strategy : _updateStrategies) {
      if (strategy.allowsUpdate(newMetadata, oldMetadata)) {
        return true;
      }
    }

    return false;
  }

  public void updateBundles(final DeploymentMetadata newMetadata,
      final DeploymentMetadata oldMetadata, final AriesApplication app,
      final BundleLocator locator, final Set<Bundle> bundles, final boolean startBundles)
      throws UpdateException
  {
    UpdateStrategy strategy = null;

    for (UpdateStrategy us : _updateStrategies) {
      if (us.allowsUpdate(newMetadata, oldMetadata)) {
        strategy = us;
        break;
      }
    }

    if (strategy == null)
      throw new IllegalArgumentException(
          "No UpdateStrategy supports the supplied DeploymentMetadata changes.");

    synchronized (BundleFrameworkManager.SHARED_FRAMEWORK_LOCK) {
      final BundleFramework appFwk = _frameworksByAppScope.get(app.getApplicationMetadata().getApplicationScope());

      strategy.update(new UpdateStrategy.UpdateInfo() {

        public void register(Bundle bundle)
        {
          bundles.add(bundle);
        }

        public void unregister(Bundle bundle)
        {
          bundles.remove(bundle);
        }

        public Map<DeploymentContent, BundleSuggestion> suggestBundle(
            Collection<DeploymentContent> bundles) throws BundleException
        {
          return locator.suggestBundle(bundles);
        }

        public boolean startBundles()
        {
          return startBundles;
        }

        public BundleFramework getSharedFramework()
        {
          return _sharedBundleFramework;
        }

        public DeploymentMetadata getOldMetadata()
        {
          return oldMetadata;
        }

        public DeploymentMetadata getNewMetadata()
        {
          return newMetadata;
        }

        public AriesApplication getApplication()
        {
          return app;
        }

        public BundleFramework getAppFramework()
        {
          return appFwk;
        }
      });
    }
  }

}

