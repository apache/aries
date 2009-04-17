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

import org.apache.xbean.recipe.Option;
import org.apache.xbean.recipe.Repository;
import org.apache.xbean.recipe.DefaultRepository;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.CollectionRecipe;
import org.apache.xbean.recipe.MapRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ReferenceRecipe;
import org.apache.xbean.recipe.Recipe;
import org.osgi.framework.Bundle;
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
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ArrayValue;
import org.osgi.service.blueprint.reflect.ReferenceNameValue;
import org.osgi.service.blueprint.reflect.PropertiesValue;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class Instanciator {

    private ModuleContextImpl moduleContext;
    
    public Instanciator(ModuleContextImpl moduleContext) {
        this.moduleContext = moduleContext;
    }
    
    private void addBuiltinComponents(Repository repository) {
        if (moduleContext != null) {
            repository.add("moduleContext", moduleContext);
            repository.add("bundleContext", moduleContext.getBundleContext());                   
            repository.add("bundle", moduleContext.getBundleContext().getBundle());
            repository.add("conversionService", moduleContext.getConversionService());
        }
    }
    
    public Repository createRepository(ComponentDefinitionRegistry registry) throws Exception {
        Repository repository = new DefaultRepository();
        addBuiltinComponents(repository);
        // Create recipes
        for (String name : (Set<String>) registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            Recipe recipe = createRecipe(component);
            repository.add(name, recipe);
        }
        return repository;
    }

    private Recipe createRecipe(ComponentMetadata component) throws Exception {
        if (component instanceof LocalComponentMetadata) {
            LocalComponentMetadata local = (LocalComponentMetadata) component;
            ObjectRecipe recipe = new BundleObjectRecipe(local.getClassName());
            recipe.allow(Option.PRIVATE_PROPERTIES);
            recipe.setName(component.getName());
            for (PropertyInjectionMetadata property : (Collection<PropertyInjectionMetadata>) local.getPropertyInjectionMetadata()) {
                // TODO: must pass the expected property type
                Object value = getValue(property.getValue(), null);
                recipe.setProperty(property.getName(), value);
            }
            // TODO: constructor args
            // TODO: init-method
            // TODO: destroy-method
            // TODO: lazy
            // TODO: scope
            // TODO: factory-method
            // TODO: factory-component
            return recipe;
        } else {
            // TODO
            throw new IllegalStateException("Unsupported component " + component.getClass());
        }
    }

    private Object getValue(Value v, Class hint) throws Exception {
        if (v instanceof NullValue) {
            return null;
        } else if (v instanceof TypedStringValue) {
            TypedStringValue stringValue = (TypedStringValue) v;            
            Class type = getType(stringValue.getTypeName());
            String value = stringValue.getStringValue();
            if (type != null) {
                if (hint == null || hint.isAssignableFrom(type)) {
                    return convert(value, type);
                } else {
                    throw new Exception("" + type + " " + hint);
                }
            } else if (hint != null) {
                return convert(value, hint);
            } else {
                return value;
            }
        } else if (v instanceof ReferenceValue) {
            String componentName = ((ReferenceValue) v).getComponentName();
            return new ReferenceRecipe(componentName);
        } else if (v instanceof ListValue) {
            ListValue listValue = (ListValue) v;
            Class type = getType(listValue.getValueType());
            CollectionRecipe cr = new CollectionRecipe(ArrayList.class);
            for (Value lv : (List<Value>) listValue.getList()) {
                cr.add(getValue(lv, type));
            }
            return cr;
        } else if (v instanceof SetValue) {
            SetValue setValue = (SetValue) v;
            Class type = getType(setValue.getValueType());
            CollectionRecipe cr = new CollectionRecipe(HashSet.class);
            for (Value lv : (Set<Value>) setValue.getSet()) {
                cr.add(getValue(lv, type));
            }
            return cr;
        } else if (v instanceof MapValue) {
            MapValue mapValue = (MapValue) v;
            Class keyType = getType(mapValue.getKeyType());
            Class valueType = getType(mapValue.getValueType());            
            MapRecipe mr = new MapRecipe(HashMap.class);
            for (Map.Entry<Value,Value> entry : ((Map<Value,Value>) mapValue.getMap()).entrySet()) {
                Object key = getValue(entry.getKey(), keyType);
                Object val = getValue(entry.getValue(), valueType);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof ArrayValue) {
            ArrayValue arrayValue = (ArrayValue) v;
            Class type = getType(arrayValue.getValueType());
            
            // TODO
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        } else if (v instanceof ComponentValue) {
            return createRecipe(((ComponentValue) v).getComponentMetadata());
        } else if (v instanceof PropertiesValue) {
            return ((PropertiesValue) v).getPropertiesValue();
        } else if (v instanceof ReferenceNameValue) {
            return ((ReferenceNameValue) v).getReferenceName();
        } else {
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        }
    }
    
    private Object convert(Object source, Class type) throws Exception {
        return moduleContext.getConversionService().convert(source, type);
    }
    
    private Class getType(String typeName) throws ClassNotFoundException {
        if (typeName == null) {
            return null;
        }
        if (moduleContext == null) {
            return Class.forName(typeName);            
        } else {
            return moduleContext.getBundleContext().getBundle().loadClass(typeName);
        }
    }
    
    private class BundleObjectRecipe extends ObjectRecipe {
        
        String typeName;
        
        public BundleObjectRecipe(String typeName) {
            super(typeName);
            this.typeName = typeName;
        }
        
        @Override
        public Class getType() {
            if (moduleContext == null) {
                return super.getType();
            }
            try {
                return moduleContext.getBundleContext().getBundle().loadClass(typeName);
            } catch (ClassNotFoundException e) {
                throw new ConstructionException("Type class could not be found: " + typeName);
            }
        }
    }

}
