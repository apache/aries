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
package org.apache.aries.transaction;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.osgi.framework.Bundle;

public class DummyNamespaceHandlerRegistry implements NamespaceHandlerRegistry {
    Map<URI, NamespaceHandler> handlers = new HashMap<URI, NamespaceHandler>();

    @Override
    public NamespaceHandlerSet getNamespaceHandlers(Set<URI> uriSet, Bundle bundle) {
        Map<URI, NamespaceHandler> matching = new HashMap<URI, NamespaceHandler>();
        for (URI uri : uriSet) {
            if (handlers.containsKey(uri)) {
                matching.put(uri, handlers.get(uri));
            }
        }
        return  new DummyNamespaceHandlerSet(matching);
    }

    @Override
    public void destroy() {
    }

    public void addNamespaceHandlers(String[] namespaces, NamespaceHandler namespaceHandler) {
        for (String namespace : namespaces) {
            try {
                handlers.put(new URI(namespace), namespaceHandler);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

}
