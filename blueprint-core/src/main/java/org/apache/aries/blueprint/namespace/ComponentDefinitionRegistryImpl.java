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
package org.apache.aries.blueprint.namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentNameAlreadyInUseException;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.reflect.PassThroughMetadataImpl;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Target;

/**
 * ComponentDefinitionRegistry implementation.
 *
 * This implementation uses concurrent lists and maps to store components and converters metadata
 * to allow its use by concurrent threads. 
 *
 * @version $Rev$, $Date$
 */
public class ComponentDefinitionRegistryImpl implements ComponentDefinitionRegistry {

    private final Map<String, ComponentMetadata> components;
    private final List<Target> typeConverters;
    private final Map<ComponentMetadata, List<Interceptor>> interceptors;

    public ComponentDefinitionRegistryImpl() {
        // Use a linked hash map to keep the declaration order 
        components = Collections.synchronizedMap(new LinkedHashMap<String, ComponentMetadata>());
        typeConverters = new CopyOnWriteArrayList<Target>();
        interceptors = Collections.synchronizedMap(new HashMap<ComponentMetadata, List<Interceptor>>());
    }

    public boolean containsComponentDefinition(String name) {
        return components.containsKey(name);
    }

    public ComponentMetadata getComponentDefinition(String name) {
        return components.get(name);
    }

    public Set<String> getComponentDefinitionNames() {
        return Collections.unmodifiableSet(components.keySet());
    }

    public void registerComponentDefinition(ComponentMetadata component) {
        String id = component.getId();
        if (id == null) {
            // TODO: should we generate a unique name?
            throw new IllegalArgumentException("Component must have a valid id");
        }
        if (id.startsWith("blueprint") && !(component instanceof PassThroughMetadataImpl)) {
            // TODO: log a warning
        }
        // TODO: perform other validation: scope, class/runtimeClass/factoryMethod, etc...
        if (components.containsKey(id)) {
            throw new ComponentNameAlreadyInUseException(id);
        }
        components.put(id, component);
    }

    public void removeComponentDefinition(String name) {
        ComponentMetadata removed = components.remove(name);
        if(removed!=null){
            interceptors.remove(removed);
        }
    }

    public void registerTypeConverter(Target component) {
        typeConverters.add(component);
        if (component instanceof ComponentMetadata) {
            registerComponentDefinition((ComponentMetadata) component);
        }
    }

    public List<Target> getTypeConverters() {
        return typeConverters;
    }

    public void registerInterceptorWithComponent(ComponentMetadata component, Interceptor interceptor) {
        if(interceptor!=null){
            List<Interceptor> componentInterceptorList = interceptors.get(component);
            if(componentInterceptorList==null){
                componentInterceptorList = new ArrayList<Interceptor>();
                interceptors.put(component, componentInterceptorList);
            }
            if(!componentInterceptorList.contains(interceptor)){
                componentInterceptorList.add(interceptor);
                Collections.sort(componentInterceptorList, new Comparator<Interceptor>(){
                    public int compare(Interceptor object1, Interceptor object2) {
                        //invert the order so higher ranks are sorted 1st
                        return object2.getRank() - object1.getRank();
                    }
                });
            }
        }
    }

    public List<Interceptor> getInterceptors(ComponentMetadata component) {
        return interceptors.get(component);
    }
    
}
