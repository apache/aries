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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;

import org.apache.aries.blueprint.BlueprintConstants;
import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.ExtendedServiceReferenceMetadata;
import org.apache.aries.blueprint.di.AbstractRecipe;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.utils.BundleDelegatingClassLoader;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for service reference recipes.
 *
 * TODO: if we have a single interface (which is the standard behavior), then we should be able to get rid of
 *       the proxyClassloader and just use this interface classloader to define the proxy
 *
 * TODO: it is allowed to have no interface defined at all, which should result in an empty proxy
 *
 * @version $Rev$, $Date$
 */
public abstract class AbstractServiceReferenceRecipe extends AbstractRecipe implements ServiceListener, SatisfiableRecipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServiceReferenceRecipe.class);

    protected final ExtendedBlueprintContainer blueprintContainer;
    protected final ServiceReferenceMetadata metadata;
    protected final CollectionRecipe listenersRecipe;
    protected final List<Recipe> explicitDependencies;
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
    private volatile ProxyFactory proxyFactory;

    protected AbstractServiceReferenceRecipe(String name,
                                             ExtendedBlueprintContainer blueprintContainer,
                                             ServiceReferenceMetadata metadata,
                                             CollectionRecipe listenersRecipe,
                                             List<Recipe> explicitDependencies) {
        super(name);
        this.prototype = false;
        this.blueprintContainer = blueprintContainer;
        this.metadata = metadata;
        this.listenersRecipe = listenersRecipe;
        this.explicitDependencies = explicitDependencies;
        
        
        this.proxyClassLoader = makeProxyClassLoader(blueprintContainer, metadata);

        this.optional = (metadata.getAvailability() == ReferenceMetadata.AVAILABILITY_OPTIONAL);
        this.filter = createOsgiFilter(metadata);
    }



    // Create a ClassLoader delegating to the bundle, but also being able to see our bundle classes
    // so that the created proxy can access cglib classes.
    // TODO: we should be able to get rid of this classloader when using JDK 1.4 proxies with a single interface
    //         (the case defined by the spec) and use the interface classloader instead
    private ClassLoader makeProxyClassLoader(
        final ExtendedBlueprintContainer blueprintContainer,
        ServiceReferenceMetadata metadata) {
      
      String typeName = metadata.getInterface();
      
      if (typeName == null) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
          public ClassLoader run() {
            return new BundleDelegatingClassLoader(blueprintContainer.getBundleContext().getBundle(),
                AbstractServiceReferenceRecipe.class.getClassLoader());
          }      
        });
      }
      
      final ClassLoader interfaceClassLoader;
      try {
        Bundle clientBundle = blueprintContainer.getBundleContext().getBundle();
        interfaceClassLoader = clientBundle.loadClass(typeName).getClassLoader();
      } catch (ClassNotFoundException cnfe) {
        throw new ComponentDefinitionException("Unable to load class " + typeName + " from recipe " + this, cnfe);
      }
      
      return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
        public ClassLoader run() {
          return new DualClassloader(interfaceClassLoader);
        }      
      });
    }

    private static class DualClassloader extends ClassLoader {
      DualClassloader(ClassLoader parent) {
        super(parent);
      }
      
      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        return getClass().getClassLoader().loadClass(name);
      }

      @Override
      protected URL findResource(String name) {
        return getClass().getClassLoader().getResource(name);
      }
    }
    
    public CollectionRecipe getListenersRecipe() {
        return listenersRecipe;
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
                            this.references.add(reference);
                            track(reference);                           
                        }
                        satisfied.set(optional || !this.references.isEmpty());
                    }
                    LOGGER.debug("Found initial references {} for OSGi service {}", references, getOsgiFilter());
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
                doStop();
                for (Iterator<ServiceReference> it = references.iterator(); it.hasNext();) {
                    ServiceReference ref = it.next();
                    it.remove();
                    untrack(ref);
                }
                satisfied.set(false);
            }
        }
    }

    protected void doStop() {
    }

    protected boolean isStarted() {
        return started.get();
    }

    public boolean isSatisfied() {
        return satisfied.get();
    }

    @Override
    public List<Recipe> getConstructorDependencies() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        if (explicitDependencies != null) {
            recipes.addAll(explicitDependencies);
        }
        return recipes;
    }
    
    public List<Recipe> getDependencies() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        if (listenersRecipe != null) {
            recipes.add(listenersRecipe);
        }
        recipes.addAll(getConstructorDependencies());
        return recipes;
    }

    public String getOsgiFilter() {
        return filter;
    }

    protected void createListeners() {
        try {
            if (listenersRecipe != null) {
                List<Listener> listeners = (List<Listener>) listenersRecipe.create();
                for (Listener listener : listeners) {
                    List<Class> cl = new ArrayList<Class>();
                    if (metadata.getInterface() != null) {
                        cl.addAll(loadAllClasses(Collections.singletonList(metadata.getInterface())));
                    } else {
                        cl.add(Object.class);
                    }
                    listener.init(cl);
                }
                this.listeners = listeners;
            } else {
                this.listeners = Collections.emptyList();
            }
        } catch (ClassNotFoundException e) {
            throw new ComponentDefinitionException(e);
        }
    }

    protected List<Class> loadAllClasses(Iterable<String> interfaceNames) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        for (String name : interfaceNames) {
            Class clazz = loadClass(name);
            classes.add(clazz);
        }
        return classes;
    }

    protected ReifiedType loadType(String typeName, ClassLoader fromClassLoader) {
        if (typeName == null) {
            return null;
        }
        try {
            // this method is overriden to use the blueprint container directly
            // because proxies can be created outside of the recipe creation which
            // would lead to an exception because the context is not set
            // TODO: consider having the context as a property on the recipe rather than a thread local
            return GenericType.parse(typeName, fromClassLoader != null ? fromClassLoader : blueprintContainer);
        } catch (ClassNotFoundException e) {
            throw new ComponentDefinitionException("Unable to load class " + typeName + " from recipe " + this, e);
        }
    }


    protected Object createProxy(final Callable<Object> dispatcher, Iterable<String> interfaces) throws Exception {
        if (!interfaces.iterator().hasNext()) {
            return new Object();
        } else {
            return getProxyFactory().createProxy(proxyClassLoader, toClassArray(loadAllClasses(interfaces)), dispatcher);
        }
    }

    protected synchronized ProxyFactory getProxyFactory() throws ClassNotFoundException {
        if (proxyFactory == null) {
            boolean proxyClass = false;
            if (metadata instanceof ExtendedServiceReferenceMetadata) {
                proxyClass = (((ExtendedServiceReferenceMetadata) metadata).getProxyMethod() & ExtendedServiceReferenceMetadata.PROXY_METHOD_CLASSES) != 0;
            }
            List<Class> classes = loadAllClasses(Collections.singletonList(this.metadata.getInterface()));
            if (!proxyClass) {
                for (Class cl : classes) {
                    if (!cl.isInterface()) {
                        throw new ComponentDefinitionException("A class " + cl.getName() + " was found in the interfaces list, but class proxying is not allowed by default. The ext:proxy-method='classes' attribute needs to be added to this service reference.");
                    }
                }
            }
            try {
                // Try load load a cglib class (to make sure it's actually available
                // then create the cglib factory
                getClass().getClassLoader().loadClass("net.sf.cglib.proxy.Enhancer");
                proxyFactory = new CgLibProxyFactory();
            } catch (Throwable t) {
                if (proxyClass) {
                    throw new ComponentDefinitionException("Class proxying has been enabled but cglib can not be used", t);
                }
                proxyFactory = new JdkProxyFactory();
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
        LOGGER.debug("Tracking reference {} for OSGi service {}", ref, getOsgiFilter());
        synchronized (references) {
            references.add(ref);
        }
        track(ref);
        setSatisfied(true);
    }

    private void serviceModified(ServiceReference ref) {
        // ref must be in references and must be satisfied
        track(ref);
    }

    private void serviceRemoved(ServiceReference ref) {
        LOGGER.debug("Untracking reference {} for OSGi service {}", ref, getOsgiFilter());
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

    protected abstract void track(ServiceReference reference);

    protected abstract void untrack(ServiceReference reference);

    protected abstract void retrack();

    protected void updateListeners() {  
        if (references.isEmpty()) {
            unbind(null, null);
        } else {
            retrack();
        }
    }
    
    protected void bind(ServiceReference reference, Object service) {
        if (listeners != null) {    
            for (Listener listener : listeners) {
                if (listener != null) {
                    listener.bind(reference, service);
                }
            } 
        }
    }
    
    protected void unbind(ServiceReference reference, Object service) {
        if (listeners != null) {    
            for (Listener listener : listeners) {
                if (listener != null) {
                    listener.unbind(reference, service);
                }
            } 
        }
    }
    
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

        private Object listener;
        private ReferenceListener metadata;
        private ExtendedBlueprintContainer blueprintContainer;

        private Set<Method> bindMethodsReference = new HashSet<Method>();
        private Set<Method> bindMethodsObjectProp = new HashSet<Method>();
        private Set<Method> bindMethodsObject = new HashSet<Method>();
        private Set<Method> unbindMethodsReference = new HashSet<Method>();
        private Set<Method> unbindMethodsObject = new HashSet<Method>();
        private Set<Method> unbindMethodsObjectProp = new HashSet<Method>();

        public void setListener(Object listener) {
            this.listener = listener;
        }

        public void setMetadata(ReferenceListener metadata) {
            this.metadata = metadata;
        }

        public void setBlueprintContainer(ExtendedBlueprintContainer blueprintContainer) {
            this.blueprintContainer = blueprintContainer;
        }
        
        public void init(Collection<Class> classes) {
            Set<Class> clazzes = new HashSet<Class>(classes);
            clazzes.add(Object.class);
            Class listenerClass = listener.getClass();
            String bindName = metadata.getBindMethod();
            if (bindName != null) {
                bindMethodsReference.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, bindName, new Class[] { ServiceReference.class }));
                for (Class clazz : clazzes) {
                    bindMethodsObject.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, bindName, new Class[] { clazz }));
                    bindMethodsObjectProp.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, bindName, new Class[] { clazz, Map.class }));
                }
                if (bindMethodsReference.size() + bindMethodsObject.size() + bindMethodsObjectProp.size() == 0) {
                    throw new ComponentDefinitionException("No matching methods found for listener bind method: " + bindName);
                }
            }
            String unbindName = metadata.getUnbindMethod();
            if (unbindName != null) {
                unbindMethodsReference.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, new Class[] { ServiceReference.class }));
                for (Class clazz : clazzes) {
                    unbindMethodsObject.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, new Class[] { clazz }));
                    unbindMethodsObjectProp.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, new Class[] { clazz, Map.class }));
                }
                if (unbindMethodsReference.size() + unbindMethodsObject.size() + unbindMethodsObjectProp.size() == 0) {
                    throw new ComponentDefinitionException("No matching methods found for listener unbind method: " + unbindName);
                }
            }
        }

        public void bind(ServiceReference reference, Object service) {
            invokeMethods(bindMethodsReference, bindMethodsObject, bindMethodsObjectProp, reference, service);
        }

        public void unbind(ServiceReference reference, Object service) {
            invokeMethods(unbindMethodsReference, unbindMethodsObject, unbindMethodsObjectProp, reference, service);
        }

        private void invokeMethods(Set<Method> referenceMethods, Set<Method> objectMethods, Set<Method> objectPropMethods, ServiceReference reference, Object service) {
            for (Method method : referenceMethods) {
                try {
                    ReflectionUtils.invoke(blueprintContainer.getAccessControlContext(), 
                                           method, listener, reference);
                } catch (Exception e) {
                    LOGGER.error("Error calling listener method " + method, e);
                }
            }
            for (Method method : objectMethods) {
                try {
                    ReflectionUtils.invoke(blueprintContainer.getAccessControlContext(), 
                                           method, listener, service);
                } catch (Exception e) {
                    LOGGER.error("Error calling listener method " + method, e);
                }
            }
            Map<String, Object> props = null;
            for (Method method : objectPropMethods) {
                if (props == null) {
                    props = new HashMap<String, Object>();
                    if (reference != null) {
                        for (String name : reference.getPropertyKeys()) {
                            props.put(name, reference.getProperty(name));
                        }
                    }
                }
                try {
                    ReflectionUtils.invoke(blueprintContainer.getAccessControlContext(), 
                                           method, listener, service, props);
                } catch (Exception e) {
                    LOGGER.error("Error calling listener method " + method, e);
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
        String interfaceName = metadata.getInterface();
        if (interfaceName != null && interfaceName.length() > 0) {
            members.add("(" + Constants.OBJECTCLASS + "=" + interfaceName + ")");
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
        if (members.size() == 1) {
            return members.get(0);
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
                    try {
                        return method.invoke(dispatcher.call(), args);
                    } catch (InvocationTargetException ite) {
                      throw ite.getTargetException();
                    }
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

        protected Class<?> getTargetClass(Class<?>[] interfaceNames) {
            // Only allow class proxying if specifically asked to
            Class<?> root = Object.class;
            for (Class<?> clazz : interfaceNames) {
                if (!clazz.isInterface()) {
                    if (root.isAssignableFrom(clazz)) {
                        root = clazz;
                    } else if (clazz.isAssignableFrom(root)) {
                        //nothing to do, root is correct
                    } else {
                        throw new ComponentDefinitionException("Classes " + root.getClass().getName() + " and " + clazz.getName() + " are not in the same hierarchy");
                    }
                }
            }
            return root;
        }

    }

}
