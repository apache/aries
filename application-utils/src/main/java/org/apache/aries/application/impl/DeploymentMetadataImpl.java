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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.Version;

public class DeploymentMetadataImpl implements DeploymentMetadata {
  
  ApplicationMetadata _applicationMetadata;
  List<DeploymentContent> _deploymentContent;
  
  public DeploymentMetadataImpl (AriesApplication app, Set<BundleInfo> additionalBundlesRequired) {
    _applicationMetadata = app.getApplicationMetadata();
    _deploymentContent = new ArrayList<DeploymentContent>();
    
    // DeploymentContent needs to list everything in the application content
    // plus all the bundles in additonalBundlesRequired
    for (Content c: _applicationMetadata.getApplicationContents()) { 
      _deploymentContent.add(new DeploymentContentImpl(c.getContentName(), c.getVersion().getMinimumVersion()));
    }
    for (BundleInfo bundleInfo : additionalBundlesRequired) { 
      DeploymentContentImpl dci = new DeploymentContentImpl(bundleInfo.getSymbolicName(), 
          bundleInfo.getVersion()); 
      _deploymentContent.add(dci);
    }
  }
  
  /**
   * Construct a DeploymentMetadata from an IFile
   * @param src
   * @throws IOException
   */
  public DeploymentMetadataImpl (IFile src) throws IOException { 
    InputStream is = src.open();
    try { 
      // Populate application symbolic name and version fields
      Manifest mf = ManifestProcessor.parseManifest(is);
      _applicationMetadata = new ApplicationMetadataImpl (mf);

      Attributes attributes = mf.getMainAttributes();
      String deploymentContent = attributes.getValue(AppConstants.DEPLOYMENT_CONTENT);
      List<String> dcList = ManifestProcessor.split(deploymentContent, ",");
      _deploymentContent = new ArrayList<DeploymentContent>();
      for (String s : dcList) { 
        _deploymentContent.add(new DeploymentContentImpl(s));
      }
    } finally { 
      is.close();
    }
  }

  public List<DeploymentContent> getApplicationDeploymentContents() {
    return Collections.unmodifiableList(_deploymentContent);
  }

  public ApplicationMetadata getApplicationMetadata() {
    return _applicationMetadata;
  }

  public String getApplicationSymbolicName() {
    return _applicationMetadata.getApplicationSymbolicName();
  }

  public Version getApplicationVersion() {
    return _applicationMetadata.getApplicationVersion();
  }


  public void store(File f) throws FileNotFoundException, IOException{
    FileOutputStream fos = new FileOutputStream (f);
    store(fos);
    fos.close();
  }

  public void store(OutputStream out) throws IOException {
    // We weren't built from a Manifest, so construct one. 
    Manifest mf = new Manifest();
    Attributes attributes = mf.getMainAttributes();
    attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), AppConstants.MANIFEST_VERSION);
    attributes.putValue(AppConstants.APPLICATION_VERSION, getApplicationVersion().toString());
    attributes.putValue(AppConstants.APPLICATION_SYMBOLIC_NAME, getApplicationSymbolicName());
    attributes.putValue(AppConstants.DEPLOYMENT_CONTENT, getDeploymentContentsAsString());
    mf.write(out);
  }
  
  
  
  private String getDeploymentContentsAsString () { 
    StringBuilder builder = new StringBuilder();
    for (DeploymentContent dc : getApplicationDeploymentContents()) {
      builder.append(dc.getContentName());
      builder.append(';' + AppConstants.DEPLOYMENT_BUNDLE_VERSION + "=");
      builder.append(dc.getExactVersion());
      builder.append(",");
    }
    if (builder.length() > 0) { 
      builder.deleteCharAt(builder.length() - 1);
    }
    return builder.toString();
  }

}
