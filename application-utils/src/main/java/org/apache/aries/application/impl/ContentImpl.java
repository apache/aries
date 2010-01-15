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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.application.Content;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.utils.internal.MessageUtil;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


/**
 * Implementation of Content 
 *
 */
public final class ContentImpl implements Content
{
  private String contentName;
  protected Map<String, String> attributes;
  private Map<String, String> directives;
  private NameValueMap<String, String> nameValueMap;
  
  /**
   * 
   * @param content  Application-Content, Import-Package content
   */
  public ContentImpl(String content) {
    Map<String, NameValueMap<String, String>> appContentsMap = ManifestHeaderProcessor.parseImportString(content);
    if (appContentsMap.size() != 1) {
      throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0004E",content));
    }
    for (Map.Entry<String, NameValueMap<String, String>> entry : appContentsMap.entrySet()) {
      this.contentName = entry.getKey();
      this.nameValueMap= entry.getValue();
      setup();
      break;
    }
  }
  
  public ContentImpl (String bundleSymbolicName, Version version) { 
    this.contentName = bundleSymbolicName;
    this.nameValueMap = new NameValueMap<String, String>();
    nameValueMap.put("version", version.toString());
    setup();
  }
  
  /**
   * 
   * @param contentName  
   * @param nameValueMap
   */
  public ContentImpl(String contentName, NameValueMap<String, String> nameValueMap) {
    this.contentName = contentName;
    this.nameValueMap= nameValueMap;
    setup();
  }
  
  public String getContentName() {
    return this.contentName;
  }
  
  public Map<String, String> getAttributes() {
    return Collections.unmodifiableMap(this.attributes);
  }
  
  public Map<String, String> getDirectives() {
    return Collections.unmodifiableMap(this.directives);
  }
  
  public String getAttribute(String key) {
    String toReturn = this.attributes.get(key);
    return toReturn;
  }
  
  /**
   * add key value to the attributes map
   * @param key
   * @param value
   */
  public void addAttribute(String key, String value) {
    this.attributes.put(key, value);
  }
  
  public String getDirective(String key) {
    String toReturn = this.directives.get(key);
    return toReturn;
  }
  
  public NameValueMap<String, String> getNameValueMap() {
    NameValueMap<String, String> nvm = new NameValueMap<String, String>();
    for (String key : this.nameValueMap.keySet()) {
      nvm.addToCollection(key, this.nameValueMap.get(key));
    }
    return nvm;
  }
  
  /**
   * add key value to the directives map
   * @param key
   * @param value
   */
  public void addDirective(String key, String value) {
    this.directives.put(key, value);
  }
  
  public VersionRange getVersion() {
    VersionRange vi = null;
    if (this.attributes.get(Constants.VERSION_ATTRIBUTE) != null 
        && this.attributes.get(Constants.VERSION_ATTRIBUTE).length() > 0) {
      vi = ManifestHeaderProcessor.parseVersionRange(this.attributes.get(Constants.VERSION_ATTRIBUTE));
    } else {
      // what if version is not specified?  let's interpret it as 0.0.0 
      vi = ManifestHeaderProcessor.parseVersionRange("0.0.0");
    }
    return vi;
  }
  
  @Override
  public String toString()
  {
    return this.contentName + ";" + this.nameValueMap.toString();
  }
  
  @Override
  public boolean equals(Object other)
  {
    if (other == this) return true;
    if (other == null) return false;
    
    if (other instanceof ContentImpl) {
      ContentImpl otherContent = (ContentImpl)other;
      
      Map<String,String> attributesWithoutVersion = attributes;
      
      if (attributes.containsKey("version")) {
        attributesWithoutVersion = new HashMap<String, String>(attributes);
        attributesWithoutVersion.remove("version");
      }
      
      Map<String, String> otherAttributesWithoutVersion = otherContent.attributes;
      
      if (otherContent.attributes.containsKey("version")) {
        otherAttributesWithoutVersion = new HashMap<String, String>(otherContent.attributes);
        otherAttributesWithoutVersion.remove("version");
      }
      
      return contentName.equals(otherContent.contentName) && 
             attributesWithoutVersion.equals(otherAttributesWithoutVersion) &&
             directives.equals(otherContent.directives) &&
             getVersion().equals(otherContent.getVersion());
    }
    
    return false;
  }
  
  @Override
  public int hashCode()
  {
    return contentName.hashCode();
  }
  
  /**
   * set up directives and attributes
   */
  protected void setup() {
    this.attributes = new HashMap<String, String>();
    this.directives = new HashMap<String, String>();
    
    for (String key : this.nameValueMap.keySet()) {
      if (key.endsWith(":")) {
        this.directives.put(key.substring(0, key.length() - 1), this.nameValueMap.get(key));
      } else {
        this.attributes.put(key, this.nameValueMap.get(key));
      }
    }
  }
}
