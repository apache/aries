/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint;

import java.util.List;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Target;

public interface ComponentDefinitionRegistry  {
    
    /**
     * Determine if the component registry contains a component definition for the given id
     * @param id
     * @return
     */
    boolean containsComponentDefinition(String id);
    
    /**
     * Retrieve a component's metadata by id
     * @param id The id of the component. This is either the id specified in the Blueprint xml or the
     * generated id of an unnamed component
     * @return the <code>ComponentMetadata</code> or <code>null</code> if the id does not match
     * any registered component 
     */
    ComponentMetadata getComponentDefinition(String id);
    
    /**
     * Returns a set of the id of top-level blueprint components (both named and unnamed).
     * 
     * The ids of unnamed components are Blueprint generated. Anonymous components, which have no
     * id, are not part of the set.
     * @return
     */
    Set<String> getComponentDefinitionNames();
    
    /**
     * Register a new component
     * 
     * The <code>ComponentMetadata</code> argument must have an id. So unnamed components should have an id 
     * generated prior to invoking this method. Also, no component definition may already be registered
     * under the same id.
     * 
     * @param component the component to be registered
     * @throws IllegalArgumentException if the component has no id 
     * @throws ComponentNameAlreadyInUseException if there already exists a component definition
     * in the registry with the same id
     */
    void registerComponentDefinition(ComponentMetadata component);
    
    /**
     * Remove the component definition with a given id
     * 
     * If no component is registered under the id, this method is a no-op.
     * @param id the id of the component definition to be removed
     */
    void removeComponentDefinition(String id);

    void registerTypeConverter(Target component);

    List<Target> getTypeConverters();
    
    /**
     * Register an interceptor for a given component
     * 
     * Since the interceptor is registered against a <code>ComponentMetadata</code> instance and not an id,
     * interceptors can be registered for anonymous components as well as named and unnamed components.
     * 
     * Note: Although an interceptor is registered against a specific <code>ComponentMetadata</code> instance,
     * an interceptor should not rely on this fact. This will allow <code>NamespaceHandlers</code> and 
     * <code>ComponentDefinitionRegistryProcessors</code> to respect registered interceptors even when
     * the actual <code>ComponentMetadata</code> instance is changed or augmented. If an interceptor does
     * not support such a scenario it should nevertheless fail gracefully in the case of modified 
     * <code>ComponentMetadata</code> instances.
     * 
     * Note: at the time of this writing (version 0.1) interceptors are only supported for <code>BeanMetadata</code>. 
     * Interceptors registered against other component types will be ignored.
     * 
     * @param component the component the interceptor is to be registered against
     * @param interceptor the interceptor to be used
     */
    void registerInterceptorWithComponent(ComponentMetadata component, Interceptor interceptor);
    
    /**
     * Retrieve all interceptors registered against a <code>ComponentMetadata</code> instance
     * @param component
     * @return a list of interceptors sorted by decreasing rank. The list may be empty if no interceptors have been defined
     */
    List<Interceptor> getInterceptors(ComponentMetadata component);
    
}
