package org.apache.geronimo.blueprint;

import org.osgi.service.blueprint.namespace.ParserContext;
import org.osgi.service.blueprint.reflect.Metadata;

/**
 * An extended ParserContext that also acts as a factory of Metadata objects.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public interface ExtendedParserContext extends ParserContext {

    <T extends Metadata> T createMetadata(Class<T> type);

}
