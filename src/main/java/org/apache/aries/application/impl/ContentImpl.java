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


/**
 * Implementation of Content 
 *
 */
public final class ContentImpl implements Content
{
  private String content;
  private String contentName;
  protected Map<String, String> attributes;
  private Map<String, String> directives;
  
  /**
   * 
   * @param content  Application-Content, Import-Package content
   */
  public ContentImpl(String content) {
    this.content = content;
    this.attributes = new HashMap<String, String>();
    this.directives = new HashMap<String, String>();
    setup(content, this.attributes, this.directives);
  }
  
  public String getContent() {
    return this.content;
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
    if (this.attributes.get("version") != null && this.attributes.get("version").length() > 0) {
      vi = new VersionRangeImpl(this.attributes.get("version"));
    } else {
      vi = new VersionRangeImpl("0.0.0");
    }
    return vi;
  }
  
  @Override
  public String toString()
  {
    return content;
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
   * setup attributes and directives from the Application-Content or Import-Package
   * @param content
   * @param attributes
   * @param directives
   */
  protected void setup(String content, Map<String, String> attributes, Map<String, String> directives)
  {
    String[] tokens = content.split(";");
    if (tokens.length < 1) {
      throw new IllegalArgumentException("Invalid content: " + content);
    }
    this.contentName = tokens[0].trim();
    for (int i = 1; i < tokens.length; i++) {
      int pos = tokens[i].indexOf('=');
      if (pos != -1) {
        if (pos > 0 && tokens[i].charAt(pos - 1) == ':') {
          String name = tokens[i].substring(0, pos - 1).trim();
          String value = tokens[i].substring(pos + 1).trim();
          directives.put(name, trimDoubleQuotes(value));
        } else {
          String name = tokens[i].substring(0, pos).trim();
          String value = tokens[i].substring(pos + 1).trim();
          attributes.put(name, trimDoubleQuotes(value));
        }
      }
    }
  }
  
  /**
   * this method trims the double quotes at the beginning and end, for example version="1.0.0"
   * @param value
   * @return
   */
  private String trimDoubleQuotes(String value) {
    if (value.startsWith("\"") && value.endsWith("\"")) {
      value = value.substring(1, value.length() -1);
    }   
    return value;
  }
}
