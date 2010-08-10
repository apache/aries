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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolverException;

/**
 * Methods for creating a DeploymentMetadata instance
 */
public interface DeploymentMetadataFactory {

  /** 
   * Create a DeploymentMetadata from an AriesApplication and its by-value bundles. 
   * 
   * @param  app The AriesApplication in question
   * @param  bundleInfo A resolved set of BundleInfo objects
   * @throws ResolverException
   * @return DeploymentMetadata instance
   */
  public DeploymentMetadata createDeploymentMetadata (AriesApplication app, Set<BundleInfo> bundleInfo)
    throws ResolverException;
  
  /**
   * Deprecated. Use parseDeploymentMetadata.
   * 
   * @param src DEPLOYMENT.MF file, either in an exploded directory or within a jar file. 
   * @throws IOException
   * @return DeploymentMetadata instance
   */
  @Deprecated
  public DeploymentMetadata createDeploymentMetadata (IFile src) throws IOException;

  
  /**
   * Extract a DeploymentMetadata instance from an IFile
   * 
   * @param src DEPLOYMENT.MF file, either in an exploded directory or within a jar file. 
   * @throws IOException
   * @return DeploymentMetadata instance
   */
  public DeploymentMetadata parseDeploymentMetadata (IFile src) throws IOException;
  
  /**
   * Deprecated. Use parseDeploymentMetadata.
   * 
   * @param in InputStream
   * @throws IOException
   * @return DeploymentMetadata instance
   */
  @Deprecated
  public DeploymentMetadata createDeploymentMetadata (InputStream in) throws IOException;

  /**
   * Extract a DeploymentMetadata instance from InputStream.
   * 
   * @param in InputStream
   * @throws IOException
   * @return DeploymentMetadata instance
   */
  public DeploymentMetadata parseDeploymentMetadata (InputStream in) throws IOException;
  
  /**
   * Extract a DeploymentMetadata instance from Manifest.
   * 
   * @param manifest Manifest
   * @throws IOException
   * @return DeploymentMetadata instance
   */
  public DeploymentMetadata createDeploymentMetadata (Manifest manifest) throws IOException;

}
