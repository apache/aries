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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ManagementException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;

public class ApplicationContextImpl implements AriesApplicationContext {
  
  private AriesApplication _application;
  private Map<BundleInfo, Bundle> _bundles;
  private ApplicationState _state;
  private BundleContext _bundleContext;
  
  public ApplicationContextImpl (BundleContext b, AriesApplication app) throws BundleException, ManagementException { 
    _bundleContext = b;
    _application = app;
    _bundles = new HashMap<BundleInfo, Bundle>();
    
    DeploymentMetadata meta = _application.getDeploymentMetadata();
                
    AriesApplicationResolver resolver = null;
    PackageAdmin packageAdmin = null;
    
    ServiceReference resolverRef = b.getServiceReference(AriesApplicationResolver.class.getName());
    ServiceReference packageAdminRef = b.getServiceReference(PackageAdmin.class.getName());
    
    try {            
      resolver = getService(resolverRef, AriesApplicationResolver.class);
      packageAdmin = getService(packageAdminRef, PackageAdmin.class);
    
      List<DeploymentContent> bundlesToInstall = new ArrayList<DeploymentContent>(meta.getApplicationDeploymentContents());
      bundlesToInstall.addAll(meta.getApplicationProvisionBundles());
    
      for (DeploymentContent content : bundlesToInstall) {
        String bundleSymbolicName = content.getContentName();
        Version bundleVersion = content.getExactVersion();
        
        // Step 1: See if bundle is already installed in the framework
        if (findBundleInFramework(packageAdmin, bundleSymbolicName, bundleVersion) != null) {
            continue;
        }
        
        // Step 2: See if the bundle is included in the application
        BundleInfo bundleInfo = findBundleInfoInApplication(bundleSymbolicName, bundleVersion);
        if (bundleInfo == null) {
            // Step 3: Lookup bundle location using the resolver
            bundleInfo = findBundleInfoUsingResolver(resolver, bundleSymbolicName, bundleVersion);
        }
        
        if (bundleInfo == null) {
            throw new ManagementException("Cound not find bundles: " + bundleSymbolicName + "_" + bundleVersion);
        }
            
        Bundle bundle = _bundleContext.installBundle(bundleInfo.getLocation());
            
        _bundles.put(bundleInfo, bundle);        
      }
    } catch (BundleException be) {
      for (Bundle bundle : _bundles.values()) {
        bundle.uninstall();
      }
      
      _bundles.clear();
      
      throw be;
    } finally {
      if (resolver != null) {
          b.ungetService(resolverRef);
      }
      if (packageAdmin != null) {
          b.ungetService(packageAdminRef);
      }
    }
    
    _state = ApplicationState.INSTALLED;
  }

  private <T> T getService(ServiceReference ref, Class<T> type) throws ManagementException {
      Object service = null;
      if (ref != null) {
          service = _bundleContext.getService(ref);
      }
      
      if (service == null) {
          throw new ManagementException(new ServiceException(type.getName(), ServiceException.UNREGISTERED));
      }
      
      return type.cast(service);
  }
  
  private Bundle findBundleInFramework(PackageAdmin admin, String symbolicName, Version version) {
      String exactVersion = "[" + version + "," + version + "]";
      Bundle[] bundles = admin.getBundles(symbolicName, exactVersion);
      if (bundles != null && bundles.length == 1) {
          return bundles[0];
      } else {
          return null;
      }
  }
  
  private BundleInfo findBundleInfoInApplication(String symbolicName, Version version) {
      for (BundleInfo info : _application.getBundleInfo()) {
          if (info.getSymbolicName().equals(symbolicName)
              && info.getVersion().equals(version)) {
              return info;
          }
      }
      return null;
  }
  
  private BundleInfo findBundleInfoUsingResolver(AriesApplicationResolver resolver, String symbolicName, Version version) {
      return resolver.getBundleInfo(symbolicName, version);
  }
  
  public AriesApplication getApplication() {
    return _application;
  }
  
  public Set<Bundle> getApplicationContent() {
    Set<Bundle> result = new HashSet<Bundle>();
    for (Map.Entry<BundleInfo, Bundle> entry : _bundles.entrySet()) { 
      result.add (entry.getValue());
    }
    return result;
  } 

  public ApplicationState getApplicationState() {
    return _state;
  }

  public void start() throws BundleException 
  {
    _state = ApplicationState.STARTING;
    
    List<Bundle> bundlesWeStarted = new ArrayList<Bundle>();
    
    try {
      for (Bundle b : _bundles.values()) { 
        if (b.getState() != Bundle.ACTIVE) { 
          b.start(Bundle.START_ACTIVATION_POLICY);
          bundlesWeStarted.add(b);
        }
      }
    } catch (BundleException be) {
      for (Bundle b : bundlesWeStarted) {
        try {
          b.stop();
        } catch (BundleException be2) {
          // we are doing tidyup here, so we don't want to replace the bundle exception
          // that occurred during start with one from stop. We also want to try to stop
          // all the bundles we started even if some bundles wouldn't stop.
        }
      }
      
      _state = ApplicationState.INSTALLED;
      throw be;
    }
    _state = ApplicationState.ACTIVE;
  }

  public void stop() throws BundleException {
    for (Map.Entry<BundleInfo, Bundle> entry : _bundles.entrySet()) { 
      Bundle b = entry.getValue();
      b.stop();
    }
    _state = ApplicationState.RESOLVED;
  }

  public void setState(ApplicationState state)
  {
    _state = state;
  }
}
