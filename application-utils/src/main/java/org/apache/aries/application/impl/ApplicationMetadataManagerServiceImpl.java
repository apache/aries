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
package org.apache.aries.application.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataManager;
import org.osgi.framework.Version;

/**
 * This class provides a facade in front of the manager to ensure we correctly
 * remove registered applications when the requesting bundle releases the service.
 * There is one instance of this class per requesting bundle.
 */
public class ApplicationMetadataManagerServiceImpl implements ApplicationMetadataManager
{
  /** The core application metadata manager */
  private ApplicationMetadataManagerImpl manager;
  /** A list of all applications registered via this service instance */
  private List<ApplicationMetadata> appMetaData = new ArrayList<ApplicationMetadata>();
  
  /**
   * Called by blueprint.
   * 
   * @param appManager the core app metadata manager.
   */
  public void setManager(ApplicationMetadataManagerImpl appManager)
  {
    manager = appManager;
  }
  
  public ApplicationMetadata getApplication(String applicationSymbolicName, Version version)
  {
    return manager.getApplication(applicationSymbolicName, version);
  }

  public ApplicationMetadata parseApplication(InputStream in) throws IOException
  {
    return manager.parseApplication(in);
  }

  public ApplicationMetadata createApplication(Manifest man)
  {
    return manager.createApplication(man);
  }

  public boolean registerApplication(ApplicationMetadata app)
  {
    if (manager.registerApplication(app)) {
      synchronized (appMetaData) {
        appMetaData.add(app);
      }
      return true;
    }
    return false;
  }
  
  /**
   * This method is called by blueprint when the calling bundle releases the
   * service. It removes all the registered applications from the core manager.
   */
  public void close()
  {
    synchronized (appMetaData) {
      for (ApplicationMetadata app : appMetaData) {
        manager.removeApplication(app);
      }
      
      appMetaData.clear();
    }
  }
}