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
package org.apache.aries.blueprint.container;

import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.ExtendedBeanMetadata;
import org.apache.aries.blueprint.Processor;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.parser.Parser;
import org.apache.aries.blueprint.reflect.MetadataUtil;
import org.apache.aries.blueprint.reflect.PassThroughMetadataImpl;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.NoSuchComponentException;
import org.osgi.service.blueprint.reflect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.security.AccessControlContext;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlueprintContainerImpl implements ExtendedBlueprintContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintContainerImpl.class);

    private final ClassLoader loader;
    private final List<URL> resources;
    private final AggregateConverter converter;
    private final ComponentDefinitionRegistryImpl componentDefinitionRegistry;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final IdSpace tempRecipeIdSpace = new IdSpace();
    private BlueprintRepository repository;
    private List<Processor> processors = new ArrayList<Processor>();

    public BlueprintContainerImpl(ClassLoader loader, List<URL> resources) throws Exception {
        this(loader, resources,  true);
    }

    public BlueprintContainerImpl(ClassLoader loader, List<URL> resources, boolean init) throws Exception {
        this.loader = loader;
        this.converter = new AggregateConverter(this);
        this.componentDefinitionRegistry = new ComponentDefinitionRegistryImpl();
        this.resources = resources;
        if (init) {
            init();
        }
    }

    public void init() throws Exception {
        // Parse xml resources
        Parser parser = new Parser();
        parser.parse(getResources());
        // Create handler set
        SimpleNamespaceHandlerSet handlerSet = new SimpleNamespaceHandlerSet();
        // Check namespaces
        Set<URI> namespaces = parser.getNamespaces();
        Set<URI> unsupported = new LinkedHashSet<URI>();
        for (URI ns : namespaces) {
            if (!handlerSet.getNamespaces().contains(ns)) {
                unsupported.add(ns);
            }
        }
        if (unsupported.size() > 0) {
            throw new IllegalArgumentException("Unsupported namespaces: " + unsupported.toString());
        }
        // Add predefined beans
        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintContainer", this));
        // Validate
        parser.validate(handlerSet.getSchema());
        // Populate
        parser.populate(handlerSet, componentDefinitionRegistry);
        // Create repository
        repository = new NoOsgiRecipeBuilder(this, tempRecipeIdSpace).createRepository();
        // Processors handling
        processTypeConverters();
        processProcessors();
        // Instantiate eager singletons
        instantiateEagerComponents();
    }

    public void destroy() {
        repository.destroy();
    }

    public List<URL> getResources() {
        return resources;
    }

    public Converter getConverter() {
        return converter;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return loader.loadClass(name);
    }

    public URL getResource(String name) {
        return loader.getResource(name);
    }

    public AccessControlContext getAccessControlContext() {
        return null;
    }

    public ComponentDefinitionRegistryImpl getComponentDefinitionRegistry() {
        return componentDefinitionRegistry;
    }

    public <T extends Processor> List<T> getProcessors(Class<T> clazz) {
        List<T> p = new ArrayList<T>();
        for (Processor processor : processors) {
            if (clazz.isInstance(processor)) {
                p.add(clazz.cast(processor));
            }
        }
        return p;
    }

    public Set<String> getComponentIds() {
        return new LinkedHashSet<String>(componentDefinitionRegistry.getComponentDefinitionNames());
    }

    public Object getComponentInstance(String id) {
        if (repository == null || destroyed.get()) {
            throw new NoSuchComponentException(id);
        }
        try {
            LOGGER.debug("Instantiating component {}", id);
            return repository.create(id);
        } catch (NoSuchComponentException e) {
            throw e;
        } catch (ComponentDefinitionException e) {
            throw e;
        } catch (Throwable t) {
            throw new ComponentDefinitionException("Cound not create component instance for " + id, t);
        }
    }

    public ComponentMetadata getComponentMetadata(String id) {
        ComponentMetadata metadata = componentDefinitionRegistry.getComponentDefinition(id);
        if (metadata == null) {
            throw new NoSuchComponentException(id);
        }
        return metadata;
    }

    public <T extends ComponentMetadata> Collection<T> getMetadata(Class<T> clazz) {
        Collection<T> metadatas = new ArrayList<T>();
        for (String name : componentDefinitionRegistry.getComponentDefinitionNames()) {
            ComponentMetadata component = componentDefinitionRegistry.getComponentDefinition(name);
            getMetadata(clazz, component, metadatas);
        }
        metadatas = Collections.unmodifiableCollection(metadatas);
        return metadatas;
    }

    public BlueprintRepository getRepository() {
        return repository;
    }

    private <T extends ComponentMetadata> void getMetadata(Class<T> clazz, Metadata component, Collection<T> metadatas) {
        if (component == null) {
            return;
        }
        if (clazz.isInstance(component)) {
            metadatas.add(clazz.cast(component));
        }
        if (component instanceof BeanMetadata) {
            getMetadata(clazz, ((BeanMetadata) component).getFactoryComponent(), metadatas);
            for (BeanArgument arg : ((BeanMetadata) component).getArguments()) {
                getMetadata(clazz, arg.getValue(), metadatas);
            }
            for (BeanProperty prop : ((BeanMetadata) component).getProperties()) {
                getMetadata(clazz, prop.getValue(), metadatas);
            }
        }
        if (component instanceof CollectionMetadata) {
            for (Metadata m : ((CollectionMetadata) component).getValues()) {
                getMetadata(clazz, m, metadatas);
            }
        }
        if (component instanceof MapMetadata) {
            for (MapEntry m : ((MapMetadata) component).getEntries()) {
                getMetadata(clazz, m.getKey(), metadatas);
                getMetadata(clazz, m.getValue(), metadatas);
            }
        }
        if (component instanceof PropsMetadata) {
            for (MapEntry m : ((PropsMetadata) component).getEntries()) {
                getMetadata(clazz, m.getKey(), metadatas);
                getMetadata(clazz, m.getValue(), metadatas);
            }
        }
        if (component instanceof ServiceReferenceMetadata) {
            for (ReferenceListener l : ((ServiceReferenceMetadata) component).getReferenceListeners()) {
                getMetadata(clazz, l.getListenerComponent(), metadatas);
            }
        }
        if (component instanceof ServiceMetadata) {
            getMetadata(clazz, ((ServiceMetadata) component).getServiceComponent(), metadatas);
            for (MapEntry m : ((ServiceMetadata) component).getServiceProperties()) {
                getMetadata(clazz, m.getKey(), metadatas);
                getMetadata(clazz, m.getValue(), metadatas);
            }
            for (RegistrationListener l : ((ServiceMetadata) component).getRegistrationListeners()) {
                getMetadata(clazz, l.getListenerComponent(), metadatas);
            }
        }
    }

    private void processTypeConverters() throws Exception {
        List<String> typeConverters = new ArrayList<String>();
        for (Target target : componentDefinitionRegistry.getTypeConverters()) {
            if (target instanceof ComponentMetadata) {
                typeConverters.add(((ComponentMetadata) target).getId());
            } else if (target instanceof RefMetadata) {
                typeConverters.add(((RefMetadata) target).getComponentId());
            } else {
                throw new ComponentDefinitionException("Unexpected metadata for type converter: " + target);
            }
        }

        Map<String, Object> objects = repository.createAll(typeConverters, Arrays.<Class<?>>asList(Converter.class));
        for (String name : typeConverters) {
            Object obj = objects.get(name);
            if (obj instanceof Converter) {
                converter.registerConverter((Converter) obj);
            } else {
                throw new ComponentDefinitionException("Type converter " + obj + " does not implement the " + Converter.class.getName() + " interface");
            }
        }
    }

    private void processProcessors() throws Exception {
        // Instantiate ComponentDefinitionRegistryProcessor and BeanProcessor
        for (BeanMetadata bean : getMetadata(BeanMetadata.class)) {
            if (bean instanceof ExtendedBeanMetadata && !((ExtendedBeanMetadata) bean).isProcessor()) {
                continue;
            }

            Class clazz = null;
            if (bean instanceof ExtendedBeanMetadata) {
                clazz = ((ExtendedBeanMetadata) bean).getRuntimeClass();
            }
            if (clazz == null && bean.getClassName() != null) {
                clazz = loadClass(bean.getClassName());
            }
            if (clazz == null) {
                continue;
            }

            if (ComponentDefinitionRegistryProcessor.class.isAssignableFrom(clazz)) {
                Object obj = repository.create(bean.getId(), Arrays.<Class<?>>asList(ComponentDefinitionRegistryProcessor.class));
                ((ComponentDefinitionRegistryProcessor) obj).process(componentDefinitionRegistry);
            } else if (Processor.class.isAssignableFrom(clazz)) {
                Object obj = repository.create(bean.getId(), Arrays.<Class<?>>asList(Processor.class));
                this.processors.add((Processor) obj);
            } else {
                continue;
            }
            updateUninstantiatedRecipes();
        }
    }
    private void updateUninstantiatedRecipes() {
        Repository tmpRepo = new NoOsgiRecipeBuilder(this, tempRecipeIdSpace).createRepository();

        LOGGER.debug("Updating blueprint repository");

        for (String name : repository.getNames()) {
            if (repository.getInstance(name) == null) {
                LOGGER.debug("Removing uninstantiated recipe {}", new Object[] { name });
                repository.removeRecipe(name);
            } else {
                LOGGER.debug("Recipe {} is already instantiated", new Object[] { name });
            }
        }

        for (String name : tmpRepo.getNames()) {
            if (repository.getInstance(name) == null) {
                LOGGER.debug("Adding new recipe {}", new Object[] { name });
                Recipe r = tmpRepo.getRecipe(name);
                if (r != null) {
                    repository.putRecipe(name, r);
                }
            } else {
                LOGGER.debug("Recipe {} is already instantiated and cannot be updated", new Object[] { name });
            }
        }
    }

    protected void instantiateEagerComponents() {
        List<String> components = new ArrayList<String>();
        for (String name : componentDefinitionRegistry.getComponentDefinitionNames()) {
            ComponentMetadata component = componentDefinitionRegistry.getComponentDefinition(name);
            boolean eager = component.getActivation() == ComponentMetadata.ACTIVATION_EAGER;
            if (component instanceof BeanMetadata) {
                BeanMetadata local = (BeanMetadata) component;
                eager &= MetadataUtil.isSingletonScope(local);
            }
            if (eager) {
                components.add(name);
            }
        }
        LOGGER.debug("Instantiating components: {}", components);
        try {
            repository.createAll(components);
        } catch (ComponentDefinitionException e) {
            throw e;
        } catch (Throwable t) {
            throw new ComponentDefinitionException("Unable to instantiate components", t);
        }
    }



}
