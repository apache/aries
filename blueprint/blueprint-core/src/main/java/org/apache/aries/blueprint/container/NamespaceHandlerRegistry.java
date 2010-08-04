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

import java.net.URI;
import java.util.Set;
import java.io.IOException;

import javax.xml.validation.Schema;

import org.apache.aries.blueprint.NamespaceHandler;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

/**
 * Registry of NamespaceHandler.
 *
 * @version $Rev$, $Date$
 */
public interface NamespaceHandlerRegistry {

    /**
     * Retrieve the <code>NamespaceHandler</code> for the specified URI. Make sure
     *
     * @param uri the namespace identifying the namespace handler
     * @param bundle the blueprint bundle to be checked for class space consistency
     *
     * @return a set of registered <code>NamespaceHandler</code>s compatible with the class space of the given bundle
     */
    NamespaceHandlerSet getNamespaceHandlers(Set<URI> uri, Bundle bundle);

    /**
     * Destroy this registry
     */
    void destroy();

    /**
     * Interface used to managed a set of namespace handlers
     */
    public interface NamespaceHandlerSet {

        Set<URI> getNamespaces();

        boolean isComplete();

        /**
         * Retrieve the NamespaceHandler to use for the given namespace
         *
         * @return the NamespaceHandler to use or <code>null</code> if none is available at this time
         */
        NamespaceHandler getNamespaceHandler(URI namespace);

        /**
         * Obtain a schema to validate the XML for the given list of namespaces
         *
         * @return the schema to use to validate the XML
         */
        Schema getSchema() throws SAXException, IOException;

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
         * Destroy this handler set
         */
        void destroy();
    }

    /**
     * Interface used to listen to registered or unregistered namespace handlers.
     *
     * @see NamespaceHandlerSet#addListener(org.apache.aries.blueprint.container.NamespaceHandlerRegistry.Listener)
     * @see NamespaceHandlerSet#removeListener(org.apache.aries.blueprint.container.NamespaceHandlerRegistry.Listener) 
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
