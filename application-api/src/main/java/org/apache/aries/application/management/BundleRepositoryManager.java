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
import java.util.Iterator;
import java.util.Map;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.management.BundleRepository.BundleSuggestion;

public interface BundleRepositoryManager
{
  /**
   * Gets a collection of all bundle repositories which can provide bundles to
   * the given application scope.
   * @param applicationName
   * @param applicationVersion
   * @return
   */
  public Collection<BundleRepository> getBundleRepositoryCollection(
      String applicationName, String applicationVersion);
  
  /**
   * Gets all known bundle repositories
   * @return
   */
  public Collection<BundleRepository> getAllBundleRepositories();
  
  /**
   * Get a collection of bundle installation suggestions from repositories
   * suitable for the given application scope
   * @param applicationName
   * @param applicationVersion
   * @param content
   * @return
   * @throws ContextException
   */
  public Map<DeploymentContent, BundleSuggestion> getBundleSuggestions(
      String applicationName,
      String applicationVersion,
      Collection<DeploymentContent> content) throws ContextException;
  
  /**
   * Get a collection of bundle installation suggestions from all 
   * known repositories
   * @param content
   * @return
   * @throws ContextException
   */
  public Map<DeploymentContent, BundleSuggestion> getBundleSuggestions(
      Collection<DeploymentContent> content) throws ContextException;
  
  /**
   * Get a collection of bundle installation suggestions from the collection of
   * given repositories
   * @param brs
   * @param content
   * @return
   * @throws ContextException
   */
  public Map<DeploymentContent, BundleSuggestion> getBundleSuggestions(
      Collection<BundleRepository> brs,
      Collection<DeploymentContent> content) throws ContextException;
  
}
