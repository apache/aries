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
package org.apache.geronimo.blueprint.namespace;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.ComponentNameAlreadyInUseException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.apache.geronimo.blueprint.ExtendedComponentDefinitionRegistry;
import org.apache.geronimo.blueprint.ComponentDefinitionRegistryProcessor;

/**
 * ComponentDefinitionRegistry implementation.
 *
 * This implementation uses concurrent lists and maps to store components and converters metadata
 * to allow its use by concurrent threads. 
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ComponentDefinitionRegistryImpl implements ExtendedComponentDefinitionRegistry {

    private final Map<String, ComponentMetadata> components;
    private final List<Target> typeConverters;
    private String defaultInitMethod;
    private String defaultDestroyMethod;

    public ComponentDefinitionRegistryImpl() {
        components = new ConcurrentHashMap<String, ComponentMetadata>();
        typeConverters = new CopyOnWriteArrayList<Target>();
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
        if (components.containsKey(id)) {
            throw new ComponentNameAlreadyInUseException(id);
        }
        components.put(id, component);
    }

    public void removeComponentDefinition(String name) {
        components.remove(name);
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

    public void setDefaultInitMethod(String method) {
        defaultInitMethod = method;
    }
    
    public String getDefaultInitMethod() {
        return defaultInitMethod;
    }
    
    public void setDefaultDestroyMethod(String method) {
        defaultDestroyMethod = method;
    }
    
    public String getDefaultDestroyMethod() {
        return defaultDestroyMethod;
    }
    
}
