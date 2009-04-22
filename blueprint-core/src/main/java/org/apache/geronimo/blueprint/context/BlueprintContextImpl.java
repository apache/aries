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

import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.HeaderParser;
import org.apache.geronimo.blueprint.HeaderParser.PathElement;
import org.apache.geronimo.blueprint.ModuleContextEventSender;
import org.apache.geronimo.blueprint.NamespaceHandlerRegistry;
import org.apache.geronimo.blueprint.Destroyable;
import org.apache.geronimo.blueprint.convert.ConversionServiceImpl;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.xbean.recipe.ObjectGraph;
import org.apache.xbean.recipe.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.NoSuchComponentException;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.convert.Converter;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.NamespaceHandler;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class BlueprintContextImpl implements BlueprintContext, NamespaceHandlerRegistry.Listener, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintContextImpl.class);

    private enum State {
        Unknown,
        WaitForNamespaceHandlers,
        Populated,
        Created,
        Failed
    }

    private final BundleContext bundleContext;
    private final ModuleContextEventSender sender;
    private final NamespaceHandlerRegistry handlers;
    private final List<URL> urls;
    private final ComponentDefinitionRegistryImpl componentDefinitionRegistry;
    private final ConversionServiceImpl conversionService;
    private final ExecutorService executors;
    private Set<URI> namespaces;
    private State state = State.Unknown;
    private Parser parser;
    private ObjectGraph objectGraph;
    private ServiceRegistration registration;
    private boolean waitForNamespaceHandlersEventSent;
    private Map<String, Destroyable> destroyables = new HashMap<String, Destroyable>();

    public BlueprintContextImpl(BundleContext bundleContext, ModuleContextEventSender sender, NamespaceHandlerRegistry handlers, ExecutorService executors, List<URL> urls) {
        this.bundleContext = bundleContext;
        this.sender = sender;
        this.handlers = handlers;
        this.urls = urls;
        this.conversionService = new ConversionServiceImpl();
        this.componentDefinitionRegistry = new ComponentDefinitionRegistryImpl();
        this.executors = executors;
    }

    public void addDestroyable(String name, Destroyable destroyable) {
        destroyables.put(name, destroyable);
    }

    public ModuleContextEventSender getSender() {
        return sender;
    }

    private void checkDirectives() {
        Bundle bundle = bundleContext.getBundle();
        Dictionary headers = bundle.getHeaders();
        String symbolicName = (String)headers.get(Constants.BUNDLE_SYMBOLICNAME);
        List<PathElement> paths = HeaderParser.parseHeader(symbolicName);
        String timeout = paths.get(0).getDirective(BlueprintConstants.TIMEOUT_DIRECTIVE);
        String waitForDependencies = paths.get(0).getDirective(BlueprintConstants.WAIT_FOR_DEPENDENCIES_DIRECTIVE);

        // TODO: hook this up
        
        if (timeout != null) {
            System.out.println("Timeout: " + timeout);
        }
        if (waitForDependencies != null) {
            System.out.println("Wait-for-dependencies: " + waitForDependencies);
        }
    }
    
    public synchronized void run() {
        try {
            for (;;) {
                switch (state) {
                    case Unknown:
                        checkDirectives();
                        sender.sendCreating(this);
                        parser = new Parser();
                        parser.parse(urls);
                        namespaces = parser.getNamespaces();
                        if (namespaces.size() > 0) {
                            handlers.addListener(this);
                        }
                        state = State.WaitForNamespaceHandlers;
                        break;
                    case WaitForNamespaceHandlers:
                        for (URI ns : namespaces) {
                            if (handlers.getNamespaceHandler(ns) == null) {
                                if (!waitForNamespaceHandlersEventSent) {
                                    sender.sendWaiting(this, new String[] {NamespaceHandler.class.getName() }, null);
                                    waitForNamespaceHandlersEventSent = true;
                                }
                                return;
                            }
                        }
                        parser.populate(handlers, componentDefinitionRegistry);
                        state = State.Populated;
                        break;
                    case Populated:
                        Instanciator i = new Instanciator(this);
                        Repository repository = i.createRepository();
                        objectGraph = new ObjectGraph(repository);

                        registerTypeConverters();

                        instantiateComponents();

                        // TODO: access to any OSGi reference proxy is currently a problem at this point, because calling toString() will
                        // TODO:      wait for a service to be available.  We may need to catch toString(), equals() and hashCode() and make them
                        // TODO:      work even if there's no service available.

                        registerAllServices();

                        // Register the ModuleContext in the OSGi registry
                        if (registration == null) {
                            Properties props = new Properties();
                            props.put(BlueprintConstants.CONTEXT_SYMBOLIC_NAME_PROPERTY,
                                      bundleContext.getBundle().getSymbolicName());
                            props.put(BlueprintConstants.CONTEXT_VERSION_PROPERTY,
                                      bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
                            registration = bundleContext.registerService(BlueprintContext.class.getName(), this, props);

                            sender.sendCreated(this);
                            state = State.Created;
                        }
                        break;
                    case Created:
                    case Failed:
                        return;
                }
            }
        } catch (Exception e) {
            state = State.Failed;
            // TODO: clean up
            LOGGER.error("Unable to start blueprint context", e);
            sender.sendFailure(this, e);
        }
    }

    private void registerTypeConverters() {
        List<String> typeConvertersNames = componentDefinitionRegistry.getTypeConverterNames();
        Map<String, Object> typeConverters = objectGraph.createAll(typeConvertersNames);
        System.out.println("Type converters: " + typeConverters);
        for (String name : typeConvertersNames) {
            Object typeConverterInstance = typeConverters.get(name);
            if (typeConverterInstance instanceof Converter) {
                Converter converter = (Converter) typeConverterInstance;
                conversionService.registerConverter(converter);
            } else {
                // TODO: throw exception or log
            }
        }
    }
    
    private void instantiateComponents() {
        List<String> components = new ArrayList<String>();
        for (String name : componentDefinitionRegistry.getComponentDefinitionNames()) {
            ComponentMetadata component = componentDefinitionRegistry.getComponentDefinition(name);
            if (component instanceof BeanMetadata) {
                BeanMetadata local = (BeanMetadata) component;
                String scope = local.getScope();
                if (!local.isLazyInit() &&
                    (BeanMetadata.SCOPE_BUNDLE.equals(scope) ||
                     BeanMetadata.SCOPE_SINGLETON.equals(scope))) {
                    components.add(name);
                }
            }
        }
        Map instances = objectGraph.createAll(components);
        System.out.println("Component instances: " + instances);
    }

    private void destroyComponents() {
        Map<String, Destroyable> destroyables = new HashMap<String, Destroyable>(this.destroyables);
        this.destroyables.clear();
        for (Map.Entry<String, Destroyable> entry : destroyables.entrySet()) {
            try {
                entry.getValue().destroy();
            } catch (Exception e) {
                LOGGER.info("Error destroying bean " + entry.getKey(), e);
            }
        }
    }
    
    private void registerAllServices() {
        for (ServiceMetadata service : getExportedServicesMetadata()) {
            ServiceRegistrationProxy proxy = (ServiceRegistrationProxy) getComponent(service.getId());
            proxy.register();
        }
    }
    
    private void unregisterAllServices() {
        for (ServiceMetadata service : getExportedServicesMetadata()) {
            ServiceRegistrationProxy proxy = (ServiceRegistrationProxy) getComponent(service.getId());
            proxy.unregister();
        }
    }
    
    public Set<String> getComponentNames() {
        return componentDefinitionRegistry.getComponentDefinitionNames();
    }
    
    public Object getComponent(String name) throws NoSuchComponentException {
        ComponentMetadata metadata = getComponentMetadata(name);
        if (metadata == null) {
            throw new NoSuchComponentException(name);
        }
        return objectGraph.create(name);
    }

    public ComponentMetadata getComponentMetadata(String name) {
        ComponentMetadata metadata = componentDefinitionRegistry.getComponentDefinition(name);
        if (metadata == null) {
            throw new NoSuchComponentException(name);
        }
        return metadata;
    }

    public Collection<ServiceReferenceMetadata> getReferencedServicesMetadata() {
        return getMetadata(ServiceReferenceMetadata.class);
    }

    public Collection<ServiceMetadata> getExportedServicesMetadata() {
        return getMetadata(ServiceMetadata.class);
    }

    public Collection<BeanMetadata> getBeanComponentsMetadata() {
        return getMetadata(BeanMetadata.class);
    }

    private <T> Collection<T> getMetadata(Class<T> clazz) {
        Collection<T> metadatas = new ArrayList<T>();
        for (String name : componentDefinitionRegistry.getComponentDefinitionNames()) {
            ComponentMetadata component = componentDefinitionRegistry.getComponentDefinition(name);
            if (clazz.isInstance(component)) {
                metadatas.add(clazz.cast(component));
            }
        }
        metadatas = Collections.unmodifiableCollection(metadatas);
        return metadatas;

    }

    protected ObjectGraph getObjectGraph() {
        return objectGraph;
    }
    
    protected ComponentDefinitionRegistryImpl getComponentDefinitionRegistry() {
        return componentDefinitionRegistry;
    }
    
    protected ConversionService getConversionService() {
        return conversionService;
    }
    
    public BundleContext getBundleContext() {
        return bundleContext;
    }
    
    public void destroy() {
        if (registration != null) {
            registration.unregister();
        }
        handlers.removeListener(this);
        sender.sendDestroying(this);
        unregisterAllServices();
        destroyComponents();
        // TODO: stop all reference / collections
        System.out.println("Module context destroyed: " + this.bundleContext);
        sender.sendDestroyed(this);
    }

    public synchronized void namespaceHandlerRegistered(URI uri) {
        if (namespaces != null && namespaces.contains(uri)) {
            executors.submit(this);
        }
    }

    public synchronized void namespaceHandlerUnregistered(URI uri) {
        if (namespaces != null && namespaces.contains(uri)) {
            unregisterAllServices();
            destroyComponents();
            // TODO: stop all reference / collections
            waitForNamespaceHandlersEventSent = false;
            state = State.WaitForNamespaceHandlers;
            executors.submit(this);
        }
    }
}
