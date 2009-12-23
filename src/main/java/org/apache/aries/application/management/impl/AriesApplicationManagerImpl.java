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

package org.apache.aries.application.management.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataManager;
import org.apache.aries.application.management.ApplicationContext;
import org.apache.aries.application.management.ApplicationListener;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ManifestDefaultsInjector;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.Bundle;

public class AriesApplicationManagerImpl implements AriesApplicationManager {

  private ApplicationMetadataManager _applicationMetadataManager;

  public void setApplicationMetadataManager (ApplicationMetadataManager amm) { 
    _applicationMetadataManager = amm;
  }
  
  
  
  /**
   * Create an AriesApplication from a .eba file
   */
  public AriesApplication createApplication(File ebaFile) throws ManagementException {
    /* 
     * ebaFile should be a zip file with a '.eba' extension 
     * as per http://incubator.apache.org/aries/applications.html
     */    

    ApplicationMetadata applicationMetadata;
    List<Bundle> bundlesInEba;
    
    try { 
      if (!ebaFile.isFile()) { 
        throw new ManagementException ("Cannot create .eba from directory yet");
      }
      ZipFile zipFile = new ZipFile(ebaFile);
      Manifest applicationManifest = new Manifest();
      
      // TODO: If there's a deployment.mf we can ignore the rest of the content
      
      // Locate META-INF/APPLICATION.MF
      ZipEntry entry = null;
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        entry = entries.nextElement();
        if (entry.getName().replace("\\", "/").equalsIgnoreCase(AppConstants.APPLICATION_MF)) {
          applicationManifest = ManifestProcessor.parseManifest(zipFile.getInputStream(entry));
          break;
        }
        // Also look for application.xml to support .war file migration
      }

      // Ensure that the manifest has the necessary fields set 
      boolean manifestChanged = ManifestDefaultsInjector.updateManifest(applicationManifest, ebaFile.getName(), ebaFile); 
      
      applicationMetadata = _applicationMetadataManager.createApplicationMetadata(applicationManifest);
      
      // Process any other files in the .eba
      // i.e. migrate wars to wabs
      
      // Validate contents
      
      // Perform provisioning
      
      // Create deployment.mf if missing
      
      // Write out updated .eba if we changed its contents
      
    } catch (IOException iox) { 
      throw new ManagementException(iox);
    }
    
    
    // AriesApplication ariesApp = new AriesApplicationImpl (appMeta, bundlesInEba);
    return null; // ariesApp
  }

  public AriesApplication createApplication(URL url) throws ManagementException {
    return null;
  }

  public ApplicationContext getApplicationContext(AriesApplication app) {
    // TODO Auto-generated method stub
    return null;
  }

  public Set<ApplicationContext> getApplicationContexts() {
    // TODO Auto-generated method stub
    return null;
  }

  public ApplicationContext install(AriesApplication app) {
    // TODO Auto-generated method stub
    return null;
  }
  
  public void uninstall(ApplicationContext app) {
    // TODO Auto-generated method stub

  }

  public void addApplicationListener(ApplicationListener l) {
    // Need application listener lifecycle support
  }

  public void removeApplicationListener(ApplicationListener l) {
    // TODO Auto-generated method stub

  }



}
