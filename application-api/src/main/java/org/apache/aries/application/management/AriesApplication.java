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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.DeploymentMetadata;


/**
 * Metadata about an Aries application
 * @see <a href="http://incubator.apache.org/aries/applications.html">
 * http://incubator.apache.org/aries/applications.html</a>. 
 *
 */
public interface AriesApplication
{
  /**
   * Get the application metadata, which is stored in META-INF/APPLICATION.MF.
   * @return ApplicationMetadata
   */
  public ApplicationMetadata getApplicationMetadata();
  
  /**
   * Get the deployment metadata, which is stored in META-INF/DEPLOYMENT.MF.
   * @return DeploymentMetadata
   */
  public DeploymentMetadata getDeploymentMetadata();

  /** 
   * @return the set of bundles included in the application by value 
   */
  public Set<BundleInfo> getBundleInfo();

  /** 
   * Persist this metadata. 
   * @param f The file to store this metadata to
   * @throws IOException
   */
  public void store(File f) throws FileNotFoundException, IOException;
  
  /** 
   * Persist this metadata. 
   * @param out The output stream to store this metadata to
   * @throws IOException
   */
  public void store(OutputStream out) throws FileNotFoundException, IOException;
}
