
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

package org.apache.aries.application.modelling;

import java.util.Collection;

import org.apache.aries.application.management.ResolverException;

/** A model of a collection of bundles and similar resources */
public interface DeployedBundles {

  /** Add a modelled resource */
  void addBundle(ModelledResource modelledBundle);
    
  /**
   * Get the value corresponding to the Deployed-Content header in the deployment.mf.
   * @return a manifest entry, or an empty string if there is no content.
   */
  String getContent();
  
  /**
   * Get the value corresponding to the Deployed-Use-Bundle header in the deployment.mf.
   * @return a manifest entry, or an empty string if there is no content.
   */
  String getUseBundle();
  
  /**
   * Get the value corresponding to the Provision-Bundle header in the deployment.mf.
   * @return a manifest entry, or an empty string if there is no content.
   */
  String getProvisionBundle();
  
  /**
   * Get the value corresponding to the Import-Package header in the deployment.mf. 
   * @return a manifest entry, or an empty string if there is no content.
   * @throws ResolverException if the requirements could not be resolved.
   */
  String getImportPackage() throws ResolverException;
  
  /**
   * Get the Deployed-ImportService header. 
   * this.deployedImportService contains all the service import filters for every 
   * blueprint component within the application. We will only write an entry
   * to Deployed-ImportService if
   *   a) the reference isMultiple(), or
   *   b) the service was not available internally when the app was first deployed
   *   
   */
  String getDeployedImportService(); 

  String toString();
  
  /**
   * Get the set of bundles that are going to be deployed into an isolated framework
   * @return a set of bundle metadata
   */
  Collection<ModelledResource> getDeployedContent();
  
  /**
   * Get the set of bundles that map to Provision-Bundle: these plus 
   * getRequiredUseBundle combined give the bundles that will be provisioned
   * into the shared bundle space
   * 'getProvisionBundle' returns the manifest header string, so this method 
   * needs to be called something else. 
   *
   */
  Collection<ModelledResource> getDeployedProvisionBundle (); 
  
  /**
   * Get the subset of bundles specified in use-bundle that are actually required to
   * satisfy direct requirements of deployed content.
   * @return a set of bundle metadata.
   * @throws ResolverException if the requirements could not be resolved.
   */
  Collection<ModelledResource> getRequiredUseBundle() throws ResolverException;
}
