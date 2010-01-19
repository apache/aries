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
import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.Content;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.Version;

public class ApplicationMetadataFactoryImpl implements ApplicationMetadataFactory
{
  /** The applications managed, keyed based on the app symbolic name and version */
  public ConcurrentMap<String, ApplicationMetadata> applications = new ConcurrentHashMap<String, ApplicationMetadata>();

  public ApplicationMetadata getApplicationMetadata (String applicationSymbolicName, Version version)
  {
    ApplicationMetadata metadata = applications.get(applicationSymbolicName + "_" + version);
    return metadata;
  }

  public ApplicationMetadata parseApplicationMetadata(InputStream in) throws IOException
  {
    Manifest man = ManifestProcessor.parseManifest(in);
    
    ApplicationMetadata metadata = new ApplicationMetadataImpl(man);
    
    return metadata;
  }
  
  public boolean registerApplication(ApplicationMetadata app)
  {
    ApplicationMetadata existingApp = applications.putIfAbsent(app.getApplicationScope(), app);
    
    return existingApp == null;
  }
  
  public ApplicationMetadata createApplicationMetadata(Manifest man)
  {
    return new ApplicationMetadataImpl(man);
  }
  
  public boolean unregisterApplication(ApplicationMetadata app)
  {
    return applications.remove(app.getApplicationScope()) != null;
  }

  public Content parseContent(String content)
  {
    return new ContentImpl(content);
  }

  public VersionRange parseVersionRange(String versionRange)
  {
    return new VersionRangeImpl(versionRange);
  }

}