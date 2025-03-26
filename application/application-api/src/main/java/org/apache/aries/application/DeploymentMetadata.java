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

package org.apache.aries.application;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.Version;

/**
 * Represents the parsed contents of a DEPLOYMENT.MF file
 *
 */
public interface DeploymentMetadata {

  /**
   * get the value of the Application-SymbolicName header
   * @return the value of the Application-SymbolicName header
   */
  String getApplicationSymbolicName();
  
  /**
   * get the value of the Application-Version header
   * @return the value of the Application-Version header
   */
  Version getApplicationVersion();
  
  /**
   * get the value of the Deployed-Content header 
   * @return the list of the deployed content 
   */
  List<DeploymentContent> getApplicationDeploymentContents();
  
  /**
   * get the value of the Provision-Bundle header
   * @return the list of non-app bundles to provision.
   */
  List<DeploymentContent> getApplicationProvisionBundles();
  
  /**
   * get the value of Deployed-UseBundle header
   * 
   * @return the list of bundles to use from the deployment.
   */
  Collection<DeploymentContent> getDeployedUseBundle();
  
  /**
   * get the value of Import-Package
   * @return all the packages to import from non-app content.
   */
  Collection<Content> getImportPackage();

  /**
   * Get the list of DeployedService-Import
   * @return DeployedService-Import
   */
  Collection<Filter> getDeployedServiceImport();
  
  /**
   * get the contents of deployment manifest in a map
   * @return    the required feature map
   */
  Map<String, String> getHeaders();
  /**
   * Obtain the associated 
   * {@link org.apache.aries.application.ApplicationMetadata ApplicationMetadata}. 
   * @return The application metadata.
   */
  ApplicationMetadata getApplicationMetadata();
  
  /** 
   * Persist this metadata as Manifest-formatted text. 
   * @param f The file to store this metadata to
   * @throws IOException
   */
  void store(File f) throws IOException;
  
  /** 
   * Persist this metadata.  
   * @param out The OutputStream to store this metadata to. 
   * @throws IOException
   */

  void store(OutputStream out) throws IOException;

}
