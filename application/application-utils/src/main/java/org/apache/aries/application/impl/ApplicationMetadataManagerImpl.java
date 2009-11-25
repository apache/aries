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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataManager;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.Version;

/**
 * This class implements the application metadata manager. It is not directly
 * exposed outside of the bundle, their is a service facade in front of it which
 * is used by clients.
 */
public class ApplicationMetadataManagerImpl implements ApplicationMetadataManager
{
  /** The applications managed, keyed based on the app symbolic name and version */
  public ConcurrentMap<String, ApplicationMetadata> applications = new ConcurrentHashMap<String, ApplicationMetadata>();

  public ApplicationMetadata getApplication(String applicationSymbolicName, Version version)
  {
    ApplicationMetadata metadata = applications.get(applicationSymbolicName + "_" + version);
    return metadata;
  }

  public ApplicationMetadata parseApplication(InputStream in) throws IOException
  {
    Manifest man = ManifestProcessor.parseManifest(in);
    
    ApplicationMetadata metadata = new ApplicationMetadataImpl(man);
    
    return metadata;
  }
  
  public boolean registerApplication(ApplicationMetadata app)
  {
    String key = app.getApplicationSymbolicName() + "_" + app.getApplicationVersion();
    
    ApplicationMetadata existingApp = applications.putIfAbsent(key, app);
    
    return existingApp == null;
  }
  
  public ApplicationMetadata createApplication(Manifest man)
  {
    return new ApplicationMetadataImpl(man);
  }
  
  /**
   * This method is called by the service facade to remove applications when
   * the client bundle releases the service. It is not public.
   * 
   * @param app the application to remove.
   */
  public void removeApplication(ApplicationMetadata app)
  {
    String key = app.getApplicationSymbolicName() + "_" + app.getApplicationVersion();
    applications.remove(key);
  }

}