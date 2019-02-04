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
import org.apache.aries.blueprint.NamespaceHandler2;
import org.apache.aries.blueprint.Processor;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.namespace.MissingNamespaceException;
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
import org.apache.aries.blueprint.utils.ServiceUtil;
import org.apache.aries.proxy.ProxyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
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
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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

    private static final String DEFAULT_TIMEOUT_PROPERTY = "org.apache.aries.blueprint.default.timeout";
    private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintContainerImpl.class);

    private static final Class[] SECURITY_BUGFIX = {
            BlueprintDomainCombiner.class,
            BlueprintProtectionDomain.class,
    };
    
    public enum State {
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
    private final Bundle bundle;
    private final Bundle extenderBundle;
    private final BlueprintListener eventDispatcher;
    private final NamespaceHandlerRegistry handlers;
    private final List<URL> pathList;
    private final ComponentDefinitionRegistryImpl componentDefinitionRegistry;
    private final AggregateConverter converter;
    private final ExecutorService executors;
    private final ScheduledExecutorService timer;
    private final Collection<URI> additionalNamespaces;
    private Set<URI> namespaces;
    private State state = State.Unknown;
    private NamespaceHandlerSet handlerSet;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private Parser parser;
    private BlueprintRepository repository;
    private ServiceRegistration registration;
    private final List<Processor> processors;
    private final Object satisfiablesLock = new Object();
    private Map<String, List<SatisfiableRecipe>> satisfiables;
    private long timeout;
    private boolean waitForDependencies = true;
    private String xmlValidation;
    private ScheduledFuture timeoutFuture;
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private List<ServiceRecipe> services;
    private final AccessControlContext accessControlContext;
    private final IdSpace tempRecipeIdSpace = new IdSpace();
    private final ProxyManager proxyManager;

    public BlueprintContainerImpl(Bundle bundle, BundleContext bundleContext, Bundle extenderBundle, BlueprintListener eventDispatcher,
                                  NamespaceHandlerRegistry handlers, ExecutorService executor, ScheduledExecutorService timer,
                                  List<URL> pathList, ProxyManager proxyManager, Collection<URI> namespaces) {
        this.bundle = bundle;
        this.bundleContext = bundleContext;
        this.extenderBundle = extenderBundle;
        this.eventDispatcher = eventDispatcher;
        this.handlers = handlers;
        this.pathList = pathList;
        this.converter = new AggregateConverter(this);
        this.componentDefinitionRegistry = new ComponentDefinitionRegistryImpl();
        this.executors = executor != null ? new ExecutorServiceWrapper(executor) : null;
        this.timer = timer;
        this.timeout = getDefaultTimeout();
        this.processors = new ArrayList<Processor>();
        if (System.getSecurityManager() != null) {
            this.accessControlContext = BlueprintDomainCombiner.createAccessControlContext(bundle);
        } else {
            this.accessControlContext = null;
        }
        this.proxyManager = proxyManager;
        this.additionalNamespaces = namespaces;
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

    private long getDefaultTimeout() {
        long timeout = DEFAULT_TIMEOUT;
        try {
            timeout = Long.getLong(DEFAULT_TIMEOUT_PROPERTY, DEFAULT_TIMEOUT);
            if (timeout != DEFAULT_TIMEOUT) {
                LOGGER.debug(DEFAULT_TIMEOUT_PROPERTY + " is set to " + timeout + ".");
            }
        }
        catch (Exception e) {
            LOGGER.error(DEFAULT_TIMEOUT_PROPERTY + " is not a number. Using default value " + timeout + ".");
        }
        return timeout;
    }

    private void readDirectives() {
        Dictionary headers = bundle.getHeaders();
        String symbolicName = (String) headers.get(Constants.BUNDLE_SYMBOLICNAME);
        List<PathElement> paths = HeaderParser.parseHeader(symbolicName);

        String timeoutDirective = paths.get(0).getDirective(BlueprintConstants.TIMEOUT_DIRECTIVE);
        if (timeoutDirective != null) {
            LOGGER.debug("Timeout directive: {}", timeoutDirective);
            timeout = Integer.parseInt(timeoutDirective);
        }
        else {
        	timeout = getDefaultTimeout();
        }

        String graceperiod = paths.get(0).getDirective(BlueprintConstants.GRACE_PERIOD);
        if (graceperiod != null) {
            LOGGER.debug("Grace-period directive: {}", graceperiod);
            waitForDependencies = Boolean.parseBoolean(graceperiod);
        }

        xmlValidation = bundleContext.getProperty(BlueprintConstants.XML_VALIDATION_PROPERTY);
        if (xmlValidation == null) {
            xmlValidation = paths.get(0).getDirective(BlueprintConstants.XML_VALIDATION);
        }
        // enabled if null or "true"; structure-only if "structure"; disabled otherwise
        LOGGER.debug("Xml-validation directive: {}", xmlValidation);
    }

    public void schedule() {
        if (scheduled.compareAndSet(false, true)) {
            executors.submit(this);
        }
    }

    public void reload() {
        synchronized (scheduled) {
            if (destroyed.get()) {
                return;
            }
            tidyupComponents();
            resetComponentDefinitionRegistry();
            cancelFutureIfPresent();
            this.repository = null;
            this.processors.clear();
            timeout = 5 * 60 * 1000;
            waitForDependencies = true;
            xmlValidation = null;
            if (handlerSet != null) {
                handlerSet.removeListener(this);
                handlerSet.destroy();
                handlerSet = null;
            }
            state = State.Unknown;
            schedule();
        }
    }
    
    protected void resetComponentDefinitionRegistry() {
        this.componentDefinitionRegistry.reset();
        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintContainer", this));
        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundle", bundle));
        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundleContext", bundleContext));
        componentDefinitionRegistry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintConverter", converter));
    }

    public void run() {
        scheduled.set(false);
        synchronized (scheduled) {
            doRun();
        }
    }

    public State getState() {
        return state;
    }

    /**
     * This method must be called inside a synchronized block to ensure this method is not run concurrently
     */
    private void doRun() {
        try {
            for (;;) {
                if (destroyed.get()) {
                    return;
                }
                if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
                    return;
                }
                if (bundle.getBundleContext() != bundleContext) {
                    return;
                }
                LOGGER.debug("Running container for blueprint bundle {}/{} in state {}", getBundle().getSymbolicName(), getBundle().getVersion(), state);
                switch (state) {
                    case Unknown:
                        readDirectives();
                        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.CREATING, getBundle(), getExtenderBundle()));
                        parser = new Parser();
                        parser.parse(pathList);
                        namespaces = parser.getNamespaces();
                        if (additionalNamespaces != null) {
                            namespaces.addAll(additionalNamespaces);
                        }
                        handlerSet = handlers.getNamespaceHandlers(namespaces, getBundle());
                        handlerSet.addListener(this);
                        state = State.WaitForNamespaceHandlers;
                        break;
                    case WaitForNamespaceHandlers:
                    {
                        List<String> missing = new ArrayList<String>();
                        List<URI> missingURIs = new ArrayList<URI>();
                        for (URI ns : handlerSet.getNamespaces()) {
                            if (handlerSet.getNamespaceHandler(ns) == null) {
                                missing.add("(&(" + Constants.OBJECTCLASS + "=" + NamespaceHandler.class.getName() + ")(" + NamespaceHandlerRegistryImpl.NAMESPACE + "=" + ns + "))");
                                missingURIs.add(ns);
                            }
                        }
                        if (missing.size() > 0) {
                            LOGGER.info("Blueprint bundle {}/{} is waiting for namespace handlers {}", getBundle().getSymbolicName(), getBundle().getVersion(), missingURIs);
                            eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.GRACE_PERIOD, getBundle(), getExtenderBundle(), missing.toArray(new String[missing.size()])));
                            return;
                        }
                        resetComponentDefinitionRegistry();
                        if (xmlValidation == null || "true".equals(xmlValidation)) {
                            for (URI ns : handlerSet.getNamespaces()) {
                                NamespaceHandler handler = handlerSet.getNamespaceHandler(ns);
                                if (handler instanceof NamespaceHandler2) {
                                    if (((NamespaceHandler2) handler).usePsvi()) {
                                        xmlValidation = "psvi";
                                        break;
                                    }
                                }
                            }
                        }
                        try {
                            if (xmlValidation == null || "true".equals(xmlValidation)) {
                                parser.validate(handlerSet.getSchema(parser.getSchemaLocations()));
                            } else if ("structure".equals(xmlValidation)) {
                                parser.validate(handlerSet.getSchema(parser.getSchemaLocations()), new ValidationHandler());
                            } else if ("psvi".equals(xmlValidation)) {
                                parser.validatePsvi(handlerSet.getSchema(parser.getSchemaLocations()));
                            }
                            parser.populate(handlerSet, componentDefinitionRegistry);
                            state = State.Populated;
                        } catch (MissingNamespaceException e) {
                            // If we found a missing namespace when parsing the schema,
                            // we remain in the current state
                            handlerSet.getNamespaces().add(e.getNamespace());
                        }
                        break;
                    }
                    case Populated:
                        getRepository();
                        trackServiceReferences();
                        Runnable r = new Runnable() {
                            public void run() {
                                synchronized (scheduled) {
                                    if (destroyed.get()) {
                                        return;
                                    }
                                    String[] missingDependecies = getMissingDependencies();
                                    if (missingDependecies.length == 0) {
                                        return;
                                    }
                                    Throwable t = new TimeoutException();
                                    state = State.Failed;
                                    tidyupComponents();
                                    LOGGER.error("Unable to start container for blueprint bundle {}/{} due to unresolved dependencies {}", getBundle().getSymbolicName(), getBundle().getVersion(), Arrays.asList(missingDependecies), t);
                                    eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.FAILURE, getBundle(), getExtenderBundle(), missingDependecies, t));
                                }
                            }
                        };
                        timeoutFuture = timer.schedule(r, timeout, TimeUnit.MILLISECONDS);
                        state = State.WaitForInitialReferences;
                        break;
                    case WaitForInitialReferences:
                        if (waitForDependencies) {
                            String[] missingDependencies = getMissingDependencies();
                            if (missingDependencies.length > 0) {
                                LOGGER.info("Blueprint bundle {}/{} is waiting for dependencies {}", getBundle().getSymbolicName(), getBundle().getVersion(), Arrays.asList(missingDependencies));
                                eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.GRACE_PERIOD, getBundle(), getExtenderBundle(), missingDependencies));
                                return;
                            }
                        }
                        state = State.InitialReferencesSatisfied;
                        break;
                    case InitialReferencesSatisfied:
                        processTypeConverters();
                        processProcessors();
                        state = State.WaitForInitialReferences2;
                        break;
                    case WaitForInitialReferences2:
                        if (waitForDependencies) {
                            String[] missingDependencies = getMissingDependencies();
                            if (missingDependencies.length > 0) {
                                LOGGER.info("Blueprint bundle {}/{} is waiting for dependencies {}", getBundle().getSymbolicName(), getBundle().getVersion(), Arrays.asList(missingDependencies));
                                eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.GRACE_PERIOD, getBundle(), getExtenderBundle(), missingDependencies));
                                return;
                            }
                        }                       
                        state = State.Create;
                        break;
                    case Create:
                        cancelFutureIfPresent();
                        instantiateEagerComponents();
                        //Register the services after the eager components are ready, as per 121.6
                        registerServices();
                        // Register the BlueprintContainer in the OSGi registry
                        int bs = bundle.getState();
                        if (registration == null && (bs == Bundle.ACTIVE || bs == Bundle.STARTING)) {
                            Properties props = new Properties();
                            props.put(BlueprintConstants.CONTAINER_SYMBOLIC_NAME_PROPERTY,
                                    bundle.getSymbolicName());
                            props.put(BlueprintConstants.CONTAINER_VERSION_PROPERTY,
                                    JavaUtils.getBundleVersion(bundle));
                            registration = registerService(new String[]{BlueprintContainer.class.getName()}, this, props);
                        }
                        LOGGER.info("Blueprint bundle {}/{} has been started", getBundle().getSymbolicName(), getBundle().getVersion());
                        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.CREATED, getBundle(), getExtenderBundle()));
                        state = State.Created;
                        break;
                    case Created:
                    case Failed:
                        return;
                }
            }
        } catch (Throwable t) {
            try {
                state = State.Failed;
                cancelFutureIfPresent();
                tidyupComponents();
                LOGGER.error("Unable to start container for blueprint bundle {}/{}", getBundle().getSymbolicName(), getBundle().getVersion(), t);
                eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.FAILURE, getBundle(), getExtenderBundle(), t));
            } catch (RuntimeException re) {
                LOGGER.debug("Tidying up components failed. ", re);
                throw re;
            }
        }
    }

    public Class loadClass(final String name) throws ClassNotFoundException {
        if (accessControlContext == null) {
            return bundle.loadClass(name);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class>() {
                    public Class run() throws Exception {
                        return bundle.loadClass(name);
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

    @Override
    public ClassLoader getClassLoader() {
        return getBundle().adapt(BundleWiring.class).getClassLoader();
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

    protected void processTypeConverters() throws Exception {
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

        Map<String, Object> objects = getRepository().createAll(typeConverters, ProxyUtils.asList(Converter.class));
        for (String name : typeConverters) {
            Object obj = objects.get(name);
            if (obj instanceof Converter) {
                converter.registerConverter((Converter) obj);
            } else {
                throw new ComponentDefinitionException("Type converter " + obj + " does not implement the " + Converter.class.getName() + " interface");
            }
        }
    }

    protected void processProcessors() throws Exception {
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

            Object obj = null;
            if (ComponentDefinitionRegistryProcessor.class.isAssignableFrom(clazz)) {
                obj = repository.create(bean.getId(), ProxyUtils.asList(ComponentDefinitionRegistryProcessor.class));
                ((ComponentDefinitionRegistryProcessor) obj).process(componentDefinitionRegistry);
            }
            if (Processor.class.isAssignableFrom(clazz)) {
                obj = repository.create(bean.getId(), ProxyUtils.asList(Processor.class));
                this.processors.add((Processor) obj);
            }
            if (obj == null) {
                continue;
            }
            untrackServiceReferences();
            updateUninstantiatedRecipes();
            getSatisfiableDependenciesMap(true);
            trackServiceReferences();        
        }
    }
    private void updateUninstantiatedRecipes() {
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
        if (destroyed.get()) {
            return;
        }
        LOGGER.debug("Notified satisfaction {} in bundle {}/{}: {}",
                satisfiable.getName(), bundle.getSymbolicName(), getBundle().getVersion(), satisfiable.isSatisfied());

        if ((state == State.Create || state == State.Created) && satisfiable.isStaticLifecycle()) {
            if (satisfiable.isSatisfied()) {
                repository.reCreateInstance(satisfiable.getName());
            }
            else {
                repository.destroyInstance(satisfiable.getName());
            }
        }
        else if (state == State.Create || state == State.Created) {
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
        return new LinkedHashSet<String>(componentDefinitionRegistry.getComponentDefinitionNames());
    }
    
    public Object getComponentInstance(String id) throws NoSuchComponentException {
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

    public Bundle getBundle() {
        return bundle;
    }

    public void destroy() {
        synchronized (scheduled) {
            destroyed.set(true);
        }
        cancelFutureIfPresent();

        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.DESTROYING, getBundle(), getExtenderBundle()));

        executors.shutdownNow();
        if (handlerSet != null) {
            handlerSet.removeListener(this);
            handlerSet.destroy();
        }

        try {
            executors.awaitTermination(5 * 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted waiting for executor to shut down");
        }

        tidyupComponents();

        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.DESTROYED, getBundle(), getExtenderBundle()));
        LOGGER.debug("Container destroyed for blueprint bundle {}/{}", getBundle().getSymbolicName(), getBundle().getVersion());
    }

    public static void safeUnregisterService(ServiceRegistration reg) {
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                //This can be safely ignored
            }
        }
    }
    
    protected void quiesce() {
        destroyed.set(true);
        eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.DESTROYING, getBundle(), getExtenderBundle()));

        cancelFutureIfPresent();
        ServiceUtil.safeUnregisterService(registration);
        if (handlerSet != null) {
            handlerSet.removeListener(this);
            handlerSet.destroy();
        }
        LOGGER.debug("Blueprint container {} quiesced", getBundle().getSymbolicName(), getBundle().getVersion());
    }

    private void cancelFutureIfPresent() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    public void namespaceHandlerRegistered(URI uri) {
        if (handlerSet != null && handlerSet.getNamespaces().contains(uri)) {
            schedule();
        }
    }

    public void namespaceHandlerUnregistered(URI uri) {
        if (handlerSet != null && handlerSet.getNamespaces().contains(uri)) {
            synchronized (scheduled) {
                if (destroyed.get()) {
                    return;
                }
                tidyupComponents();
                resetComponentDefinitionRegistry();
                cancelFutureIfPresent();
                this.repository = null;
                this.processors.clear();
                handlerSet.removeListener(this);
                handlerSet.destroy();
                handlerSet = handlers.getNamespaceHandlers(namespaces, getBundle());
                handlerSet.addListener(this);
                state = State.WaitForNamespaceHandlers;
                schedule();
            }
        }
    }

    private void tidyupComponents() {
        unregisterServices();
        destroyComponents();
        untrackServiceReferences();
    }

    public void injectBeanInstance(BeanMetadata bmd, Object o)
            throws IllegalArgumentException, ComponentDefinitionException {
        ExecutionContext origContext
                = ExecutionContext.Holder.setContext((ExecutionContext) getRepository());
        try {
            ComponentMetadata cmd = componentDefinitionRegistry.getComponentDefinition(bmd.getId());
            if (cmd == null || cmd != bmd) {
                throw new IllegalArgumentException(bmd.getId() + " not found in blueprint container");
            }
            Recipe r = this.getRepository().getRecipe(bmd.getId());
            if (r instanceof BeanRecipe) {
                BeanRecipe br = (BeanRecipe) r;
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

    // this could be parameterized/customized, but for now, hard-coded for ignoring datatype validation
    private static class ValidationHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            // ignore
        }
        @Override
        public void error(SAXParseException exception) throws SAXException {
            final String cvctext = exception.getMessage(); 
            if (cvctext != null && 
                (cvctext.startsWith("cvc-datatype-valid.1") || cvctext.startsWith("cvc-attribute.3"))) {
                return;
            }
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }
}
