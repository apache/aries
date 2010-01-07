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
import java.util.List;

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
  public String getApplicationSymbolicName();
  
  /**
   * get the value of the Application-Version header
   * @return the value of the Application-Version header
   */
  public Version getApplicationVersion();
  
  /**
   * get the value of the Deployed-Content header 
   * @return the list of the deployed content 
   */
  public List<DeploymentContent> getApplicationDeploymentContents();
  
  /**
   * return the application this deployment metadata is associated with.
   * 
   * @return the application.
   */
  public ApplicationMetadata getApplicationMetadata();
  
  /** Stores any changes to disk using this implementation's storage form */
  public void store(File f) throws IOException;
  public void store(OutputStream in) throws IOException;
}
