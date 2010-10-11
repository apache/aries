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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.spi.framework.BundleFramework;
import org.apache.aries.application.management.spi.framework.BundleFrameworkFactory;
import org.apache.aries.application.management.spi.framework.BundleFrameworkManager;
import org.apache.aries.application.management.spi.repository.ContextException;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.apache.aries.application.management.spi.update.UpdateStrategy;
import org.apache.aries.application.utils.runtime.InstallUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleFrameworkManagerImpl implements BundleFrameworkManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleFrameworkManagerImpl.class);

  BundleContext _ctx;
  BundleFramework _sharedBundleFramework;
  BundleFrameworkFactory _bundleFrameworkFactory;
  Map<Bundle, BundleFramework> _frameworks = new HashMap<Bundle, BundleFramework>();
  Map<AriesApplication, BundleFramework> _frameworksByApp = new HashMap<AriesApplication, BundleFramework>();
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

  public void init()
  {
    try {
      _sharedBundleFramework = SharedBundleFramework.getSharedBundleFramework(_ctx,
          _bundleFrameworkFactory);
      _frameworks.put(_sharedBundleFramework.getFrameworkBundle(), _sharedBundleFramework);
    } catch (ContextException e) {
      LOGGER.error(LOG_EXCEPTION, e);
    }
  }

  public BundleFramework getBundleFramework(Bundle frameworkBundle)
  {
    return _frameworks.get(frameworkBundle);
  }

  public Bundle installIsolatedBundles(Collection<BundleSuggestion> bundlesToInstall,
      AriesApplication app) throws BundleException
  {
    // We need to create a new isolated framework for this content and install
    // the bundles to it
    BundleFramework isolatedFramework = isolatedInstall(bundlesToInstall, app);

    _frameworks.put(isolatedFramework.getFrameworkBundle(), isolatedFramework);
    _frameworksByApp.put(app, isolatedFramework);

    return isolatedFramework.getFrameworkBundle();
  }

  public Collection<Bundle> installSharedBundles(Collection<BundleSuggestion> bundlesToInstall,
      AriesApplication app) throws BundleException
  {
    Collection<Bundle> installedBundles = new ArrayList<Bundle>();

    // Shared bundle : Install to the shared bundle framework
    for (BundleSuggestion suggestion : bundlesToInstall)
      installedBundles.add(_sharedBundleFramework.install(suggestion, app));

    return installedBundles;
  }

  private BundleFramework isolatedInstall(
      Collection<BundleSuggestion> bundlesToBeInstalled, 
      AriesApplication app)
      throws BundleException
  {
    LOGGER.debug(LOG_ENTRY, "isolatedInstall", new Object[] { bundlesToBeInstalled, app });

    BundleFramework bundleFramework = null;
    BundleContext parentCtx = _sharedBundleFramework.getIsolatedBundleContext();
    DeploymentMetadata deploymentMF = app.getDeploymentMetadata();

    /**
     * Set up framework config properties
     */
    Properties frameworkConfig = new Properties();
    frameworkConfig.put("osgi.console", "0");

    String osgiFrameworkLocation = parentCtx.getProperty(FrameworkConstants.OSGI_FRAMEWORK);
    if (osgiFrameworkLocation != null) {
      frameworkConfig.put(FrameworkConstants.OSGI_FRAMEWORK, osgiFrameworkLocation);

      // let's only set javax.transaction as system extra packages because of the split package. 
      // It is reasonable to do so because we always flow userTransaction service into child framework anyway.
      frameworkConfig.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javax.transaction;version=1.1.0");
    }

    /**
     * Set up BundleManifest for the framework bundle
     */
    Properties frameworkBundleManifest = new Properties();
    frameworkBundleManifest.put(Constants.BUNDLE_SYMBOLICNAME, deploymentMF.getApplicationSymbolicName());
    frameworkBundleManifest.put(Constants.BUNDLE_VERSION, deploymentMF.getApplicationVersion().toString());

    // Extract the import packages and remove anything we already have available in the current framework
    Collection<Content> imports = 
      InstallUtils.calculateImports(
          deploymentMF.getImportPackage(), 
          InstallUtils.getExportPackages(_sharedBundleFramework.getIsolatedBundleContext()));
    
    if (imports != null && !imports.isEmpty())
    {
      StringBuffer buffer = new StringBuffer();
      for (Content i : imports)
        buffer.append(InstallUtils.contentToString(i) + ",");
      frameworkBundleManifest.put(Constants.IMPORT_PACKAGE, buffer.substring(0, buffer.length()-1));
    }
    
    /**
     * Install and start the new isolated bundle framework
     */
    bundleFramework = 
      _bundleFrameworkFactory.createBundleFramework(
          parentCtx, 
          deploymentMF.getApplicationSymbolicName() + " " + deploymentMF.getApplicationVersion(), 
          frameworkConfig, 
          frameworkBundleManifest);

    // We should now have a bundleFramework
    if (bundleFramework != null) {

      boolean frameworkStarted = false;
      try {
        // Start the empty framework bundle
        bundleFramework.init();
        frameworkStarted = true;
      } catch (BundleException e) {
        // This may fail if the framework bundle has exports but we will retry later
      }

      /**
       * Install the bundles into the new framework
       */
      
      List<Bundle> installedBundles = new ArrayList<Bundle>();
      BundleContext frameworkBundleContext = bundleFramework.getIsolatedBundleContext();
      if (frameworkBundleContext != null) {
        for (BundleSuggestion suggestion : bundlesToBeInstalled)
          installedBundles.add(bundleFramework.install(suggestion, app));
      }
      
      // Finally, start the whole lot
      if (!frameworkStarted)
        bundleFramework.init();
    }

    LOGGER.debug(LOG_EXIT, "isolatedInstall", bundleFramework);

    return bundleFramework;
  }

  public BundleFramework getSharedBundleFramework()
  {
    return _sharedBundleFramework;
  }

  public void uninstallBundle(Bundle b) throws BundleException
  {
    BundleFramework framework = getBundleFramework(b);
    if (framework != null) framework.close();
  }

  public void startBundle(Bundle b) throws BundleException
  {
    BundleFramework framework = getBundleFramework(b);
    if (framework != null) // App Content
    {
      for (Bundle bundle : framework.getBundles())
        framework.start(bundle);
    } else // Shared bundle
      _sharedBundleFramework.start(b);
  }

  public void stopBundle(Bundle b) throws BundleException
  {
    BundleFramework framework = getBundleFramework(b);
    if (framework != null) // App Content
    {
      for (Bundle bundle : framework.getBundles())
        framework.stop(bundle);
    }
    // Do not stop shared bundles
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

  public void updateBundles(
      final DeploymentMetadata newMetadata, 
      final DeploymentMetadata oldMetadata, 
      final AriesApplication app, 
      final BundleLocator locator,
      final Set<Bundle> bundles,
      final boolean startBundles) 
    throws UpdateException 
  {
    UpdateStrategy strategy = null;
    
    for (UpdateStrategy us : _updateStrategies) {
      if (us.allowsUpdate(newMetadata, oldMetadata)) {
        strategy = us;
        break;
      }
    }
    
    if (strategy == null) throw new IllegalArgumentException("No UpdateStrategy supports the supplied DeploymentMetadata changes.");
    
    final BundleFramework appFwk = _frameworksByApp.get(app);
    
    strategy.update(new UpdateStrategy.UpdateInfo() {
      
      public void register(Bundle bundle) {
        bundles.add(bundle);
      }
      
      public void unregister(Bundle bundle) {
        bundles.remove(bundle);
      }
      
      public Map<DeploymentContent, BundleSuggestion> suggestBundle(Collection<DeploymentContent> bundles) throws ContextException {
        return locator.suggestBundle(bundles);
      }
      
      public boolean startBundles() {
        return startBundles;
      }
      
      public BundleFramework getSharedFramework() {
        return _sharedBundleFramework;
      }
      
      public DeploymentMetadata getOldMetadata() {
        return oldMetadata;
      }
      
      public DeploymentMetadata getNewMetadata() {
        return newMetadata;
      }
      
      public AriesApplication getApplication() {
        return app;
      }
      
      public BundleFramework getAppFramework() {
        return appFwk;
      }
    });
  }
  
  
}
