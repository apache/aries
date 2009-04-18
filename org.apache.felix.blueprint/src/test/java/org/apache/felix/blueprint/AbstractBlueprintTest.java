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
package org.apache.felix.blueprint;

import java.net.URI;
import java.util.Collections;

import junit.framework.TestCase;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.NamespaceHandler;
import org.apache.felix.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.felix.blueprint.context.Parser;

public abstract class AbstractBlueprintTest extends TestCase {

    protected ComponentDefinitionRegistryImpl parse(String name) throws Exception {
        NamespaceHandlerRegistry handlers = new NamespaceHandlerRegistry() {
            public NamespaceHandler getNamespaceHandler(URI uri) {
                return null;
            }
            public void addCallback(Runnable runnable) {
            }
            public void destroy() {
            }
        };
        return parse(name, handlers);
    }

    protected ComponentDefinitionRegistryImpl parse(String name, NamespaceHandlerRegistry handlers) throws Exception {
        ComponentDefinitionRegistryImpl registry = new ComponentDefinitionRegistryImpl();
        Parser parser = new Parser(handlers, registry, Collections.singletonList(getClass().getResource(name)));
        parser.parse();
        return registry;
    }

}
