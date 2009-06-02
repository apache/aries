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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Callable;

import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;
import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.ExtendedServiceReferenceMetadata;
import org.apache.geronimo.blueprint.di.AbstractRecipe;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.utils.BundleDelegatingClassLoader;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for service reference recipes.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public abstract class AbstractServiceReferenceRecipe extends AbstractRecipe implements ServiceListener, SatisfiableRecipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefCollectionRecipe.class);

    protected final ExtendedBlueprintContainer blueprintContainer;
    protected final ServiceReferenceMetadata metadata;
    protected final Recipe listenersRecipe;
    protected final ClassLoader proxyClassLoader;
    protected final boolean optional;
    /** The OSGi filter for tracking references */
    protected final String filter;
    /** The list of listeners for this reference.  This list will be lazy created */
    protected List<Listener> listeners;

    private final List<ServiceReference> references = new ArrayList<ServiceReference>();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean satisfied = new AtomicBoolean();
    private SatisfactionListener satisfactionListener;
    private ProxyFactory proxyFactory;

    protected AbstractServiceReferenceRecipe(String name,
                                             ExtendedBlueprintContainer blueprintContainer,
                                             ServiceReferenceMetadata metadata,
                                             Recipe listenersRecipe) {
        super(name);
        this.prototype = false;
        this.blueprintContainer = blueprintContainer;
        this.metadata = metadata;
        this.listenersRecipe = listenersRecipe;
        // Create a ClassLoader delegating to the bundle, but also being able to see our bundle classes
        // so that the created proxy can access cglib classes.
        this.proxyClassLoader = new BundleDelegatingClassLoader(blueprintContainer.getBundleContext().getBundle(),
                                                                getClass().getClassLoader());
        
        this.optional = (metadata.getAvailability() == ReferenceMetadata.AVAILABILITY_OPTIONAL);
        this.filter = createOsgiFilter(metadata);
    }

    public void start(SatisfactionListener listener) {
        if (listener == null) throw new NullPointerException("satisfactionListener is null");
        if (started.compareAndSet(false, true)) {
            try {
                satisfactionListener = listener;
                satisfied.set(optional);
                // Synchronized block on references so that service events won't interfere with initial references tracking
                // though this may not be sufficient because we don't control ordering of those events
                synchronized (references) {
                    blueprintContainer.getBundleContext().addServiceListener(this, getOsgiFilter());
                    ServiceReference[] references = blueprintContainer.getBundleContext().getServiceReferences(null, getOsgiFilter());
                    if (references != null) {
                        for (ServiceReference reference : references) {
                            serviceAdded(reference);
                        }
                    }
                }
            } catch (InvalidSyntaxException e) {
                throw new ComponentDefinitionException(e);
            }
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            synchronized (references) {
                blueprintContainer.getBundleContext().removeServiceListener(this);
                references.clear();
                satisfied.set(false);
            }
        }
    }

    protected boolean isStarted() {
        return started.get();
    }

    public boolean isSatisfied() {
        return satisfied.get();
    }

    @Override
    public List<Recipe> getNestedRecipes() {
        List<Recipe> recipes = super.getNestedRecipes();
        if (listenersRecipe != null) {
            recipes.add(listenersRecipe);
        }
        return recipes;
    }

    public String getOsgiFilter() {
        return filter;
    }

    private void createListeners() {
        try {
            if (listenersRecipe != null) {
                listeners = (List<Listener>) listenersRecipe.create();
                for (Listener listener : listeners) {
                    listener.init(loadAllClasses(metadata.getInterfaceNames()));
                }
            } else {
                listeners = Collections.emptyList();
            }
        } catch (ClassNotFoundException e) {
            throw new ComponentDefinitionException(e);
        }
    }

    protected List<Class> loadAllClasses(Iterable<String> interfaceNames) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        for (String name : interfaceNames) {
            Class clazz = proxyClassLoader.loadClass(name);
            classes.add(clazz);
        }
        return classes;
    }

    protected Object createProxy(final Callable<Object> dispatcher, Iterable<String> interfaces) throws Exception {
        return getProxyFactory().createProxy(proxyClassLoader, toClassArray(loadAllClasses(interfaces)), dispatcher);
    }

    protected ProxyFactory getProxyFactory() throws ClassNotFoundException {
        if (proxyFactory == null) {
            synchronized (this) {
                if (proxyFactory == null) {
                    boolean proxyClass = false;
                    if (metadata instanceof ExtendedServiceReferenceMetadata) {
                        proxyClass = (((ExtendedServiceReferenceMetadata) metadata).getProxyMethod() & ExtendedServiceReferenceMetadata.PROXY_METHOD_CLASSES) != 0;
                    }
                    List<Class> classes = loadAllClasses(this.metadata.getInterfaceNames());
                    if (!proxyClass) {
                        for (Class cl : classes) {
                            if (!cl.isInterface()) {
                                throw new ComponentDefinitionException("A class " + cl.getName() + " was found in the interfaces list, but class proxying is not allowed by default. The ext:proxy-method='class' attribute needs to be added to this service reference.");
                            }
                        }
                    }
                    try {
                        proxyFactory = new CgLibProxyFactory();
                    } catch (Throwable t) {
                        if (proxyClass) {
                            throw new ComponentDefinitionException("Class proxying has been enabled but cglib can not be used", t);
                        }
                        proxyFactory = new JdkProxyFactory();
                    }
                }
            }
        }
        return proxyFactory;
    }

    public void serviceChanged(ServiceEvent event) {
        int eventType = event.getType();
        ServiceReference ref = event.getServiceReference();
        switch (eventType) {
            case ServiceEvent.REGISTERED:
                serviceAdded(ref);
                break;
            case ServiceEvent.MODIFIED:
                serviceModified(ref);
                break;
            case ServiceEvent.UNREGISTERING:
                serviceRemoved(ref);
                break;
        }
    }

    private void serviceAdded(ServiceReference ref) {
        boolean added;
        boolean satisfied;
        synchronized (references) {
            added = !references.contains(ref);
            if (added) {
                references.add(ref);
            }
            satisfied = optional || !references.isEmpty();
        }
        if (added) {
            track(ref);
        }
        setSatisfied(satisfied);
    }

    private void serviceModified(ServiceReference ref) {
        track(ref);
    }

    private void serviceRemoved(ServiceReference ref) {
        boolean removed;
        boolean satisfied;
        synchronized (references) {
            removed = references.remove(ref);
            satisfied = optional || !references.isEmpty();
        }
        if (removed) {
            untrack(ref);
        }
        setSatisfied(satisfied);
    }

    protected void setSatisfied(boolean s) {
        // This check will ensure an atomic comparision and set
        // so that it will only be true if the value actually changed
        if (satisfied.getAndSet(s) != s) {
            LOGGER.debug("Service reference with filter {} satisfied {}", getOsgiFilter(), this.satisfied);
            this.satisfactionListener.notifySatisfaction(this);
        }
    }

    @Override
    public void postCreate() {
        // Create the listeners and initialize them
        createListeners();
        // Retrack to inform listeners
        retrack();
    }

    protected abstract void track(ServiceReference reference);

    protected abstract void untrack(ServiceReference reference);

    protected abstract void retrack();

    public List<ServiceReference> getServiceReferences() {
        synchronized (references) {
            return new ArrayList<ServiceReference>(references);
        }
    }

    public ServiceReference getBestServiceReference() {
        synchronized (references) {
            int length = references.size();
            if (length == 0) { /* if no service is being tracked */
                return null;
            }
            int index = 0;
            if (length > 1) { /* if more than one service, select highest ranking */
                int maxRanking = Integer.MIN_VALUE;
                long minId = Long.MAX_VALUE;
                for (int i = 0; i < length; i++) {
                    Object property = references.get(i).getProperty(Constants.SERVICE_RANKING);
                    int ranking = (property instanceof Integer) ? (Integer) property : 0;
                    long id = (Long) references.get(i).getProperty(Constants.SERVICE_ID);
                    if ((ranking > maxRanking) || (ranking == maxRanking && id < minId)) {
                        index = i;
                        maxRanking = ranking;
                        minId = id;
                    }
                }
            }
            return references.get(index);
        }
    }

    public static class Listener {

        private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);

        /* Inject by ObjectRecipe */
        private Object listener;
        /* Inject by ObjectRecipe */
        private org.osgi.service.blueprint.reflect.Listener metadata;

        private Set<Method> bindMethodsOneArg = new HashSet<Method>();
        private Set<Method> bindMethodsTwoArgs = new HashSet<Method>();
        private Set<Method> unbindMethodsOneArg = new HashSet<Method>();
        private Set<Method> unbindMethodsTwoArgs = new HashSet<Method>();

        public void setListener(Object listener) {
            this.listener = listener;
        }

        public void setMetadata(org.osgi.service.blueprint.reflect.Listener metadata) {
            this.metadata = metadata;
        }

        public void init(Collection<Class> classes) {
            Set<Class> clazzes = new HashSet<Class>(classes);
            clazzes.add(Object.class);
            Class listenerClass = listener.getClass();
            String bindName = metadata.getBindMethodName();
            if (bindName != null) {
                bindMethodsOneArg.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, bindName, new Class[] { ServiceReference.class }));
                for (Class clazz : clazzes) {
                    bindMethodsTwoArgs.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, bindName, new Class[] { clazz, Map.class }));
                }
                if (bindMethodsOneArg.size() + bindMethodsTwoArgs.size() == 0) {
                    throw new ComponentDefinitionException("No matching methods found for listener bind method: " + bindName);
                }
            }
            String unbindName = metadata.getUnbindMethodName();
            if (unbindName != null) {
                unbindMethodsOneArg.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, new Class[] { ServiceReference.class }));
                for (Class clazz : clazzes) {
                    unbindMethodsTwoArgs.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, new Class[] { clazz, Map.class }));
                }
                if (unbindMethodsOneArg.size() + unbindMethodsTwoArgs.size() == 0) {
                    throw new ComponentDefinitionException("No matching methods found for listener unbind method: " + unbindName);
                }
            }
        }

        public void bind(ServiceReference reference, Object service) {
            invokeMethods(bindMethodsOneArg, bindMethodsTwoArgs, reference, service);
        }

        public void unbind(ServiceReference reference, Object service) {
            invokeMethods(unbindMethodsOneArg, unbindMethodsTwoArgs, reference, service);
        }

        private void invokeMethods(Set<Method> oneArgMethods, Set<Method> twoArgsMethods, ServiceReference reference, Object service) {
            for (Method method : oneArgMethods) {
                try {
                    method.invoke(listener, reference);
                } catch (Exception e) {
                    LOGGER.info("Error calling listener method " + method, e);
                }
            }
            Map<String, Object> props = null;
            for (Method method : twoArgsMethods) {
                if (props == null) {
                    props = new HashMap<String, Object>();
                    for (String name : reference.getPropertyKeys()) {
                        props.put(name, reference.getProperty(name));
                    }
                }
                try {
                    method.invoke(listener, service, props);
                } catch (Exception e) {
                    LOGGER.info("Error calling listener method " + method, e);
                }
            }
        }
    }

    /**
     * Create the OSGi filter corresponding to the ServiceReferenceMetadata constraints
     *
     * @param metadata the service reference metadata
     * @return the OSGi filter
     */
    private static String createOsgiFilter(ServiceReferenceMetadata metadata) {
        List<String> members = new ArrayList<String>();
        // Handle filter
        String flt = metadata.getFilter();
        if (flt != null && flt.length() > 0) {
            if (!flt.startsWith("(")) {
                flt = "(" + flt + ")";
            }
            members.add(flt);
        }
        // Handle interfaces
        Set<String> interfaces = new HashSet<String>(metadata.getInterfaceNames());
        if (!interfaces.isEmpty()) {
            for (String itf : interfaces) {
                members.add("(" + Constants.OBJECTCLASS + "=" + itf + ")");
            }
        }
        // Handle component name
        String componentName = metadata.getComponentName();
        if (componentName != null && componentName.length() > 0) {
            members.add("(" + BlueprintConstants.COMPONENT_NAME_PROPERTY + "=" + componentName + ")");
        }
        // Create filter
        if (members.isEmpty()) {
            throw new IllegalStateException("No constraints were specified on the service reference");
        }
        StringBuilder sb = new StringBuilder("(&");
        for (String member : members) {
            sb.append(member);
        }
        sb.append(")");
        return sb.toString();
    }

    private static Class[] getInterfaces(Class[] classes) {
        List<Class> interfaces = new ArrayList<Class>();
        for (Class clazz : classes) {
            if (clazz.isInterface()) {
                interfaces.add(clazz);
            }
        }
        return toClassArray(interfaces);
    }

    private static Class[] toClassArray(List<Class> classes) {
        return classes.toArray(new Class [classes.size()]);
    }

    public static interface ProxyFactory {

        public Object createProxy(ClassLoader classLoader, Class[] classes, Callable<Object> dispatcher);

    }

    public static class JdkProxyFactory implements ProxyFactory {

        public Object createProxy(final ClassLoader classLoader, final Class[] classes, final Callable<Object> dispatcher) {
            return Proxy.newProxyInstance(classLoader, getInterfaces(classes), new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return method.invoke(dispatcher.call(), args);
                }
            });
        }

    }

    public static class CgLibProxyFactory implements ProxyFactory {

        public Object createProxy(final ClassLoader classLoader, final Class[] classes, final Callable<Object> dispatcher) {
            Enhancer e = new Enhancer();
            e.setClassLoader(classLoader);
            e.setSuperclass(getTargetClass(classes));
            e.setInterfaces(getInterfaces(classes));
            e.setInterceptDuringConstruction(false);
            e.setCallback(new Dispatcher() {
                public Object loadObject() throws Exception {
                    return dispatcher.call();
                }
            });
            e.setUseFactory(false);
            return e.create();
        }

        protected Class getTargetClass(Class[] interfaceNames) {
            // Only allow class proxying if specifically asked to
            Class root = Object.class;
            for (Class clazz : interfaceNames) {
                if (!clazz.isInterface()) {
                    if (root == Object.class) {
                        root = clazz;
                        continue;
                    }
                    // Check that all classes are in the same hierarchy
                    for (Class p = clazz; p != Object.class; p = p.getSuperclass()) {
                        if (p == root) {
                            root = clazz;
                            continue;
                        }
                    }
                    for (Class p = root; p != Object.class; p = p.getSuperclass()) {
                        if (p == clazz) {
                            continue;
                        }
                    }
                    throw new ComponentDefinitionException("Classes " + root.getClass().getName() + " and " + clazz.getName() + " are not in the same hierarchy");
                }
            }
            return root;
        }

    }

}
