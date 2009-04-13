package org.apache.felix.blueprint.namespace;

import java.net.URI;

import org.osgi.service.blueprint.namespace.NamespaceHandler;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 3, 2009
 * Time: 12:26:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NamespaceHandlerRegistry {

    NamespaceHandler getNamespaceHandler(URI uri);
}
