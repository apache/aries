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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.ExtendedServiceReferenceMetadata;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A recipe to create an unary OSGi service reference.
 *
 * TODO: check synchronization / thread safety
 *
 * TODO: looks there is a potential problem if the service is unregistered between a call
 *        to ServiceDispatcher#loadObject() and when the actual invocation finish
 *
 * @version $Rev$, $Date$
 */
public class ReferenceRecipe extends AbstractServiceReferenceRecipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceRecipe.class);

    private final ReferenceMetadata metadata;
    private Object proxy;

    private volatile ServiceReference trackedServiceReference;
    private volatile Object trackedService;
    private final Object monitor = new Object();

    public ReferenceRecipe(String name,
                           ExtendedBlueprintContainer blueprintContainer,
                           ReferenceMetadata metadata,
                           CollectionRecipe listenersRecipe,
                           List<Recipe> explicitDependencies) {
        super(name, blueprintContainer, metadata, listenersRecipe, explicitDependencies);
        this.metadata = metadata;
    }

    @Override
    protected Object internalCreate() throws ComponentDefinitionException {
        try {
            if (explicitDependencies != null) {
                for (Recipe recipe : explicitDependencies) {
                    recipe.create();
                }
            }
            // Create the proxy
            Set<Class> interfaces = new HashSet<Class>();
            if (this.metadata.getInterface() != null) {
                interfaces.add(loadClass(this.metadata.getInterface()));
            }
            if (this.metadata instanceof ExtendedServiceReferenceMetadata && ((ExtendedServiceReferenceMetadata) this.metadata).getRuntimeInterface() != null) {
                interfaces.add(((ExtendedServiceReferenceMetadata) this.metadata).getRuntimeInterface());
            }

            proxy = createProxy(new ServiceDispatcher(), interfaces);

            // Add partially created proxy to the context
            ServiceProxyWrapper wrapper = new ServiceProxyWrapper();

            addPartialObject(wrapper);

            // Handle initial references
            createListeners();
            updateListeners();            

            // Return a ServiceProxy that can injection of references or proxies can be done correctly
            return wrapper;
        } catch (ComponentDefinitionException e) {
            throw e;
        } catch (Throwable t) {
            throw new ComponentDefinitionException(t);
        }
    }

    protected void doStop() {
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
        // TODO: make this behavior configurable through a custom attribute
        // TODO:      policy = sticky | replace
        synchronized (monitor) {
            if (trackedServiceReference == null) {
                retrack();
            }
        }
    }

    protected void untrack(ServiceReference ref) {
        // TODO: make this behavior configurable through a custom attribute
        // TODO:      policy = sticky | replace
        synchronized (monitor) {
            if (trackedServiceReference == ref) {
                retrack();
            }
        }
    }

    private void bind(ServiceReference ref) {
        LOGGER.debug("Binding reference {} to {}", getName(), ref);
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                blueprintContainer.getBundleContext().ungetService(trackedServiceReference);
            }
            trackedServiceReference = ref;
            trackedService = null;
            monitor.notifyAll();
            bind(trackedServiceReference, proxy);
        }
    }

    private void unbind() {
        LOGGER.debug("Unbinding reference {}", getName());
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                unbind(trackedServiceReference, proxy);
                blueprintContainer.getBundleContext().ungetService(trackedServiceReference);
                trackedServiceReference = null;
                trackedService = null;
                monitor.notifyAll();
            }
        }
    }

    private Object getService() throws InterruptedException {
        synchronized (monitor) {
            if (isStarted() && trackedServiceReference == null && metadata.getTimeout() > 0
                    && metadata.getAvailability() == ServiceReferenceMetadata.AVAILABILITY_MANDATORY) {
                blueprintContainer.getEventDispatcher().blueprintEvent(new BlueprintEvent(BlueprintEvent.WAITING, blueprintContainer.getBundleContext().getBundle(), blueprintContainer.getExtenderBundle(), new String[] { getOsgiFilter() }));
                monitor.wait(metadata.getTimeout());
            }
            if (trackedServiceReference == null) {
                if (isStarted()) {
                    LOGGER.info("Timeout expired when waiting for OSGi service {}", getOsgiFilter());
                    throw new ServiceUnavailableException("Timeout expired when waiting for OSGi service", getOsgiFilter());
                } else {
                    throw new ServiceUnavailableException("The Blueprint container is being or has been destroyed", getOsgiFilter());
                }
            }
            if (trackedService == null) {
                trackedService = blueprintContainer.getService(trackedServiceReference);
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

    public class ServiceProxyWrapper implements AggregateConverter.Convertible {

        public Object convert(ReifiedType type) throws Exception {
            if (type.getRawClass() == ServiceReference.class) {
                return getServiceReference();
            } else if (type.getRawClass().isInstance(proxy)) {
                return proxy;
            } else {
                throw new ComponentDefinitionException("Unable to convert to " + type);
            }
        }

    }

}
