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

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.blueprint.BlueprintConstants;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.ExtendedBeanMetadata;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.Processor;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.parser.Parser;
import org.apache.aries.blueprint.proxy.ProxyUtils;
import org.apache.aries.blueprint.reflect.MetadataUtil;
import org.apache.aries.blueprint.reflect.PassThroughMetadataImpl;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.utils.HeaderParser;
import org.apache.aries.blueprint.utils.HeaderParser.PathElement;
import org.apache.aries.blueprint.utils.JavaUtils;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.NoSuchComponentException;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 *
 * @version $Rev$, $Date$
 */
@SuppressWarnings("deprecation") // due to the deprecated org.apache.aries.blueprint.ExtendedBlueprintContainer
public class BlueprintContainerImpl 
    implements ExtendedBlueprintContainer, NamespaceHandlerSet.Listener, 
    Runnable, SatisfiableRecipe.SatisfactionListener,
    org.apache.aries.blueprint.ExtendedBlueprintContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintContainerImpl.class);

    private static final Class[] SECURITY_BUGFIX = {
            BlueprintDomainCombiner.class,
            BlueprintProtectionDomain.class,
    };
    
    private enum State {
        Unknown,
        WaitForNamespaceHandlers,
        Populated,
        WaitForInitialReferences,
        InitialReferencesSatisfied,
        WaitForInitialReferences2,
        Create,
        Created,
        Failed,
    }

    private final BundleContext bundleContext;
    private final Bundle extenderBundle;
    private final BlueprintListener eventDispatcher;
    private final NamespaceHandlerRegistry handlers;
    private final List<Object> pathList;
    private final ComponentDefinitionRegistryImpl componentDefinitionRegistry;
    private final AggregateConverter converter;
    private final ScheduledExecutorService executors;
    private Set<URI> namespaces;
    private State state = State.Unknown;
    private NamespaceHandlerSet handlerSet;
    private boolean destroyed;
    private Parser parser;
    private BlueprintRepository repository;
    private ServiceRegistration registration;
    private List<Processor> processors;
    private final Object satisfiablesLock = new Object();
    private Map<String, List<SatisfiableRecipe>> satisfiables;
    private long timeout = 5 * 60 * 1000;
    private boolean waitForDependencies = true;
    private boolean xmlValidation = true;
    private ScheduledFuture timeoutFuture;
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread runningThread;
    private List<ServiceRecipe> services;
    private AccessControlContext accessControlContext;
    private final IdSpace tempRecipeIdSpace = new IdSpace();
    private ProxyManager proxyManager;

    public BlueprintContainerImpl(BundleContext bundleContext, Bundle extenderBundle, BlueprintListener eventDispatcher,
                                  NamespaceHandlerRegistry handlers, ScheduledExecutorService executors, List<Object> pathList, ProxyManager proxyManager) {
        this.bundleContext = bundleContext;
        this.extenderBundle = extenderBundle;
        this.eventDispatcher = eventDispatcher;
        this.handlers = handlers;
        this.pathList = pathList;
        this.converter = new AggregateConverter(this);
        this.componentDefinitionRegistry = new ComponentDefinitionRegistryImpl();
        this.executors = executors;
        this.processors = new ArrayList<Processor>();
        if (System.getSecurityManager() != null) {
            this.accessControlContext = BlueprintDomainCombiner.createAccessControlContext(bundleContext);
        }
        this.proxyManager = proxyManager;
    }

    public ExecutorService getExecutors() {
        return executors;
    }

    public Bundle getExtenderBundle() {
        return extenderBundle;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
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

    public BlueprintListener getEventDispatcher() {
        return eventDispatcher;
    }

    private void readDirectives() {
        Bundle bundle = bundleContext.getBundle();
        Dictionary headers = bundle.getHeaders();
        String symbolicName = (String)headers.get(Constants.BUNDLE_SYMBOLICNAME);
        List<PathElement> paths = HeaderParser.parseHeader(symbolicName);

        String timeoutDirective = paths.get(0).getDirective(BlueprintConstants.TIMEOUT_DIRECTIVE);
        if (timeoutDirective != null) {
            LOGGER.debug("Timeout directive: {}", timeoutDirective);
            timeout = Integer.parseInt(timeoutDirective);
        }

        String graceperiod = paths.get(0).getDirective(BlueprintConstants.GRACE_PERIOD);
        if (graceperiod != null) {
            LOGGER.debug("Grace-period directive: {}", graceperiod);
            waitForDependencies = Boolean.parseBoolean(graceperiod);
        }

        String xmlValidationDirective = paths.get(0).getDirective(BlueprintConstants.XML_VALIDATION);
        if (xmlValidationDirective != null) {
            LOGGER.debug("Xml-validation directive: {}", xmlValidationDirective);
            xmlValidation = Boolean.parseBoolean(xmlValidationDirective);
        }
    }
    
    public void schedule() {
        if (scheduled.compareAndSet(false, true)) {
            executors.submit(this);
        }
    }

    public void reload() {
        tidyupComponents();
        this.componentDefinitionRegistry.reset();
        this.repository = null;
        this.processors = new ArrayList<Processor>();
        timeout = 5 * 60 * 1000;
        waitForDependencies = true;
        xmlValidation = true;
        state = State.Unknown;
        schedule();
    }

    public void run() {
        scheduled.set(false);
        synchronized (scheduled) {
            synchronized (running) {
                runningThread = Thread.currentThread();
                running.set(true);
            }
            try {
                doRun();
            } finally {
                synchronized (running) {
                    running.set(false);
                    runningThread = null;
                    running.notifyAll();
                }
            }
        }
    }

    /**
     * This method must be called inside a synchronized block to ensure this method is not run concurrently
     */
    private void doRun() {
        try {
            for (;;) {
                if (destroyed) {
                    return;
                }
                LOGGER.debug("Running blueprint container for bundle {} in state {}", bundleContext.getBundle().getSymbolicName(), state);
                switch (state) {
                    case Unknown:
                        readDirectives();
                        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.CREATING, getBundleContext().getBundle(), getExtenderBundle()));
                        parser = new Parser();
                        parser.parse(getResources());
                        namespaces = parser.getNamespaces();
                        handlerSet = handlers.getNamespaceHandlers(namespaces, getBundleContext().getBundle());
                        handlerSet.addListener(this);
                        state = State.WaitForNamespaceHandlers;
                    case WaitForNamespaceHandlers:
                    {
                        List<String> missing = new ArrayList<String>();
                        List<URI> missingURIs = new ArrayList<URI>();
                        for (URI ns : namespaces) {
                            if (handlerSet.getNamespaceHandler(ns) == null) {
                                missing.add("(&(" + Constants.OBJECTCLASS + "=" + NamespaceHandler.class.getName() + ")(" + NamespaceHandlerRegistryImpl.NAMESPACE + "=" + ns + "))");
                                missingURIs.add(ns);
                            }
                        }
                        if (missing.size() > 0) {
                            LOGGER.info("Bundle {} is waiting for namespace handlers {}", bundleContext.getBundle().getSymbolicName(), missingURIs);
                            eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.GRACE_PERIOD, getBundleContext().getBundle(), getExtenderBundle(), missing.toArray(new String[missing.size()])));
                            return;
                        }
                        componentDefinitionRegistry.reset();
                        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintContainer", this));
                        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundle", bundleContext.getBundle()));
                        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundleContext", bundleContext));
                        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintConverter", converter));
                        if (xmlValidation) {
                            parser.validate(handlerSet.getSchema());
                        }
                        parser.populate(handlerSet, componentDefinitionRegistry);
                        state = State.Populated;
                    }
                    case Populated:
                        getRepository();
                        trackServiceReferences();
                        Runnable r = new Runnable() {
                            public void run() {
                                synchronized (scheduled) {
                                    Throwable t = new TimeoutException();
                                    state = State.Failed;
                                    String[] missingDependecies = getMissingDependencies();
                                    tidyupComponents();
                                    LOGGER.error("Unable to start blueprint container for bundle " + bundleContext.getBundle().getSymbolicName() + " due to unresolved dependencies " + Arrays.asList(missingDependecies), t);
                                    eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.FAILURE, getBundleContext().getBundle(), getExtenderBundle(), missingDependecies, t));
                                }
                            }
                        };
                        timeoutFuture = executors.schedule(r, timeout, TimeUnit.MILLISECONDS);
                        state = State.WaitForInitialReferences;
                    case WaitForInitialReferences:
                        if (waitForDependencies) {
                            String[] missingDependencies = getMissingDependencies();
                            if (missingDependencies.length > 0) {
                                LOGGER.info("Bundle {} is waiting for dependencies {}", bundleContext.getBundle().getSymbolicName(), Arrays.asList(missingDependencies));
                                eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.GRACE_PERIOD, getBundleContext().getBundle(), getExtenderBundle(), missingDependencies));
                                return;
                            }
                        }
                        state = State.InitialReferencesSatisfied;
                    case InitialReferencesSatisfied:
                        processTypeConverters();
                        processProcessors();
                        state = State.WaitForInitialReferences2;
                    case WaitForInitialReferences2:
                        if (waitForDependencies) {
                            String[] missingDependencies = getMissingDependencies();
                            if (missingDependencies.length > 0) {
                                LOGGER.info("Bundle {} is waiting for dependencies {}", bundleContext.getBundle().getSymbolicName(), Arrays.asList(missingDependencies));
                                eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.GRACE_PERIOD, getBundleContext().getBundle(), getExtenderBundle(), missingDependencies));
                                return;
                            }
                        }                       
                        state = State.Create;
                    case Create:
                        cancelFutureIfPresent();
                        registerServices();
                        instantiateEagerComponents();
                        // Register the BlueprintContainer in the OSGi registry
                        int bs = bundleContext.getBundle().getState();
                        if (registration == null && (bs == Bundle.ACTIVE || bs == Bundle.STARTING)) {
                            Properties props = new Properties();
                            props.put(BlueprintConstants.CONTAINER_SYMBOLIC_NAME_PROPERTY,
                                      bundleContext.getBundle().getSymbolicName());
                            props.put(BlueprintConstants.CONTAINER_VERSION_PROPERTY,
                                      JavaUtils.getBundleVersion(bundleContext.getBundle()));
                            registration = registerService(new String [] { BlueprintContainer.class.getName() }, this, props);
                        }
                        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.CREATED, getBundleContext().getBundle(), getExtenderBundle()));
                        state = State.Created;
                    case Created:
                    case Failed:
                        return;
                }
            }
        } catch (Throwable t) {
            state = State.Failed;
            cancelFutureIfPresent();
            tidyupComponents();
            LOGGER.error("Unable to start blueprint container for bundle " + bundleContext.getBundle().getSymbolicName(), t);
            eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.FAILURE, getBundleContext().getBundle(), getExtenderBundle(), t));
        }
    }

    private List<URL> getResources() throws FileNotFoundException {
        List<URL> resources = new ArrayList<URL>();
        for (Object path : pathList) {
            if (path instanceof URL) {
                resources.add((URL) path);                
            } else if (path instanceof String) {
                URL url = bundleContext.getBundle().getEntry((String) path);
                if (url == null) {
                    throw new FileNotFoundException("Unable to find configuration file for " + path);
                } else {
                    resources.add(url);
                }
            } else {
                throw new IllegalArgumentException("Unexpected path type: " + path.getClass());
            }
        }
        return resources;
    }
    
    public Class loadClass(final String name) throws ClassNotFoundException {
        if (accessControlContext == null) {
            return bundleContext.getBundle().loadClass(name);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class>() {
                    public Class run() throws Exception {
                        return bundleContext.getBundle().loadClass(name);
                    }            
                }, accessControlContext);
            } catch (PrivilegedActionException e) {
                Exception cause = e.getException();
                if (cause instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) cause;
                }
                throw new IllegalStateException("Unexpected checked exception", cause);
            }
        }
    }
    
    public ServiceRegistration registerService(final String[] classes, final Object service, final Dictionary properties) {
        if (accessControlContext == null) {
            return bundleContext.registerService(classes, service, properties);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ServiceRegistration>() {
                public ServiceRegistration run() {
                    return bundleContext.registerService(classes, service, properties);
                }            
            }, accessControlContext);
        }
    }
    
    public Object getService(final ServiceReference reference) {
        if (accessControlContext == null) {
            return bundleContext.getService(reference);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    return bundleContext.getService(reference);
                }            
            }, accessControlContext);
        }
    }
    
    public AccessControlContext getAccessControlContext() {
        return accessControlContext;
    }
    
    public BlueprintRepository getRepository() {
        if (repository == null) {
            repository = new RecipeBuilder(this, tempRecipeIdSpace).createRepository();
        }
        return repository;
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

        Map<String, Object> objects = repository.createAll(typeConverters, ProxyUtils.asList(Converter.class));
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
        boolean changed = false;
        // Instanciate ComponentDefinitionRegistryProcessor and BeanProcessor
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
                Object obj = repository.create(bean.getId(), ProxyUtils.asList(ComponentDefinitionRegistryProcessor.class));
                ((ComponentDefinitionRegistryProcessor) obj).process(componentDefinitionRegistry);
                changed = true;
            } else if (Processor.class.isAssignableFrom(clazz)) {
                Object obj = repository.create(bean.getId(), ProxyUtils.asList(Processor.class));
                this.processors.add((Processor) obj);
                changed = true;
            } else {
                continue;
            }
        }
        if (changed) {
            // Update repository with recipes processed by the processors
            untrackServiceReferences();
            Repository tmpRepo = new RecipeBuilder(this, tempRecipeIdSpace).createRepository();
            
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
            getSatisfiableDependenciesMap(true);
            trackServiceReferences();
        }
    }

    private Map<String, List<SatisfiableRecipe>> getSatisfiableDependenciesMap() {
        return getSatisfiableDependenciesMap(false);
    }

    private Map<String, List<SatisfiableRecipe>> getSatisfiableDependenciesMap(boolean recompute) {
        synchronized (satisfiablesLock) {
            if ((recompute || satisfiables == null) && repository != null) {
                satisfiables = new HashMap<String, List<SatisfiableRecipe>>();
                for (Recipe r : repository.getAllRecipes()) {
                    List<SatisfiableRecipe> recipes = repository.getAllRecipes(SatisfiableRecipe.class, r.getName());
                    if (!recipes.isEmpty()) {
                        satisfiables.put(r.getName(), recipes);
                    }
                }
            }
            return satisfiables;
        }
    }

    private void trackServiceReferences() {
        Map<String, List<SatisfiableRecipe>> dependencies = getSatisfiableDependenciesMap();
        Set<String> satisfiables = new HashSet<String>();
        for (List<SatisfiableRecipe> recipes : dependencies.values()) {
            for (SatisfiableRecipe satisfiable : recipes) {
                if (satisfiables.add(satisfiable.getName())) {
                    satisfiable.start(this);
                }
            }
        }
        LOGGER.debug("Tracking service references: {}", satisfiables);
    }
    
    private void untrackServiceReferences() {
        Map<String, List<SatisfiableRecipe>> dependencies = getSatisfiableDependenciesMap();
        if (dependencies != null) {
            Set<String> stopped = new HashSet<String>();
            for (List<SatisfiableRecipe> recipes : dependencies.values()) {
                for (SatisfiableRecipe satisfiable : recipes) {
                    untrackServiceReference(satisfiable, stopped, dependencies);
                }
            }
        }

        synchronized (satisfiablesLock) {
            satisfiables = null;
        }
    }

    private void untrackServiceReference(SatisfiableRecipe recipe, Set<String> stopped, Map<String, List<SatisfiableRecipe>> dependencies) {
        if (stopped.add(recipe.getName())) {
            for (Map.Entry<String, List<SatisfiableRecipe>> entry : dependencies.entrySet()) {
                if (entry.getValue().contains(recipe)) {
                    Recipe r = getRepository().getRecipe(entry.getKey());
                    if (r instanceof SatisfiableRecipe) {
                        untrackServiceReference((SatisfiableRecipe) r, stopped, dependencies);
                    }
                }
            }
            recipe.stop();
        }
    }

    public void notifySatisfaction(SatisfiableRecipe satisfiable) {
        LOGGER.debug("Notified satisfaction {} in bundle {}: {}",
                new Object[] { satisfiable.getName(), bundleContext.getBundle().getSymbolicName(), satisfiable.isSatisfied() });
        if (state == State.Create || state == State.Created ) {
            Map<String, List<SatisfiableRecipe>> dependencies = getSatisfiableDependenciesMap();
            for (Map.Entry<String, List<SatisfiableRecipe>> entry : dependencies.entrySet()) {
                String name = entry.getKey();
                ComponentMetadata metadata = componentDefinitionRegistry.getComponentDefinition(name);
                if (metadata instanceof ServiceMetadata) {
                    ServiceRecipe reg = (ServiceRecipe) repository.getRecipe(name);
                    synchronized (reg) {
                        boolean satisfied = true;
                        for (SatisfiableRecipe recipe : entry.getValue()) {
                            if (!recipe.isSatisfied()) {
                                satisfied = false;
                                break;
                            }
                        }
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
        } else {
            schedule();
        }
    }

    private void instantiateEagerComponents() {
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

    private void registerServices() {
        services = repository.getAllRecipes(ServiceRecipe.class);
        for (ServiceRecipe r : services) {
            List<SatisfiableRecipe> dependencies = getSatisfiableDependenciesMap().get(r.getName());
            boolean enabled = true;
            if (dependencies != null) {
                for (SatisfiableRecipe recipe : dependencies) {
                    if (!recipe.isSatisfied()) {
                        enabled = false;
                        break;
                    }
                }
            }
            if (enabled) {
                r.register();
            }
        }
    }

    protected void unregisterServices() {
        if (repository != null) {
            List<ServiceRecipe> recipes = this.services;
            this.services = null;
            if (recipes != null) {
                for (ServiceRecipe r : recipes) {
                    r.unregister();
                }
            }
        }
    }

    private void destroyComponents() {
        if (repository != null) {
            repository.destroy();
        }
    }

    private String[] getMissingDependencies() {
        List<String> missing = new ArrayList<String>();
        Map<String, List<SatisfiableRecipe>> dependencies = getSatisfiableDependenciesMap();
        Set<SatisfiableRecipe> recipes = new HashSet<SatisfiableRecipe>();
        for (List<SatisfiableRecipe> deps : dependencies.values()) {
            for (SatisfiableRecipe recipe : deps) {
                if (!recipe.isSatisfied()) {
                    recipes.add(recipe);
                }
            }
        }
        for (SatisfiableRecipe recipe : recipes) {
            missing.add(recipe.getOsgiFilter());
        }
        return missing.toArray(new String[missing.size()]);
    }
    
    public Set<String> getComponentIds() {
        Set<String> set = new LinkedHashSet<String>();
        set.addAll(componentDefinitionRegistry.getComponentDefinitionNames());
        set.add("blueprintContainer");
        set.add("blueprintBundle");
        set.add("blueprintBundleContext");
        set.add("blueprintConverter");
        return set;
    }
    
    public Object getComponentInstance(String id) throws NoSuchComponentException {
        if (repository == null) {
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

    public Converter getConverter() {
        return converter;
    }
    
    public ComponentDefinitionRegistryImpl getComponentDefinitionRegistry() {
        return componentDefinitionRegistry;
    }
        
    public BundleContext getBundleContext() {
        return bundleContext;
    }
    
    public void destroy() {
        destroyed = true;
        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.DESTROYING, getBundleContext().getBundle(), getExtenderBundle()));

        cancelFutureIfPresent();
        AriesFrameworkUtil.safeUnregisterService(registration);
        
        unregisterServices();

        synchronized (running) {
            while (running.get()) {
                if (runningThread != null) {
                    runningThread.interrupt();
                }
                try {
                    running.wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        if (handlerSet != null) {
            handlerSet.removeListener(this);
            handlerSet.destroy();
        }

        destroyComponents();
        
        untrackServiceReferences();

        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.DESTROYED, getBundleContext().getBundle(), getExtenderBundle()));
        LOGGER.debug("Blueprint container destroyed: {}", this.bundleContext);
    }
    
    protected void quiesce() {
        destroyed = true;
        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.DESTROYING, getBundleContext().getBundle(), getExtenderBundle()));

        cancelFutureIfPresent();
        AriesFrameworkUtil.safeUnregisterService(registration);
        if (handlerSet != null) {
            handlerSet.removeListener(this);
            handlerSet.destroy();
        }
        LOGGER.debug("Blueprint container quiesced: {}", this.bundleContext);
    }

    private void cancelFutureIfPresent()
    {
      if (timeoutFuture != null) {
          timeoutFuture.cancel(false);
      }
    }

    public void namespaceHandlerRegistered(URI uri) {
        if (namespaces != null && namespaces.contains(uri)) {
            schedule();
        }
    }

    public void namespaceHandlerUnregistered(URI uri) {
        if (namespaces != null && namespaces.contains(uri)) {
            tidyupComponents();
            this.componentDefinitionRegistry.reset();
            this.repository = null;
            state = State.WaitForNamespaceHandlers;
            schedule();
        }
    }

    private void tidyupComponents()
    {
      unregisterServices();
      destroyComponents();
      untrackServiceReferences();
    }

    public void injectBeanInstance(BeanMetadata bmd, Object o) 
        throws IllegalArgumentException, ComponentDefinitionException {
        ExecutionContext origContext 
            = ExecutionContext.Holder.setContext((ExecutionContext)getRepository());
        try {
            ComponentMetadata cmd = componentDefinitionRegistry.getComponentDefinition(bmd.getId());
            if (cmd == null || cmd != bmd) {
                throw new IllegalArgumentException(bmd.getId() + " not found in blueprint container");
            }
            Recipe r = this.getRepository().getRecipe(bmd.getId());
            if (r instanceof BeanRecipe) {
                BeanRecipe br = (BeanRecipe)r;
                if (!br.getType().isInstance(o)) {
                    throw new IllegalArgumentException("Instance class " + o.getClass().getName() 
                                                       + " is not an instance of " + br.getClass());
                }
                br.setProperties(o);
            } else {
                throw new IllegalArgumentException(bmd.getId() + " does not refer to a BeanRecipe");
            }
        } finally {
            ExecutionContext.Holder.setContext(origContext);
        }
    }
}
