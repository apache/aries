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
import org.apache.geronimo.blueprint.BlueprintContextEventSender;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.Recipe;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;
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

    private volatile ServiceReference trackedServiceReference;
    private volatile Object trackedService;
    private final Object monitor = new Object();
    private final boolean optional;

    public UnaryServiceReferenceRecipe(BlueprintContext blueprintContext,
                                       BlueprintContextEventSender sender,
                                       ReferenceMetadata metadata,
                                       Recipe listenersRecipe) {
        super(blueprintContext,  sender, metadata, listenersRecipe);
        this.metadata = metadata;
        this.optional = metadata.getAvailability() == ReferenceMetadata.OPTIONAL_AVAILABILITY;
        if (this.optional) {
            setSatisfied(true);
        }
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
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
            blueprintContext.getBundleContext().addServiceListener(this, getOsgiFilter());
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
        blueprintContext.getBundleContext().removeServiceListener(this);
        unbind();
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
        synchronized (monitor) {
            try {
                ServiceReference[] refs = blueprintContext.getBundleContext().getServiceReferences(null, getOsgiFilter());
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
            trackedService = blueprintContext.getBundleContext().getService(trackedServiceReference);
            for (Listener listener : listeners) {
                listener.bind(trackedServiceReference, trackedService);
            }
            if (!optional) {
                setSatisfied(true);
            }
        }
    }

    private void unbind() {
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                for (Listener listener : listeners) {
                    listener.unbind(trackedServiceReference, trackedService);
                }
                blueprintContext.getBundleContext().ungetService(trackedServiceReference);
                trackedServiceReference = null;
                trackedService = null;
                if (!optional) {
                    setSatisfied(false);
                }
            }
        }
    }

    public class ServiceDispatcher implements Dispatcher {

        public Object loadObject() throws Exception {
            synchronized (monitor) {
                if (trackedService == null && metadata.getTimeout() > 0) {
                    Set<String> interfaces = new HashSet<String>(metadata.getInterfaceNames());
                    sender.sendWaiting(blueprintContext, interfaces.toArray(new String[interfaces.size()]), getOsgiFilter());
                    monitor.wait(metadata.getTimeout());
                }
                if (trackedService == null) {
                    throw new ServiceUnavailableException("Timeout expired when waiting for OSGi service", proxyClass.getSuperclass(), getOsgiFilter());
                }
                return trackedService;
            }
        }

    }

}
