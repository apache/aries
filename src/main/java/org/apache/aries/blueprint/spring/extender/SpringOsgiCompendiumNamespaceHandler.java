package org.apache.aries.blueprint.spring.extender;

import java.net.URL;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SpringOsgiCompendiumNamespaceHandler implements NamespaceHandler {

    @Override
    public URL getSchemaLocation(String namespace) {
        if (namespace.startsWith("http://www.springframework.org/schema/osgi-compendium/spring-osgi-compendium")) {
            String sub = namespace.substring("http://www.springframework.org/schema/osgi-compendium/".length());
            if ("spring-osgi-compendium.xsd".equals(sub)) {
                sub = "spring-osgi-compendium-1.2.xsd";
            }
            return getClass().getResource(sub);
        }
        return null;
    }

    @Override
    public Set<Class> getManagedClasses() {
        return null;
    }

    @Override
    public Metadata parse(Element element, ParserContext context) {
        if ("managed-properties".equals(element.getLocalName())) {

        }
        else if ("managed-service-factory".equals(element.getLocalName())) {

        }
        else if ("cm-properties".equals(element.getLocalName())) {

        }
        throw new UnsupportedOperationException();
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return component;
    }
}
