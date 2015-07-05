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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ext.impl.ExtNamespaceHandler;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

public class SimpleNamespaceHandlerSet implements NamespaceHandlerSet {

    public static final URI EXT_1_2_NAMESPACE = URI.create("http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.2.0");

    private Map<URI, URL> namespaces;
    private Map<URI, NamespaceHandler> handlers;
    private Schema schema;

    public SimpleNamespaceHandlerSet() {
        this.namespaces = new LinkedHashMap<URI, URL>();
        this.handlers = new LinkedHashMap<URI, NamespaceHandler>();
        addNamespace(EXT_1_2_NAMESPACE,
                getClass().getResource("/org/apache/aries/blueprint/ext/impl/blueprint-ext-1.2.xsd"),
                new ExtNamespaceHandler());
    }

    public Set<URI> getNamespaces() {
        return Collections.unmodifiableSet(namespaces.keySet());
    }

    public void addNamespace(URI namespace, URL schema, NamespaceHandler handler) {
        namespaces.put(namespace, schema);
        handlers.put(namespace, handler);
    }

    public boolean isComplete() {
        return true;
    }

    public NamespaceHandler getNamespaceHandler(URI uri) {
        return handlers.get(uri);
    }

    public Schema getSchema() throws SAXException, IOException {
        if (schema == null) {
            final List<StreamSource> schemaSources = new ArrayList<StreamSource>();
            final List<InputStream> streams = new ArrayList<InputStream>();
            try {
                InputStream is = getClass().getResourceAsStream("/org/apache/aries/blueprint/blueprint.xsd");
                streams.add(is);
                schemaSources.add(new StreamSource(is));
                for (URI uri : namespaces.keySet()) {
                    is = namespaces.get(uri).openStream();
                    streams.add(is);
                    schemaSources.add(new StreamSource(is));
                }
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                schemaFactory.setResourceResolver(new LSResourceResolver() {
                    
                    public LSInput resolveResource(String type, String namespace, String publicId,
                                                   String systemId, String baseURI) {
                        try {
                            if (systemId != null && !URI.create(systemId).isAbsolute()) {
                                URL namespaceURL = namespaces.get(URI.create(namespace));
                                if (namespaceURL != null) {
                                    URI systemIdUri = namespaceURL.toURI().resolve(systemId);
                                    if (!systemIdUri.isAbsolute() && "jar".equals(namespaceURL.getProtocol())) {
                                        String urlString = namespaceURL.toString();
                                        int jarFragmentIndex = urlString.lastIndexOf('!');
                                        if (jarFragmentIndex > 0 && jarFragmentIndex < urlString.length() - 1) {
                                            String jarUrlOnly = urlString.substring(0, jarFragmentIndex);
                                            String oldFragment = urlString.substring(jarFragmentIndex + 1);
                                            String newFragment = URI.create(oldFragment).resolve(systemId).toString();
                                            String newJarUri = jarUrlOnly + '!' + newFragment;
                                            systemIdUri = URI.create(newJarUri);
                                        }
                                    }
                                    InputStream resourceStream = systemIdUri.toURL().openStream();
                                    return new LSInputImpl(publicId, systemId, resourceStream);
                                }
                            }
                        } catch (Exception ex) {
                            // ignore
                        }
                        return null;
                    }
                });
                schema = schemaFactory.newSchema(schemaSources.toArray(new Source[schemaSources.size()]));
            } finally {
                for (InputStream is : streams) {
                    is.close();
                }
            }
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

    private static class LSInputImpl implements LSInput {

        protected String fPublicId;

        protected String fSystemId;

        protected String fBaseSystemId;

        protected InputStream fByteStream;

        protected Reader fCharStream;

        protected String fData;

        protected String fEncoding;

        protected boolean fCertifiedText;

        LSInputImpl(String publicId, String systemId, InputStream byteStream) {
            fPublicId = publicId;
            fSystemId = systemId;
            fByteStream = byteStream;
        }

        public InputStream getByteStream() {
            return fByteStream;
        }

        public void setByteStream(InputStream byteStream) {
            fByteStream = byteStream;
        }

        public Reader getCharacterStream() {
            return fCharStream;
        }

        public void setCharacterStream(Reader characterStream) {
            fCharStream = characterStream;
        }

        public String getStringData() {
            return fData;
        }

        public void setStringData(String stringData) {
            fData = stringData;
        }

        public String getEncoding() {
            return fEncoding;
        }

        public void setEncoding(String encoding) {
            fEncoding = encoding;
        }

        public String getPublicId() {
            return fPublicId;
        }

        public void setPublicId(String publicId) {
            fPublicId = publicId;
        }

        public String getSystemId() {
            return fSystemId;
        }

        public void setSystemId(String systemId) {
            fSystemId = systemId;
        }

        public String getBaseURI() {
            return fBaseSystemId;
        }

        public void setBaseURI(String baseURI) {
            fBaseSystemId = baseURI;
        }

        public boolean getCertifiedText() {
            return fCertifiedText;
        }

        public void setCertifiedText(boolean certifiedText) {
            fCertifiedText = certifiedText;
        }

    }
}
