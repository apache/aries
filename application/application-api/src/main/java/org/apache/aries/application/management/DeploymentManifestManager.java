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
import java.util.jar.Manifest;

import org.apache.aries.application.Content;
import org.apache.aries.application.modelling.ModelledResource;

public interface DeploymentManifestManager
{
  /**
   * Generate the deployment manifest map for the application. The method is designed to be used when installing an application.
   * @param app The Aries application
   * @param constraints the constraints, used to narrow down the deployment content
   * @return the deployment manifest 
   * @throws ResolverException
   */
  Manifest generateDeploymentManifest( AriesApplication app, ResolveConstraint... constraints ) throws ResolverException;
  
  
  /**
   * Generate the deployment manifest map. The method can be used for some advanced scenarios.
   * @param app The Aries application
   * @param byValueBundles By value bundles
   * @param useBundleSet Use Bundle set
   * @param otherBundles Other bundles to be used to narrow the resolved bundles
   * @return the deployment manifest 
   * @throws ResolverException
   */
  Manifest generateDeploymentManifest( AriesApplication app, Collection<ModelledResource> byValueBundles, Collection<Content> useBundleSet, Collection<Content> otherBundles) throws ResolverException;

  /**
   * 
   * @return the AriesApplicationResolver
   */
  AriesApplicationResolver getResolver();
  
}
