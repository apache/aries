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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.BlueprintContextEventSender;
import org.apache.geronimo.blueprint.SatisfiableRecipe;
import org.apache.geronimo.blueprint.utils.BundleDelegatingClassLoader;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.Recipe;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;

/**
 * Abstract class for service reference recipes.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public abstract class AbstractServiceReferenceRecipe extends AbstractRecipe implements ServiceListener, SatisfiableRecipe {

    protected final BlueprintContext blueprintContext;
    protected final BlueprintContextEventSender sender;
    protected final ServiceReferenceMetadata metadata;
    protected final Recipe listenersRecipe;
    protected List<Listener> listeners;
    private String filter;
    protected final ClassLoader proxyClassLoader;
    protected ServiceReferenceTracker tracker;
    protected boolean optional;

    protected AbstractServiceReferenceRecipe(BlueprintContext blueprintContext,
                                             BlueprintContextEventSender sender,
                                             ServiceReferenceMetadata metadata,
                                             Recipe listenersRecipe) {
        this.blueprintContext = blueprintContext;
        this.sender = sender;
        this.metadata = metadata;
        this.listenersRecipe = listenersRecipe;
        // Create a ClassLoader delegating to the bundle, but also being able to see our bundle classes
        // so that the created proxy can access cglib classes.
        this.proxyClassLoader = new BundleDelegatingClassLoader(blueprintContext.getBundleContext().getBundle(),
                                                                getClass().getClassLoader());
        
        this.optional = (metadata.getAvailability() == ReferenceMetadata.AVAILABILITY_OPTIONAL);
        this.tracker = new ServiceReferenceTracker(blueprintContext.getBundleContext(), getOsgiFilter(), optional);
    }

    public void start() {
        try {
            tracker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void stop() {
        tracker.stop();
    }
    
    public void registerListener(SatisfactionListener listener) {
        tracker.registerListener(new SatisfactionListenerWrapper(listener));
    }
    
    public void unregisterListener(SatisfactionListener listener) {        
    }

    public boolean isSatisfied() {
        return tracker.isSatisfied();
    }

    protected String getOsgiFilter() {
        if (filter == null) {
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
            filter = sb.toString();
        }
        return filter;
    }

    protected void createListeners() throws ClassNotFoundException {
        if (listenersRecipe != null) {
            listeners = (List<Listener>) listenersRecipe.create(proxyClassLoader);
            for (Listener listener : listeners) {
                listener.init(getAllClasses(metadata.getInterfaceNames()));
            }
        } else {
            listeners = Collections.emptyList();
        }
    }

    protected List<Class> getAllClasses(Iterable<String> interfaceNames) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        for (String name : interfaceNames) {
            Class clazz = proxyClassLoader.loadClass(name);
            classes.add(clazz);
        }
        return classes;
    }

    protected List<Class> getInterfaces(Iterable<String> interfaceNames) throws ClassNotFoundException {
        List<Class> interfaces = new ArrayList<Class>();
        for (String name : interfaceNames) {
            Class clazz = proxyClassLoader.loadClass(name);
            if (clazz.isInterface()) {
                interfaces.add(clazz);
            }
        }
        return interfaces;
    }

    protected static Class[] toClassArray(List<Class> classes) {
        return classes.toArray(new Class [classes.size()]);
    }
    
    protected Class getTargetClass(Iterable<String> interfaceNames) throws ClassNotFoundException {
        Class root = Object.class;
        for (String name : interfaceNames) {
            Class clazz = proxyClassLoader.loadClass(name);
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
                throw new ConstructionException("Classes " + root.getClass().getName() + " and " + clazz.getName() + " are not in the same hierarchy");
            }
        }
        return root;
    }

    protected Object createProxy(Dispatcher dispatcher, Iterable<String> interfaces) throws Exception {
        // TODO: we only use cglib for this small piece of code, we might want to use asm directly to
        //       lower the number of dependencies / reduce size of jars
        Enhancer e = new Enhancer();
        e.setClassLoader(proxyClassLoader);
        e.setSuperclass(getTargetClass(interfaces));
        e.setInterfaces(toClassArray(getInterfaces(interfaces)));
        e.setInterceptDuringConstruction(false);
        e.setCallback(dispatcher);
        e.setUseFactory(false);
        return e.create();
    }

    public void serviceChanged(ServiceEvent event) {
        int eventType = event.getType();
        ServiceReference ref = event.getServiceReference();
        switch (eventType) {
            case ServiceEvent.REGISTERED:
            case ServiceEvent.MODIFIED:
                track(ref);
                break;
            case ServiceEvent.UNREGISTERING:
                untrack(ref);
                break;
        }
    }

    protected abstract void track(ServiceReference reference);

    protected abstract void untrack(ServiceReference reference);

    private class SatisfactionListenerWrapper implements ServiceReferenceTracker.SatisfactionListener {

        SatisfiableRecipe.SatisfactionListener listener;
        
        public SatisfactionListenerWrapper(SatisfiableRecipe.SatisfactionListener listener) {
            this.listener = listener;
        }
        
        public void notifySatisfaction(ServiceReferenceTracker satisfiable) {
            this.listener.notifySatisfaction(AbstractServiceReferenceRecipe.this);
        }
        
    }
    
    public static class Listener {

        /* Inject by ObjectRecipe */
        private Object listener;
        /* Inject by ObjectRecipe */
        private org.osgi.service.blueprint.reflect.Listener metadata;

        private Set<Method> bindMethodsOneArg = new HashSet<Method>();
        private Set<Method> bindMethodsTwoArgs = new HashSet<Method>();
        private Set<Method> unbindMethodsOneArg = new HashSet<Method>();
        private Set<Method> unbindMethodsTwoArgs = new HashSet<Method>();

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
            }
            String unbindName = metadata.getUnbindMethodName();
            if (unbindName != null) {
                unbindMethodsOneArg.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, new Class[] { ServiceReference.class }));
                for (Class clazz : clazzes) {
                    unbindMethodsTwoArgs.addAll(ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, new Class[] { clazz, Map.class }));
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
                    e.printStackTrace(); // TODO: log
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
                    e.printStackTrace(); // TODO: log
                }
            }
        }
    }

}
