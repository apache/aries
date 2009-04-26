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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Comparator;

import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.reflect.ServiceMetadataImpl;
import org.apache.geronimo.blueprint.reflect.MapMetadataImpl;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.apache.xbean.recipe.ArrayRecipe;
import org.apache.xbean.recipe.CollectionRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.MapRecipe;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Option;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.ReferenceRecipe;
import org.apache.xbean.recipe.Repository;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.Listener;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;

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
    
    private BlueprintContextImpl blueprintContext;

    public Instanciator(BlueprintContextImpl blueprintContext) {
        this.blueprintContext = blueprintContext;
    }
    
    private void addBuiltinComponents(Repository repository) {
        if (blueprintContext != null) {
            repository.add("moduleContext", blueprintContext);
            repository.add("bundleContext", blueprintContext.getBundleContext());
            repository.add("bundle", blueprintContext.getBundleContext().getBundle());
            repository.add("conversionService", blueprintContext.getConversionService());
        }
    }
    
    public Repository createRepository() throws Exception {
        ComponentDefinitionRegistryImpl registry = getComponentDefinitionRegistry();
        Repository repository = new ScopedRepository();
        addBuiltinComponents(repository);
        
        // Create type-converter recipes
        for (Metadata value : registry.getTypeConverters()) {
            if (value instanceof ComponentMetadata) {
                Recipe recipe = (Recipe) getValue(value, null);
                repository.add(recipe.getName(), recipe);
            } else if (value instanceof RefMetadata) {
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
        if (component instanceof BeanMetadata) {
            return createComponentRecipe((BeanMetadata) component);
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
            for (Listener listener : (Collection<Listener>) metadata.getServiceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        Recipe comparatorRecipe = null;
        if (metadata.getComparator() != null) {
            comparatorRecipe = (Recipe) getValue(metadata.getComparator(), Comparator.class);
        }
        CollectionBasedServiceReferenceRecipe recipe = new CollectionBasedServiceReferenceRecipe(
                blueprintContext,
                                                                   blueprintContext.getSender(),
                                                                   metadata,
                                                                   listenersRecipe,
                                                                   comparatorRecipe);
        recipe.setName(metadata.getId());
        return recipe;
    }

    private UnaryServiceReferenceRecipe createUnaryServiceReferenceRecipe(ReferenceMetadata metadata) throws Exception {
        CollectionRecipe listenersRecipe = null;
        if (metadata.getServiceListeners() != null) {
            listenersRecipe = new CollectionRecipe(ArrayList.class);
            for (Listener listener : (Collection<Listener>) metadata.getServiceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        UnaryServiceReferenceRecipe recipe = new UnaryServiceReferenceRecipe(blueprintContext,
                                                                   blueprintContext.getSender(),
                                                                   metadata,
                                                                   listenersRecipe);
        recipe.setName(metadata.getId());
        return recipe;
    }

    private ObjectRecipe createServiceRecipe(ServiceMetadata serviceExport) throws Exception {
        BlueprintObjectRecipe recipe = new BlueprintObjectRecipe(blueprintContext, ServiceRegistrationProxy.class);
        recipe.allow(Option.PRIVATE_PROPERTIES);
        recipe.setName(serviceExport.getId());
        recipe.setExplicitDependencies(serviceExport.getExplicitDependencies());
        recipe.setProperty("moduleContext", blueprintContext);
        BeanMetadata exportedComponent = getLocalServiceComponent(serviceExport.getServiceComponent());
        if (exportedComponent != null && BeanMetadata.SCOPE_BUNDLE.equals(exportedComponent.getScope())) {
            BlueprintObjectRecipe exportedComponentRecipe = createComponentRecipe(exportedComponent);
            recipe.setProperty("service", new BundleScopeServiceFactory(blueprintContext, exportedComponentRecipe));
        } else {
            recipe.setProperty("service", getValue(serviceExport.getServiceComponent(), null));
        }
        recipe.setProperty("metadata", serviceExport);
        if (serviceExport instanceof ServiceMetadataImpl) {
            ServiceMetadataImpl impl = (ServiceMetadataImpl) serviceExport;
            if (impl.getServiceProperties() != null) {
                recipe.setProperty("serviceProperties", getValue(new MapMetadataImpl(null, null, impl.getServiceProperties()), null));
            }
        }
        if (serviceExport.getRegistrationListeners() != null) {
            CollectionRecipe listenersRecipe = new CollectionRecipe(ArrayList.class);
            for (RegistrationListener listener : (Collection<RegistrationListener>)serviceExport.getRegistrationListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
            recipe.setProperty("listeners", listenersRecipe);
        }
        return recipe;
    }

    private BlueprintObjectRecipe createComponentRecipe(BeanMetadata local) throws Exception {
        BlueprintObjectRecipe recipe = new BlueprintObjectRecipe(blueprintContext, loadClass(local.getClassName()));
        recipe.allow(Option.PRIVATE_PROPERTIES);
        recipe.setName(local.getId());
        recipe.setExplicitDependencies(local.getExplicitDependencies());
        for (BeanProperty property : local.getProperties()) {
            Object value = getValue(property.getValue(), null);
            recipe.setProperty(property.getName(), value);
        }
        if (BeanMetadata.SCOPE_PROTOTYPE.equals(local.getScope())) {
            recipe.setKeepRecipe(true);
        }
        ComponentDefinitionRegistryImpl registry = getComponentDefinitionRegistry();
        // check for init-method and set it on Recipe
        String initMethod = local.getInitMethodName();
        if (initMethod == null) {
            Method method = ReflectionUtils.getLifecycleMethod(recipe.getType(), registry.getDefaultInitMethod());
            recipe.setInitMethod(method);
        } else if (initMethod.length() > 0) {
            Method method = ReflectionUtils.getLifecycleMethod(recipe.getType(), initMethod);
            if (method == null) {
                throw new ConstructionException("Component '" + local.getId() + "' does not have init-method: " + initMethod);
            }
            recipe.setInitMethod(method);
        }
        // check for destroy-method and set it on Recipe
        String destroyMethod = local.getDestroyMethodName();
        if (destroyMethod == null) {
            Method method = ReflectionUtils.getLifecycleMethod(recipe.getType(), registry.getDefaultDestroyMethod());
            recipe.setDestroyMethod(method);
        } else if (destroyMethod.length() > 0) {
            Method method = ReflectionUtils.getLifecycleMethod(recipe.getType(), destroyMethod);
            if (method == null) {
                throw new ConstructionException("Component '" + local.getId() + "' does not have destroy-method: " + destroyMethod);
            }
            recipe.setDestroyMethod(method);
        }
        List<BeanArgument> beanArguments = local.getArguments(); 
        if (beanArguments != null && !beanArguments.isEmpty()) {
            boolean hasIndex = (beanArguments.get(0).getIndex() >= 0);
            if (hasIndex) {
                List<BeanArgument> beanArgumentsCopy = new ArrayList<BeanArgument>(beanArguments);
                Collections.sort(beanArgumentsCopy, new BeanArgumentComparator());
                beanArguments = beanArgumentsCopy;
            }
            List<Object> arguments = new ArrayList<Object>();
            for (BeanArgument argument : beanArguments) {
                Object value = getValue(argument.getValue(), null);
                arguments.add(value);
            }
            recipe.setArguments(arguments);
            recipe.setBeanArguments(beanArguments);
            recipe.setReorderArguments(!hasIndex);
        }
        recipe.setFactoryMethod(local.getFactoryMethodName());
        if (local.getFactoryComponent() != null) {
            recipe.setFactoryComponent(getValue(local.getFactoryComponent(), null));
        }
        return recipe;
    }

    private Recipe createRecipe(RegistrationListener listener) throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(ServiceRegistrationProxy.Listener.class);
        recipe.allow(Option.PRIVATE_PROPERTIES);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        recipe.setProperty("metadata", listener);
        return recipe;
    }

    private Recipe createRecipe(Listener listener) throws Exception {
        ObjectRecipe recipe = new ObjectRecipe(AbstractServiceReferenceRecipe.Listener.class);
        recipe.allow(Option.PRIVATE_PROPERTIES);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        recipe.setProperty("metadata", listener);
        return recipe;
    }

    private BeanMetadata getLocalServiceComponent(Metadata value) throws Exception {
        ComponentMetadata metadata = null;
        if (value instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) value;
            ComponentDefinitionRegistry registry = getComponentDefinitionRegistry();
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
    
    private Object getValue(Metadata v, Class groupingType) throws Exception {
        if (v instanceof NullMetadata) {
            return null;
        } else if (v instanceof ComponentMetadata) {
            return createRecipe((ComponentMetadata) v);
        } else if (v instanceof ValueMetadata) {
            ValueMetadata stringValue = (ValueMetadata) v;
            String value = stringValue.getStringValue();
            Class type = loadClass(stringValue.getTypeName());
            return new ValueRecipe(getConversionService(), value, type, groupingType);
        } else if (v instanceof RefMetadata) {
            String componentName = ((RefMetadata) v).getComponentId();
            return new ReferenceRecipe(componentName);
        } else if (v instanceof CollectionMetadata) {
            CollectionMetadata collectionMetadata = (CollectionMetadata) v;
            Class type = loadClass(collectionMetadata.getValueTypeName());
            if (collectionMetadata.getCollectionClass() == Object[].class) {
                ArrayRecipe ar = new ArrayRecipe();
                for (Metadata lv : collectionMetadata.getValues()) {
                    ar.add(getValue(lv, type));
                }
                return ar;
            } else {
                Class cl = collectionMetadata.getCollectionClass() == List.class ? ArrayList.class : HashSet.class;
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
                Object key = getValue(entry.getKey(), keyType);
                Object val = getValue(entry.getValue(), valueType);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof PropsMetadata) {
            PropsMetadata mapValue = (PropsMetadata) v;
            MapRecipe mr = new MapRecipe(Properties.class);
            for (MapEntry entry : mapValue.getEntries()) {
                Object key = getValue(entry.getKey(), String.class);
                Object val = getValue(entry.getValue(), String.class);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof IdRefMetadata) {
            return ((IdRefMetadata) v).getComponentId();
        } else {
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        }
    }
    
    protected ComponentDefinitionRegistryImpl getComponentDefinitionRegistry() {
        return blueprintContext.getComponentDefinitionRegistry();
    }
    
    protected ConversionService getConversionService() {
        return blueprintContext.getConversionService();
    }
    
    private Class loadClass(String typeName) throws ClassNotFoundException {
        return loadClass(blueprintContext, typeName);
    }
    
    public static Class loadClass(BlueprintContext context, String typeName) throws ClassNotFoundException {
        if (typeName == null) {
            return null;
        }

        Class clazz = primitiveClasses.get(typeName);
        if (clazz == null) {
            if (context == null) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                clazz = loader.loadClass(typeName);
            } else {
                clazz = context.getBundleContext().getBundle().loadClass(typeName);
            }
        }
        return clazz;
    }
    
    private static class BeanArgumentComparator implements Comparator<BeanArgument> {
        public int compare(BeanArgument object1, BeanArgument object2) {
            return object1.getIndex() - object2.getIndex();
        }        
    }
                
}
