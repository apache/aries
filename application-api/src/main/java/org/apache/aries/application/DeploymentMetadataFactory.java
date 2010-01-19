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
import java.util.Set;

import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;

/**
 * Methods for creating a DeploymentMetadata instance
 */
public interface DeploymentMetadataFactory {

  /** 
   * Create a DeploymentMetadata from an AriesApplication and its by-value bundles. 
   * @param  app The AriesApplication in question
   * @param  bundleInfo A resolved set of BundleInfo objects
   * @throws IOException
   * @return DeploymentMetadata instance
   */
  public DeploymentMetadata createDeploymentMetadata (AriesApplication app, Set<BundleInfo> bundleInfo);
  
  /**
   * Extract a DeploymentMetadata instance from an IFile
   * @param src DEPLOYMENT.MF file, either in an exploded directory or within a jar file. 
   * @throws IOException
   * @return DeploymentMetadata instance
   */
  public DeploymentMetadata createDeploymentMetadata (IFile src) throws IOException;
  
}
