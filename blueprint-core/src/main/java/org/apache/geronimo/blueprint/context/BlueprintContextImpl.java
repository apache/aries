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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.utils.HeaderParser;
import org.apache.geronimo.blueprint.utils.HeaderParser.PathElement;
import org.apache.geronimo.blueprint.BlueprintContextEventSender;
import org.apache.geronimo.blueprint.NamespaceHandlerRegistry;
import org.apache.geronimo.blueprint.Destroyable;
import org.apache.geronimo.blueprint.SatisfiableRecipe;
import org.apache.geronimo.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.geronimo.blueprint.ExtendedBlueprintContext;
import org.apache.geronimo.blueprint.BeanProcessor;
import org.apache.geronimo.blueprint.convert.ConversionServiceImpl;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.xbean.recipe.Repository;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.DefaultExecutionContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.NoSuchComponentException;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.convert.Converter;
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
public class BlueprintContextImpl implements ExtendedBlueprintContext, NamespaceHandlerRegistry.Listener, Runnable, SatisfiableRecipe.SatisfactionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintContextImpl.class);

    private enum State {
        Unknown,
        WaitForNamespaceHandlers,
        Populated,
        WaitForInitialReferences,
        InitialReferencesSatisfied,
        WaitForTrigger,
        Create,
        Created,
        Failed
    }

    private final BundleContext bundleContext;
    private final BlueprintContextEventSender sender;
    private final NamespaceHandlerRegistry handlers;
    private final List<URL> urls;
    private final boolean lazyActivation;
    private final ComponentDefinitionRegistryImpl helperComponentDefinitionRegistry;
    private final ComponentDefinitionRegistryImpl componentDefinitionRegistry;
    private final ConversionServiceImpl conversionService;
    private final ExecutorService executors;
    private Set<URI> namespaces;
    private State state = State.Unknown;
    private Parser parser;
    private BlueprintObjectInstantiator instantiator;
    private ServiceRegistration registration;
    private boolean waitForNamespaceHandlersEventSent;
    private List<BeanProcessor> beanProcessors;
    private Map<String, Destroyable> destroyables = new HashMap<String, Destroyable>();
    private Map<String, List<SatisfiableRecipe>> satisfiables;
    private boolean serviceActivation;
    private Map<ServiceMetadata, TriggerService> triggerServices;
    private long timeout = 5 * 60 * 1000; 
    private boolean waitForDependencies = true;

    public BlueprintContextImpl(BundleContext bundleContext, BlueprintContextEventSender sender, NamespaceHandlerRegistry handlers, ExecutorService executors, List<URL> urls, boolean lazyActivation) {
        this.bundleContext = bundleContext;
        this.sender = sender;
        this.handlers = handlers;
        this.urls = urls;
        this.conversionService = new ConversionServiceImpl(this);
        this.helperComponentDefinitionRegistry = new ComponentDefinitionRegistryImpl();
        this.componentDefinitionRegistry = new ComponentDefinitionRegistryImpl();
        this.executors = executors;
        this.lazyActivation = lazyActivation;
        this.triggerServices = new HashMap<ServiceMetadata, TriggerService>();
        this.beanProcessors = new ArrayList<BeanProcessor>();
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return bundleContext.getBundle().loadClass(name);
    }

    public void addDestroyable(String name, Destroyable destroyable) {
        destroyables.put(name, destroyable);
    }

    public List<BeanProcessor> getBeanProcessors() {
        return beanProcessors;
    }

    public BlueprintContextEventSender getSender() {
        return sender;
    }

    private void checkDirectives() {
        Bundle bundle = bundleContext.getBundle();
        Dictionary headers = bundle.getHeaders();
        String symbolicName = (String)headers.get(Constants.BUNDLE_SYMBOLICNAME);
        List<PathElement> paths = HeaderParser.parseHeader(symbolicName);
        
        String timeoutDirective = paths.get(0).getDirective(BlueprintConstants.TIMEOUT_DIRECTIVE);        
        if (timeoutDirective != null) {
            LOGGER.debug("Timeout directive: " + timeoutDirective);
            timeout = Integer.parseInt(timeoutDirective);
        }
        
        String waitForDependenciesDirective = paths.get(0).getDirective(BlueprintConstants.WAIT_FOR_DEPENDENCIES_DIRECTIVE);
        if (waitForDependenciesDirective != null) {
            LOGGER.debug("Wait-for-dependencies directive: " + waitForDependenciesDirective);
            waitForDependencies = Boolean.parseBoolean(waitForDependenciesDirective);
        }
        
        // TODO: add support for custom directive to disable schema validation?
    }
    
    public void run(boolean asynch) {
        if (asynch) {
            executors.submit(this);
        } else {
            run();
        }
    }
    
    public synchronized void run() {
        try {
            for (;;) {
                LOGGER.debug("Running blueprint context for bundle {} in state {}", bundleContext.getBundle().getSymbolicName(), state);
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
                        parser.populateHelperSection(handlers, helperComponentDefinitionRegistry);
                        parser.populateMainSection(handlers, componentDefinitionRegistry);
                        // TODO: need to wait for references from the helper section, such as ConfigAdmin or Converter from the OSGi registry
                        processHelperSection();
                        state = State.Populated;
                        break;
                    case Populated:
                        RecipeBuilder i = new RecipeBuilder(this);
                        Repository repository = i.createRepository(componentDefinitionRegistry);
                        instantiator = new BlueprintObjectInstantiator(repository);
                        trackServiceReferences();
                        if (checkAllSatisfiables() || !waitForDependencies) {
                            state = State.InitialReferencesSatisfied;
                        } else {
                            // TODO: pass correct parameters
                            // TODO: do we need to send one event for each missing reference ?
                            // TODO: create a timer, then fail after it elapsed
                            sender.sendWaiting(this, null, null);
                            state = State.WaitForInitialReferences;
                        }
                        break;
                    case WaitForInitialReferences:
                        if (checkAllSatisfiables()) {
                            state = State.InitialReferencesSatisfied;
                            break;
                        } else {
                            return;
                        }
                    case InitialReferencesSatisfied:
                        if (lazyActivation) {
                            registerTriggerServices();
                            state = State.WaitForTrigger;                            
                        } else {
                            state = State.Create;
                        }
                        break;
                    case WaitForTrigger:
                        return;
                    case Create:
                        instantiateComponents();
                        registerAllServices();

                        // Register the BlueprintContext in the OSGi registry
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
        } catch (Throwable t) {
            state = State.Failed;
            // TODO: clean up
            LOGGER.error("Unable to start blueprint context for bundle " + bundleContext.getBundle().getSymbolicName(), t);
            sender.sendFailure(this, t);
        }
    }

    private void processHelperSection() throws Exception {
        RecipeBuilder i = new RecipeBuilder(this);
        Repository repository = i.createRepository(helperComponentDefinitionRegistry);
        BlueprintObjectInstantiator instantiator = new BlueprintObjectInstantiator(repository);
        Map<String, Object> objects = instantiator.createAll(helperComponentDefinitionRegistry.getComponentDefinitionNames());
        for (Object obj : objects.values()) {
            if (obj instanceof Converter) {
                conversionService.registerConverter((Converter) obj);
            }
        }
        for (Object obj : objects.values()) {
            if (obj instanceof ComponentDefinitionRegistryProcessor) {
                ((ComponentDefinitionRegistryProcessor) obj).process(componentDefinitionRegistry);
            }
        }
        for (Object obj : objects.values()) {
            if (obj instanceof BeanProcessor) {
                this.beanProcessors.add((BeanProcessor) obj);
            }
        }

        // Instanciate ComponentDefinitionRegistryProcessor and BeanProcessor
        repository = i.createRepository(componentDefinitionRegistry);
        instantiator = new BlueprintObjectInstantiator(repository);
        for (BeanMetadata bean : getBeanComponentsMetadata()) {
            Class clazz = bean.getRuntimeClass();
            if (clazz == null && bean.getClassName() != null) {
                clazz = loadClass(bean.getClassName());
            } 
            if (clazz == null) {
                continue;
            }
            if (ComponentDefinitionRegistryProcessor.class.isAssignableFrom(clazz)) {
                Object obj = instantiator.create(bean.getId());
                ((ComponentDefinitionRegistryProcessor) obj).process(componentDefinitionRegistry);
            } else if (BeanProcessor.class.isAssignableFrom(clazz)) {
                Object obj = instantiator.create(bean.getId());
                this.beanProcessors.add((BeanProcessor) obj);
            }
        }
        
        // TODO: need to destroy all those objects at the end
    }

    private Map<String, List<SatisfiableRecipe>> getSatisfiableDependenciesMap() {
        if (satisfiables == null) {
            boolean createNewContext = !ExecutionContext.isContextSet();
            if (createNewContext) {
                ExecutionContext.setContext(new DefaultExecutionContext(instantiator.getRepository()));
            }
            try {
                satisfiables = new HashMap<String, List<SatisfiableRecipe>>();
                for (String name : componentDefinitionRegistry.getComponentDefinitionNames()) {
                    Object val = instantiator.getRepository().get(name);
                    if (val instanceof Recipe) {
                        Recipe r = (Recipe) val;
                        List<SatisfiableRecipe> recipes = new ArrayList<SatisfiableRecipe>();
                        if (r instanceof SatisfiableRecipe) {
                            recipes.add((SatisfiableRecipe) r);
                        }
                        getSatisfiableDependencies(r, recipes, new HashSet<Recipe>());
                        if (!recipes.isEmpty()) {
                            satisfiables.put(name, recipes);
                        }
                    }
                }
                return satisfiables;
            } finally {
                if (createNewContext) {
                    ExecutionContext.setContext(null);
                }
            }
        }
        return satisfiables;
    }

    private void getSatisfiableDependencies(Recipe r, List<SatisfiableRecipe> recipes, Set<Recipe> visited) {
        if (!visited.contains(r)) {
            visited.add(r);
            for (Recipe dep : r.getNestedRecipes()) {
                if (dep instanceof SatisfiableRecipe) {
                    recipes.add((SatisfiableRecipe) dep);
                }
                getSatisfiableDependencies(dep, recipes, visited);
            }
        }
    }

    private void trackServiceReferences() {
        Map<String, List<SatisfiableRecipe>> dependencies = getSatisfiableDependenciesMap();
        List<String> satisfiables = new ArrayList<String>();
        for (String name : dependencies.keySet()) {
            for (SatisfiableRecipe satisfiable : dependencies.get(name)) {
                satisfiable.registerListener(this);
                satisfiable.start();
                satisfiables.add(satisfiable.getName());
            }
        }
        LOGGER.debug("Tracking service references: {}", satisfiables);
    }

    private boolean checkAllSatisfiables() {
        Map<String, List<SatisfiableRecipe>> dependencies = getSatisfiableDependenciesMap();
        for (String name : dependencies.keySet()) {
            for (SatisfiableRecipe recipe : dependencies.get(name)) {
                if (!recipe.isSatisfied()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void notifySatisfaction(SatisfiableRecipe satisfiable) {
        LOGGER.debug("Notified satisfaction {} in bundle {}: {}",
                new Object[] { satisfiable.getName(), bundleContext.getBundle().getSymbolicName(), satisfiable.isSatisfied() });
        if (state == State.WaitForInitialReferences) {
            executors.submit(this);
        } else if (state == State.Created) {
            Map<String, List<SatisfiableRecipe>> dependencies = getSatisfiableDependenciesMap();
            for (String name : dependencies.keySet()) {
                ComponentMetadata metadata = componentDefinitionRegistry.getComponentDefinition(name);
                if (metadata instanceof ServiceMetadata) {
                    boolean satisfied = true;
                    for (SatisfiableRecipe recipe : dependencies.get(name)) {
                        if (!recipe.isSatisfied()) {
                            satisfied = false;
                            break;
                        }
                    }
                    ServiceRegistrationProxy reg = (ServiceRegistrationProxy) getComponent(name);
                    if (satisfied && !reg.isRegistered()) {
                        LOGGER.debug("Registering service {} due to satisfied references", name);
                        reg.register();
                    } else if (!satisfied && reg.isRegistered()) {
                        LOGGER.debug("Unregistering service {} due to unsatisfied references", name);
                        reg.unregister();
                    }
                }
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
                if (!local.isLazyInit() && BeanMetadata.SCOPE_SINGLETON.equals(scope)) {
                    components.add(name);
                }
            } else if (components instanceof ServiceReferenceMetadata) {
                components.add(name);
            }
        }
        LOGGER.debug("Instantiating components: {}", components);
        instantiator.createAll(components);
    }

    private void destroyComponents() {
        if (instantiator != null) {
            ((BlueprintObjectRepository)instantiator.getRepository()).destroy();
        }
        
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
            if (proxy != null) {
                proxy.register();
            }
        }
    }
    
    private void unregisterAllServices() {
        for (ServiceMetadata service : getExportedServicesMetadata()) {
            ServiceRegistrationProxy proxy = (ServiceRegistrationProxy) getComponent(service.getId());
            if (proxy != null) {
                proxy.unregister();
            }
        }
    }
        
    private void registerTriggerServices() {
        for (ServiceMetadata service : getExportedServicesMetadata()) {
            // Trigger services are only created for services without listeners and explicitly defined interface classes
            if (service.getRegistrationListeners().isEmpty() && !service.getInterfaceNames().isEmpty()) {
                TriggerService triggerService = new TriggerService(service, this);
                triggerService.register();
                triggerServices.put(service, triggerService);
            }
        }
    }
    
    private void unregisterTriggerServices() {
        for (TriggerService service : triggerServices.values()) {
            service.unregister();
        }
        triggerServices.clear();
    }
        
    protected TriggerService removeTriggerService(ServiceMetadata metadata) {
        return triggerServices.remove(metadata);
    }
    
    protected void forceActivation(boolean serviceActivation) throws BundleException {
        this.serviceActivation = serviceActivation;
        getBundleContext().getBundle().start(Bundle.START_TRANSIENT);
    }
    
    public synchronized void triggerActivation() {
        if (!lazyActivation) {
            throw new IllegalStateException("triggerActivation can only be called for bundles with lazy activation policy");
        }
        if (state == State.WaitForTrigger) {
            LOGGER.debug("Activation triggered (service activation: {})", serviceActivation); 
            state = State.Create;
            // service triggered activation runs synchronously but classloader triggered activation runs asynchronously
            run(serviceActivation ? false : true);
        }
    }
    
    public Set<String> getComponentNames() {
        return componentDefinitionRegistry.getComponentDefinitionNames();
    }
    
    public Object getComponent(String name) throws NoSuchComponentException {
        if (instantiator == null) {
            throw new NoSuchComponentException(name);
        }
        try {
            return instantiator.create(name);
        } catch (org.apache.xbean.recipe.NoSuchObjectException e) {
            throw new NoSuchComponentException(name);
        }
    }

    public ComponentMetadata getComponentMetadata(String name) {
        ComponentMetadata metadata = componentDefinitionRegistry.getComponentDefinition(name);
        if (metadata == null) {
            throw new NoSuchComponentException(name);
        }
        return metadata;
    }

    public Collection<ServiceReferenceMetadata> getReferencedServicesMetadata() {
        return getComponentsMetadata(ServiceReferenceMetadata.class);
    }

    public Collection<ServiceMetadata> getExportedServicesMetadata() {
        return getComponentsMetadata(ServiceMetadata.class);
    }

    public Collection<BeanMetadata> getBeanComponentsMetadata() {
        return getComponentsMetadata(BeanMetadata.class);
    }

    public <T extends ComponentMetadata> List<T> getComponentsMetadata(Class<T> clazz) {
        List<T> metadatas = new ArrayList<T>();
        for (String name : componentDefinitionRegistry.getComponentDefinitionNames()) {
            ComponentMetadata component = componentDefinitionRegistry.getComponentDefinition(name);
            if (clazz.isInstance(component)) {
                metadatas.add(clazz.cast(component));
            }
        }
        metadatas = Collections.unmodifiableList(metadatas);
        return metadatas;

    }

    protected Repository getRepository() {
        return instantiator.getRepository();
    }
    
    public ConversionService getConversionService() {
        return conversionService;
    }
    
    protected ComponentDefinitionRegistryImpl getComponentDefinitionRegistry() {
        return componentDefinitionRegistry;
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
        unregisterTriggerServices();
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
            // TODO: clear the repository
            waitForNamespaceHandlersEventSent = false;
            state = State.WaitForNamespaceHandlers;
            executors.submit(this);
        }
    }
}
