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
package org.apache.aries.application.modelling;


/* Base interface for a model of a requirement, or need for something, such as a bundle, 
 * package or service. 
 */
public interface Consumer
{
  /**
   * Return the resource type
   * @return the resource type, such as BUNDLE, PACKAGE, SERVICE, OTHER
   */
  ResourceType getType();
  
  /**
   * This is not the same as the filter which you might get, say, by parsing a blueprint
   * reference. It is more specific - and usable at runtime.  
   * 
   * @return String filter matching every property required by this consumer
   */
  String getAttributeFilter();
  /**
   * Whether the resources consumed can be multiple.
   * @return true if multiple resources can be consumed.
   */
  boolean isMultiple();

  /**
   * Whether the resource consumed can be optional.
   * @return true if optional.
   */
  boolean isOptional();

  /**
   * Whether the provider object satisfies the consume criteria.
   * @param capability The provider capability
   * @return true if the capability satisfies the consuming criteria.
   */
  boolean isSatisfied(Provider capability);
  
}
