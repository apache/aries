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
  /** The service property key mapped to the {@link PersistenceProvider} implementation class name */
  public static final String OSGI_UNIT_PROVIDER = "osgi.unit.provider";
  /** The service property key mapped to a Boolean indicating whether this persistence unit is container managed */ 
  public static final String CONTAINER_MANAGED_PERSISTENCE_UNIT = "org.apache.aries.jpa.container.managed";

}
