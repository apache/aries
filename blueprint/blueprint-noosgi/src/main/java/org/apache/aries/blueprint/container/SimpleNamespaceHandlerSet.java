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

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ext.impl.ExtNamespaceHandler;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class SimpleNamespaceHandlerSet implements NamespaceHandlerSet {

    public static final URI EXT_1_2_NAMESPACE = URI.create("http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.2.0");

    private Set<URI> namespaces;
    private Schema schema;

    public SimpleNamespaceHandlerSet() {
        this.namespaces = new LinkedHashSet<URI>();
        this.namespaces.add(EXT_1_2_NAMESPACE);
    }

    public Set<URI> getNamespaces() {
        return Collections.unmodifiableSet(namespaces);
    }

    public boolean isComplete() {
        return true;
    }

    public NamespaceHandler getNamespaceHandler(URI uri) {
        if (EXT_1_2_NAMESPACE.equals(uri)) {
            return new ExtNamespaceHandler();
        }
        return null;
    }

    public Schema getSchema() throws SAXException, IOException {
        if (schema == null) {
            final List<StreamSource> schemaSources = new ArrayList<StreamSource>();
            schemaSources.add(new StreamSource(getClass().getResourceAsStream("/org/apache/aries/blueprint/blueprint.xsd")));
            schemaSources.add(new StreamSource(getClass().getResourceAsStream("/org/apache/aries/blueprint/ext/impl/blueprint-ext-1.2.xsd")));
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = schemaFactory.newSchema(schemaSources.toArray(new Source[schemaSources.size()]));
        }
        return schema;
    }

    public void addListener(Listener listener) {
        throw new IllegalStateException();
    }

    public void removeListener(Listener listener) {
        throw new IllegalStateException();
    }

    public void destroy() {
        schema = null;
    }

}
