/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.context;

import java.util.HashMap;

import javax.persistence.PersistenceContextType;

import org.osgi.framework.Bundle;

public interface PersistenceContextProvider {
  
  /** The key to use when storing the {@link PersistenceContextType} for this context */
  public static final String PERSISTENCE_CONTEXT_TYPE = "org.apache.aries.jpa.context.type";
  
  /**
   * The service property key indicating that a registered EMF is used to create
   * managed persistence contexts
   */
  public static final String PROXY_FACTORY_EMF_ATTRIBUTE = "org.apache.aries.jpa.proxy.factory";
  
  /**
   * This method will be called whenever a persistence context element is processed by the jpa
   * blueprint namespace handler.
   * 
   * @param unitName   The name of the persistence unit
   * @param client     The blueprint bundle that declares the dependency
   * @param properties Properties that should be used to create the persistence unit
   */
  void registerContext(String unitName, Bundle client, HashMap<String,Object> properties);
}
