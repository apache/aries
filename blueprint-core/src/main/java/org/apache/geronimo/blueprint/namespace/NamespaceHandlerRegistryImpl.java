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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.geronimo.blueprint.NamespaceHandlerRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.namespace.NamespaceHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class NamespaceHandlerRegistryImpl implements NamespaceHandlerRegistry, ServiceTrackerCustomizer {

    public static final String NAMESPACE = "org.osgi.blueprint.namespace";

    private final BundleContext bundleContext;
    private final Map<URI, NamespaceHandler> handlers;
    private final ServiceTracker tracker;

    public NamespaceHandlerRegistryImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        handlers = new ConcurrentHashMap<URI, NamespaceHandler>();
        tracker = new ServiceTracker(bundleContext, NamespaceHandler.class.getName(), this);
    }

    public Object addingService(ServiceReference reference) {
        NamespaceHandler handler = (NamespaceHandler) bundleContext.getService(reference);
        Map props = new HashMap();
        for (String name : reference.getPropertyKeys()) {
            props.put(name, reference.getProperty(name));
        }
        try {
            registerHandler(handler, props);
            return handler;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void modifiedService(ServiceReference reference, Object service) {
        removedService(reference, service);
        addingService(reference);
    }

    public void removedService(ServiceReference reference, Object service) {
        NamespaceHandler handler = (NamespaceHandler) service;
        Map props = new HashMap();
        for (String name : reference.getPropertyKeys()) {
            props.put(name, reference.getProperty(name));
        }
        try {
            unregisterHandler(handler, props);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerHandler(NamespaceHandler handler, Map properties) throws Exception {
        Object ns = properties != null ? properties.get(NAMESPACE) : null;
        if (ns instanceof URI[]) {
            for (URI uri : (URI[]) ns) {
                if (handlers.containsKey(uri)) {
                    throw new IllegalArgumentException("A NamespaceHandler service is already registered for namespace " + uri);
                }
            }
            for (URI uri : (URI[]) ns) {
                handlers.put(uri, handler);
            }
        } else {
            throw new IllegalArgumentException("NamespaceHandler service does not have an associated " + NAMESPACE + " property defined");
        }
    }

    public void unregisterHandler(NamespaceHandler handler, Map properties) throws Exception {
        Object ns = properties != null ? properties.get(NAMESPACE) : null;
        if (ns instanceof URI[]) {
            for (URI uri : (URI[]) ns) {
                if (handlers.get(uri) != handler) {
                    throw new IllegalArgumentException("A NamespaceHandler service is already registered for namespace " + uri);
                }
            }
            for (URI uri : (URI[]) ns) {
                handlers.remove(uri);
            }
        } else {
            throw new IllegalArgumentException("NamespaceHandler service does not have an associated " + NAMESPACE + " property defined");
        }
    }

    public NamespaceHandler getNamespaceHandler(URI uri) {
        return handlers.get(uri);
    }

    public void destroy() {
        tracker.close();
    }
}
