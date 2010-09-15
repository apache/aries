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


import java.util.Collection;
import java.util.Map;

/**
 * A proxy for org.osgi.service.blueprint.reflect.ServiceMetadata, which we cannot
 * pass to clients in the outer framework. We'll just expose the methods that we 
 * know we're going to need. 
 *
 */
public interface WrappedServiceMetadata extends Comparable<WrappedServiceMetadata> {

  /**
   * Get the properties of the associated blueprint service
   * @return Service properties. The values in the Map will be either String or String[]. 
   */
  Map<String, Object> getServiceProperties();
  
  /**
   * Get the interfaces implemented by the service
   * @return List of interfaces
   */
  Collection<String> getInterfaces();
  
  /**
   * Get the service name. This we hope will be short, human-readable, and unique.
   * @return Service name 
   */
  String getName();
  
  /**
   * Get the service ranking
   * @return ranking
   */
  int getRanking();
  
  /**
   * Sometimes we want to know if two services are identical except for their names
   * @param w A wrapped service metadata for comparison.
   * @return true if the two services are indistinguishable (in the OSGi service registry) - 
   * this will be true if only their names are different, and their interfaces and service 
   * properties the same. 
   */
  boolean identicalOrDiffersOnlyByName (WrappedServiceMetadata w);
}
