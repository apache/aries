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

import javax.xml.validation.Schema;

import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.osgi.framework.Bundle;

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
}
