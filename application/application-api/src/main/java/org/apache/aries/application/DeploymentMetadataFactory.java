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

public interface DeploymentMetadataFactory {

  /** 
   * Create a DeploymentMetadata instance
   * @param app The AriesApplication in question
   * @param bundleInfo A resolved set of BundleInfo objects
   * @return
   */
  public DeploymentMetadata createDeploymentMetadata (AriesApplication app, Set<BundleInfo> bundleInfo);
  
  /**
   * Create a DeploymentMetadata instance from an IFile
   */
  public DeploymentMetadata createDeploymentMetadata (IFile src) throws IOException;
  
}
