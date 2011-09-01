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
package org.apache.aries.jpa.container;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * This interface is used to provide the Aries JPA container with
 * the information it needs to create a container {@link EntityManagerFactory}
 * using the {@link PersistenceProvider} service from a JPA provider.
 * Instances of this interface should be obtained from a 
 * {@link ManagedPersistenceUnitInfoFactory}
 */
public interface ManagedPersistenceUnitInfo {

  /**
   * Get the {@link PersistenceUnitInfo} object for this persistence unit.
   * This method should only be called when the backing bundle for the
   * persistence unit has been resolved. If the bundle later becomes unresolved
   * then any {@link PersistenceUnitInfo} objects obtained from this object
   * will be discarded.
   * 
   * @return A {@link PersistenceUnitInfo} that can be used to create an {@link EntityManagerFactory}
   */
  public PersistenceUnitInfo getPersistenceUnitInfo();
  
  /**
   * Get a {@link Map} of continer properties to pass to the {@link PersistenceProvider}
   * when creating an {@link EntityManagerFactory}.  
   * @return A {@link Map} of properties, or null if no properties are needed.
   */
  public Map<String, Object> getContainerProperties();
  
  /**
   * Called to indicate that this persistence unit has been registered in the OSGi
   * service registry. Note that because this method is called after the service
   * is registered other threads and listeners may have already accessed the 
   * persistence unit service.
   */
  public void registered();
  
  /**
   * Called to indicate that this persistence unit has been unregistered from the OSGi
   * service registry. 
   */
  public void unregistered();
}
