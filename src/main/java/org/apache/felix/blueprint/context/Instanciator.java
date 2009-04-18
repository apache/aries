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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Dictionary;
import java.util.Hashtable;
import java.lang.reflect.Type;

import org.apache.felix.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.felix.blueprint.reflect.ServiceExportComponentMetadataImpl;
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
import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.RecipeHelper;
import org.osgi.service.blueprint.convert.ConversionService;
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
import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.Value;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
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
    
    public Repository createRepository(ComponentDefinitionRegistryImpl registry) throws Exception {
        Repository repository = new DefaultRepository();
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
            ExportedServiceRecipe recipe = new ExportedServiceRecipe((ServiceExportComponentMetadata) component);
            recipe.setName(component.getName());
            return recipe;
        } else {
            // TODO
            throw new IllegalStateException("Unsupported component " + component.getClass());
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

    /**
     * TODO: section 5.4.1.5 : expose a proxy to the ServiceRegistration
     */
    private class ExportedServiceRecipe extends AbstractRecipe {

        private final ServiceExportComponentMetadata metadata;
        private ServiceRegistration serviceRegistration;

        public ExportedServiceRecipe(ServiceExportComponentMetadata metadata) {
            this.metadata = metadata;
        }

        public boolean canCreate(Type type) {
            return true;
        }

        protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
            // TODO: metadata.getRegistrationListeners()
            try {
                Object service = getValue(metadata.getExportedComponent(),  null);
                service = RecipeHelper.convert(Object.class, service, false);
                Set<String> classes;
                switch (metadata.getAutoExportMode()) {
                    case ServiceExportComponentMetadata.EXPORT_MODE_INTERFACES:
                        classes = getImplementedInterfaces(new HashSet<String>(), service.getClass());
                        break;
                    case ServiceExportComponentMetadata.EXPORT_MODE_CLASS_HIERARCHY:
                        classes = getSuperClasses(new HashSet<String>(), service.getClass());
                        break;
                    case ServiceExportComponentMetadata.EXPORT_MODE_ALL:
                        classes = getSuperClasses(new HashSet<String>(), service.getClass());
                        classes = getImplementedInterfaces(classes, service.getClass());
                        break;
                    default:
                        classes = metadata.getInterfaceNames();
                        break;
                }
                Map map = metadata.getServiceProperties();
                if (map == null && metadata instanceof ServiceExportComponentMetadataImpl) {
                    Object val = getValue(((ServiceExportComponentMetadataImpl) metadata).getServicePropertiesValue(), null);
                    map = (Map) RecipeHelper.convert(Map.class, val, false); 
                }
                if (map == null) {
                    map = new HashMap();
                }
                map.put(Constants.SERVICE_RANKING, metadata.getRanking());
                String[] classesArray = classes.toArray(new String[classes.size()]);
                serviceRegistration = moduleContext.getBundleContext().registerService(classesArray, service, new Hashtable(map));
                return serviceRegistration;
            } catch (Exception e) {
                throw new ConstructionException(e);
            }
        }

        private Set<String> getImplementedInterfaces(Set<String> classes, Class clazz) {
            if (clazz != null && clazz != Object.class) {
                for (Class itf : clazz.getInterfaces()) {
                    classes.add(itf.getName());
                    getImplementedInterfaces(classes, itf);
                }
                getImplementedInterfaces(classes, clazz.getSuperclass());
            }
            return classes;
        }

        private Set<String> getSuperClasses(Set<String> classes, Class clazz) {
            if (clazz != null && clazz != Object.class) {
                classes.add(clazz.getName());
                getSuperClasses(classes, clazz.getSuperclass());
            }
            return classes;
        }

    }
            
}
