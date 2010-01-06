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

import java.util.Map;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.osgi.framework.Version;

public final class DeploymentContentImpl implements DeploymentContent {
  
  private ContentImpl _content;
  
  /**
   * DeploymentContent relates to a bundle at a particular version. 
   * We can therefore assume that the Version passed into this 
   * constructor is the exact version in question. 
   * @param bundleSymbolicName
   * @param version
   */
  public DeploymentContentImpl (String bundleSymbolicName, Version version) {
    NameValueMap<String, String> nvMap = new NameValueMap<String, String>();
    nvMap.put(AppConstants.DEPLOYMENT_BUNDLE_VERSION, version.toString());
    _content = new ContentImpl (bundleSymbolicName, nvMap);
  }
  
  /**
   * Construct a DeploymentContent from a string of the form, 
   *   bundle.symbolic.name;deployedContent="1.2.3"
   * @param deployedContent
   */
  public DeploymentContentImpl (String deployedContent) {
    _content = new ContentImpl (deployedContent);
  }
  
  public Version getExactVersion() {
    return getVersion().getExactVersion();
  }

  public String getAttribute(String key) {
    return _content.getAttribute(key);
  }

  public Map<String, String> getAttributes() {
    return _content.getAttributes();
  }

  public String getContentName() {
    return _content.getContentName();
  }

  public String getDirective(String key) {
    return _content.getDirective(key);
  }

  public Map<String, String> getDirectives() {
    return _content.getDirectives();
  }

  public VersionRange getVersion() {
    String deployedVersion = _content.getAttribute(AppConstants.DEPLOYMENT_BUNDLE_VERSION);
    VersionRange vr = null;
    if (deployedVersion != null && deployedVersion.length() > 0) {
      vr = ManifestHeaderProcessor.parseVersionRange(deployedVersion, true);
    }
    return vr;
  }

  @Override
  public boolean equals(Object other) { 
    if (other == null) 
      return false;
    if (this == other) 
      return true;
    if (other instanceof DeploymentContentImpl) {
      return _content.equals(((DeploymentContentImpl) other)._content);
    } else { 
      return false;
    }
  }

  public Map<String, String> getNameValueMap() {
    return _content.getNameValueMap();
  }
}
