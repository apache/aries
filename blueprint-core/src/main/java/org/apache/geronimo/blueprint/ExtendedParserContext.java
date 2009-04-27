package org.apache.geronimo.blueprint;

import org.w3c.dom.Element;

import org.osgi.service.blueprint.namespace.ParserContext;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * An extended ParserContext that also acts as a factory of Metadata objects.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public interface ExtendedParserContext extends ParserContext {

    <T extends Metadata> T createMetadata(Class<T> type);

    <T> T parseElement(Class<T> type, ComponentMetadata enclosingComponent, Element element);

}
