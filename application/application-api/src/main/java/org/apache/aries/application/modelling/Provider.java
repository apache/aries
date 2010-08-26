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



import java.util.Map;


/** Base interface to model a bundle, package or service
 */
public interface Provider
{
  /**
   * Get resource type.
   * @return The resource type.
   */
  ResourceType getType();
  
  /** ResourceType-dependent things. See Capability.getProperties() 
   * Define constants for things like a service's list of interfaces, versions, etc */
  Map<String, Object> getAttributes();
  
  
}