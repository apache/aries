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
package org.apache.aries.blueprint.namespace;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import static org.apache.aries.blueprint.parser.Parser.BLUEPRINT_NAMESPACE;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

/**
 * Default implementation of the NamespaceHandlerRegistry.
 * 
 * This registry will track NamespaceHandler objects in the OSGi registry and make
 * them available, calling listeners when handlers are registered or unregistered.
 *
 * @version $Rev$, $Date$
 */
public class NamespaceHandlerRegistryImpl implements NamespaceHandlerRegistry, ServiceTrackerCustomizer {
    
    public static final String NAMESPACE = "osgi.service.blueprint.namespace";

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceHandlerRegistryImpl.class);

    // The bundle context is thread safe
    private final BundleContext bundleContext;

    // The service tracker is thread safe
    private final ServiceTracker tracker;

    // The handlers map is concurrent
    private final ConcurrentHashMap<URI, CopyOnWriteArraySet<NamespaceHandler>> handlers =
                        new ConcurrentHashMap<URI, CopyOnWriteArraySet<NamespaceHandler>>();

    // Access to the LRU schemas map is synchronized on itself
    private final LRUMap<Map<URI, NamespaceHandler>, Reference<Schema>> schemas =
                        new LRUMap<Map<URI, NamespaceHandler>, Reference<Schema>>(10);

    // Access to this factory is synchronized on itself
    private final SchemaFactory schemaFactory =
                        SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);

    // Access to this variable is must be synchronized on itself
    private final ArrayList<NamespaceHandlerSetImpl> sets =
                        new ArrayList<NamespaceHandlerSetImpl>();

    public NamespaceHandlerRegistryImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        tracker = new ServiceTracker(bundleContext, NamespaceHandler.class.getName(), this);
        tracker.open();
    }

    public Object addingService(ServiceReference reference) {
        LOGGER.debug("Adding NamespaceHandler " + reference.toString());
        NamespaceHandler handler = (NamespaceHandler) bundleContext.getService(reference);
        if (handler != null) {
            try {
                Map<String, Object> props = new HashMap<String, Object>();
                for (String name : reference.getPropertyKeys()) {
                    props.put(name, reference.getProperty(name));
                }
                registerHandler(handler, props);
            } catch (Exception e) {
                LOGGER.warn("Error registering NamespaceHandler", e);
            }
        } else {
            Bundle bundle = reference.getBundle();
            // If bundle is null, the service has already been unregistered,
            // so do nothing in that case
            if (bundle != null) {
                LOGGER.warn("Error resolving NamespaceHandler, null Service obtained from tracked ServiceReference {} for bundle {}/{}",
                        reference.toString(), reference.getBundle().getSymbolicName(), reference.getBundle().getVersion());
            }
        }
        return handler;
    }

    public void modifiedService(ServiceReference reference, Object service) {
        removedService(reference, service);
        addingService(reference);
    }

    public void removedService(ServiceReference reference, Object service) {
        try {
            LOGGER.debug("Removing NamespaceHandler " + reference.toString());
            NamespaceHandler handler = (NamespaceHandler) service;
            Map<String, Object> props = new HashMap<String, Object>();
            for (String name : reference.getPropertyKeys()) {
                props.put(name, reference.getProperty(name));
            }
            unregisterHandler(handler, props);
        } catch (Exception e) {
            LOGGER.warn("Error unregistering NamespaceHandler", e);
        }
    }

    public void registerHandler(NamespaceHandler handler, Map properties) {
        List<URI> namespaces = getNamespaces(properties);
        for (URI uri : namespaces) {
            CopyOnWriteArraySet<NamespaceHandler> h = handlers.putIfAbsent(uri, new CopyOnWriteArraySet<NamespaceHandler>());
            if (h == null) {
                h = handlers.get(uri);
            }
            if (h.add(handler)) {
                List<NamespaceHandlerSetImpl> sets;
                synchronized (this.sets) {
                    sets = new ArrayList<NamespaceHandlerSetImpl>(this.sets);
                }
                for (NamespaceHandlerSetImpl s : sets) {
                    s.registerHandler(uri, handler);
                }
            }
        }
    }

    public void unregisterHandler(NamespaceHandler handler, Map properties) {
        List<URI> namespaces = getNamespaces(properties);
        for (URI uri : namespaces) {
            CopyOnWriteArraySet<NamespaceHandler> h = handlers.get(uri);
            if (!h.remove(handler)) {
                continue;
            }
            List<NamespaceHandlerSetImpl> sets;
            synchronized (this.sets) {
                sets = new ArrayList<NamespaceHandlerSetImpl>(this.sets);
            }
            for (NamespaceHandlerSetImpl s : sets) {
                s.unregisterHandler(uri, handler);
            }
        }
        removeSchemasFor(handler);
    }

    private static List<URI> getNamespaces(Map properties) {
        Object ns = properties != null ? properties.get(NAMESPACE) : null;
        if (ns == null) {
            throw new IllegalArgumentException("NamespaceHandler service does not have an associated "
                            + NAMESPACE + " property defined");
        } else if (ns instanceof URI[]) {
            return Arrays.asList((URI[]) ns);
        } else if (ns instanceof URI) {
            return Collections.singletonList((URI) ns);
        } else if (ns instanceof String) {
            return Collections.singletonList(URI.create((String) ns));
        } else if (ns instanceof String[]) {
            String[] strings = (String[]) ns;
            List<URI> namespaces = new ArrayList<URI>(strings.length);
            for (String string : strings) {
                namespaces.add(URI.create(string));
            }
            return namespaces;
        } else if (ns instanceof Collection) {
            Collection col = (Collection) ns;
            List<URI> namespaces = new ArrayList<URI>(col.size());
            for (Object o : col) {
                namespaces.add(toURI(o));
            }
            return namespaces;
        } else if (ns instanceof Object[]) {
            Object[] array = (Object[]) ns;
            List<URI> namespaces = new ArrayList<URI>(array.length);
            for (Object o : array) {
                namespaces.add(toURI(o));
            }
            return namespaces;
        } else {
            throw new IllegalArgumentException("NamespaceHandler service has an associated "
                            + NAMESPACE + " property defined which can not be converted to an array of URI");
        }
    }

    private static URI toURI(Object o) {
        if (o instanceof URI) {
            return (URI) o;
        } else if (o instanceof String) {
            return URI.create((String) o);
        } else {
            throw new IllegalArgumentException("NamespaceHandler service has an associated "
                            + NAMESPACE + " property defined which can not be converted to an array of URI");
        }
    }
    
    public NamespaceHandlerSet getNamespaceHandlers(Set<URI> uris, Bundle bundle) {
        NamespaceHandlerSetImpl s;
        synchronized (sets) {
            s = new NamespaceHandlerSetImpl(uris, bundle);
            sets.add(s);
        }
        return s;
    }

    public void destroy() {
        tracker.close();
    }

    private Schema getExistingSchema(Map<URI, NamespaceHandler> handlers) {
        synchronized (schemas) {
            for (Map<URI, NamespaceHandler> key : schemas.keySet()) {
                boolean found = true;
                for (URI uri : handlers.keySet()) {
                    if (!handlers.get(uri).equals(key.get(uri))) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return schemas.get(key).get();
                }
            }
            return null;
        }
    }

    private void removeSchemasFor(NamespaceHandler handler) {
        synchronized (schemas) {
            List<Map<URI, NamespaceHandler>> keys = new ArrayList<Map<URI, NamespaceHandler>>();
            for (Map<URI, NamespaceHandler> key : schemas.keySet()) {
                if (key.values().contains(handler)) {
                    keys.add(key);
                }
            }
            for (Map<URI, NamespaceHandler> key : keys) {
                schemas.remove(key);
            }
        }
    }

    private void cacheSchema(Map<URI, NamespaceHandler> handlers, Schema schema) {
        synchronized (schemas) {
            // Remove schemas that are fully included
            for (Iterator<Map<URI, NamespaceHandler>> iterator = schemas.keySet().iterator(); iterator.hasNext();) {
                Map<URI, NamespaceHandler> key = iterator.next();
                boolean found = true;
                for (URI uri : key.keySet()) {
                    if (!key.get(uri).equals(handlers.get(uri))) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    iterator.remove();
                    break;
                }
            }
            // Add our new schema
            schemas.put(handlers, new SoftReference<Schema>(schema));
        }
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private static class SourceLSInput implements LSInput {
        private final StreamSource source;

        public SourceLSInput(StreamSource source) {
            this.source = source;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public InputStream getByteStream() {
            return source.getInputStream();
        }

        public void setByteStream(InputStream byteStream) {
        }

        public String getStringData() {
            return null;
        }

        public void setStringData(String stringData) {
        }

        public String getSystemId() {
            return source.getSystemId();
        }

        public void setSystemId(String systemId) {
        }

        public String getPublicId() {
            return null;
        }

        public void setPublicId(String publicId) {
        }

        public String getBaseURI() {
            return null;
        }

        public void setBaseURI(String baseURI) {
        }

        public String getEncoding() {
            return null;
        }

        public void setEncoding(String encoding) {
        }

        public boolean getCertifiedText() {
            return false;
        }

        public void setCertifiedText(boolean certifiedText) {
        }
    }

    protected class NamespaceHandlerSetImpl implements NamespaceHandlerSet {

        private final List<Listener> listeners;
        private final Bundle bundle;
        private final Set<URI> namespaces;
        private final Map<URI, NamespaceHandler> handlers;
        private final Properties schemaMap = new Properties();
        private Schema schema;

        public NamespaceHandlerSetImpl(Set<URI> namespaces, Bundle bundle) {
            this.listeners = new CopyOnWriteArrayList<Listener>();
            this.namespaces = new HashSet<URI>(namespaces);
            this.bundle = bundle;
            handlers = new ConcurrentHashMap<URI, NamespaceHandler>();
            for (URI ns : namespaces) {
                findCompatibleNamespaceHandler(ns);
            }
            URL url = bundle.getResource("OSGI-INF/blueprint/schema.map");
            if (url != null) {
                InputStream ins = null;
                try {
                    ins = url.openStream();
                    schemaMap.load(ins);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    //ignore
                } finally {
                    closeQuietly(ins);
                }
            }
            for (Object ns : schemaMap.keySet()) {
                try {
                    this.namespaces.remove(new URI(ns.toString()));
                } catch (URISyntaxException e) {
                    //ignore
                }
            }
        }

        public boolean isComplete() {
            return handlers.size() == namespaces.size();
        }

        public Set<URI> getNamespaces() {
            return namespaces;
        }

        public NamespaceHandler getNamespaceHandler(URI namespace) {
            return handlers.get(namespace);
        }

        public Schema getSchema() throws SAXException, IOException {
            return getSchema(null);
        }

        public Schema getSchema(Map<String, String> locations) throws SAXException, IOException {
            if (!isComplete()) {
                throw new IllegalStateException("NamespaceHandlerSet is not complete");
            }
            if (schema == null) {
                schema = doGetSchema(locations);
            }
            return schema;
        }

        private Schema doGetSchema(Map<String, String> locations) throws IOException, SAXException {
            if (schemaMap != null && !schemaMap.isEmpty()) {
                return createSchema(locations);
            }
            // Find a schema that can handle all the requested namespaces
            // If it contains additional namespaces, it should not be a problem since
            // they won't be used at all
            Schema schema = getExistingSchema(handlers);
            if (schema == null) {
                // Create schema
                schema = createSchema(locations);
                cacheSchema(handlers, schema);
            }
            return schema;
        }

        private class Loader implements LSResourceResolver, Closeable {
            final List<StreamSource> sources = new ArrayList<StreamSource>();
            final Map<String, URL> loaded = new HashMap<String, URL>();
            final Map<String, String> namespaces = new HashMap<String, String>();
            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                // Compute id
                String id;
                String prevNamespace = baseURI != null ? namespaces.get(baseURI) : null;
                if (namespaceURI != null && prevNamespace != null && namespaceURI.equals(prevNamespace)) {
                    // This is an include
                    id = getId(type, namespaceURI, publicId, systemId);
                } else {
                    id = getId(type, namespaceURI, publicId, null);
                }
                // Check if it has already been loaded
                if (loaded.containsKey(id)) {
                    return createLSInput(loaded.get(id), id, namespaceURI);
                }
                // Schema map
                //-----------
                // Use provided schema map to find the resource.
                // If the schema map contains the namespaceURI, publicId or systemId,
                // load the corresponding resource directly from the bundle.
                String loc = null;
                if (namespaceURI != null) {
                    loc = schemaMap.getProperty(namespaceURI);
                }
                if (loc == null && publicId != null) {
                    loc = schemaMap.getProperty(publicId);
                }
                if (loc == null && systemId != null) {
                    loc = schemaMap.getProperty(systemId);
                }
                if (loc != null) {
                    URL url = bundle.getResource(loc);
                    if (url != null) {
                        return createLSInput(url, id, namespaceURI);
                    }
                }
                // Relative uris
                //---------------
                // For relative uris, don't use the namespace handlers, but simply resolve the uri
                // and use that one directly to load the resource.
                String resolved = resolveIfRelative(systemId, baseURI);
                if (resolved != null) {
                    URL url;
                    try {
                        url = new URL(resolved);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return createLSInput(url, id, namespaceURI);
                }
                // Only support xml schemas from now on
                if (namespaceURI == null || !W3C_XML_SCHEMA_NS_URI.equals(type)) {
                    return null;
                }
                // We are now loading a schema, or schema part with
                //  * notNull(namespaceURI)
                //  * null(systemId) or absolute(systemId)
                URI nsUri = URI.create(namespaceURI);
                String rid = systemId != null ? systemId : namespaceURI;
                NamespaceHandler h = getNamespaceHandler(nsUri);
                // This is a resource from a known namespace
                if (h != null) {
                    URL url = h.getSchemaLocation(rid);
                    if (isCorrectUrl(url)) {
                        return createLSInput(url, id, namespaceURI);
                    }
                }
                else {
                    // Ask known handlers if they have this schema
                    for (NamespaceHandler hd : handlers.values()) {
                        URL url = hd.getSchemaLocation(rid);
                        if (isCorrectUrl(url)) {
                            return createLSInput(url, id, namespaceURI);
                        }
                    }
                }
                LOGGER.warn("Unable to find namespace handler for {}", namespaceURI);
                return null;
            }

            public String getId(String type, String namespaceURI, String publicId, String systemId) {
                return type + "|" + namespaceURI + "|" + publicId + "|" + systemId;
            }

            public StreamSource use(URL resource, String id, String namespace) throws IOException {
                String url = resource.toExternalForm();
                StreamSource ss = new StreamSource(resource.openStream(), url);
                sources.add(ss);
                loaded.put(id, resource);
                namespaces.put(url, namespace);
                return ss;
            }

            @Override
            public void close() {
                for (StreamSource source : sources) {
                    closeQuietly(source.getInputStream());
                }
            }

            public Source[] getSources() {
                return sources.toArray(new Source[sources.size()]);
            }

            private boolean isCorrectUrl(URL url) {
                return url != null && !loaded.values().contains(url);
            }
            
            private String resolveIfRelative(String systemId, String baseURI) {
                if (baseURI != null && systemId != null) {
                    URI sId = URI.create(systemId);
                    if (!sId.isAbsolute()) {
                        try {
                            // http://stackoverflow.com/questions/13046150/java-net-uri-relativize-doesnt-work-with-jar-uris
                            return new URL(new URL(baseURI), systemId).toString();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                return null;
            }
            private LSInput createLSInput(URL url, String id, String namespace) {
                try {
                    return new SourceLSInput(use(url, id, namespace));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private Schema createSchema(Map<String, String> locations) throws IOException, SAXException {
            Loader loader = new Loader();
            try {
                loader.use(getClass().getResource("/org/osgi/service/blueprint/blueprint.xsd"),
                           loader.getId(W3C_XML_SCHEMA_NS_URI, BLUEPRINT_NAMESPACE, null, null),
                           BLUEPRINT_NAMESPACE);
                loader.use(getClass().getResource("/org/apache/aries/blueprint/ext/impl/xml.xsd"),
                           loader.getId(W3C_XML_SCHEMA_NS_URI, XML_NS_URI, null, null),
                           XML_NS_URI);

                // Create a schema for the namespaces
                for (URI ns : handlers.keySet()) {
                    URL url = handlers.get(ns).getSchemaLocation(ns.toString());
                    if (url == null && locations != null) {
                        String loc = locations.get(ns.toString());
                        if (loc != null) {
                            url = handlers.get(ns).getSchemaLocation(loc);
                        }
                    }
                    if (url == null) {
                        LOGGER.warn("No URL is defined for schema " + ns + ". This schema will not be validated");
                    } else {
                        loader.use(url, loader.getId(W3C_XML_SCHEMA_NS_URI, ns.toString(), null, null), ns.toString());
                    }
                }
                for (Object ns : schemaMap.values()) {
                    URL url = bundle.getResource(ns.toString());
                    if (url == null) {
                        LOGGER.warn("No URL is defined for schema " + ns + ". This schema will not be validated");
                    } else {
                        loader.use(url, loader.getId(W3C_XML_SCHEMA_NS_URI, ns.toString(), null, null), ns.toString());
                    }
                }
                synchronized (schemaFactory) {
                    schemaFactory.setResourceResolver(loader);
                    return schemaFactory.newSchema(loader.getSources());
                }
            } finally {
                loader.close();
            }
        }

        public void addListener(Listener listener) {
            listeners.add(listener);
        }

        public void removeListener(Listener listener) {
            listeners.remove(listener);
        }

        public void destroy() {
            synchronized (NamespaceHandlerRegistryImpl.this.sets) {
                NamespaceHandlerRegistryImpl.this.sets.remove(this);
            }
        }

        public void registerHandler(URI uri, NamespaceHandler handler) {
            if (namespaces.contains(uri) && handlers.get(uri) == null) {
                if (findCompatibleNamespaceHandler(uri) !=  null) {
                    for (Listener listener : listeners) {
                        try {
                            listener.namespaceHandlerRegistered(uri);
                        } catch (Throwable t) {
                            LOGGER.debug("Unexpected exception when notifying a NamespaceHandler listener", t);
                        }
                    }
                }
            }
        }

        public void unregisterHandler(URI uri, NamespaceHandler handler) {
            if (handlers.get(uri) == handler) {
                handlers.remove(uri);
                for (Listener listener : listeners) {
                    try {
                        listener.namespaceHandlerUnregistered(uri);
                    } catch (Throwable t) {
                        LOGGER.debug("Unexpected exception when notifying a NamespaceHandler listener", t);
                    }
                }
            }
        }

        private NamespaceHandler findCompatibleNamespaceHandler(URI ns) {
            Set<NamespaceHandler> candidates = NamespaceHandlerRegistryImpl.this.handlers.get(ns);
            if (candidates != null) {
                for (NamespaceHandler h : candidates) {
                    Set<Class> classes = h.getManagedClasses();
                    boolean compat = true;
                    if (classes != null) {
                        Set<Class> allClasses = new HashSet<Class>();
                        for (Class cl : classes) {
                            for (Class c = cl; c != null; c = c.getSuperclass()) {
                                allClasses.add(c);
                                for (Class i : c.getInterfaces()) {
                                    allClasses.add(i);
                                }
                            }
                        }
                        for (Class cl : allClasses) {
                            Class clb;
                            try {
                                clb = bundle.loadClass(cl.getName());
                                if (clb != cl) {
                                    compat = false;
                                    break;
                                }
                            } catch (ClassNotFoundException e) {
                                // Ignore
                            } catch (NoClassDefFoundError e) {
                                // Ignore
                            }
                        }
                    }
                    if (compat) {
                        namespaces.add(ns);
                        handlers.put(ns, wrapIfNeeded(h));
                        return h;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Wrap the handler if needed to fix its behavior.
     * When asked for a schema location, some simple handlers always return
     * the same url, whatever the asked location is.  This can lead to lots
     * of problems, so we need to verify and fix those behaviors.
     */
    private static NamespaceHandler wrapIfNeeded(final NamespaceHandler handler) {
        URL result = null;
        try {
            result = handler.getSchemaLocation("");
        } catch (Throwable t) {
            // Ignore
        }
        if (result != null) {
            LOGGER.warn("NamespaceHandler " + handler.getClass().getName() + " is behaving badly and should be fixed");
            final URL res = result;
            return new NamespaceHandler() {
                final ConcurrentMap<String, Boolean> cache = new ConcurrentHashMap<String, Boolean>();
                @Override
                public URL getSchemaLocation(String s) {
                    URL url = handler.getSchemaLocation(s);
                    if (url != null && url.equals(res)) {
                        Boolean v, newValue;
                        Boolean valid = ((v = cache.get(s)) == null &&
                                (newValue = isValidSchema(s, url)) != null &&
                                (v = cache.putIfAbsent(s, newValue)) == null) ? newValue : v;
                        return valid ? url : null;
                    }
                    return url;
                }
                @Override
                public Set<Class> getManagedClasses() {
                    return handler.getManagedClasses();
                }
                @Override
                public Metadata parse(Element element, ParserContext parserContext) {
                    return handler.parse(element, parserContext);
                }
                @Override
                public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext parserContext) {
                    return handler.decorate(node, componentMetadata, parserContext);
                }
                private boolean isValidSchema(String ns, URL url) {
                    try {
                        InputStream is = url.openStream();
                        try {
                            XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(is);
                            try {
                                reader.nextTag();
                                String nsuri = reader.getNamespaceURI();
                                String name = reader.getLocalName();
                                if ("http://www.w3.org/2001/XMLSchema".equals(nsuri) && "schema".equals(name)) {
                                    String target = reader.getAttributeValue(null, "targetNamespace");
                                    if (ns.equals(target)) {
                                        return true;
                                    }
                                }
                            } finally {
                                reader.close();
                            }
                        } finally {
                            is.close();
                        }
                    } catch (Throwable t) {
                        // Ignore
                    }
                    return false;
                }
            };
        } else {
            return handler;
        }
    }

    public static class LRUMap<K,V> extends AbstractMap<K,V> {

        private final int bound;
        private final LinkedList<Entry<K,V>> entries = new LinkedList<Entry<K,V>>();

        private static class LRUEntry<K,V> implements Entry<K,V> {
            private final K key;
            private final V value;

            private LRUEntry(K key, V value) {
                this.key = key;
                this.value = value;
            }

            public K getKey() {
                return key;
            }

            public V getValue() {
                return value;
            }

            public V setValue(V value) {
                throw new UnsupportedOperationException();
            }
        }

        private LRUMap(int bound) {
            this.bound = bound;
        }

        public V get(Object key) {
            if (key == null) {
                throw new NullPointerException();
            }
            for (Entry<K,V> e : entries) {
                if (e.getKey().equals(key)) {
                    entries.remove(e);
                    entries.addFirst(e);
                    return e.getValue();
                }
            }
            return null;
        }

        public V put(K key, V value) {
            if (key == null) {
                throw new NullPointerException();
            }
            V old = null;
            for (Entry<K,V> e : entries) {
                if (e.getKey().equals(key)) {
                    entries.remove(e);
                    old = e.getValue();
                    break;
                }
            }
            if (value != null) {
                entries.addFirst(new LRUEntry<K,V>(key, value));
                while (entries.size() > bound) {
                    entries.removeLast();
                }
            }
            return old;
        }

        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<Entry<K,V>>() {
                public Iterator<Entry<K, V>> iterator() {
                    return entries.iterator();
                }

                public int size() {
                    return entries.size();
                }
            };
        }
    }

}
