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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SpringOsgiNamespaceHandler implements NamespaceHandler {

    public static final String BLUEPRINT_NAMESPACE = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    public static final String SPRING_NAMESPACE = "http://www.springframework.org/schema/beans";

    public static final String BEAN_ELEMENT = "bean";
    public static final String BEAN_NAME_ELEMENT = "bean-name";
    public static final String FILTER_ATTRIBUTE = "filter";
    public static final String INTERFACE_ATTRIBUTE = "interface";
    public static final String TIMEOUT_ATTRIBUTE = "timeout";
    public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";
    public static final String CARDINALITY_ATTRIBUTE = "cardinality";
    public static final String LISTENER_ELEMENT = "listener";
    public static final String REF_ATTRIBUTE = "ref";
    public static final String BIND_METHOD_ATTRIBUTE = "bind-method";
    public static final String UNBIND_METHOD_ATTRIBUTE = "unbind-method";
    public static final String ID_ATTRIBUTE = "id";
    public static final String CARDINALITY_0_1 = "0..1";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String VALUE_REF_ATTRIBUTE = "value-ref";
    public static final String KEY_ATTRIBUTE = "key";
    public static final String KEY_REF_ATTRIBUTE = "key-ref";
    public static final String ENTRY_ELEMENT = "entry";
    public static final String SERVICE_PROPERTIES_ELEMENT = "service-properties";
    public static final String REGISTRATION_LISTENER_ELEMENT = "registration-listener";
    public static final String INTERFACES_ELEMENT = "interfaces";
    public static final String VALUE_ELEMENT = "value";
    public static final String AUTO_EXPORT_ATTRIBUTE = "auto-export";
    public static final String AUTO_EXPORT_INTERFACES = "interfaces";
    public static final String AUTO_EXPORT_CLASS_HIERARCHY = "class-hierarchy";
    public static final String AUTO_EXPORT_ALL_CLASSES = "all-classes";
    public static final String RANKING_ATTRIBUTE = "ranking";
    public static final String REFERENCE_ELEMENT = "reference";
    public static final String SERVICE_ELEMENT = "service";
    public static final String BUNDLE_ELEMENT = "bundle";
    public static final String SET_ELEMENT = "set";
    public static final String LIST_ELEMENT = "list";
    public static final int DEFAULT_TIMEOUT = 300000;
    public static final String REGISTRATION_METHOD_ATTRIBUTE = "registration-method";
    public static final String UNREGISTRATION_METHOD_ATTRIBUTE = "unregistration-method";

    private int idCounter;

    @Override
    public URL getSchemaLocation(String namespace) {
        if (namespace.startsWith("http://www.springframework.org/schema/osgi/spring-osgi")) {
            String sub = namespace.substring("http://www.springframework.org/schema/osgi/".length());
            if ("spring-osgi.xsd".equals(sub)) {
                sub = "spring-osgi-1.2.xsd";
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
        if (REFERENCE_ELEMENT.equals(element.getLocalName())) {
            return parseReference(element, context);
        }
        else if (SERVICE_ELEMENT.equals(element.getLocalName())) {
            return parseService(element, context);
        }
        else if (BUNDLE_ELEMENT.equals(element.getLocalName())) {
            return parseBundle(element, context);
        }
        else if (SET_ELEMENT.equals(element.getLocalName())) {
            return parseSet(element, context);
        }
        else if (LIST_ELEMENT.equals(element.getLocalName())) {
            return parseList(element, context);
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    private Metadata parseBundle(Element element, ParserContext context) {
        throw new UnsupportedOperationException();
    }

    private Metadata parseList(Element element, ParserContext context) {
        // TODO: support list
        throw new UnsupportedOperationException();
    }

    private Metadata parseSet(Element element, ParserContext context) {
        // TODO: support set
        throw new UnsupportedOperationException();
    }

    private Metadata parseService(Element element, ParserContext context) {
        MutableServiceMetadata metadata = context.createMetadata(MutableServiceMetadata.class);
        // Parse attributes
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            metadata.setId(element.getAttribute(ID_ATTRIBUTE));
        } else {
            metadata.setId(generateId(context));
        }
        if (nonEmpty(element.getAttribute(REF_ATTRIBUTE)) != null) {
            MutableRefMetadata ref = context.createMetadata(MutableRefMetadata.class);
            ref.setComponentId(element.getAttribute(REF_ATTRIBUTE));
            metadata.setServiceComponent(ref);
        }
        metadata.setRanking(nonEmpty(element.getAttribute(RANKING_ATTRIBUTE)) != null
                ? Integer.parseInt(element.getAttribute(RANKING_ATTRIBUTE))
                : 0);
        String itf = nonEmpty(element.getAttribute(INTERFACE_ATTRIBUTE));
        if (itf != null) {
            metadata.addInterface(itf);
        }
        String[] dependsOn = StringUtils.tokenizeToStringArray(nonEmpty(element.getAttribute(DEPENDS_ON_ATTRIBUTE)), ",; ");
        metadata.setDependsOn(dependsOn != null ? Arrays.asList(dependsOn) : null);
        String autoExp = nonEmpty(element.getAttribute(AUTO_EXPORT_ATTRIBUTE));
        if (AUTO_EXPORT_INTERFACES.equals(autoExp)) {
            metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_INTERFACES);
        } else if (AUTO_EXPORT_CLASS_HIERARCHY.equals(autoExp)) {
            metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY);
        } else if (AUTO_EXPORT_ALL_CLASSES.equals(autoExp)) {
            metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
        } else {
            metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_DISABLED);
        }
        // TODO: @context-class-loader
        // Parse child elements
        for (Element child : getChildren(element)) {
            if (element.getNamespaceURI().equals(child.getNamespaceURI())) {
                if (INTERFACES_ELEMENT.equals(child.getLocalName())) {
                    List<String> itfs = parseInterfaces(child);
                    for (String intf : itfs) {
                        metadata.addInterface(intf);
                    }
                }
                else if (REGISTRATION_LISTENER_ELEMENT.equals(child.getLocalName())) {
                    String regMethod = nonEmpty(child.getAttribute(REGISTRATION_METHOD_ATTRIBUTE));
                    String unregMethod = nonEmpty(child.getAttribute(UNREGISTRATION_METHOD_ATTRIBUTE));
                    String refStr = nonEmpty(child.getAttribute(REF_ATTRIBUTE));
                    Target listenerComponent = null;
                    if (refStr != null) {
                        MutableRefMetadata ref = context.createMetadata(MutableRefMetadata.class);
                        ref.setComponentId(refStr);
                        listenerComponent = ref;
                    }
                    for (Element cchild : getChildren(child)) {
                        if (listenerComponent != null) {
                            throw new IllegalArgumentException("Only one of @ref attribute or inlined bean definition element is allowed");
                        }
                        listenerComponent = parseInlinedTarget(context, metadata, cchild);
                    }
                    if (listenerComponent == null) {
                        throw new IllegalArgumentException("Missing @ref attribute or inlined bean definition element");
                    }
                    metadata.addRegistrationListener(listenerComponent, regMethod, unregMethod);
                }
                else if (SERVICE_PROPERTIES_ELEMENT.equals(child.getLocalName())) {
                    // TODO: @key-type
                    for (Element e : getChildren(child)) {
                        if (ENTRY_ELEMENT.equals(e.getLocalName())) {
                            NonNullMetadata key;
                            Metadata val;
                            boolean hasKeyAttribute = e.hasAttribute(KEY_ATTRIBUTE);
                            boolean hasKeyRefAttribute = e.hasAttribute(KEY_REF_ATTRIBUTE);
                            if (hasKeyRefAttribute && !hasKeyAttribute) {
                                MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
                                r.setComponentId(e.getAttribute(KEY_REF_ATTRIBUTE));
                                key = r;
                            } else if (hasKeyAttribute && !hasKeyRefAttribute) {
                                MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
                                v.setStringValue(e.getAttribute(KEY_ATTRIBUTE));
                                key = v;
                            } else {
                                throw new IllegalStateException("Either key or key-ref must be specified");
                            }
                            // TODO: support key
                            boolean hasValAttribute = e.hasAttribute(VALUE_ATTRIBUTE);
                            boolean hasValRefAttribute = e.hasAttribute(VALUE_REF_ATTRIBUTE);
                            if (hasValRefAttribute && !hasValAttribute) {
                                MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
                                r.setComponentId(e.getAttribute(VALUE_REF_ATTRIBUTE));
                                val = r;
                            } else if (hasValAttribute && !hasValRefAttribute) {
                                MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
                                v.setStringValue(e.getAttribute(VALUE_ATTRIBUTE));
                                val = v;
                            } else {
                                throw new IllegalStateException("Either val or val-ref must be specified");
                            }
                            // TODO: support children elements ?
                            metadata.addServiceProperty(key, val);
                        }
                    }
                }
            }
            else if (BLUEPRINT_NAMESPACE.equals(child.getNamespaceURI())
                    && BEAN_ELEMENT.equals(child.getLocalName())) {
                if (metadata.getServiceComponent() != null) {
                    throw new IllegalArgumentException("Only one of @ref attribute and bean element is allowed");
                }
                Target bean = context.parseElement(BeanMetadata.class, metadata, child);
                metadata.setServiceComponent(bean);
            }
            else {
                if (metadata.getServiceComponent() != null) {
                    throw new IllegalArgumentException("Only one of @ref attribute or inlined bean definition element is allowed");
                }
                NamespaceHandler handler = context.getNamespaceHandler(URI.create(child.getNamespaceURI()));
                if (handler == null) {
                    throw new IllegalStateException("No NamespaceHandler found for " + child.getNamespaceURI());
                }
                Metadata md = handler.parse(child, context);
                if (!(md instanceof Target)) {
                    throw new IllegalStateException("NamespaceHandler did not return a Target instance but " + md);
                }
                metadata.setServiceComponent((Target) md);
            }
        }
        return metadata;
    }

    private Metadata parseReference(Element element, ParserContext context) {
        MutableReferenceMetadata metadata = context.createMetadata(MutableReferenceMetadata.class);
        // Parse attributes
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            metadata.setId(element.getAttribute(ID_ATTRIBUTE));
        } else {
            metadata.setId(generateId(context));
        }
        metadata.setAvailability(CARDINALITY_0_1.equals(element.getAttribute(CARDINALITY_ATTRIBUTE))
                ? ReferenceMetadata.AVAILABILITY_OPTIONAL
                : ReferenceMetadata.AVAILABILITY_MANDATORY);
        metadata.setTimeout(getLong(element.getAttribute(TIMEOUT_ATTRIBUTE), DEFAULT_TIMEOUT));
        metadata.setInterface(element.getAttribute(INTERFACE_ATTRIBUTE));
        metadata.setFilter(element.getAttribute(FILTER_ATTRIBUTE));
        String[] dependsOn = StringUtils.tokenizeToStringArray(element.getAttribute(DEPENDS_ON_ATTRIBUTE), ",; ");
        metadata.setDependsOn(dependsOn != null ? Arrays.asList(dependsOn) : null);
        metadata.setComponentName(element.getAttribute(BEAN_NAME_ELEMENT));
        // TODO: @context-class-loader
        // Parse child elements
        for (Element child : getChildren(element)) {
            if (element.getNamespaceURI().equals(child.getNamespaceURI())) {
                if (INTERFACES_ELEMENT.equals(child.getLocalName())) {
                    List<String> itfs = parseInterfaces(child);
                    metadata.setExtraInterfaces(itfs);
                }
                else if (LISTENER_ELEMENT.equals(child.getLocalName())) {
                    String bindMethod = nonEmpty(child.getAttribute(BIND_METHOD_ATTRIBUTE));
                    String unbindMethod = nonEmpty(child.getAttribute(UNBIND_METHOD_ATTRIBUTE));
                    String refStr = nonEmpty(child.getAttribute(REF_ATTRIBUTE));
                    Target listenerComponent = null;
                    if (refStr != null) {
                        MutableRefMetadata ref = context.createMetadata(MutableRefMetadata.class);
                        ref.setComponentId(refStr);
                        listenerComponent = ref;
                    }
                    for (Element cchild : getChildren(child)) {
                        if (listenerComponent != null) {
                            throw new IllegalArgumentException("Only one of @ref attribute or inlined bean definition element is allowed");
                        }
                        listenerComponent = parseInlinedTarget(context, metadata, cchild);
                    }
                    if (listenerComponent == null) {
                        throw new IllegalArgumentException("Missing @ref attribute or inlined bean definition element");
                    }
                    metadata.addServiceListener(listenerComponent, bindMethod, unbindMethod);
                }
            }
            else {
                throw new UnsupportedOperationException("Custom namespaces not supported");
            }
        }
        return metadata;
    }

    private Target parseInlinedTarget(ParserContext context, ComponentMetadata metadata, Element element) {
        Target listenerComponent;
        if (BLUEPRINT_NAMESPACE.equals(element.getNamespaceURI())
                && BEAN_ELEMENT.equals(element.getLocalName())) {
            listenerComponent = context.parseElement(BeanMetadata.class, metadata, element);
        }
        else {
            NamespaceHandler handler = context.getNamespaceHandler(URI.create(element.getNamespaceURI()));
            if (handler == null) {
                throw new IllegalStateException("No NamespaceHandler found for " + element.getNamespaceURI());
            }
            Metadata md = handler.parse(element, context);
            if (!(md instanceof Target)) {
                throw new IllegalStateException("NamespaceHandler did not return a Target instance but " + md);
            }
            listenerComponent = (Target) md;
        }
        return listenerComponent;
    }

    private List<String> parseInterfaces(Element element) {
        List<String> extra = new ArrayList<String>();
        for (Element e : getChildren(element)) {
            if (VALUE_ELEMENT.equals(e.getLocalName())) {
                extra.add(getTextValue(e));
            } else {
                // The schema support all kind of children for a list type
                // The type for the spring property is converted to a Class[] array
                // TODO: support other elements ?
                throw new UnsupportedOperationException("Unsupported child: " + element.getLocalName());
            }
        }
        return extra;
    }

    private String nonEmpty(String ref) {
        return ref != null && ref.isEmpty() ? null : ref;
    }

    private String generateId(ParserContext context) {
        String id;
        do {
            id = ".spring-osgi-" + ++idCounter;
        } while (context.getComponentDefinitionRegistry().containsComponentDefinition(id));
        return id;
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return component;
    }

    private List<Element> getChildren(Element element) {
        List<Element> children = new ArrayList<Element>();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) {
                children.add((Element) child);
            }
        }
        return children;
    }

    private long getLong(String str, long def) {
        if (str == null || str.isEmpty()) {
            return def;
        } else {
            return Long.parseLong(str);
        }
    }

    public static String getTextValue(Element valueEle) {
        Assert.notNull(valueEle, "Element must not be null");
        StringBuilder sb = new StringBuilder();
        NodeList nl = valueEle.getChildNodes();
        for(int i = 0, l = nl.getLength(); i < l; ++i) {
            Node item = nl.item(i);
            if(item instanceof CharacterData && !(item instanceof Comment) || item instanceof EntityReference) {
                sb.append(item.getNodeValue());
            }
        }
        return sb.toString();
    }
}
