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

import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.CollectionRecipe;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.geronimo.blueprint.LifeCycle;
import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.ModuleContextEventSender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.blueprint.reflect.UnaryServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.context.ServiceUnavailableException;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import net.sf.cglib.proxy.ProxyRefDispatcher;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Dispatcher;

/**
 * A recipe to create an unary OSGi service reference.
 *
 * TODO: check synchronization / thread safety
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ReferenceServiceRecipe extends AbstractRecipe implements Dispatcher, LifeCycle, ServiceListener {

    private final ModuleContext moduleContext;
    private final ModuleContextEventSender sender;
    private final UnaryServiceReferenceComponentMetadata metadata;
    private final CollectionRecipe listenersRecipe;
    private Class proxyClass;
    private String filter;
    private List<Listener> listeners;

    private volatile ServiceReference trackedServiceReference;
    private volatile Object trackedService;
    private final Object monitor = new Object();

    public ReferenceServiceRecipe(ModuleContext moduleContext,
                                  ModuleContextEventSender sender,
                                  UnaryServiceReferenceComponentMetadata metadata,
                                  CollectionRecipe listenersRecipe) {
        this.moduleContext = moduleContext;
        this.sender = sender;
        this.metadata = metadata;
        this.listenersRecipe = listenersRecipe;
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        // TODO: serviceAvailabilitySpecification
        try {
            ClassLoader classLoader = new BundleDelegatingClassLoader(moduleContext.getBundleContext().getBundle(),
                                                                      getClass().getClassLoader());
            Enhancer e = new Enhancer();
            e.setClassLoader(classLoader);
            e.setSuperclass(getTargetClass(classLoader));
            e.setInterfaces(getInterfaces(classLoader));
            e.setInterceptDuringConstruction(false);
            e.setCallback(this);
            e.setUseFactory(false);
            Object obj = e.create();
            proxyClass = obj.getClass();

            if (listenersRecipe != null) {
                listeners = (List<Listener>) listenersRecipe.create(classLoader);
                for (Listener listener : listeners) {
                    listener.init(proxyClass);
                }
            } else {
                listeners = Collections.emptyList();
            }

            filter = getOsgiFilter();
            moduleContext.getBundleContext().addServiceListener(this, filter);
            retrack();

            if (getName() != null) {
                ExecutionContext.getContext().addObject(getName(), obj);
            }

            return obj;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new ConstructionException(t);
        }
    }

    private String getOsgiFilter() {
        List<String> members = new ArrayList<String>();
        // Handle filter
        String filter = metadata.getFilter();
        if (filter != null && filter.length() > 0) {
            if (!filter.startsWith("(")) {
                filter = "(" + filter + ")";
            }
            members.add(filter);
        }
        // Handle interfaces
        Set<String> interfaces = (Set<String>) metadata.getInterfaceNames();
        if (interfaces != null && !interfaces.isEmpty()) {
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

    private Class[] getInterfaces(ClassLoader classLoader) throws ClassNotFoundException {
        List<Class> interfaces = new ArrayList<Class>();
        for (String name : (Set<String>) metadata.getInterfaceNames()) {
            Class clazz = classLoader.loadClass(name);
            if (clazz.isInterface()) {
                interfaces.add(clazz);
            }
        }
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    private Class getTargetClass(ClassLoader classLoader) throws ClassNotFoundException {
        Class root = Object.class;
        for (String name : (Set<String>) metadata.getInterfaceNames()) {
            Class clazz = classLoader.loadClass(name);
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

    public boolean canCreate(Type type) {
        return true;
    }

    public Object loadObject() throws Exception {
        Object svc = trackedService;
        if (svc == null && metadata.getTimeout() > 0) {
            Set<String> interfaces = (Set<String>) metadata.getInterfaceNames();
            sender.sendWaiting(moduleContext, interfaces.toArray(new String[interfaces.size()]), filter);
            synchronized (monitor) {
                monitor.wait(metadata.getTimeout());
            }
            svc = trackedService;
        }
        if (svc == null) {
            throw new ServiceUnavailableException("Timeout expired when waiting for OSGi service", proxyClass.getSuperclass(), filter);
        }
        return svc;
    }

    public void init() throws Exception {
    }

    public void destroy() throws Exception {
        moduleContext.getBundleContext().removeServiceListener(this);
    }

    private ServiceReference getBestServiceReference(ServiceReference[] references) {
        int length = (references == null) ? 0 : references.length;
        if (length == 0) { /* if no service is being tracked */
            return null;
        }
        int index = 0;
        if (length > 1) { /* if more than one service, select highest ranking */
            int rankings[] = new int[length];
            int count = 0;
            int maxRanking = Integer.MIN_VALUE;
            for (int i = 0; i < length; i++) {
                Object property = references[i].getProperty(Constants.SERVICE_RANKING);
                int ranking = (property instanceof Integer) ? ((Integer) property).intValue() : 0;
                rankings[i] = ranking;
                if (ranking > maxRanking) {
                    index = i;
                    maxRanking = ranking;
                    count = 1;
                } else {
                    if (ranking == maxRanking) {
                        count++;
                    }
                }
            }
            if (count > 1) { /* if still more than one service, select lowest id */
                long minId = Long.MAX_VALUE;
                for (int i = 0; i < length; i++) {
                    if (rankings[i] == maxRanking) {
                        long id = ((Long) (references[i].getProperty(Constants.SERVICE_ID))).longValue();
                        if (id < minId) {
                            index = i;
                            minId = id;
                        }
                    }
                }
            }
        }
        return references[index];
    }

    private void retrack() {
        try {
            ServiceReference[] refs = moduleContext.getBundleContext().getServiceReferences(null, filter);
            ServiceReference ref = getBestServiceReference(refs);
            if (ref != null) {
                bind(ref);
            } else {
                unbind();
            }
        } catch (InvalidSyntaxException e) {
            // Ignore, should never happen
        }
    }

    private void track(ServiceReference ref) {
        if (trackedServiceReference == null) {
            bind(ref);
        } else {
            Object property = trackedServiceReference.getProperty(Constants.SERVICE_RANKING);
            int trackedRanking = (property instanceof Integer) ? ((Integer) property).intValue() : 0;
            property = ref.getProperty(Constants.SERVICE_RANKING);
            int newRanking = (property instanceof Integer) ? ((Integer) property).intValue() : 0;
            if (trackedRanking > newRanking) {
                return;
            } else if (trackedRanking == newRanking) {
                long trackedId = ((Long) (trackedServiceReference.getProperty(Constants.SERVICE_ID))).longValue();
                long newId = ((Long) (ref.getProperty(Constants.SERVICE_ID))).longValue();
                if (trackedId < newId) {
                    return;
                }
            }
            bind(ref);
        }
    }

    private void bind(ServiceReference ref) {
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                moduleContext.getBundleContext().ungetService(trackedServiceReference);
            }
            trackedServiceReference = ref;
            trackedService = moduleContext.getBundleContext().getService(trackedServiceReference);
            for (Listener listener : listeners) {
                listener.bind(trackedServiceReference, trackedService);
            }
        }
    }

    private void unbind() {
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                for (Listener listener : listeners) {
                    listener.unbind(trackedServiceReference, trackedService);
                }
                moduleContext.getBundleContext().ungetService(trackedServiceReference);
                trackedServiceReference = null;
                trackedService = null;
            }
        }
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
                retrack();
                break;
        }
    }

    public static class Listener {

        private Object listener;
        private BindingListenerMetadata metadata;
        private List<Method> bindMethodsOneArg;
        private List<Method> bindMethodsTwoArgs;
        private List<Method> unbindMethodsOneArg;
        private List<Method> unbindMethodsTwoArgs;

        public void init(Class proxyClass) {
            Class listenerClass = listener.getClass();
            Class[] oneArgParams = new Class[] { ServiceReference.class };
            Class[] twoArgsParams = new Class[] { proxyClass, Map.class };
            String bindName = metadata.getBindMethodName();
            if (bindName != null) {
                bindMethodsOneArg = ReflectionUtils.findCompatibleMethods(listenerClass, bindName, oneArgParams);
                bindMethodsTwoArgs = ReflectionUtils.findCompatibleMethods(listenerClass, bindName, twoArgsParams);
            } else {
                bindMethodsOneArg = Collections.emptyList();
                bindMethodsTwoArgs = Collections.emptyList();
            }
            String unbindName = metadata.getUnbindMethodName();
            if (unbindName != null) {
                unbindMethodsOneArg = ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, oneArgParams);
                unbindMethodsTwoArgs = ReflectionUtils.findCompatibleMethods(listenerClass, unbindName, twoArgsParams);
            } else {
                unbindMethodsOneArg = Collections.emptyList();
                unbindMethodsTwoArgs = Collections.emptyList();
            }
        }

        public void bind(ServiceReference reference, Object service) {
            invokeMethods(bindMethodsOneArg, bindMethodsTwoArgs, reference, service);
        }

        public void unbind(ServiceReference reference, Object service) {
            invokeMethods(unbindMethodsOneArg, unbindMethodsTwoArgs, reference, service);
        }

        private void invokeMethods(List<Method> oneArgMethods, List<Method> twoArgsMethods, ServiceReference reference, Object service) {
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
