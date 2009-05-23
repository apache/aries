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

import java.lang.reflect.Type;

import net.sf.cglib.proxy.Dispatcher;
import org.apache.geronimo.blueprint.BlueprintEventSender;
import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.utils.ConversionUtils;
import org.apache.geronimo.blueprint.utils.TypeUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
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

    public UnaryServiceReferenceRecipe(ExtendedBlueprintContainer blueprintContainer,
                                       BlueprintEventSender sender,
                                       ReferenceMetadata metadata,
                                       Recipe listenersRecipe) {
        super(blueprintContainer,  sender, metadata, listenersRecipe);
        this.metadata = metadata;
    }

    @Override
    protected Object internalCreate() throws ComponentDefinitionException {
        try {
            // Create the proxy
            proxy = createProxy(new ServiceDispatcher(), this.metadata.getInterfaceNames());
            proxyClass = proxy.getClass();

            // Add partially created proxy to the context
            addObject(proxy, false);

            // Start track the service
            tracker.registerServiceListener(this);
            // Handle initial references
            retrack();

            // Return a ServiceProxy that can injection of references or proxies can be done correctly
            return new ServiceProxyWrapper();
        } catch (Throwable t) {
            throw new ComponentDefinitionException(t);
        }
    }

    @Override
    public void postCreate() {
        // Create the listeners and initialize them
        createListeners();
        // Retrack to inform listeners
        retrack();
    }

    @Override
    public void stop() {
        super.stop();
        unbind();
        synchronized (monitor) {
            monitor.notifyAll();
        }
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
                blueprintContainer.getBundleContext().ungetService(trackedServiceReference);
            }
            trackedServiceReference = ref;
            trackedService = null;
            monitor.notifyAll();
            if (listeners != null) {
                for (Listener listener : listeners) {
                    listener.bind(trackedServiceReference, proxy);
                }
            }
        }
    }

    private void unbind() {
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                if (listeners != null) {
                    for (Listener listener : listeners) {
                        listener.unbind(trackedServiceReference, proxy);
                    }
                }
                blueprintContainer.getBundleContext().ungetService(trackedServiceReference);
                trackedServiceReference = null;
                trackedService = null;
            }
        }
    }

    private Object getService() throws InterruptedException {
        synchronized (monitor) {
            if (tracker.isStarted() && trackedServiceReference == null && metadata.getTimeout() > 0) {
                sender.sendWaiting(blueprintContainer.getBundleContext().getBundle(), getOsgiFilter());
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
                trackedService = blueprintContainer.getBundleContext().getService(trackedServiceReference);
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

    public class ServiceProxyWrapper implements ConversionUtils.Convertible {

        public Object convert(Type type) throws Exception {
            if (type == ServiceReference.class) {
                return getServiceReference();
            } else if (TypeUtils.toClass(type).isInstance(proxy)) {
                return proxy;
            } else {
                throw new ComponentDefinitionException("Unable to convert to " + type);
            }
        }

    }

}
