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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.Version;

public class DeploymentMetadataImpl implements DeploymentMetadata {
  private ApplicationMetadata _applicationMetadata;
  private List<DeploymentContent> _deploymentContent = new ArrayList<DeploymentContent>();
  private List<DeploymentContent> _provisionSharedContent = new ArrayList<DeploymentContent>();
  
  public DeploymentMetadataImpl (AriesApplication app, Set<BundleInfo> bundlesRequired) throws ResolverException
  {
    _applicationMetadata = app.getApplicationMetadata();
    _deploymentContent = new ArrayList<DeploymentContent>();
    _provisionSharedContent = new ArrayList<DeploymentContent>();
    
    Map<String, VersionRange> appContent = new HashMap<String, VersionRange>();
    
    for (Content c : app.getApplicationMetadata().getApplicationContents()) {
      appContent.put(c.getContentName(), c.getVersion());
    }
    
    for (BundleInfo info : bundlesRequired) {
      
      VersionRange range = appContent.get(info.getSymbolicName());
      
      DeploymentContent dp = new DeploymentContentImpl(info.getSymbolicName(), info.getVersion());
      
      if (range == null) {
        _provisionSharedContent.add(dp);
      } else if (range.matches(info.getVersion())) {
        _deploymentContent.add(dp);
      } else {
        throw new ResolverException("Bundle " + info.getSymbolicName() + " at version " + info.getVersion() + " is not in the range " + range);
      }
    }
  }
  
  /**
   * Construct a DeploymentMetadata from Manifest
   * @param src
   * @throws IOException
   */
  public DeploymentMetadataImpl(Manifest mf) { 
    _applicationMetadata = new ApplicationMetadataImpl (mf);

    Attributes attributes = mf.getMainAttributes();
      
    parseContent(attributes.getValue(AppConstants.DEPLOYMENT_CONTENT), _deploymentContent);
    parseContent(attributes.getValue(AppConstants.PROVISION_CONTENT), _provisionSharedContent);
  }

  public List<DeploymentContent> getApplicationDeploymentContents() {
    return Collections.unmodifiableList(_deploymentContent);
  }
  
  public List<DeploymentContent> getApplicationProvisionBundles() {
    return Collections.unmodifiableList(_provisionSharedContent);
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
    if (!_deploymentContent.isEmpty()) {
      attributes.putValue(AppConstants.DEPLOYMENT_CONTENT, getDeploymentContentsAsString(_deploymentContent));
    }
    if (!_provisionSharedContent.isEmpty()) {
      attributes.putValue(AppConstants.PROVISION_CONTENT, getDeploymentContentsAsString(_provisionSharedContent));
    }
    mf.write(out);
  }
  
  
  
  private String getDeploymentContentsAsString (List<DeploymentContent> content) { 
    StringBuilder builder = new StringBuilder();
    for (DeploymentContent dc : content) {
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

  private void parseContent(String content, List<DeploymentContent> contents)
  {
    List<String> pcList = ManifestProcessor.split(content, ",");
    for (String s : pcList) {
      contents.add(new DeploymentContentImpl(s));
    }
  }
}