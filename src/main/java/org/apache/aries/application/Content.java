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
package org.apache.aries.application;

import java.util.Map;


/**
 * this interface describes the content metadata such as Application-Content, Import-Package, etc
 *
 */
public interface Content
{
  /**
   * get the package name of the content
   * @return    the package name of the content
   */
  public String getContentName();
  
  /**
   * get the attributes of the content
   * @return    the attributes of the content
   */
  public Map<String, String> getAttributes();
  
  /**
   * get the directives of the content
   * @return the directives of the content
   */
  public Map<String, String> getDirectives();
  
  /**
   * get the value of the attribute with the specified key
   * @param key  
   * @return   value of the attribute specified by the key
   */
  public String getAttribute(String key);
  
  /**
   * get the value of the directive with the specified key
   * @param key
   * @return    the value of the directive specified by the key
   */
  public String getDirective(String key);
  
  /**
   * get the version info for the version attribute
   * @return null if there is no version associated with this content
   * ASK ALASDAIR: should we return default version 0.0.0 instead of null?
   */
  public VersionRange getVersion();
  
  /**
   * get the attribute and directive info in NameValueMap
   * @return namevalueMap that contains attribute and directive info
   */
  public Map<String, String> getNameValueMap();
}
