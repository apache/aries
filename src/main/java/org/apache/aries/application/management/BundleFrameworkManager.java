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

package org.apache.aries.application.management;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.BundleRepository.BundleSuggestion;

public interface BundleFrameworkManager
{
  /**
   * Gets the BundleFramework object associated with the given bundle
   * @param frameworkBundle - The bundle representing the bundle framework
   * @return
   */
  public BundleFramework getBundleFramework(Bundle frameworkBundle);
  
  /**
   * Gets a reference to the single shared bundle framework. The Shared Bundle 
   * Framework contains bundle shared between applications
   * @return
   */
  public BundleFramework getSharedBundleFramework();
  
  /**
   * Creates a new framework inside the shared bundle framework and installs a 
   * collection of bundles into the framework.
   * @param bundlesToInstall The collection of bundles to be installed
   * @param app The application associated with this install
   * @return
   * @throws BundleException
   */
  public Bundle installIsolatedBundles(
      Collection<BundleSuggestion> bundlesToInstall, 
      AriesApplication app)
    throws BundleException;
  
  /**
   * Installs a collection of shared bundles to the shared bundle framework
   * @param bundlesToInstall
   * @param app
   * @return
   * @throws BundleException
   */
  public Collection<Bundle> installSharedBundles(
      Collection<BundleSuggestion> bundlesToInstall, 
      AriesApplication app)
    throws BundleException;
  
  public boolean allowsUpdate(DeploymentMetadata newMetadata, DeploymentMetadata oldMetadata);
  
  public interface BundleLocator {
    public Map<DeploymentContent, BundleSuggestion> suggestBundle(Collection<DeploymentContent> bundles) throws ContextException;    
  }
  
  public void updateBundles(
      DeploymentMetadata newMetadata, 
      DeploymentMetadata oldMetadata, 
      AriesApplication app, 
      BundleLocator locator,
      Set<Bundle> bundles,
      boolean startBundles) throws UpdateException;
  
  /**
   * Starts a previously installed bundle 
   * @param b
   * @throws BundleException
   */
  public void startBundle(Bundle b) throws BundleException;
  
  /**
   * Stops a previously installed bundle   
   * @param b
   * @throws BundleException
   */
  public void stopBundle(Bundle b) throws BundleException;
  
  /**
   * Removes a bundle from the runtime
   * @param b
   * @throws BundleException
   */
  public void uninstallBundle(Bundle b) throws BundleException;
  
}
