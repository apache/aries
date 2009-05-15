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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.geronimo.blueprint.BlueprintContextEventSender;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.RecipeHelper;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.ServiceUnavailableException;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;

/**
 * A recipe to create an unary OSGi service reference.
 *
 * TODO: check synchronization / thread safety
 *
 * TODO: looks there is a potential problem if the service is unregistered between a call
 *        to ServiceDispatcher#loadObject() and when the actual invocation finish
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class UnaryServiceReferenceRecipe extends AbstractServiceReferenceRecipe {

    private final ReferenceMetadata metadata;
    private Class proxyClass;
    private Object proxy;

    private volatile ServiceReference trackedServiceReference;
    private volatile Object trackedService;
    private final Object monitor = new Object();

    public UnaryServiceReferenceRecipe(BlueprintContext blueprintContext,
                                       BlueprintContextEventSender sender,
                                       ReferenceMetadata metadata,
                                       Recipe listenersRecipe) {
        super(blueprintContext,  sender, metadata, listenersRecipe);
        this.metadata = metadata;
    }

    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        try {
            // Create the proxy
            proxy = createProxy();
            proxyClass = proxy.getClass();
            
            // Add partially created proxy to the context
            addObject(proxy, true);
            
            // Create the listeners and initialize them
            createListeners();
            
            // Add fully created proxy to the context
            addObject(proxy, false);

            // Start tracking the service
            tracker.registerServiceListener(this);
            retrack();
            
            // Return the object
            Class expectedClass = RecipeHelper.toClass(expectedType);
            if (ServiceReference.class.equals(expectedClass)) {
                return getServiceReference();
            } else {
                return proxy;
            }
        } catch (Throwable t) {
            throw new ConstructionException(t);
        }
    }

    private List<Class> getSupportedTypes() throws ClassNotFoundException {
        List<Class> list = getAllClasses(metadata.getInterfaceNames());
        list.add(ServiceReference.class);
        return list;
    }
    
    public Type[] getTypes() {
        try {
            List<Class> interfaceList = getSupportedTypes();
            return (Type[]) interfaceList.toArray(new Type [interfaceList.size()]);
        } catch (ClassNotFoundException e) {
            throw new ConstructionException(e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        unbind();
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    private Object createProxy() throws Exception {
        Enhancer e = new Enhancer();
        e.setClassLoader(proxyClassLoader);
        e.setSuperclass(getTargetClass(metadata.getInterfaceNames()));
        List<Class> interfaceList = getInterfaces(metadata.getInterfaceNames());
        interfaceList.add(ServiceReferenceAccessor.class);
        e.setInterfaces(toClassArray(interfaceList));
        e.setInterceptDuringConstruction(false);
        e.setCallbacks(new Callback [] {new ServiceDispatcher(), new ServiceReferenceMethodInterceptor() });
        e.setCallbackFilter(new ServiceCallbackFilter());
        e.setUseFactory(false);
        return e.create();
    }
    
    private void retrack() {
        synchronized (monitor) {
            ServiceReference ref = tracker.getBestServiceReference();
            if (ref != null) {
                bind(ref);
            } else {
                unbind();
            }
        }
    }

    protected void track(ServiceReference ref) {
        retrack();
    }

    protected void untrack(ServiceReference ref) {
        retrack();
    }

    private void bind(ServiceReference ref) {
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                blueprintContext.getBundleContext().ungetService(trackedServiceReference);
            }
            trackedServiceReference = ref;
            trackedService = null;
            monitor.notifyAll();
            for (Listener listener : listeners) {
                listener.bind(trackedServiceReference, proxy);
            }
        }
    }

    private void unbind() {
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                for (Listener listener : listeners) {
                    listener.unbind(trackedServiceReference, proxy);
                }
                blueprintContext.getBundleContext().ungetService(trackedServiceReference);
                trackedServiceReference = null;
                trackedService = null;
            }
        }
    }

    private Object getService() throws InterruptedException {
        synchronized (monitor) {
            if (tracker.isStarted() && trackedServiceReference == null && metadata.getTimeout() > 0) {
                Set<String> interfaces = new HashSet<String>(metadata.getInterfaceNames());
                sender.sendWaiting(blueprintContext, interfaces.toArray(new String[interfaces.size()]), getOsgiFilter());
                monitor.wait(metadata.getTimeout());
            }
            if (trackedServiceReference == null) {
                if (tracker.isStarted()) {
                    throw new ServiceUnavailableException("Timeout expired when waiting for OSGi service", proxyClass.getSuperclass(), getOsgiFilter());
                } else {
                    throw new ServiceUnavailableException("Service tracker is stopped", proxyClass.getSuperclass(), getOsgiFilter());
                }
            }
            if (trackedService == null) {
                trackedService = blueprintContext.getBundleContext().getService(trackedServiceReference);
            }
            return trackedService;
        }
    }
    
    private ServiceReference getServiceReference() throws InterruptedException {
        synchronized (monitor) {
            if (!optional) {
                getService();
            }           
            return trackedServiceReference;
        }           
    }
    
    public class ServiceDispatcher implements Dispatcher {

        public Object loadObject() throws Exception {
            return getService();
        }

    }
    
    public class ServiceReferenceMethodInterceptor implements MethodInterceptor {
        
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            return getServiceReference();
        }
        
    }
    
    private static class ServiceCallbackFilter implements CallbackFilter {

        private Method getReferenceMethod;
        
        public ServiceCallbackFilter() throws NoSuchMethodException {
            getReferenceMethod = ServiceReferenceAccessor.class.getMethod("getServiceReference", null);
        }
        
        public int accept(Method method) {
            if (isGetReferenceMethod(method)) {
                // use getServiceReference callback
                return 1;
            } 
            // use Dispatcher callback
            return 0;
        }
        
        private boolean isGetReferenceMethod(Method method) {
            return getReferenceMethod.equals(method);
        }
    }
        
}
