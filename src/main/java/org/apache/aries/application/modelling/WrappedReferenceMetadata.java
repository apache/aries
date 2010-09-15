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


/**
 * Information about a parsed blueprint reference
 */
public interface WrappedReferenceMetadata
{
  /**
   * Get the properties of the associated blueprint service
   * @return The filter, or null for no filter
   */
  String getFilter();
  
  /**
   * Get the interface required by the reference
   * @return the interface, or null if unspecified
   */
  String getInterface();
  
  /**
   * Get the component-name attribute.
   * @return Service name
   */
  String getComponentName();
 
  
  /**
   * Is this a reference list or a reference
   * @return true if a reference list
   */
  boolean isList();

  /**
   * Is this an optional reference
   * @return true if optional
   */
  boolean isOptional();

  /**
   * Get the reference's id as defined in the blueprint
   * @return the blueprint reference id
   */
  String getId();
}
