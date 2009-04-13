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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.osgi.service.blueprint.context;

import java.util.Set;
import java.util.Collection;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.framework.BundleContext;

/**
 * ModuleContext providing access to the components, service exports, and
 * service references of a module. Only bundles in the ACTIVE state may
 * have an associated ModuleContext. A given BundleContext has at most one associated
 * ModuleContext.
 * <p/>
 * An instance of ModuleContext may be obtained from within a module context
 * by implementing the ModuleContextAware interface on a component class.
 * Alternatively you can look up ModuleContext services in the service registry.
 * <p/>
 * The Constants.BUNDLE_SYMBOLICNAME and Constants.BUNDLE_VERSION service
 * properties can be used to determine which bundle the published ModuleContext
 * service is associated with.
 *
 * @see org.osgi.framework.Constants
 */
public interface ModuleContext {

    static int CONFIGURATION_ADMIN_OBJECT_DELETED = 1;
    
    static int BUNDLE_STOPPING = 2;

    /**
     * The names of all the named components within the module context.
     *
     * @return an immutable set (of Strings) containing the names of all of the
     *         components within the module.
     */
    Set getComponentNames();

    /**
     * Get the component instance for a given named component. If the component has
     * not yet been instantiated, calling this operation will cause the component instance
     * to be created and initialized. If the component
     * has a prototype scope then each call to getComponent will return a new
     * component instance. If the component has a bundle scope then the component
     * instance returned will be the instance for the caller's bundle (and that
     * instance will be instantiated if it has not already been created).
     * 
     * Note: calling getComponent from logic executing during the instantiation and
     * configuration of a component, before the init method (if specified) has returned,
     * may trigger a circular dependency (for a trivial example, consider a component
     * that looks itself up by name during its init method). Implementations of the
     * Blueprint Service are not required to support cycles in the dependency graph
     * and may throw an exception if a cycle is detected. Implementations that can
     * support certain kinds of cycles are free to do so.
     *
     * @param name the name of the component for which the instance is to be retrieved
     * @return the component instance, the type of the returned object is dependent
     *         on the component definition, and may be determined by introspecting the
     *         component metadata.
     * @throws NoSuchComponentException if the name specified is not the name of a
     *                                  component within the module.
     */
    Object getComponent(String name) throws NoSuchComponentException;

    /**
     * Get the component metadata for a given named component.
     *
     * @param name the name of the component for which the metadata is to be retrieved.
     * @return the component metadata for the component.
     * @throws NoSuchComponentException if the name specified is not the name of a
     *                                  component within the module.
     */
    ComponentMetadata getComponentMetadata(String name) throws NoSuchComponentException;

    /**
     * Get the service export metadata for every service exported by this
     * module.
     *
     * @return an immutable collection of ServiceExportComponentMetadata, with one entry for each service export.
     */
    Collection getExportedServicesMetadata();

    /**
     * Get the metadata for all components defined locally within this module.
     *
     * @return an immutable collection of LocalComponentMetadata, with one entry for each component.
     */
    Collection getLocalComponentsMetadata();

    /**
     * Get the service reference metadata for every OSGi service referenced by
     * this module.
     *
     * @return an immutable collection of ServiceReferenceComponentMetadata, with one entry for each referenced service.
     */
    Collection getReferencedServicesMetadata();

    /**
     * Get the bundle context of the bundle this module context is
     * associated with.
     *
     * @return the module's bundle context
     */
    BundleContext getBundleContext();

}
