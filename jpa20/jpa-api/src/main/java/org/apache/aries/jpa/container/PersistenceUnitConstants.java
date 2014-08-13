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

import javax.persistence.spi.PersistenceProvider;

/**
 * Constants used when registering Persistence Units in the service registry
 */
public interface PersistenceUnitConstants {
  /** The service property key mapped to the persistence unit name */
  public static final String OSGI_UNIT_NAME = "osgi.unit.name";
  /** The version of the persistence bundle. */
  public static final String OSGI_UNIT_VERSION = "osgi.unit.version";
  /** The service property key mapped to the {@link PersistenceProvider} implementation class name */
  public static final String OSGI_UNIT_PROVIDER = "osgi.unit.provider";
  /** The service property key mapped to a Boolean indicating whether this persistence unit is container managed */ 
  public static final String CONTAINER_MANAGED_PERSISTENCE_UNIT = "org.apache.aries.jpa.container.managed";
  /** The service property key mapped to a Boolean indicating whether this persistence unit has the default (empty string) unit name 
   *  This allows clients to filter for empty string persistence unit names.
   */
  public static final String EMPTY_PERSISTENCE_UNIT_NAME = "org.apache.aries.jpa.default.unit.name";
  
  /**
   * This property determines whether the Aries JPA container should monitor for DataSourceFactories and only
   * register the EMF when the DataSource is available
   */
  public static final String USE_DATA_SOURCE_FACTORY = "org.apache.aries.jpa.use.data.source.factory";
  
  /**
   * This property name is used to store the JDBC driver class name when using DataSourceFactory integration
   */
  public static final String DATA_SOURCE_FACTORY_CLASS_NAME = "org.apache.aries.jpa.data.source.factory.class";
}
