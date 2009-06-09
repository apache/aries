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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.geronimo.blueprint.ComponentDefinitionRegistry;
import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.di.ArrayRecipe;
import org.apache.geronimo.blueprint.di.CollectionRecipe;
import org.apache.geronimo.blueprint.di.DefaultRepository;
import org.apache.geronimo.blueprint.di.IdRefRecipe;
import org.apache.geronimo.blueprint.di.MapRecipe;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.di.RefRecipe;
import org.apache.geronimo.blueprint.di.Repository;
import org.apache.geronimo.blueprint.di.ValueRecipe;
import org.apache.geronimo.blueprint.mutable.MutableMapMetadata;
import org.apache.geronimo.blueprint.reflect.MetadataUtil;
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
import org.osgi.service.blueprint.reflect.RefListMetadata;
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

    private Set<String> names = new HashSet<String>();
    private int nameCounter;
    private ExtendedBlueprintContainer blueprintContainer;
    private ComponentDefinitionRegistry registry;

    public RecipeBuilder(ExtendedBlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
        this.registry = blueprintContainer.getComponentDefinitionRegistry();
    }
    
    private void addBuiltinComponents(Repository repository) {
        if (blueprintContainer != null) {
            repository.putDefault("blueprintContainer", blueprintContainer);
            repository.putDefault("blueprintBundle", blueprintContainer.getBundleContext().getBundle());
            repository.putDefault("blueprintBundleContext", blueprintContainer.getBundleContext());
            repository.putDefault("blueprintConverter", blueprintContainer.getConverter());
            repository.putDefault("blueprintExtenderBundle", blueprintContainer.getExtenderBundle());
        }
    }
    
    public Repository createRepository() throws Exception {
        Repository repository = new DefaultRepository();
        addBuiltinComponents(repository);
        // Create component recipes
        for (String name : registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            Recipe recipe = createRecipe(component);
            repository.putRecipe(recipe.getName(), recipe);
        }
        return repository;
    }

    public Recipe createRecipe(ComponentMetadata component) throws Exception {
        if (component instanceof BeanMetadata) {
            return createBeanRecipe((BeanMetadata) component);
        } else if (component instanceof ServiceMetadata) {
            return createServiceRecipe((ServiceMetadata) component);
        } else if (component instanceof ReferenceMetadata) {
            return createReferenceRecipe((ReferenceMetadata) component);
        } else if (component instanceof RefListMetadata) {
            return createRefCollectionRecipe((RefListMetadata) component);
        } else {
            throw new IllegalStateException("Unsupported component type " + component.getClass());
        }
    }

    private Recipe createRefCollectionRecipe(RefListMetadata metadata) throws Exception {
        CollectionRecipe listenersRecipe = null;
        if (metadata.getServiceListeners() != null) {
            listenersRecipe = new CollectionRecipe(getName(null), ArrayList.class);
            for (Listener listener : metadata.getServiceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        List<Recipe> deps = new ArrayList<Recipe>();
        for (String name : metadata.getDependsOn()) {
            deps.add(new RefRecipe(getName(null), name));
        }
        RefListRecipe recipe = new RefListRecipe(getName(metadata.getId()),
                                                 blueprintContainer,
                                                 metadata,
                                                 listenersRecipe,
                                                 deps);
        return recipe;
    }

    private ReferenceRecipe createReferenceRecipe(ReferenceMetadata metadata) throws Exception {
        CollectionRecipe listenersRecipe = null;
        if (metadata.getServiceListeners() != null) {
            listenersRecipe = new CollectionRecipe(getName(null), ArrayList.class);
            for (Listener listener : metadata.getServiceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        List<Recipe> deps = new ArrayList<Recipe>();
        for (String name : metadata.getDependsOn()) {
            deps.add(new RefRecipe(getName(null), name));
        }
        ReferenceRecipe recipe = new ReferenceRecipe(getName(metadata.getId()),
                                                     blueprintContainer,
                                                     metadata,
                                                     listenersRecipe,
                                                     deps);
        return recipe;
    }

    private Recipe createServiceRecipe(ServiceMetadata serviceExport) throws Exception {
        CollectionRecipe listenersRecipe = new CollectionRecipe(getName(null), ArrayList.class);
        if (serviceExport.getRegistrationListeners() != null) {
            for (RegistrationListener listener : serviceExport.getRegistrationListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        List<Recipe> deps = new ArrayList<Recipe>();
        for (String name : serviceExport.getDependsOn()) {
            deps.add(new RefRecipe(getName(null), name));
        }
        ServiceRecipe recipe = new ServiceRecipe(getName(serviceExport.getId()),
                                                 blueprintContainer,
                                                 serviceExport,
                                                 getValue(serviceExport.getServiceComponent(), null),
                                                 listenersRecipe,
                                                 getServicePropertiesRecipe(serviceExport),
                                                 deps);
        return recipe;
    }

    protected MapRecipe getServicePropertiesRecipe(ServiceMetadata metadata) throws Exception {
        List<MapEntry> properties = metadata.getServiceProperties();
        if (properties != null) {
            MutableMapMetadata map = MetadataUtil.createMetadata(MutableMapMetadata.class);
            for (MapEntry e : properties) {
                map.addEntry(e);
            }
            return createMapRecipe(map);
        } else {
            return null;
        }
    }
    
    private BeanRecipe createBeanRecipe(BeanMetadata beanMetadata) throws Exception {
        BeanRecipe recipe = new BeanRecipe(
                getName(beanMetadata.getId()),
                blueprintContainer,
                beanMetadata.getRuntimeClass() != null ? beanMetadata.getRuntimeClass() : beanMetadata.getClassName());
        // Create refs for explicit dependencies
        List<Recipe> deps = new ArrayList<Recipe>();
        for (String name : beanMetadata.getDependsOn()) {
            deps.add(new RefRecipe(getName(null), name));
        }
        recipe.setExplicitDependencies(deps);
        if (!BeanMetadata.SCOPE_PROTOTYPE.equals(beanMetadata.getScope())) {
            recipe.setPrototype(false);
        }
        recipe.setInitMethod(beanMetadata.getInitMethodName());
        recipe.setDestroyMethod(beanMetadata.getDestroyMethodName());
        List<BeanArgument> beanArguments = beanMetadata.getArguments();
        if (beanArguments != null && !beanArguments.isEmpty()) {
            boolean hasIndex = (beanArguments.get(0).getIndex() >= 0);
            if (hasIndex) {
                List<BeanArgument> beanArgumentsCopy = new ArrayList<BeanArgument>(beanArguments);
                Collections.sort(beanArgumentsCopy, MetadataUtil.BEAN_COMPARATOR);
                beanArguments = beanArgumentsCopy;
            }
            List<Object> arguments = new ArrayList<Object>();
            List<String> argTypes = new ArrayList<String>();
            for (BeanArgument argument : beanArguments) {
                Recipe value = getValue(argument.getValue(), null);
                arguments.add(value);
                argTypes.add(argument.getValueType());
            }
            recipe.setArguments(arguments);
            recipe.setArgTypes(argTypes);
            recipe.setReorderArguments(!hasIndex);
        }
        recipe.setFactoryMethod(beanMetadata.getFactoryMethodName());
        if (beanMetadata.getFactoryComponent() != null) {
            recipe.setFactoryComponent(getValue(beanMetadata.getFactoryComponent(), null));
        }
        for (BeanProperty property : beanMetadata.getProperties()) {
            Recipe value = getValue(property.getValue(), null);
            recipe.setProperty(property.getName(), value);
        }
        return recipe;
    }

    private Recipe createRecipe(RegistrationListener listener) throws Exception {
        BeanRecipe recipe = new BeanRecipe(getName(null), blueprintContainer, ServiceListener.class);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        if (listener.getRegistrationMethodName() != null) {
            recipe.setProperty("registerMethod", listener.getRegistrationMethodName());
        }
        if (listener.getUnregistrationMethodName() != null) {
            recipe.setProperty("unregisterMethod", listener.getUnregistrationMethodName());
        }
        return recipe;
    }

    private Recipe createRecipe(Listener listener) throws Exception {
        BeanRecipe recipe = new BeanRecipe(getName(null), blueprintContainer, AbstractServiceReferenceRecipe.Listener.class);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        recipe.setProperty("metadata", listener);
        return recipe;
    }

    private Recipe getValue(Metadata v, Object groupingType) throws Exception {
        if (v instanceof NullMetadata) {
            return null;
        } else if (v instanceof ComponentMetadata) {
            return createRecipe((ComponentMetadata) v);
        } else if (v instanceof ValueMetadata) {
            ValueMetadata stringValue = (ValueMetadata) v;
            Object type = stringValue.getTypeName();
            type = (type == null) ? groupingType : type;
            ValueRecipe vr = new ValueRecipe(getName(null), stringValue, type);
            return vr;
        } else if (v instanceof RefMetadata) {
            // TODO: make it work with property-placeholders?
            String componentName = ((RefMetadata) v).getComponentId();
            RefRecipe rr = new RefRecipe(getName(null), componentName);
            return rr;
        } else if (v instanceof CollectionMetadata) {
            CollectionMetadata collectionMetadata = (CollectionMetadata) v;
            Class cl = collectionMetadata.getCollectionClass();
            Object type = collectionMetadata.getValueTypeName();
            if (cl == Object[].class) {
                ArrayRecipe ar = new ArrayRecipe(getName(null), type);
                for (Metadata lv : collectionMetadata.getValues()) {
                    ar.add(getValue(lv, type));
                }
                return ar;
            } else {
                CollectionRecipe cr = new CollectionRecipe(getName(null), cl != null ? cl : ArrayList.class);
                for (Metadata lv : collectionMetadata.getValues()) {
                    cr.add(getValue(lv, type));
                }
                return cr;
            }
        } else if (v instanceof MapMetadata) {
            return createMapRecipe((MapMetadata) v);
        } else if (v instanceof PropsMetadata) {
            PropsMetadata mapValue = (PropsMetadata) v;
            MapRecipe mr = new MapRecipe(getName(null), Properties.class);
            for (MapEntry entry : mapValue.getEntries()) {
                Recipe key = getValue(entry.getKey(), String.class);
                Recipe val = getValue(entry.getValue(), String.class);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof IdRefMetadata) {
            // TODO: make it work with property-placeholders?
            String componentName = ((IdRefMetadata) v).getComponentId();
            IdRefRecipe rnr = new IdRefRecipe(getName(null), componentName);
            return rnr;
        } else {
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        }
    }

    private MapRecipe createMapRecipe(MapMetadata mapValue) throws Exception {
        String keyType = mapValue.getKeyTypeName();
        String valueType = mapValue.getValueTypeName();
        MapRecipe mr = new MapRecipe(getName(null), HashMap.class);
        for (MapEntry entry : mapValue.getEntries()) {
            Recipe key = getValue(entry.getKey(), keyType);
            Recipe val = getValue(entry.getValue(), valueType);
            mr.put(key, val);
        }
        return mr;
    }

    private String getName(String name) {
        if (name == null) {
            do {
                name = "#recipe-" + ++nameCounter;
            } while (names.contains(name) || registry.containsComponentDefinition(name));
        }
        names.add(name);
        return name;
    }

}
