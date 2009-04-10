package org.apache.felix.blueprint.namespace;

import java.util.Map;
import java.util.HashMap;
import java.net.URI;

import org.osgi.service.blueprint.namespace.NamespaceHandler;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 3, 2009
 * Time: 11:50:01 AM
 * To change this template use File | Settings | File Templates.
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
