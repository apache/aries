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
package org.apache.felix.blueprint.namespace;

import java.util.Map;
import java.util.HashMap;
import java.net.URI;

import org.osgi.service.blueprint.namespace.NamespaceHandler;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class NamespaceHandlerRegistryImpl implements NamespaceHandlerRegistry {

    public static final String NAMESPACE = "org.osgi.blueprint.namespace";

    private final Map<URI, NamespaceHandler> handlers;

    public NamespaceHandlerRegistryImpl() {
        handlers = new HashMap<URI, NamespaceHandler>();
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

}
