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
package org.apache.geronimo.blueprint.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.reflect.ServiceExportComponentMetadataImpl;
import org.apache.xbean.recipe.ArrayRecipe;
import org.apache.xbean.recipe.CollectionRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.DefaultRepository;
import org.apache.xbean.recipe.MapRecipe;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Option;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.ReferenceRecipe;
import org.apache.xbean.recipe.Repository;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.reflect.ArrayValue;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MapValue;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.PropertiesValue;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.ReferenceNameValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.Value;
import org.osgi.service.blueprint.reflect.UnaryServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadata;

/**
 * TODO: javadoc
 *
 * TODO: compound property names
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class Instanciator {

    private static Map<String, Class> primitiveClasses = new HashMap<String, Class>();
    
    static {
        primitiveClasses.put("int", int.class);
        primitiveClasses.put("short", short.class);
        primitiveClasses.put("long", long.class);
        primitiveClasses.put("byte", byte.class);
        primitiveClasses.put("char", char.class);
        primitiveClasses.put("float", float.class);
        primitiveClasses.put("double", double.class);
        primitiveClasses.put("boolean", boolean.class);
    }
    
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
    
    public Repository createRepository() throws Exception {
        ComponentDefinitionRegistryImpl registry = (ComponentDefinitionRegistryImpl)getComponentDefinitionRegistry();
        Repository repository = new ScopedRepository();
        addBuiltinComponents(repository);
        
        // Create type-converter recipes
        for (Value value : registry.getTypeConverters()) {
            if (value instanceof ComponentValue) {
                Recipe recipe = (Recipe) getValue(value, null);
                repository.add(recipe.getName(), recipe);
            } else if (value instanceof ReferenceValue) {
                ReferenceRecipe recipe = (ReferenceRecipe) getValue(value, null);
                repository.add(recipe.getReferenceName(), recipe);
            } else {
                throw new RuntimeException("Unexpected converter type: " + value);
            }
        }
        
        // Create component recipes
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
        } else if (component instanceof ServiceExportComponentMetadata) {
            ServiceExportComponentMetadata serviceExport = (ServiceExportComponentMetadata) component;
            ObjectRecipe recipe = new ObjectRecipe(ServiceRegistrationProxy.class);
            recipe.allow(Option.PRIVATE_PROPERTIES);
            recipe.setName(component.getName());
            recipe.setProperty("moduleContext", moduleContext);
            LocalComponentMetadata exportedComponent = getServiceComponent(serviceExport.getExportedComponent());
            if (LocalComponentMetadata.SCOPE_BUNDLE.equals(exportedComponent.getScope())) {
                Recipe exportedComponentRecipe = createRecipe(exportedComponent);
                recipe.setProperty("service", new BundleScopeServiceFactory(moduleContext, exportedComponentRecipe));
            } else {
                recipe.setProperty("service", getValue(serviceExport.getExportedComponent(), null));
            }
            recipe.setProperty("metadata", component);
            if (component instanceof ServiceExportComponentMetadataImpl) {
                ServiceExportComponentMetadataImpl impl = (ServiceExportComponentMetadataImpl) component;
                if (impl.getServicePropertiesValue() != null) {
                    recipe.setProperty("serviceProperties", getValue(impl.getServicePropertiesValue(), null));
                }
            }
            if (serviceExport.getRegistrationListeners() != null) {
                CollectionRecipe cr = new CollectionRecipe(ArrayList.class);;
                for (RegistrationListenerMetadata listener : (Collection<RegistrationListenerMetadata>)serviceExport.getRegistrationListeners()) {
                    cr.add(createRecipe(listener));
                }
                recipe.setProperty("listeners", cr);
            }
            return recipe;
        } else if (component instanceof UnaryServiceReferenceComponentMetadata) {
            // TODO
            throw new IllegalStateException("Unsupported component type " + component.getClass());
        } else if (component instanceof CollectionBasedServiceReferenceComponentMetadata) {
            // TODO
            throw new IllegalStateException("Unsupported component type " + component.getClass());
        } else {
            throw new IllegalStateException("Unsupported component type " + component.getClass());
        }
    }

    private Recipe createRecipe(RegistrationListenerMetadata listener) throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(ServiceRegistrationProxy.Listener.class);
        recipe.allow(Option.PRIVATE_PROPERTIES);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        recipe.setProperty("metadata", listener);
        return recipe;
    }
    
    private LocalComponentMetadata getServiceComponent(Value value) throws Exception {
        if (value instanceof ReferenceValue) {
            ReferenceValue ref = (ReferenceValue) value;
            ComponentDefinitionRegistry registry = getComponentDefinitionRegistry();
            return (LocalComponentMetadata) registry.getComponentDefinition(ref.getComponentName());
        } else if (value instanceof ComponentValue) {
            ComponentValue comp = (ComponentValue) value;
            return (LocalComponentMetadata) comp.getComponentMetadata();
        } else {
            throw new RuntimeException("Unexpected component value: " + value);
        }
    }
    
    private Object getValue(Value v, Class groupingType) throws Exception {
        if (v instanceof NullValue) {
            return null;
        } else if (v instanceof TypedStringValue) {
            TypedStringValue stringValue = (TypedStringValue) v; 
            String value = stringValue.getStringValue();
            Class type = loadType(stringValue.getTypeName());
            return new ValueRecipe(getConversionService(), value, type, groupingType);
        } else if (v instanceof ReferenceValue) {
            String componentName = ((ReferenceValue) v).getComponentName();
            return new ReferenceRecipe(componentName);
        } else if (v instanceof ListValue) {
            ListValue listValue = (ListValue) v;
            Class type = loadType(listValue.getValueType());
            CollectionRecipe cr = new CollectionRecipe(ArrayList.class);
            for (Value lv : (List<Value>) listValue.getList()) {
                cr.add(getValue(lv, type));
            }
            return cr;
        } else if (v instanceof SetValue) {
            SetValue setValue = (SetValue) v;
            Class type = loadType(setValue.getValueType());
            CollectionRecipe cr = new CollectionRecipe(HashSet.class);
            for (Value lv : (Set<Value>) setValue.getSet()) {
                cr.add(getValue(lv, type));
            }
            return cr;
        } else if (v instanceof MapValue) {
            MapValue mapValue = (MapValue) v;
            Class keyType = loadType(mapValue.getKeyType());
            Class valueType = loadType(mapValue.getValueType());            
            MapRecipe mr = new MapRecipe(HashMap.class);
            for (Map.Entry<Value,Value> entry : ((Map<Value,Value>) mapValue.getMap()).entrySet()) {
                Object key = getValue(entry.getKey(), keyType);
                Object val = getValue(entry.getValue(), valueType);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof ArrayValue) {
            ArrayValue arrayValue = (ArrayValue) v;
            Class type = loadType(arrayValue.getValueType());
            ArrayRecipe ar = (type == null) ? new ArrayRecipe() : new ArrayRecipe(type);
            for (Value value : arrayValue.getArray()) {
                ar.add(getValue(value, type));
            }
            return ar;
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
    
    protected ComponentDefinitionRegistry getComponentDefinitionRegistry() {
        return moduleContext.getComponentDefinitionRegistry();
    }
    
    protected ConversionService getConversionService() {
        return moduleContext.getConversionService();
    }
    
    private Class loadType(String typeName) throws ClassNotFoundException {
        if (typeName == null) {
            return null;
        }

        Class clazz = primitiveClasses.get(typeName);
        if (clazz == null) {
            if (moduleContext == null) {
                clazz = Class.forName(typeName);            
            } else {
                clazz = moduleContext.getBundleContext().getBundle().loadClass(typeName);
            }
        }
        return clazz;
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
