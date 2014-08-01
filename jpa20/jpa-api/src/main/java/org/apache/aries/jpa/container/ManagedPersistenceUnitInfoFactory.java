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

import java.util.Collection;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A Factory for {@link ManagedPersistenceUnitInfo} objects.
 * 
 * This interface marks a plug-point for implementations that wish to extend
 * the Aries JPA Container by providing customized {@link PersistenceUnitInfo}
 * objects.
 *
 * Customized implementations can be provided in two ways:
 * <ul>
 *   <li>
 *     By setting a config property of the "container" bean in in the blueprint for this bundle.
 *   </li>
 *   <li>
 *     By adding a config property to a properties file added to this bundle using a fragment.
 *     The properties file should be named "org.apache.aries.jpa.container.properties" and be
 *     available at the root of the classpath. This properties file name is available as a
 *     constant ARIES_JPA_CONTAINER_PROPERTIES.
 *   </li>
 * </ul>
 * 
 * Note that properties provided through the properties file will override properties supplied
 * through the blueprint container.
 * 
 * The property key to use is "org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory"
 * and is accessible using the DEFAULT_PU_INFO_FACTORY_KEY field. The value associated with this
 * key should be the class name of the {@link ManagedPersistenceUnitInfoFactory} implementation
 * that should be used. The provided class name must be loadable by this bundle.
 * 
 * Implementations of this interface <b>must</b> have a no-args constructor and be safe for
 * use by multiple concurrent threads.
 * 
 * No locks will be held by the JPA container or framework when calling methods on this interface.
 */
public interface ManagedPersistenceUnitInfoFactory {
  /** 
   * The config property key that should be used to provide a customized
   * {@link ManagedPersistenceUnitInfoFactory} to the Aries JPA Container
   */
  public static final String DEFAULT_PU_INFO_FACTORY_KEY = "org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory";
  /** 
   * The name of the config properties file that can be used to configure
   * the Aries JPA Container 
   */
  public static final String ARIES_JPA_CONTAINER_PROPERTIES = "org.apache.aries.jpa.container.properties";

  /**
   * This method will be called by the Aries JPA container when persistence descriptors have
   * been located in a persistence bundle.
   * 
   * @param containerContext  The {@link BundleContext} for the container bundle. This can be
   *                          used to get services from the service registry when creating
   *                          {@link PersistenceUnitInfo} objects.
   * @param persistenceBundle The {@link Bundle} defining the persistence units. This bundle may
   *                          be in any state, and so may not have a usable {@link BundleContext}
   *                          or be able to load classes.
   * @param providerReference A {@link ServiceReference} for the {@link PersistenceProvider} service
   *                          that will be used to create {@link EntityManagerFactory} objects from
   *                          these persistence units.
   * @param persistenceMetadata A {@link Collection} of {@link ParsedPersistenceUnit} objects containing the
   *                            metadata from the persistence descriptor.
   * @return A Collection of {@link ManagedPersistenceUnitInfo} objects that can be used to create {@link EntityManagerFactory} instances
   */
  public Collection<? extends ManagedPersistenceUnitInfo> createManagedPersistenceUnitMetadata(BundleContext containerContext, Bundle persistenceBundle, ServiceReference providerReference, Collection<ParsedPersistenceUnit> persistenceMetadata);
  
  /**
   * If no persistence units in a persistence bundle specify a JPA {@link PersistenceProvider} 
   * implementation class, then the JPA container will call this method to obtain a default
   * provider to use.
   * @return A {@link PersistenceProvider} implementation class name, or null if no default is
   *         specified.
   */
  public String getDefaultProviderClassName();

  /**
   * This method will be called when the persistence bundle is no longer being managed. This may
   * be because the bundle is being updated, or because the {@link PersistenceProvider} being
   * used is no longer available.
   * 
   * When this method is called implementations should clear up any resources associated with
   * persistence units defined by the persistence bundle. 
   * 
   * @param containerContext  The {@link BundleContext} for the container bundle. 
   * @param persistenceBundle The persistence bundle that is no longer valid.
   */
  public void destroyPersistenceBundle(BundleContext containerContext, Bundle persistenceBundle);
}
