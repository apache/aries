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
package org.apache.aries.blueprint.ext.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ExtendedReferenceListMetadata;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.ext.PlaceholdersUtils;
import org.apache.aries.blueprint.ext.PropertyPlaceholder;
import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableComponentMetadata;
import org.apache.aries.blueprint.mutable.MutableIdRefMetadata;
import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A namespace handler for Aries blueprint extensions
 *
 * @version $Rev$, $Date$
 */
public class ExtNamespaceHandler implements org.apache.aries.blueprint.NamespaceHandler {

    public static final String BLUEPRINT_NAMESPACE = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    public static final String BLUEPRINT_EXT_NAMESPACE_V1_0 = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0";
    public static final String BLUEPRINT_EXT_NAMESPACE_V1_1 = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.1.0";
    public static final String BLUEPRINT_EXT_NAMESPACE_V1_2 = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.2.0";
    public static final String BLUEPRINT_EXT_NAMESPACE_V1_3 = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.3.0";

    public static final String PROPERTY_PLACEHOLDER_ELEMENT = "property-placeholder";
    public static final String DEFAULT_PROPERTIES_ELEMENT = "default-properties";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String VALUE_ELEMENT = "value";
    public static final String LOCATION_ELEMENT = "location";

    public static final String ID_ATTRIBUTE = "id";
    public static final String PLACEHOLDER_PREFIX_ATTRIBUTE = "placeholder-prefix";
    public static final String PLACEHOLDER_SUFFIX_ATTRIBUTE = "placeholder-suffix";
    public static final String DEFAULTS_REF_ATTRIBUTE = "defaults-ref";
    public static final String IGNORE_MISSING_LOCATIONS_ATTRIBUTE = "ignore-missing-locations";
    public static final String EVALUATOR_ATTRIBUTE = "evaluator";

    public static final String SYSTEM_PROPERTIES_ATTRIBUTE = "system-properties";
    public static final String SYSTEM_PROPERTIES_NEVER = "never";
    public static final String SYSTEM_PROPERTIES_FALLBACK = "fallback";
    public static final String SYSTEM_PROPERTIES_OVERRIDE = "override";

    public static final String PROXY_METHOD_ATTRIBUTE = "proxy-method";
    public static final String PROXY_METHOD_DEFAULT = "default";
    public static final String PROXY_METHOD_CLASSES = "classes";
    public static final String PROXY_METHOD_GREEDY = "greedy";

    public static final String ROLE_ATTRIBUTE = "role";
    public static final String ROLE_PROCESSOR = "processor";
    
    public static final String FIELD_INJECTION_ATTRIBUTE = "field-injection";
    
    public static final String DEFAULT_REFERENCE_BEAN = "default";

    public static final String FILTER_ATTRIBUTE = "filter";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtNamespaceHandler.class);

    private int idCounter;
    
    private BundleContext ctx;

    public void setBundleContext(BundleContext bc) {
      this.ctx = bc;
    }
    
    public URL getSchemaLocation(String namespace) {
        if (BLUEPRINT_EXT_NAMESPACE_V1_0.equals(namespace)) {
          return getClass().getResource("blueprint-ext.xsd");
        } else if (BLUEPRINT_EXT_NAMESPACE_V1_1.equals(namespace)) {
          return getClass().getResource("blueprint-ext-1.1.xsd");
        } else if (BLUEPRINT_EXT_NAMESPACE_V1_2.equals(namespace)) {
          return getClass().getResource("blueprint-ext-1.2.xsd");
        } else if (BLUEPRINT_EXT_NAMESPACE_V1_3.equals(namespace)) {
            return getClass().getResource("blueprint-ext-1.3.xsd");
        } else if ("http://www.w3.org/XML/1998/namespace".equals(namespace)) {
            return getClass().getResource("xml.xsd");
        }
        return null;
    }

    public Set<Class> getManagedClasses() {
        return new HashSet<Class>(Arrays.asList(
                PropertyPlaceholder.class
        ));
    }

    public Metadata parse(Element element, ParserContext context) {
        LOGGER.debug("Parsing element {{}}{}", element.getNamespaceURI(), element.getLocalName());
        if (nodeNameEquals(element, PROPERTY_PLACEHOLDER_ELEMENT)) {
            return parsePropertyPlaceholder(context, element);
        } else {
            throw new ComponentDefinitionException("Unsupported element: " + element.getNodeName());
        }
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        if (node instanceof Attr && nodeNameEquals(node, PROXY_METHOD_ATTRIBUTE)) {
            return decorateProxyMethod(node, component, context);
        } else if (node instanceof Attr && nodeNameEquals(node, ROLE_ATTRIBUTE)) {
            return decorateRole(node, component, context);
        } else if (node instanceof Attr && nodeNameEquals(node, FIELD_INJECTION_ATTRIBUTE)) {
            return decorateFieldInjection(node, component, context);
        } else if (node instanceof Attr && nodeNameEquals(node, DEFAULT_REFERENCE_BEAN)) {
            return decorateDefaultBean(node, component, context);
        } else if (node instanceof Attr && nodeNameEquals(node, FILTER_ATTRIBUTE)) {
            return decorateFilter(node, component, context);
        } else {
            throw new ComponentDefinitionException("Unsupported node: " + node.getNodeName());
        }
    }
    
    private ComponentMetadata decorateDefaultBean(Node node,
        ComponentMetadata component, ParserContext context) 
    {
        if (!(component instanceof ReferenceMetadata)) {
            throw new ComponentDefinitionException("Attribute " + node.getNodeName() + " can only be used on a <reference> element");
        }
      
        if (!(component instanceof MutableReferenceMetadata)) {
            throw new ComponentDefinitionException("Expected an instanceof MutableReferenceMetadata");
        }
        
        String value = ((Attr) node).getValue();
        ((MutableReferenceMetadata) component).setDefaultBean(value);
        return component;
    }

    private ComponentMetadata decorateFieldInjection(Node node, ComponentMetadata component, ParserContext context) {
        if (!(component instanceof BeanMetadata)) {
            throw new ComponentDefinitionException("Attribute " + node.getNodeName() + " can only be used on a <bean> element");
        }
        
        if (!(component instanceof MutableBeanMetadata)) {
            throw new ComponentDefinitionException("Expected an instanceof MutableBeanMetadata");
        }
        
        String value = ((Attr) node).getValue();
        ((MutableBeanMetadata) component).setFieldInjection("true".equals(value) || "1".equals(value));
        return component;
    }

    private ComponentMetadata decorateRole(Node node, ComponentMetadata component, ParserContext context) {
        if (!(component instanceof BeanMetadata)) {
            throw new ComponentDefinitionException("Attribute " + node.getNodeName() + " can only be used on a <bean> element");
        }
        if (!(component instanceof MutableBeanMetadata)) {
            throw new ComponentDefinitionException("Expected an instance of MutableBeanMetadata");
        }
        boolean processor = false;
        String value = ((Attr) node).getValue();
        String[] flags = value.trim().split(" ");
        for (String flag : flags) {
            if (ROLE_PROCESSOR.equals(flag)) {
                processor = true;
            } else {
                throw new ComponentDefinitionException("Unknown proxy method: " + flag);
            }
        }
        ((MutableBeanMetadata) component).setProcessor(processor);
        return component;
    }

    private ComponentMetadata decorateProxyMethod(Node node, ComponentMetadata component, ParserContext context) {
        if (!(component instanceof ServiceReferenceMetadata)) {
            throw new ComponentDefinitionException("Attribute " + node.getNodeName() + " can only be used on a <reference> or <reference-list> element");
        }
        if (!(component instanceof MutableServiceReferenceMetadata)) {
            throw new ComponentDefinitionException("Expected an instance of MutableServiceReferenceMetadata");
        }
        int method = 0;
        String value = ((Attr) node).getValue();
        String[] flags = value.trim().split(" ");
        for (String flag : flags) {
            if (PROXY_METHOD_DEFAULT.equals(flag)) {
                method += ExtendedReferenceListMetadata.PROXY_METHOD_DEFAULT;
            } else if (PROXY_METHOD_CLASSES.equals(flag)) {
                method += ExtendedReferenceListMetadata.PROXY_METHOD_CLASSES;
            } else if (PROXY_METHOD_GREEDY.equals(flag)) {
                method += ExtendedReferenceListMetadata.PROXY_METHOD_GREEDY;
            } else {
                throw new ComponentDefinitionException("Unknown proxy method: " + flag);
            }
        }
        if ((method & ExtendedReferenceListMetadata.PROXY_METHOD_GREEDY) != 0 && !(component instanceof ReferenceListMetadata)) {
            throw new ComponentDefinitionException("Greedy proxying is only available for <reference-list> element");
        }
        ((MutableServiceReferenceMetadata) component).setProxyMethod(method);
        return component;
    }

    private ComponentMetadata decorateFilter(Node node,
                                             ComponentMetadata component, ParserContext context)
    {
        if (!(component instanceof ServiceReferenceMetadata)) {
            throw new ComponentDefinitionException("Attribute " + node.getNodeName() + " can only be used on a <reference> or <reference-list> element");
        }

        if (!(component instanceof MutableServiceReferenceMetadata)) {
            throw new ComponentDefinitionException("Expected an instanceof MutableServiceReferenceMetadata");
        }

        String value = ((Attr) node).getValue();
        ((MutableServiceReferenceMetadata) component).setExtendedFilter(createValue(context, value));
        return component;
    }

    private Metadata parsePropertyPlaceholder(ParserContext context, Element element) {
        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setProcessor(true);
        metadata.setId(getId(context, element));
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.setRuntimeClass(PropertyPlaceholder.class);
        metadata.setInitMethod("init");
        String prefix = element.hasAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                                    ? element.getAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                                    : "${";
        metadata.addProperty("placeholderPrefix", createValue(context, prefix));
        String suffix = element.hasAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                                    ? element.getAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                                    : "}";
        metadata.addProperty("placeholderSuffix", createValue(context, suffix));
        String defaultsRef = element.hasAttribute(DEFAULTS_REF_ATTRIBUTE) ? element.getAttribute(DEFAULTS_REF_ATTRIBUTE) : null;
        if (defaultsRef != null) {
            metadata.addProperty("defaultProperties", createRef(context, defaultsRef));
        }
        String ignoreMissingLocations = element.hasAttribute(IGNORE_MISSING_LOCATIONS_ATTRIBUTE) ? element.getAttribute(IGNORE_MISSING_LOCATIONS_ATTRIBUTE) : null;
        if (ignoreMissingLocations != null) {
            metadata.addProperty("ignoreMissingLocations", createValue(context, ignoreMissingLocations));
        }
        String systemProperties = element.hasAttribute(SYSTEM_PROPERTIES_ATTRIBUTE) ? element.getAttribute(SYSTEM_PROPERTIES_ATTRIBUTE) : null;
        if (systemProperties != null) {
            metadata.addProperty("systemProperties", createValue(context, systemProperties));
        }
        String evaluator = element.hasAttribute(EVALUATOR_ATTRIBUTE) ? element.getAttribute(EVALUATOR_ATTRIBUTE) : null;
        if (evaluator != null) {
            metadata.addProperty("evaluator", createReference(context, evaluator));
        }
        // Parse elements
        List<String> locations = new ArrayList<String>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (BLUEPRINT_EXT_NAMESPACE_V1_0.equals(e.getNamespaceURI())
                        || BLUEPRINT_EXT_NAMESPACE_V1_1.equals(e.getNamespaceURI())
                        || BLUEPRINT_EXT_NAMESPACE_V1_2.equals(e.getNamespaceURI())
                        || BLUEPRINT_EXT_NAMESPACE_V1_3.equals(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, DEFAULT_PROPERTIES_ELEMENT)) {
                        if (defaultsRef != null) {
                            throw new ComponentDefinitionException("Only one of " + DEFAULTS_REF_ATTRIBUTE + " attribute or " + DEFAULT_PROPERTIES_ELEMENT + " element is allowed");
                        }
                        Metadata props = parseDefaultProperties(context, metadata, e);
                        metadata.addProperty("defaultProperties", props);
                    } else if (nodeNameEquals(e, LOCATION_ELEMENT)) {
                        locations.add(getTextValue(e));
                    }
                }
            }
        }
        if (!locations.isEmpty()) {
            metadata.addProperty("locations", createList(context, locations));
        }

        PlaceholdersUtils.validatePlaceholder(metadata, context.getComponentDefinitionRegistry());

        return metadata;
    }

    private Metadata parseDefaultProperties(ParserContext context, MutableBeanMetadata enclosingComponent, Element element) {
        MutableMapMetadata props = context.createMetadata(MutableMapMetadata.class);
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (BLUEPRINT_EXT_NAMESPACE_V1_0.equals(e.getNamespaceURI())
                        || BLUEPRINT_EXT_NAMESPACE_V1_1.equals(e.getNamespaceURI())
                        || BLUEPRINT_EXT_NAMESPACE_V1_2.equals(e.getNamespaceURI())
                        || BLUEPRINT_EXT_NAMESPACE_V1_3.equals(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, PROPERTY_ELEMENT)) {
                        BeanProperty prop = context.parseElement(BeanProperty.class, enclosingComponent, e);
                        props.addEntry(createValue(context, prop.getName(), String.class.getName()), prop.getValue());
                    }
                }
            }
        }
        return props;
    }

    public String getId(ParserContext context, Element element) {
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            return element.getAttribute(ID_ATTRIBUTE);
        } else {
            return generateId(context);
        }
    }

    public void generateIdIfNeeded(ParserContext context, MutableComponentMetadata metadata) {
        if (metadata.getId() == null) {
            metadata.setId(generateId(context));
        }
    }

    private String generateId(ParserContext context) {
        String id;
        do {
            id = ".ext-" + ++idCounter;
        } while (context.getComponentDefinitionRegistry().containsComponentDefinition(id));
        return id;
    }

    private static ValueMetadata createValue(ParserContext context, String value) {
        return createValue(context, value, null);
    }

    private static ValueMetadata createValue(ParserContext context, String value, String type) {
        MutableValueMetadata m = context.createMetadata(MutableValueMetadata.class);
        m.setStringValue(value);
        m.setType(type);
        return m;
    }

    private static RefMetadata createRef(ParserContext context, String value) {
        MutableRefMetadata m = context.createMetadata(MutableRefMetadata.class);
        m.setComponentId(value);
        return m;
    }

    private static IdRefMetadata createIdRef(ParserContext context, String value) {
        MutableIdRefMetadata m = context.createMetadata(MutableIdRefMetadata.class);
        m.setComponentId(value);
        return m;
    }
    
    private MutableReferenceMetadata createReference(ParserContext context, String value) {
        MutableReferenceMetadata m = context.createMetadata(MutableReferenceMetadata.class);
        // use the class instance directly rather than loading the class from the specified the interface name.
        m.setRuntimeInterface(PropertyEvaluator.class);
        m.setFilter("(org.apache.aries.blueprint.ext.evaluator.name=" + value + ")");
        m.setBundleContext(ctx);
        return m;
    }

    private static CollectionMetadata createList(ParserContext context, List<String> list) {
        MutableCollectionMetadata m = context.createMetadata(MutableCollectionMetadata.class);
        m.setCollectionClass(List.class);
        m.setValueType(String.class.getName());
        for (String v : list) {
            m.addValue(createValue(context, v, String.class.getName()));
        }
        return m;
    }

    private static String getTextValue(Element element) {
        StringBuffer value = new StringBuffer();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                value.append(item.getNodeValue());
            }
        }
        return value.toString();
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return (name.equals(node.getNodeName()) || name.equals(node.getLocalName()));
    }

    public static boolean isBlueprintNamespace(String ns) {
        return BLUEPRINT_NAMESPACE.equals(ns);
    }

}
