/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.spring.extender;

import java.net.URI;
import java.net.URL;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SpringOsgiCompendiumNamespaceHandler implements NamespaceHandler {

    public static final String CM_NAMESPACE = "http://aries.apache.org/blueprint/xmlns/blueprint-cm/v1.3.0";

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
        fixDom(element, CM_NAMESPACE);
        NamespaceHandler handler = context.getNamespaceHandler(URI.create(CM_NAMESPACE));
        return handler.parse(element, context);
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return component;
    }

    private static void fixDom(Node node, String namespace) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (!namespace.equals(node.getNamespaceURI())) {
                node.getOwnerDocument().renameNode(node, namespace, node.getLocalName());
            }
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                fixDom(children.item(i), namespace);
            }
        }
    }

}
