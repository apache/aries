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
import java.util.HashSet;
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
import org.w3c.dom.*;

public class SpringOsgiNamespaceHandler implements NamespaceHandler {

    public static final String BLUEPRINT_NAMESPACE = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    public static final String SPRING_NAMESPACE = "http://www.springframework.org/schema/beans";
    public static final String BEAN_ELEMENT = "bean";

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
        if ("reference".equals(element.getLocalName())) {
            MutableReferenceMetadata metadata = context.createMetadata(MutableReferenceMetadata.class);
            // Parse attributes
            if (element.hasAttribute("id")) {
                metadata.setId(element.getAttribute("id"));
            } else {
                metadata.setId(generateId(context));
            }
            metadata.setAvailability("0..1".equals(element.getAttribute("cardinality"))
                    ? ReferenceMetadata.AVAILABILITY_OPTIONAL
                    : ReferenceMetadata.AVAILABILITY_MANDATORY);
            metadata.setTimeout(getLong(element.getAttribute("timeout"), 300000));
            metadata.setInterface(element.getAttribute("interface"));
            metadata.setFilter(element.getAttribute("filter"));
            String[] dependsOn = StringUtils.tokenizeToStringArray(element.getAttribute("depends-on"), ",; ");
            metadata.setDependsOn(dependsOn != null ? Arrays.asList(dependsOn) : null);
            metadata.setComponentName(element.getAttribute("bean-name"));
            // TODO: @context-class-loader
            // Parse child elements
            for (Element child : getChildren(element)) {
                if (element.getNamespaceURI().equals(child.getNamespaceURI())) {
                    if ("interfaces".equals(child.getLocalName())) {
                        List<String> extra = new ArrayList<String>();
                        for (Element e : getChildren(child)) {
                            if ("value".equals(e.getLocalName())) {
                                extra.add(getTextValue(e));
                            } else {
                                // TODO: support other elements ?
                                throw new UnsupportedOperationException("Unsupported child: " + element.getLocalName());
                            }
                        }
                        metadata.setExtraInterfaces(extra);
                    }
                    else if ("listener".equals(child.getLocalName())) {
                        // TODO: listener
                        String bindMethod = nonEmpty(child.getAttribute("bind-method"));
                        String unbindMethod = nonEmpty(child.getAttribute("unbind-method"));
                        String refStr = nonEmpty(child.getAttribute("ref"));
                        Target listenerComponent = null;
                        if (refStr != null) {
                            MutableRefMetadata ref = context.createMetadata(MutableRefMetadata.class);
                            ref.setComponentId(refStr);
                            listenerComponent = ref;
                        }
                        for (Element cchild : getChildren(child)) {
                            if (BLUEPRINT_NAMESPACE.equals(cchild.getNamespaceURI())
                                    && BEAN_ELEMENT.equals(cchild.getLocalName())) {
                                if (listenerComponent != null) {
                                    throw new IllegalArgumentException("Only one of @ref attribute and bean element is allowed");
                                }
                                listenerComponent = context.parseElement(BeanMetadata.class, metadata, cchild);
                            }
                            else if (SPRING_NAMESPACE.equals(cchild.getNamespaceURI())
                                    && BEAN_ELEMENT.equals(cchild.getLocalName())) {
                                if (listenerComponent != null) {
                                    throw new IllegalArgumentException("Only one of @ref attribute or inlined bean definition element is allowed");
                                }
                                listenerComponent = (Target) context.getNamespaceHandler(URI.create(SPRING_NAMESPACE))
                                        .parse(cchild, context);
                            }
                            else {
                                throw new IllegalArgumentException("Unsupported element " + cchild.getLocalName());
                            }
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
        else if ("service".equals(element.getLocalName())) {
            MutableServiceMetadata metadata = context.createMetadata(MutableServiceMetadata.class);
            // Parse attributes
            if (element.hasAttribute("id")) {
                metadata.setId(element.getAttribute("id"));
            } else {
                metadata.setId(generateId(context));
            }
            if (nonEmpty(element.getAttribute("ref")) != null) {
                MutableRefMetadata ref = context.createMetadata(MutableRefMetadata.class);
                ref.setComponentId(element.getAttribute("ref"));
                metadata.setServiceComponent(ref);
            }
            metadata.setRanking(nonEmpty(element.getAttribute("ranking")) != null
                    ? Integer.parseInt(element.getAttribute("ranking"))
                    : 0);
            String itf = nonEmpty(element.getAttribute("interface"));
            if (itf != null) {
                metadata.addInterface(itf);
            }
            String[] dependsOn = StringUtils.tokenizeToStringArray(nonEmpty(element.getAttribute("depends-on")), ",; ");
            metadata.setDependsOn(dependsOn != null ? Arrays.asList(dependsOn) : null);
            String autoExp = nonEmpty(element.getAttribute("auto-export"));
            if ("interfaces".equals(autoExp)) {
                metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_INTERFACES);
            } else if ("class-hierarchy".equals(autoExp)) {
                metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY);
            } else if ("all-classes".equals(autoExp)) {
                metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
            } else {
                metadata.setAutoExport(ServiceMetadata.AUTO_EXPORT_DISABLED);
            }
            // TODO: @context-class-loader
            // Parse child elements
            for (Element child : getChildren(element)) {
                if (element.getNamespaceURI().equals(child.getNamespaceURI())) {
                    if ("interfaces".equals(child.getLocalName())) {
                        for (Element e : getChildren(child)) {
                            if ("value".equals(e.getLocalName())) {
                                metadata.addInterface(getTextValue(e));
                            } else {
                                // TODO: support other elements ?
                                throw new UnsupportedOperationException("Unsupported child: " + element.getLocalName());
                            }
                        }
                    }
                    else if ("registration-listener".equals(child.getLocalName())) {
                        // TODO: registration-listener

                    }
                    else if ("service-properties".equals(child.getLocalName())) {
                        // TODO: @key-type
                        for (Element e : getChildren(child)) {
                            if ("entry".equals(e.getLocalName())) {
                                NonNullMetadata key;
                                Metadata val;
                                boolean hasKeyAttribute = e.hasAttribute("key");
                                boolean hasKeyRefAttribute = e.hasAttribute("key-ref");
                                if (hasKeyRefAttribute && !hasKeyAttribute) {
                                    MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
                                    r.setComponentId(e.getAttribute("key-ref"));
                                    key = r;
                                } else if (hasKeyAttribute && !hasKeyRefAttribute) {
                                    MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
                                    v.setStringValue(e.getAttribute("key"));
                                    key = v;
                                } else {
                                    throw new IllegalStateException("Either key or key-ref must be specified");
                                }
                                // TODO: support children elements ?
                                boolean hasValAttribute = e.hasAttribute("value");
                                boolean hasValRefAttribute = e.hasAttribute("value-ref");
                                if (hasValRefAttribute && !hasValAttribute) {
                                    MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
                                    r.setComponentId(e.getAttribute("value-ref"));
                                    val = r;
                                } else if (hasValAttribute && !hasValRefAttribute) {
                                    MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
                                    v.setStringValue(e.getAttribute("value"));
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
                else if (SPRING_NAMESPACE.equals(child.getNamespaceURI())
                        && BEAN_ELEMENT.equals(child.getLocalName())) {
                    if (metadata.getServiceComponent() != null) {
                        throw new IllegalArgumentException("Only one of @ref attribute or inlined bean definition element is allowed");
                    }
                    Target bean = (Target) context.getNamespaceHandler(URI.create(SPRING_NAMESPACE))
                            .parse(child, context);
                    metadata.setServiceComponent(bean);
                }
                else {
                    throw new IllegalArgumentException("Unsupported element " + child.getLocalName());
                }
            }
            return metadata;
        }
        else if ("bundle".equals(element.getLocalName())) {

        }
        else if ("set".equals(element.getLocalName())) {

        }
        else if ("list".equals(element.getLocalName())) {

        }
        throw new UnsupportedOperationException();
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
        for(int i = 0; i < nl.getLength(); ++i) {
            Node item = nl.item(i);
            if(item instanceof CharacterData && !(item instanceof Comment) || item instanceof EntityReference) {
                sb.append(item.getNodeValue());
            }
        }
        return sb.toString();
    }
}
