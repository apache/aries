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
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataManager;
import org.apache.aries.application.Content;
import org.apache.aries.application.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

/**
 * This class provides a facade in front of the manager to ensure we correctly
 * remove registered applications when the requesting bundle releases the service.
 * There is one instance of this class per requesting bundle.
 */
public class ApplicationMetadataManagerServiceImpl implements ServiceFactory
{
  private static class RealApplicationMetadataManagerServiceImpl implements ApplicationMetadataManager
  {
    /** The core application metadata manager */
    private ApplicationMetadataManager manager;
    /** A list of all applications registered via this service instance */
    private Set<ApplicationMetadata> appMetaData = new HashSet<ApplicationMetadata>();
    
    public RealApplicationMetadataManagerServiceImpl(ApplicationMetadataManager man)
    {
      manager = man;
    }

    public ApplicationMetadata getApplicationMetadata (String applicationSymbolicName, Version version)
    {
      return manager.getApplicationMetadata (applicationSymbolicName, version);
    }
  
    public ApplicationMetadata parseApplicationMetadata(InputStream in) throws IOException
    {
      return manager.parseApplicationMetadata(in);
    }
  
    public ApplicationMetadata createApplicationMetadata(Manifest man)
    {
      return manager.createApplicationMetadata(man);
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
    
    public boolean unregisterApplication(ApplicationMetadata app)
    {
      boolean remove = false;
      synchronized (appMetaData) {
        remove = appMetaData.contains(app);
      }
      
      if (remove) return manager.unregisterApplication(app);
      
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
          manager.unregisterApplication(app);
        }
        
        appMetaData.clear();
      }
    }
  
    public Content parseContent(String content)
    {
      return manager.parseContent(content);
    }
  
    public VersionRange parseVersionRange(String versionRange)
    {
      return manager.parseVersionRange(versionRange);
    }
  }

  private ApplicationMetadataManager manager;

  /**
   * Called by blueprint.
   * 
   * @param appManager the core app metadata manager.
   */
  public void setManager(ApplicationMetadataManager appManager)
  {
    manager = appManager;
  }
  
  public Object getService(Bundle bundle, ServiceRegistration registration)
  {
    return new RealApplicationMetadataManagerServiceImpl(manager);
  }

  public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
  {
    ((RealApplicationMetadataManagerServiceImpl)service).close();
  }
}