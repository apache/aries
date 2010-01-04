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
import org.osgi.framework.Version;

public class DeploymentContentImpl implements DeploymentContent {
  
  private ContentImpl _content;
  
  public DeploymentContentImpl (String content) { 
    _content = new ContentImpl (content);
  }
  
  public DeploymentContentImpl (String bundleSymbolicName, Version version) { 
    _content = new ContentImpl (bundleSymbolicName, version);
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
    return _content.getVersion();
  }

}
