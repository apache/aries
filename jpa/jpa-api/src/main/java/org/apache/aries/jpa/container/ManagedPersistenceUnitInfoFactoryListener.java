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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The <tt>ManagedPersistenceUnitInfoFactoryListener</tt> service is called by 
 * {@link ManagedPersistenceUnitInfoFactory} when persistence bundle is detected or is no longer being managed.
 * The <tt>ManagedPersistenceUnitInfoFactoryListener</tt> can be used to performs additional actions
 * when persistence bundles get installed or uninstalled, etc.
 */
public interface ManagedPersistenceUnitInfoFactoryListener {
 
    /**
     * This method will be called by the Aries JPA container when persistence descriptors have
     * been located in a persistence bundle.
     * 
     * @param containerContext  The {@link BundleContext} for the container bundle. This can be
     *                          used to get services from the service registry.
     * @param persistenceBundle The {@link Bundle} defining the persistence units. This bundle may
     *                          be in any state, and so may not have a usable {@link BundleContext}
     *                          or be able to load classes.
     * @param providerReference A {@link ServiceReference} for the {@link PersistenceProvider} service
     *                          that will be used to create {@link EntityManagerFactory} objects from
     *                          these persistence units.
     */
    public void persistenceUnitMetadataCreated(BundleContext containerContext, 
                                               Bundle persistenceBundle, 
                                               ServiceReference providerReference, 
                                               Collection<ManagedPersistenceUnitInfo> managedUnits);
  
    /**
     * This method will be called when the persistence bundle is no longer being managed. This may
     * be because the bundle is being updated, or because the {@link PersistenceProvider} being
     * used is no longer available.
     * 
     * @param containerContext  The {@link BundleContext} for the container bundle. 
     * @param persistenceBundle The persistence bundle that is no longer valid.
     */
    public void persistenceBundleDestroyed(BundleContext containerContext, 
                                           Bundle persistenceBundle);
}
