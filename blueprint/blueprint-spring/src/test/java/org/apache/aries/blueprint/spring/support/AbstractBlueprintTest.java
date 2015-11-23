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
package org.apache.aries.blueprint.spring.support;

import javax.xml.validation.Schema;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ext.impl.ExtNamespaceHandler;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.parser.Parser;
import org.xml.sax.SAXException;

public abstract class AbstractBlueprintTest extends TestCase {

    protected ComponentDefinitionRegistryImpl parse(String name) throws Exception {
        final URI extensionHandler = new URI("http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0");
        NamespaceHandlerSet handlers = new NamespaceHandlerSet() {
            public Set<URI> getNamespaces() {
                return null;
            }

            public NamespaceHandler getNamespaceHandler(URI namespace) {
                if (namespace.equals(extensionHandler)) {
                    return new ExtNamespaceHandler();
                } else {
                    return null;
                }
            }

            public void removeListener(Listener listener) {
            }

            public Schema getSchema() throws SAXException, IOException {
                return null;
            }

            public Schema getSchema(Map<String, String> locations) throws SAXException, IOException {
                return null;
            }

            public boolean isComplete() {
                return false;
            }

            public void addListener(Listener listener) {
            }

            public void destroy() {
            }
        };
        return parse(name, handlers);
    }

    protected ComponentDefinitionRegistryImpl parse(String name, NamespaceHandlerSet handlers) throws Exception {
        ComponentDefinitionRegistryImpl registry = new ComponentDefinitionRegistryImpl();
        Parser parser = new Parser();
        parser.parse(Collections.singletonList(getClass().getResource(name)));
        parser.populate(handlers, registry);
        return registry;
    }

}
