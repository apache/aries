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
package org.apache.geronimo.blueprint.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.ExtendedComponentDefinitionRegistry;
import org.apache.geronimo.blueprint.di.ArrayRecipe;
import org.apache.geronimo.blueprint.di.CollectionRecipe;
import org.apache.geronimo.blueprint.di.ConstructionException;
import org.apache.geronimo.blueprint.di.DefaultRepository;
import org.apache.geronimo.blueprint.di.MapRecipe;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.di.ReferenceNameRecipe;
import org.apache.geronimo.blueprint.di.ReferenceRecipe;
import org.apache.geronimo.blueprint.di.ValueRecipe;
import org.apache.geronimo.blueprint.mutable.MutableMapMetadata;
import org.apache.geronimo.blueprint.reflect.MetadataUtil;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.Listener;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class RecipeBuilder {

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
    
    private int nameCounter;
    private BlueprintContainerImpl blueprintContainer;
    private ExtendedComponentDefinitionRegistry registry;

    public RecipeBuilder(BlueprintContainerImpl blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
        this.registry = blueprintContainer.getComponentDefinitionRegistry();
    }
    
    private void addBuiltinComponents(DefaultRepository repository) {
        if (blueprintContainer != null) {
            repository.putDefault("blueprintContainer", blueprintContainer);
            repository.putDefault("bundleContext", blueprintContainer.getBundleContext());
            repository.putDefault("bundle", blueprintContainer.getBundleContext().getBundle());
            repository.putDefault("conversionService", blueprintContainer.getConversionService());
        }
    }
    
    public DefaultRepository createRepository() throws Exception {
        DefaultRepository repository = new DefaultRepository();
        addBuiltinComponents(repository);
        
        // Create component recipes
        for (String name : registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            Recipe recipe = createRecipe(component);
            addRecipe(repository, recipe);
        }
        return repository;
    }

    private void addRecipe(DefaultRepository repository, Recipe recipe) {
        repository.add(recipe.getName(), recipe); 
    }

    public Recipe createRecipe(ComponentMetadata component) throws Exception {
        if (component instanceof BeanMetadata) {
            return createBeanRecipe((BeanMetadata) component);
        } else if (component instanceof ServiceMetadata) {
            return createServiceRecipe((ServiceMetadata) component);
        } else if (component instanceof ReferenceMetadata) {
            return createUnaryServiceReferenceRecipe((ReferenceMetadata) component);
        } else if (component instanceof RefCollectionMetadata) {
            return createCollectionBasedServiceReferenceRecipe((RefCollectionMetadata) component);
        } else {
            throw new IllegalStateException("Unsupported component type " + component.getClass());
        }
    }

    private Recipe createCollectionBasedServiceReferenceRecipe(RefCollectionMetadata metadata) throws Exception {
        CollectionRecipe listenersRecipe = null;
        if (metadata.getServiceListeners() != null) {
            listenersRecipe = new CollectionRecipe(ArrayList.class);
            for (Listener listener : metadata.getServiceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        Recipe comparatorRecipe = null;
        if (metadata.getComparator() != null) {
            comparatorRecipe = getValue(metadata.getComparator(), Comparator.class);
        }
        CollectionBasedServiceReferenceRecipe recipe = new CollectionBasedServiceReferenceRecipe(
                blueprintContainer,
                                                                   blueprintContainer.getSender(),
                                                                   metadata,
                                                                   listenersRecipe,
                                                                   comparatorRecipe);
        recipe.setAllowPartial(true);
        recipe.setName(getName(metadata.getId()));
        return recipe;
    }

    private UnaryServiceReferenceRecipe createUnaryServiceReferenceRecipe(ReferenceMetadata metadata) throws Exception {
        CollectionRecipe listenersRecipe = null;
        if (metadata.getServiceListeners() != null) {
            listenersRecipe = new CollectionRecipe(ArrayList.class);
            for (Listener listener : metadata.getServiceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        UnaryServiceReferenceRecipe recipe = new UnaryServiceReferenceRecipe(blueprintContainer,
                                                                             blueprintContainer.getSender(),
                                                                             metadata,
                                                                             listenersRecipe);
        recipe.setAllowPartial(true);
        recipe.setName(getName(metadata.getId()));
        return recipe;
    }

    private BlueprintObjectRecipe createServiceRecipe(ServiceMetadata serviceExport) throws Exception {
        BlueprintObjectRecipe recipe = new BlueprintObjectRecipe(blueprintContainer, ServiceRegistrationProxy.class);
        recipe.setName(getName(serviceExport.getId()));
        recipe.setExplicitDependencies(serviceExport.getExplicitDependencies());
        recipe.setInitMethod("init");
        recipe.setAllowPartial(true);
        recipe.setProperty("blueprintContainer", blueprintContainer);
        BeanMetadata exportedComponent = getLocalServiceComponent(serviceExport.getServiceComponent());
        if (exportedComponent != null && BeanMetadata.SCOPE_BUNDLE.equals(exportedComponent.getScope())) {
            BlueprintObjectRecipe exportedComponentRecipe = createBeanRecipe(exportedComponent);
            recipe.setProperty("service", new BundleScopeServiceFactory(blueprintContainer, exportedComponentRecipe));
        } else {
            recipe.setProperty("service", getValue(serviceExport.getServiceComponent(), null));
        }
        recipe.setProperty("metadata", serviceExport);
        Recipe propertiesRecipe = getServicePropertiesRecipe(serviceExport);
        if (propertiesRecipe != null) {
            recipe.setProperty("serviceProperties", propertiesRecipe);
        }
        if (serviceExport.getRegistrationListeners() != null) {
            CollectionRecipe listenersRecipe = new CollectionRecipe(ArrayList.class);
            for (RegistrationListener listener : serviceExport.getRegistrationListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
            recipe.setProperty("listeners", listenersRecipe);
        }
        return recipe;
    }

    protected Recipe getServicePropertiesRecipe(ServiceMetadata metadata) throws Exception {
        List<MapEntry> properties = metadata.getServiceProperties();
        if (properties != null) {
            MutableMapMetadata map = MetadataUtil.createMetadata(MutableMapMetadata.class);
            for (MapEntry e : properties) {
                map.addEntry(e);
            }
            return getValue(map, null);
        } else {
            return null;
        }
    }
    
    private BlueprintObjectRecipe createBeanRecipe(BeanMetadata local) throws Exception {
        Class clazz = local.getRuntimeClass() != null ? local.getRuntimeClass() : loadClass(local.getClassName());
        BlueprintObjectRecipe recipe = new BlueprintObjectRecipe(blueprintContainer, clazz);
        recipe.setName(getName(local.getId()));
        recipe.setExplicitDependencies(local.getExplicitDependencies());
        for (BeanProperty property : local.getProperties()) {
            Recipe value = getValue(property.getValue(), null);
            recipe.setProperty(property.getName(), value);
        }
        if (BeanMetadata.SCOPE_PROTOTYPE.equals(local.getScope())) {
            recipe.setKeepRecipe(true);
        }
        recipe.setInitMethod(local.getInitMethodName());
        recipe.setDestroyMethod(local.getDestroyMethodName());
        List<BeanArgument> beanArguments = local.getArguments(); 
        if (beanArguments != null && !beanArguments.isEmpty()) {
            boolean hasIndex = (beanArguments.get(0).getIndex() >= 0);
            if (hasIndex) {
                List<BeanArgument> beanArgumentsCopy = new ArrayList<BeanArgument>(beanArguments);
                Collections.sort(beanArgumentsCopy, MetadataUtil.BEAN_COMPARATOR);
                beanArguments = beanArgumentsCopy;
            }
            List<Object> arguments = new ArrayList<Object>();
            List<Class> argTypes = new ArrayList<Class>();
            for (BeanArgument argument : beanArguments) {
                Recipe value = getValue(argument.getValue(), null);
                arguments.add(value);
                String valueType = argument.getValueType();
                if (valueType != null) {
                    try {
                        argTypes.add(loadClass(valueType));
                    } catch (Throwable t) {
                        throw new ConstructionException("Error loading class " + valueType + " when instanciating bean " + recipe.getName());
                    }
                } else {
                    argTypes.add(null);
                }
            }
            recipe.setArguments(arguments);
            recipe.setArgTypes(argTypes);
            recipe.setReorderArguments(!hasIndex);
        }
        recipe.setFactoryMethod(local.getFactoryMethodName());
        if (local.getFactoryComponent() != null) {
            recipe.setFactoryComponent(getValue(local.getFactoryComponent(), null));
        }
        return recipe;
    }

    private Recipe createRecipe(RegistrationListener listener) throws Exception {
        BlueprintObjectRecipe recipe = new BlueprintObjectRecipe(blueprintContainer, ServiceRegistrationProxy.Listener.class);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        recipe.setProperty("metadata", listener);
        return recipe;
    }

    private Recipe createRecipe(Listener listener) throws Exception {
        BlueprintObjectRecipe recipe = new BlueprintObjectRecipe(blueprintContainer, AbstractServiceReferenceRecipe.Listener.class);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        recipe.setProperty("metadata", listener);
        return recipe;
    }

    private BeanMetadata getLocalServiceComponent(Metadata value) throws Exception {
        ComponentMetadata metadata = null;
        if (value instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) value;
            metadata = registry.getComponentDefinition(ref.getComponentId());
        } else if (value instanceof ComponentMetadata) {
            metadata = (ComponentMetadata) value;
        }
        if (metadata instanceof BeanMetadata) {
            return (BeanMetadata) metadata;
        } else {
            return null;
        }
    }
    
    private Recipe getValue(Metadata v, Class groupingType) throws Exception {
        if (v instanceof NullMetadata) {
            return null;
        } else if (v instanceof ComponentMetadata) {
            return createRecipe((ComponentMetadata) v);
        } else if (v instanceof ValueMetadata) {
            ValueMetadata stringValue = (ValueMetadata) v;
            Class type = loadClass(stringValue.getTypeName());
            type = (type == null) ? groupingType : type;
            return new ValueRecipe(stringValue, type);
        } else if (v instanceof RefMetadata) {
            // TODO: make it work with property-placeholders?
            String componentName = ((RefMetadata) v).getComponentId();
            return new ReferenceRecipe(componentName);
        } else if (v instanceof CollectionMetadata) {
            CollectionMetadata collectionMetadata = (CollectionMetadata) v;
            Class cl = collectionMetadata.getCollectionClass();
            Class type = loadClass(collectionMetadata.getValueTypeName());
            if (cl == Object[].class) {
                ArrayRecipe ar = new ArrayRecipe(type);
                for (Metadata lv : collectionMetadata.getValues()) {
                    ar.add(getValue(lv, type));
                }
                return ar;
            } else {
                CollectionRecipe cr = new CollectionRecipe(cl);
                for (Metadata lv : collectionMetadata.getValues()) {
                    cr.add(getValue(lv, type));
                }
                return cr;
            }
        } else if (v instanceof MapMetadata) {
            MapMetadata mapValue = (MapMetadata) v;
            Class keyType = loadClass(mapValue.getKeyTypeName());
            Class valueType = loadClass(mapValue.getValueTypeName());
            MapRecipe mr = new MapRecipe(HashMap.class);
            for (MapEntry entry : mapValue.getEntries()) {
                Recipe key = getValue(entry.getKey(), keyType);
                Recipe val = getValue(entry.getValue(), valueType);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof PropsMetadata) {
            PropsMetadata mapValue = (PropsMetadata) v;
            MapRecipe mr = new MapRecipe(Properties.class);
            for (MapEntry entry : mapValue.getEntries()) {
                Recipe key = getValue(entry.getKey(), String.class);
                Recipe val = getValue(entry.getValue(), String.class);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof IdRefMetadata) {
            // TODO: make it work with property-placeholders?
            String componentName = ((IdRefMetadata) v).getComponentId();
            return new ReferenceNameRecipe(componentName);
        } else {
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        }
    }
    
    protected ConversionService getConversionService() {
        return blueprintContainer.getConversionService();
    }
    
    private Class loadClass(String typeName) throws ClassNotFoundException {
        return loadClass(blueprintContainer, typeName);
    }
    
    private String getName(String name) {
        if (name == null) {
            return "recipe-" + ++nameCounter;
        } else {
            return name;
        }
    }
        
    public static Class loadClass(ExtendedBlueprintContainer context, String typeName) throws ClassNotFoundException {
        if (typeName == null) {
            return null;
        }

        Class clazz = primitiveClasses.get(typeName);
        if (clazz == null) {
            if (context == null) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                clazz = loader.loadClass(typeName);
            } else {
                clazz = context.loadClass(typeName);
            }
        }
        return clazz;
    }
            
}
