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
package org.apache.felix.blueprint.context;

import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.xbean.recipe.Repository;
import org.apache.xbean.recipe.DefaultRepository;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.CollectionRecipe;
import org.apache.xbean.recipe.MapRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.MapValue;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class Instanciator {

    public static Repository createRepository(ComponentDefinitionRegistry registry) throws Exception {
        Repository repository = new DefaultRepository();
        // Create recipes
        for (String name : (Set<String>) registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            if (component instanceof LocalComponentMetadata) {
                LocalComponentMetadata local = (LocalComponentMetadata) component;
                ObjectRecipe recipe = new ObjectRecipe(local.getClassName());
                recipe.setName(name);
                repository.add(name, recipe);
            } else {
                throw new IllegalStateException("Unsupported component " + component.getClass());
            }
        }
        // Populate recipes
        for (String name : (Set<String>) registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            if (component instanceof LocalComponentMetadata) {
                LocalComponentMetadata local = (LocalComponentMetadata) component;
                ObjectRecipe recipe = (ObjectRecipe) repository.get(local.getName());
                for (PropertyInjectionMetadata property : (Collection<PropertyInjectionMetadata>) local.getPropertyInjectionMetadata()) {
                    Object value = getValue(repository, property.getValue());
                    recipe.setProperty(property.getName(), value);
                }
                // TODO: constructor args
                // TODO: init-method
                // TODO: destroy-method
                // TODO: lazy
                // TODO: scope
                // TODO: factory-method
                // TODO: factory-component
            } else {
                // TODO
                throw new IllegalStateException("Unsupported component " + component.getClass());
            }
        }
        return repository;
    }

    private static Object getValue(Repository repository, Value v) {
        Object value;
        if (v instanceof NullValue) {
            value = null;
        } else if (v instanceof TypedStringValue) {
            value = ((TypedStringValue) v).getStringValue();
            // TODO: type name ?
        } else if (v instanceof ReferenceValue) {
            String componentName = ((ReferenceValue) v).getComponentName();
            if (repository.contains(componentName)) {
                value = repository.get(componentName);
            } else {
                throw new IllegalStateException("Undefined reference: " + componentName);
            }
        } else if (v instanceof ListValue) {
            CollectionRecipe cr = new CollectionRecipe(ArrayList.class);
            for (Value lv : (List<Value>) ((ListValue) v).getList()) {
                cr.add(getValue(repository, lv));
            }
            value = cr;
            // TODO: ListValue#getValueType()
        } else if (v instanceof SetValue) {
            CollectionRecipe cr = new CollectionRecipe(HashSet.class);
            for (Value lv : (Set<Value>) ((SetValue) v).getSet()) {
                cr.add(getValue(repository, lv));
            }
            value = cr;
            // TODO: SetValue#getValueType()
        } else if (v instanceof MapValue) {
            MapRecipe mr = new MapRecipe(HashMap.class);
            for (Map.Entry<Value,Value> entry : ((Map<Value,Value>) ((MapValue) v).getMap()).entrySet()) {
                Object key = getValue(repository, entry.getKey());
                Object val = getValue(repository, entry.getValue());
                mr.put(key, val);
            }
            value = mr;
            // TODO: MapValue#getKeyType()
            // TODO: MapValue#getValueType()
        } else {
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        }
        return value;
    }

}
