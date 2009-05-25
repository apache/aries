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
package org.apache.geronimo.blueprint;

import java.net.URI;

import org.osgi.service.blueprint.namespace.NamespaceHandler;

/**
 * Registry of NamespaceHandler.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public interface NamespaceHandlerRegistry {

    /**
     * Retrieve the <code>NamespaceHandler</code> for the specified URI
     *
     * @param uri the namespace identifying the namespace handler
     * @return the registered <code>NamespaceHandler</code> or <code>null</code> if none has been registered for the given namespace
     */
    NamespaceHandler getNamespaceHandler(URI uri);

    /**
     * Add a new Listener to be called when namespace handlers are registerd or unregistered
     *
     * @param listener the listener to register
     */
    void addListener(Listener listener);

    /**
     * Remove a previously registered Listener
     *
     * @param listener the listener to unregister
     */
    void removeListener(Listener listener);

    /**
     * Destroy this registry
     */
    void destroy();

    /**
     * Interface used to listen to registered or unregistered namespace handlers.
     *
     * @see NamespaceHandlerRegistry#addListener(org.apache.geronimo.blueprint.NamespaceHandlerRegistry.Listener)
     * @see NamespaceHandlerRegistry#removeListener(org.apache.geronimo.blueprint.NamespaceHandlerRegistry.Listener)
     */
    public interface Listener {

        /**
         * Called when a NamespaceHandler has been registered for the specified URI.
         *
         * @param uri the URI of the newly registered namespace handler
         */
        void namespaceHandlerRegistered(URI uri);

        /**
         * Called when a NamespaceHandler has been unregistered for the specified URI.
         *
         * @param uri the URI of the newly unregistered namespace handler
         */
        void namespaceHandlerUnregistered(URI uri);

    }
}
