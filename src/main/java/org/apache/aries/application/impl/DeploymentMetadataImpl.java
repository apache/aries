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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.osgi.framework.Version;

public class DeploymentMetadataImpl implements DeploymentMetadata {
  
  ApplicationMetadata _applicationMetadata;
  List<DeploymentContent> _deploymentContent;
  
  public DeploymentMetadataImpl (AriesApplication app, Set<BundleInfo> additionalBundlesRequired) {
    _applicationMetadata = app.getApplicationMetadata();
    _deploymentContent = new ArrayList<DeploymentContent>();
    for (BundleInfo bundleInfo : additionalBundlesRequired) { 
      DeploymentContentImpl dci = new DeploymentContentImpl(bundleInfo.getSymbolicName(), 
          bundleInfo.getVersion()); 
      _deploymentContent.add(dci);
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


  public void store(File f) {
    // TODO when writing AriesApplication.store()
    
  }


  public void store(OutputStream in) {
    // TODO when writing AriesApplication.store()
    
  }

}
