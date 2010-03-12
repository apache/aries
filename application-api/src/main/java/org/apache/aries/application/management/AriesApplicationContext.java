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

import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Represents an Aries application in the runtime. See the application-runtime module for a 
 * sample implementation. 
 */
public interface AriesApplicationContext
{
  /** 
   * Get the state of a running application. An application is INSTALLED if all its bundles 
   * are installed, RESOLVED if all its bundles are resolved, ACTIVE if all its bundles are 
   * active, and so on. 
   * @return ApplicationState. 
   */
  public ApplicationState getApplicationState();
  
  /**
   * Obtain the associated AriesApplication metadata.  
   * @return AriesApplication
   */
  public AriesApplication getApplication();
  
  /**
   * Start the application by starting all its constituent bundles as per the DeploymentContent 
   * in the associated AriesApplication's DeploymentMetadata. 
   * @throws BundleException
   */
  public void start() throws BundleException;
  
  /**
   * Stop the application by stopping all its constituent bundles. 
   * @throws BundleException
   */
  public void stop() throws BundleException;
  
  /**
   * Get the org.osgi.framework.Bundle objects representing the application's runtime
   * constituents. 
   * @return The application's runtime content. 
   */
  public Set<Bundle> getApplicationContent();
  
  public enum ApplicationState
  {
  INSTALLED, RESOLVED, STARTING, STOPPING, ACTIVE, UNINSTALLED
  }
}
