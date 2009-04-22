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
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import net.sf.cglib.proxy.Dispatcher;
import org.apache.geronimo.blueprint.ModuleContextEventSender;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.Recipe;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.ServiceUnavailableException;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;

/**
 * A recipe to create an unary OSGi service reference.
 *
 * TODO: check synchronization / thread safety
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class UnaryServiceReferenceRecipe extends AbstractServiceReferenceRecipe {

    private final ReferenceMetadata metadata;
    private Class proxyClass;

    private volatile ServiceReference trackedServiceReference;
    private volatile Object trackedService;
    private final Object monitor = new Object();

    public UnaryServiceReferenceRecipe(BlueprintContext moduleContext,
                                       ModuleContextEventSender sender,
                                       ReferenceMetadata metadata,
                                       Recipe listenersRecipe) {
        super(moduleContext,  sender, metadata, listenersRecipe);
        this.metadata = metadata;
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        // TODO: availability
        try {
            // Create the proxy
            Object obj = createProxy(new ServiceDispatcher(), metadata.getInterfaceNames());
            proxyClass = obj.getClass();
            // Create the listeners and initialize them
            createListeners();
            // Add the created proxy to the context
            if (getName() != null) {
                ExecutionContext.getContext().addObject(getName(), obj);
            }

            // Start tracking the service
            moduleContext.getBundleContext().addServiceListener(this, getOsgiFilter());
            retrack();

            // Return the object
            return obj;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new ConstructionException(t);
        }
    }

    public boolean canCreate(Type type) {
        return true;
    }

    public void destroy() {
        moduleContext.getBundleContext().removeServiceListener(this);
        unbind();
    }

    private ServiceReference getBestServiceReference(ServiceReference[] references) {
        if (references == null || references.length == 0) {
            return null;
        }
        return Collections.max(Arrays.asList(references));
    }

    private void retrack() {
        try {
            ServiceReference[] refs = moduleContext.getBundleContext().getServiceReferences(null, getOsgiFilter());
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

    protected void track(ServiceReference ref) {
        if (trackedServiceReference == null) {
            bind(ref);
        } else {
            if (trackedServiceReference.compareTo(ref) > 0) {
                return;
            }
            bind(ref);
        }
    }

    protected void untrack(ServiceReference ref) {
        retrack();
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

    public class ServiceDispatcher implements Dispatcher {

        public Object loadObject() throws Exception {
            Object svc = trackedService;
            if (svc == null && metadata.getTimeout() > 0) {
                Set<String> interfaces = new HashSet<String>(metadata.getInterfaceNames());
                sender.sendWaiting(moduleContext, interfaces.toArray(new String[interfaces.size()]), getOsgiFilter());
                synchronized (monitor) {
                    monitor.wait(metadata.getTimeout());
                }
                svc = trackedService;
            }
            if (svc == null) {
                throw new ServiceUnavailableException("Timeout expired when waiting for OSGi service", proxyClass.getSuperclass(), getOsgiFilter());
            }
            return svc;
        }

    }

}
