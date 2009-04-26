/**
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
package org.apache.geronimo.blueprint.compendium;

import java.net.URL;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.geronimo.blueprint.ExtendedComponentDefinitionRegistry;
import org.apache.geronimo.blueprint.ExtendedParserContext;
import org.apache.geronimo.blueprint.context.Parser;
import org.apache.geronimo.blueprint.reflect.BeanPropertyImpl;
import org.apache.geronimo.blueprint.reflect.PropsMetadataImpl;
import org.apache.geronimo.blueprint.reflect.RefMetadataImpl;
import org.apache.geronimo.blueprint.reflect.ReferenceMetadataImpl;
import org.apache.geronimo.blueprint.reflect.ValueMetadataImpl;
import org.apache.geronimo.blueprint.mutable.MutableBeanMetadata;
import org.osgi.service.blueprint.context.ComponentDefinitionException;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.NamespaceHandler;
import org.osgi.service.blueprint.namespace.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * TODO
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766508 $, $Date: 2009-04-19 22:09:27 +0200 (Sun, 19 Apr 2009) $
 */
public class CompendiumNamespaceHandler implements NamespaceHandler {

    public static final String BLUEPRINT_COMPENDIUM_NAMESPACE = "http://www.osgi.org/xmlns/blueprint-compendium/v1.0.0";

    public static final String PROPERTY_PLACEHOLDER_ELEMENT = "property-placeholder";
    public static final String MANAGED_PROPERTIES_ELEMENT = "managed-properties";
    public static final String MANAGED_SERVICE_FACTORY_ELEMENT = "managed-service-factory";
    public static final String CM_PROPERTIES_ELEMENT = "cm-properties";
    public static final String DEFAULT_PROPERTIES_ELEMENT = "default-properties";

    public static final String ID_ATTRIBUTE = "id";
    public static final String PERSISTENT_ID_ATTRIBUTE = "persistent-id";
    public static final String PLACEHOLDER_PREFIX_ATTRIBUTE = "placeholder-prefix";
    public static final String PLACEHOLDER_SUFFIX_ATTRIBUTE = "placeholder-suffix";
    public static final String DEFAULTS_REF_ATTRIBUTE = "defaults-ref";
    public static final String UPDATE_STRATEGY_ATTRIBUTE = "update-strategy";
    public static final String UPDATE_METHOD_ATTRIBUTE = "update-method";

    public static final String CONFIG_ADMIN_REFERENCE_NAME = "blueprint.configadmin";

    public URL getSchemaLocation(String namespace) {
        return getClass().getResource("/org/apache/geronimo/blueprint/compendium/blueprint-compendium.xsd");
    }

    public ComponentMetadata parse(Element element, ParserContext ctx) {
        ExtendedParserContext context = (ExtendedParserContext) ctx;
        ExtendedComponentDefinitionRegistry registry = (ExtendedComponentDefinitionRegistry) context.getComponentDefinitionRegistry();
        Parser parser = new Parser("compendium-");
        createConfigAdminProxy(registry);
        if (nodeNameEquals(element, PROPERTY_PLACEHOLDER_ELEMENT)) {
            MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
            metadata.setId(parser.getName(element));
            metadata.setClassName(CompendiumPropertyPlaceholder.class.getName());
            metadata.addProperty(new BeanPropertyImpl("blueprintContext", new RefMetadataImpl("blueprintContext")));
            metadata.addProperty(new BeanPropertyImpl("configAdmin", new RefMetadataImpl(CONFIG_ADMIN_REFERENCE_NAME)));
            metadata.addProperty(new BeanPropertyImpl("persistentId",
                                                      new ValueMetadataImpl(element.getAttribute(PERSISTENT_ID_ATTRIBUTE))));
            String prefix = element.hasAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                                        ? element.getAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                                        : "${";
            metadata.addProperty(new BeanPropertyImpl("placeholderPrefix", new ValueMetadataImpl(prefix)));
            String suffix = element.hasAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                                        ? element.getAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                                        : "}";
            metadata.addProperty(new BeanPropertyImpl("placeholderSuffix", new ValueMetadataImpl(suffix)));
            String defaultsRef = element.hasAttribute(DEFAULTS_REF_ATTRIBUTE) ? element.getAttribute(DEFAULTS_REF_ATTRIBUTE) : null;
            if (defaultsRef != null) {
                metadata.addProperty(new BeanPropertyImpl("defaultProperties", new RefMetadataImpl("defaultsRef")));
            }
            // Parse elements
            NodeList nl = element.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    Element e = (Element) node;
                    if (BLUEPRINT_COMPENDIUM_NAMESPACE.equals(e.getNamespaceURI())) {
                        if (nodeNameEquals(e, DEFAULT_PROPERTIES_ELEMENT)) {
                            if (defaultsRef != null) {
                                throw new ComponentDefinitionException("Only one of " + DEFAULTS_REF_ATTRIBUTE + " attribute or " + DEFAULT_PROPERTIES_ELEMENT + " element is allowed");
                            }
                            MapMetadata map = parser.parseMap(e, metadata);
                            metadata.addProperty(new BeanPropertyImpl("defaultProperties", new PropsMetadataImpl(map.getEntries())));
                        }
                    }
                }
            }
            return metadata;
        } else {
            // TODO: parse other compendium elements.
            throw new ComponentDefinitionException("Unsupported element: " + element.getNodeName());
        }
    }

    /**
     * Create a reference to the ConfigurationAdmin service if not already done
     * and add it to the registry.
     *
     * @param registry the registry to add the config admin reference to
     */
    private void createConfigAdminProxy(ComponentDefinitionRegistry registry) {
        if (registry.getComponentDefinition(CONFIG_ADMIN_REFERENCE_NAME) == null) {
            ReferenceMetadataImpl reference = new ReferenceMetadataImpl();
            reference.setId(CONFIG_ADMIN_REFERENCE_NAME);
            reference.addInterfaceName(ConfigurationAdmin.class.getName());
            reference.setAvailability(ReferenceMetadata.MANDATORY_AVAILABILITY);
            reference.setTimeout(300000);
            registry.registerComponentDefinition(reference);
        }
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        throw new ComponentDefinitionException("Illegal use of blueprint compendium namespace");
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return (name.equals(node.getNodeName()) || name.equals(node.getLocalName()));
    }

}
