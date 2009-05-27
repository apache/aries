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
package org.apache.geronimo.blueprint.namespace;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.geronimo.blueprint.NamespaceHandler;
import org.apache.geronimo.blueprint.container.NamespaceHandlerRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the NamespaceHandlerRegistry.
 * 
 * This registry will track NamespaceHandler objects in the OSGi registry and make
 * them available, calling listeners when handlers are registered or unregistered.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class NamespaceHandlerRegistryImpl implements NamespaceHandlerRegistry, ServiceTrackerCustomizer {
    
    public static final String NAMESPACE = "osgi.service.blueprint.namespace";

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceHandlerRegistryImpl.class);

    private final BundleContext bundleContext;
    private final Map<URI, NamespaceHandler> handlers;
    private final ServiceTracker tracker;
    private final Map<Listener, Boolean> listeners;

    public NamespaceHandlerRegistryImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        handlers = new HashMap<URI, NamespaceHandler>();
        tracker = new ServiceTracker(bundleContext, NamespaceHandler.class.getName(), this);
        tracker.open();
        listeners = new HashMap<Listener, Boolean>();
    }

    public Object addingService(ServiceReference reference) {
        NamespaceHandler handler = (NamespaceHandler) bundleContext.getService(reference);
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            for (String name : reference.getPropertyKeys()) {
                props.put(name, reference.getProperty(name));
            }
            registerHandler(handler, props);
        } catch (Exception e) {
            LOGGER.warn("Error registering NamespaceHandler", e);
        }
        return handler;
    }

    public void modifiedService(ServiceReference reference, Object service) {
        removedService(reference, service);
        addingService(reference);
    }

    public void removedService(ServiceReference reference, Object service) {
        try {
            NamespaceHandler handler = (NamespaceHandler) service;
            Map<String, Object> props = new HashMap<String, Object>();
            for (String name : reference.getPropertyKeys()) {
                props.put(name, reference.getProperty(name));
            }
            unregisterHandler(handler, props);
        } catch (Exception e) {
            LOGGER.warn("Error unregistering NamespaceHandler", e);
        }
    }

    public synchronized void registerHandler(NamespaceHandler handler, Map properties) {
        List<URI> namespaces = getNamespaces(properties);
        for (URI uri : namespaces) {
            if (handlers.containsKey(uri)) {
                LOGGER.warn("Ignoring NamespaceHandler for namespace {}, as another handler has already been registered for the same namespace", uri);
            } else {
                handlers.put(uri, handler);
                callListeners(uri, true);
            }
        }
    }

    public synchronized void unregisterHandler(NamespaceHandler handler, Map properties) {
        List<URI> namespaces = getNamespaces(properties);
        for (URI uri : namespaces) {
            if (handlers.get(uri) != handler) {
                continue;
            }
            handlers.remove(uri);
            callListeners(uri, false);
        }
    }

    private void callListeners(URI uri, boolean registered) {
        for (Listener listener : listeners.keySet()) {
            try {
                if (registered) {
                    listener.namespaceHandlerRegistered(uri);
                } else {
                    listener.namespaceHandlerUnregistered(uri);
                }
            } catch (Throwable t) {
                LOGGER.debug("Unexpected exception when notifying a NamespaceHandler listener", t);
            }
        }
    }

    private static List<URI> getNamespaces(Map properties) {
        Object ns = properties != null ? properties.get(NAMESPACE) : null;
        if (ns == null) {
            throw new IllegalArgumentException("NamespaceHandler service does not have an associated " + NAMESPACE + " property defined");
        } else if (ns instanceof URI[]) {
            return Arrays.asList((URI[]) ns);
        } else if (ns instanceof URI) {
            return Collections.singletonList((URI) ns);
        } else if (ns instanceof String) {
            return Collections.singletonList(URI.create((String) ns));
        } else if (ns instanceof String[]) {
            String[] strings = (String[]) ns;
            List<URI> namespaces = new ArrayList<URI>(strings.length);
            for (String string : strings) {
                namespaces.add(URI.create(string));
            }
            return namespaces;
        } else if (ns instanceof Collection) {
            Collection col = (Collection) ns;
            List<URI> namespaces = new ArrayList<URI>(col.size());
            for (Object o : col) {
                namespaces.add(toURI(o));
            }
            return namespaces;
        } else if (ns instanceof Object[]) {
            Object[] array = (Object[]) ns;
            List<URI> namespaces = new ArrayList<URI>(array.length);
            for (Object o : array) {
                namespaces.add(toURI(o));
            }
            return namespaces;
        } else {
            throw new IllegalArgumentException("NamespaceHandler service has an associated " + NAMESPACE + " property defined which can not be converted to an array of URI");
        }
    }

    private static URI toURI(Object o) {
        if (o instanceof URI) {
            return (URI) o;
        } else if (o instanceof String) {
            return URI.create((String) o);
        } else {
            throw new IllegalArgumentException("NamespaceHandler service has an associated " + NAMESPACE + " property defined which can not be converted to an array of URI");
        }
    }
    
    public synchronized NamespaceHandler getNamespaceHandler(URI uri) {
        return handlers.get(uri);
    }

    public void destroy() {
        tracker.close();
    }

    public synchronized void addListener(Listener listener) {
        listeners.put(listener, Boolean.TRUE);
    }

    public synchronized void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
