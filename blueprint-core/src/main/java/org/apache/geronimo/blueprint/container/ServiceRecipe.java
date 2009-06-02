/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.blueprint.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.ServiceProcessor;
import org.apache.geronimo.blueprint.di.AbstractRecipe;
import org.apache.geronimo.blueprint.di.CollectionRecipe;
import org.apache.geronimo.blueprint.di.DefaultExecutionContext;
import org.apache.geronimo.blueprint.di.DefaultRepository;
import org.apache.geronimo.blueprint.di.ExecutionContext;
import org.apache.geronimo.blueprint.di.MapRecipe;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.di.RefRecipe;
import org.apache.geronimo.blueprint.di.Repository;
import org.apache.geronimo.blueprint.utils.JavaUtils;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>Recipe</code> to export services into the OSGi registry.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 776360 $, $Date: 2009-05-19 17:40:47 +0200 (Tue, 19 May 2009) $
 */
public class ServiceRecipe extends AbstractRecipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRecipe.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private ServiceMetadata metadata;
    private Recipe serviceRecipe;
    private CollectionRecipe listenersRecipe;
    private MapRecipe propertiesRecipe;
    private List<Recipe> explicitDependencies;

    private Map properties;
    private ServiceRegistration registration;
    private Map registrationProperties;
    private List<ServiceListener> listeners;
    private Object service;
    private boolean bundleScope;

    public ServiceRecipe(String name,
                         ExtendedBlueprintContainer blueprintContainer,
                         ServiceMetadata metadata,
                         Recipe serviceRecipe,
                         CollectionRecipe listenersRecipe,
                         MapRecipe propertiesRecipe,
                         List<Recipe> explicitDependencies) {
        super(name);
        this.prototype = false;
        this.blueprintContainer = blueprintContainer;
        this.metadata = metadata;
        this.serviceRecipe = serviceRecipe;
        this.listenersRecipe = listenersRecipe;
        this.propertiesRecipe = propertiesRecipe;
        this.explicitDependencies = explicitDependencies;
    }

    @Override
    public List<Recipe> getNestedRecipes() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        if (serviceRecipe != null) {
            recipes.add(serviceRecipe);
        }
        if (listenersRecipe != null) {
            recipes.add(listenersRecipe);
        }
        if (propertiesRecipe != null) {
            recipes.add(propertiesRecipe);
        }
        if (explicitDependencies != null) {
            recipes.addAll(explicitDependencies);
        }
        return recipes;
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        if (explicitDependencies != null) {
            for (Recipe recipe : explicitDependencies) {
                recipe.create();
            }
        }
        return new ServiceRegistrationProxy();
    }

    public synchronized void register() {
        if (registration != null) {
            return;
        }
        
        Hashtable props = new Hashtable();
        if (properties == null) {
            properties = (Map) createSimpleRecipe(propertiesRecipe);
        }
        props.putAll(properties);
        props.put(Constants.SERVICE_RANKING, metadata.getRanking());
        String componentName = getComponentName();
        if (componentName != null) {
            props.put(BlueprintConstants.COMPONENT_NAME_PROPERTY, componentName);
        }
        for (ServiceProcessor processor : blueprintContainer.getProcessors(ServiceProcessor.class)) {
            processor.updateProperties(new PropertiesUpdater(), props);
        }
        
        Set<String> classes = getClasses();
        String[] classArray = classes.toArray(new String[classes.size()]);
        registration = blueprintContainer.getBundleContext().registerService(classArray, new TriggerServiceFactory(), props);
        registrationProperties = props;

        LOGGER.debug("Trigger service {} registered with interfaces {} and properties {}",
                     new Object[] { this, classes, props });
    }

    public synchronized boolean isRegistered() {
        return registration != null;
    }

    public synchronized ServiceReference getReference() {
        if (registration == null) {
            throw new IllegalStateException("Service is not registered");
        } else {
            return registration.getReference();
        }
    }

    public synchronized void setProperties(Dictionary props) {
        if (registration == null) {
            throw new IllegalStateException("Service is not registered");
        } else {
            registration.setProperties(props);
            // TODO: set serviceProperties? convert somehow? should listeners be notified of this?
        }
    }


    public synchronized void unregister() {
        if (registration != null) {
            // TODO: shouldn't listeners be called before unregistering the service?
            registration.unregister();
            if (listeners != null) {
                LOGGER.debug("Calling listeners for service unregistration");
                for (ServiceListener listener : listeners) {
                    listener.unregister(getService(), registrationProperties);
                }
            }
            LOGGER.debug("Service {} unregistered", service);
            registration = null;
        }
    }
    
    public Object getService() {
        return getService(blueprintContainer.getBundleContext().getBundle(), null);
    }

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        LOGGER.debug("Retrieving service for bundle {} and service registration {}", bundle, registration);
        // Create initial service
        synchronized (this) {
            if (this.service == null) {
                try {
                    bundleScope = isBundleScope(metadata.getServiceComponent());
                    LOGGER.debug("Creating service instance (bundle scope = {})", bundleScope);
                    this.service = createInstance(false);
                    LOGGER.debug("Service created: {}", this.service);
                    // When the service is first requested, we need to create listeners and call them
                    if (listeners == null) {
                        LOGGER.debug("Creating listeners");
                        if (listenersRecipe != null) {
                            listeners = (List) createSimpleRecipe(listenersRecipe);
                        } else {
                            listeners = Collections.emptyList();
                        }
                        LOGGER.debug("Listeners created: {}", listeners);
                        LOGGER.debug("Calling listeners for service registration");
                        for (ServiceListener listener : listeners) {
                            listener.register(service, registrationProperties);
                        }
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("Error retrieving service from " + this, e);
                    throw e;
                }
            }
        }
        Object service = this.service;
        if (service instanceof ServiceFactory) {
            service = ((ServiceFactory) service).getService(bundle, registration);
        } else if (bundleScope) {
            service = createInstance(true);
            LOGGER.debug("Created service instance for bundle: " + bundle + " " + service.hashCode());
        }
        if (service == null) {
            throw new IllegalStateException("service is null");
        }
        return service;
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        if (bundleScope) {
            destroyInstance(service);
            LOGGER.debug("Destroyed service instance for bundle: " + bundle);
        }
    }

    private Set<String> getClasses() {
        Set<String> classes;
        switch (metadata.getAutoExportMode()) {
            case ServiceMetadata.AUTO_EXPORT_INTERFACES:
                classes = ReflectionUtils.getImplementedInterfaces(new HashSet<String>(), getService().getClass());
                break;
            case ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), getService().getClass());
                break;
            case ServiceMetadata.AUTO_EXPORT_ALL_CLASSES:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), getService().getClass());
                classes = ReflectionUtils.getImplementedInterfaces(classes, getService().getClass());
                break;
            default:
                classes = new HashSet<String>(metadata.getInterfaceNames());
                break;
        }
        return classes;
    }

    private Object createInstance(boolean scoped) {
        if (scoped) {
            Recipe recipe = serviceRecipe;
            Repository repo = blueprintContainer.getRepository();
            if (recipe instanceof RefRecipe) {
                recipe = repo.getRecipe(((RefRecipe) recipe).getIdRef());
            }
            DefaultRepository repository = new DefaultRepository((DefaultRepository) repo);
            if (repository.getRecipe(recipe.getName()) != recipe) {
                repository.putRecipe(recipe.getName(), recipe);
            }
            BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(blueprintContainer, repository);
            return graph.create(recipe.getName());
        } else {
            return createSimpleRecipe(serviceRecipe);
        }
    }

    private Object createSimpleRecipe(Recipe recipe) {
        String name = recipe.getName();
        Repository repo = blueprintContainer.getRepository();
        if (repo.getRecipe(name) != recipe) {
            repo.putRecipe(name, recipe);
        }
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(blueprintContainer, repo);
        return graph.create(name);
    }

    private void destroyInstance(Object instance) {
        Recipe recipe = serviceRecipe;
        Repository objectRepository = blueprintContainer.getRepository();
        if (recipe instanceof RefRecipe) {
            recipe = objectRepository.getRecipe(((RefRecipe) recipe).getIdRef());
        }
        ((BeanRecipe) recipe).destroyInstance(instance);
    }

    private boolean isBundleScope(Metadata value) {
        ComponentMetadata metadata = null;
        if (value instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) value;
            metadata = blueprintContainer.getComponentDefinitionRegistry().getComponentDefinition(ref.getComponentId());
        } else if (value instanceof ComponentMetadata) {
            metadata = (ComponentMetadata) value;
        }
        if (metadata instanceof BeanMetadata) {
            BeanMetadata bean = (BeanMetadata) metadata;
            Class clazz = bean.getRuntimeClass();
            // Try to get the class from the className attribute
            if (clazz == null && bean.getClassName() != null) {
                ExecutionContext oldContext = ExecutionContext.setContext(new DefaultExecutionContext(blueprintContainer, blueprintContainer.getRepository()));
                try {
                    clazz = loadClass(bean.getClassName());
                } finally {
                    ExecutionContext.setContext(oldContext);
                }
            }
            // TODO: if clazz == null, we must be using a factory, just ignore it for now
            if (clazz != null && ServiceFactory.class.isAssignableFrom(clazz)) {
                return false;
            } else {
                return BeanMetadata.SCOPE_BUNDLE.equals(bean.getScope());
            }
        } else {
            return false;
        }
    }

    private String getComponentName() {
        if (metadata.getServiceComponent() instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) metadata.getServiceComponent();
            return ref.getComponentId();
        } else {
            return null;
        }
    }

    private class TriggerServiceFactory implements ServiceFactory {

        public Object getService(Bundle bundle, ServiceRegistration registration) {
            return ServiceRecipe.this.getService(bundle, registration);
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            ServiceRecipe.this.ungetService(bundle, registration, service);
        }

    }

    private class ServiceRegistrationProxy implements ServiceRegistration {

        public ServiceReference getReference() {
            return ServiceRecipe.this.getReference();
        }

        public void setProperties(Dictionary properties) {
            ServiceRecipe.this.setProperties(properties);
        }

        public void unregister() {
            ServiceRecipe.this.unregister();
        }
    }

    private class PropertiesUpdater implements ServiceProcessor.ServicePropertiesUpdater {

        public String getId() {
            return metadata.getId();
        }

        public void updateProperties(Dictionary properties) {
            Hashtable table = JavaUtils.getProperties(getReference());
            JavaUtils.copy(table, properties);
            setProperties(table);
        }        
    }

}
