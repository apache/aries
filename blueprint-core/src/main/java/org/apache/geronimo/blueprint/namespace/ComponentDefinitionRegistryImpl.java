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

import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.ComponentNameAlreadyInUseException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.Value;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ComponentDefinitionRegistryImpl implements ComponentDefinitionRegistry {

    private final Map<String, ComponentMetadata> components;
    private final List<Value> typeConverters;
    private String defaultInitMethod;
    private String defaultDestroyMethod;

    public ComponentDefinitionRegistryImpl() {
        components = new HashMap<String, ComponentMetadata>();
        typeConverters = new ArrayList<Value>();
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

    public void registerComponentDefinition(ComponentMetadata component) throws ComponentNameAlreadyInUseException {
        String name = component.getName();
        if (components.containsKey(name)) {
            throw new ComponentNameAlreadyInUseException(name);
        }
        components.put(name, component);
    }

    public void removeComponentDefinition(String name) {
        components.remove(name);
    }

    public void registerTypeConverter(Value typeConverter) {
        typeConverters.add(typeConverter);
    }
    
    public List<Value> getTypeConverters() {
        return Collections.unmodifiableList(typeConverters);
    }
    
    public List<String> getTypeConverterNames() {
        List<String> names = new ArrayList<String>();
        for (Value value : typeConverters) {
            if (value instanceof ComponentValue) {
                ComponentValue componentValue = (ComponentValue) value;
                names.add(componentValue.getComponentMetadata().getName());
            } else if (value instanceof ReferenceValue) {
                ReferenceValue referenceValue = (ReferenceValue) value;
                names.add(referenceValue.getComponentName());
            } else {
                throw new RuntimeException("Unexpected converter type: " + value);
            }
        }
        return names;
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
