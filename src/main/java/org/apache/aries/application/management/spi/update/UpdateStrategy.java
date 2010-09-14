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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application.management.spi.update;

import java.util.Collection;
import java.util.Map;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.spi.framework.BundleFramework;
import org.apache.aries.application.management.spi.repository.BundleRepository;
import org.apache.aries.application.management.spi.repository.ContextException;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.osgi.framework.Bundle;

/**
 * Plug point for update implementation
 */
public interface UpdateStrategy {

  /**
   * Are the two deployments subject to update or do they require full reinstall
   */
  public boolean allowsUpdate(DeploymentMetadata newMetadata, DeploymentMetadata oldMetadata);
  
  /**
   * Update an application
   */
  public void update(UpdateInfo paramUpdateInfo) throws UpdateException;

  /**
   * Representation for an update request
   */
  public static interface UpdateInfo
  {
    /**
     * Find {@link BundleSuggestion} objects for new bundle requests
     */
    public Map<DeploymentContent, BundleRepository.BundleSuggestion> suggestBundle(Collection<DeploymentContent> bundles)
      throws ContextException;

    /**
     * Register a new bundle with the application (i.e. a new bundle was installed)
     */
    public void register(Bundle bundle);

    /**
     * Unregister a bundle from the application (i.e. the bundle was uninstalled)
     */
    public void unregister(Bundle bundle);

    /**
     * Get a {@link BundleFramework} object for the shared framework
     */
    public BundleFramework getSharedFramework();

    /**
     * Get a {@link BundleFramework} object for the isolated framework corresponding 
     * to the application to be updated
     */
    public BundleFramework getAppFramework();

    /**
     * Get the {@link DeploymentMetadata} that is currently active and to be phased out
     */
    public DeploymentMetadata getOldMetadata();

    /**
     * Get the {@link DeploymentMetadata} that is to be activated
     */
    public DeploymentMetadata getNewMetadata();

    /**
     * Get the {@link AriesApplication} object being updated
     */
    public AriesApplication getApplication();

    /**
     * Whether to start any newly installed bundles
     */
    public boolean startBundles();
  }
}
