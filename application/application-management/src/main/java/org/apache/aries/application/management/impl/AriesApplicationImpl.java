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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.LocalPlatform;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AriesApplicationImpl implements AriesApplication {

  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.application.management");

  private Set<BundleInfo> _bundleInfo;
  private ApplicationMetadata _applicationMetadata;
  private DeploymentMetadata _deploymentMetadata;
  private LocalPlatform _localPlatform;
  
  // Placeholders for information we'll need for store()
  private Map<String, InputStream> _modifiedBundles = null;
  
  public AriesApplicationImpl(ApplicationMetadata meta, Set<BundleInfo> bundleInfo,
      LocalPlatform lp) {
    _applicationMetadata = meta;
    _bundleInfo = bundleInfo;
    _deploymentMetadata = null;
    _localPlatform = lp;
    
  }
  
  public AriesApplicationImpl(ApplicationMetadata meta, DeploymentMetadata dep, 
      Set<BundleInfo> bundleInfo, LocalPlatform lp) {
    _applicationMetadata = meta;
    _bundleInfo = bundleInfo;
    _deploymentMetadata = dep;
    _localPlatform = lp;
    
  }
  
  public ApplicationMetadata getApplicationMetadata() {
    return _applicationMetadata;
  }

  public Set<BundleInfo> getBundleInfo() {
    return _bundleInfo;
  }

  public DeploymentMetadata getDeploymentMetadata() {
    return _deploymentMetadata;
  }
  
  public void setDeploymentMetadata (DeploymentMetadata dm) { 
    _deploymentMetadata = dm;
  }

  public Map<String, InputStream> getModifiedBundles() {
    return _modifiedBundles;
  }

  public void setModifiedBundles (Map<String, InputStream> modifiedBundles) {
    _modifiedBundles = modifiedBundles;
  }
  
  public void setLocalPlatform (LocalPlatform lp) { 
    _localPlatform = lp;
  }

  public boolean isResolved() {
    return getDeploymentMetadata() != null;
  }

  public void store(File f) throws FileNotFoundException, IOException {
    OutputStream os = new FileOutputStream (f);
    store(os);
    os.close();
  }

  /**
   * Construct an eba in a temporary directory
   * Copy the eba to the target output stream 
   * Delete the temporary directory.
   * Leave target output stream open
   */
  public void store(OutputStream targetStream) throws FileNotFoundException, IOException {
 
    //
    // This code will be run on various application server platforms, each of which
    // will have its own policy about where to create temporary directories. We 
    // can't just ask the local filesystem for a temporary directory since it may
    // be quite large: the app server implementation will be better able to select
    // an appropriate location. 
    File tempDir = _localPlatform.getTemporaryDirectory();
    OutputStream out = null;
    InputStream in = null;
    try {
      out = IOUtils.getOutputStream(tempDir, AppConstants.APPLICATION_MF);
      _applicationMetadata.store(out);

    } finally {
      IOUtils.close(out);
    }
    try {
      out = IOUtils.getOutputStream(tempDir, AppConstants.DEPLOYMENT_MF);
      _deploymentMetadata.store(out);
    } finally {
      IOUtils.close(out);
    }
    
    // Write the by-value eba files out
    for (BundleInfo bi : _bundleInfo) { 
      // bi.getLocation() will return a URL to the source bundle. It may be of the form
      // file:/path/to/my/file.jar, or
      // jar:file:/my/path/to/eba.jar!/myBundle.jar
      String bundleLocation = bi.getLocation();
      String bundleFileName = bundleLocation.substring(bundleLocation.lastIndexOf('/') + 1);
      try { 
        out = IOUtils.getOutputStream(tempDir, bundleFileName);
        URL bundleURL = new URL (bundleLocation);
        InputStream is = bundleURL.openStream();
        IOUtils.copy(is, out);
      } finally { 
        IOUtils.close(out);
        IOUtils.close(in);
      }
    }

    // Write the migrated bundles out
    if (_modifiedBundles != null) { 
      for (Map.Entry<String, InputStream> modifiedBundle : _modifiedBundles.entrySet()) {
        try { 
          out = IOUtils.getOutputStream(tempDir, modifiedBundle.getKey());
          IOUtils.copy(modifiedBundle.getValue(), out);
        } finally { 
          IOUtils.close(out);
        }
      }
    }
    
    // We now have an exploded eba in tempDir which we need to copy into targetStream
    IOUtils.zipUp(tempDir, targetStream);
    if (!IOUtils.deleteRecursive(tempDir))
    {
      _logger.warn("APPMANAGEMENT0001E", tempDir);
    }
  }
}
