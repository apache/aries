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
import java.util.concurrent.Callable;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.utils.ConversionUtils;
import org.apache.geronimo.blueprint.utils.TypeUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintEvent;
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
public class ReferenceRecipe extends AbstractServiceReferenceRecipe {

    private final ReferenceMetadata metadata;
    private Object proxy;

    private volatile ServiceReference trackedServiceReference;
    private volatile Object trackedService;
    private final Object monitor = new Object();

    public ReferenceRecipe(String name,
                           ExtendedBlueprintContainer blueprintContainer,
                           ReferenceMetadata metadata,
                           Recipe listenersRecipe) {
        super(name, blueprintContainer, metadata, listenersRecipe);
        this.metadata = metadata;
    }

    @Override
    protected Object internalCreate() throws ComponentDefinitionException {
        try {
            // Create the proxy
            proxy = createProxy(new ServiceDispatcher(), this.metadata.getInterfaceNames());

            // Add partially created proxy to the context
            addObject(proxy, true);

            // Handle initial references
            createListeners();
            retrack();

            // Return a ServiceProxy that can injection of references or proxies can be done correctly
            return new ServiceProxyWrapper();
        } catch (Throwable t) {
            throw new ComponentDefinitionException(t);
        }
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (monitor) {
            unbind();
            monitor.notifyAll();
        }
    }

    protected void retrack() {
        ServiceReference ref = getBestServiceReference();
        if (ref != null) {
            bind(ref);
        } else {
            unbind();
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
            if (isStarted() && trackedServiceReference == null && metadata.getTimeout() > 0) {
                blueprintContainer.getEventDispatcher().blueprintEvent(new BlueprintEvent(BlueprintEvent.WAITING, blueprintContainer.getBundleContext().getBundle(), blueprintContainer.getExtenderBundle(), new String[] { getOsgiFilter() }));
                monitor.wait(metadata.getTimeout());
            }
            if (trackedServiceReference == null) {
                if (isStarted()) {
                    throw new ServiceUnavailableException("Timeout expired when waiting for OSGi service", getOsgiFilter());
                } else {
                    throw new ServiceUnavailableException("Service tracker is stopped", getOsgiFilter());
                }
            }
            if (trackedService == null) {
                trackedService = blueprintContainer.getBundleContext().getService(trackedServiceReference);
            }
            if (trackedService == null) {
                throw new IllegalStateException("getService() returned null for " + trackedServiceReference);
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

    public class ServiceDispatcher implements Callable<Object> {

        public Object call() throws Exception {
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
