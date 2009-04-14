/*
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
package org.apache.felix.blueprint.context;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;

import org.apache.felix.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.felix.blueprint.NamespaceHandlerRegistry;
import org.apache.felix.blueprint.reflect.ComponentValueImpl;
import org.apache.felix.blueprint.reflect.ListValueImpl;
import org.apache.felix.blueprint.reflect.LocalComponentMetadataImpl;
import org.apache.felix.blueprint.reflect.MapValueImpl;
import org.apache.felix.blueprint.reflect.ParameterSpecificationImpl;
import org.apache.felix.blueprint.reflect.PropertiesValueImpl;
import org.apache.felix.blueprint.reflect.PropertyInjectionMetadataImpl;
import org.apache.felix.blueprint.reflect.ReferenceValueImpl;
import org.apache.felix.blueprint.reflect.RegistrationListenerMetadataImpl;
import org.apache.felix.blueprint.reflect.ServiceExportComponentMetadataImpl;
import org.apache.felix.blueprint.reflect.ServiceReferenceComponentMetadataImpl;
import org.apache.felix.blueprint.reflect.SetValueImpl;
import org.apache.felix.blueprint.reflect.TypedStringValueImpl;
import org.apache.felix.blueprint.reflect.UnaryServiceReferenceComponentMetadataImpl;
import org.apache.felix.blueprint.reflect.BindingListenerMetadataImpl;
import org.apache.felix.blueprint.reflect.ComponentMetadataImpl;
import org.apache.felix.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadataImpl;
import org.apache.felix.blueprint.reflect.ArrayValueImpl;
import org.apache.felix.blueprint.convert.ConversionServiceImpl;
import org.osgi.service.blueprint.context.ComponentDefinitionException;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MapValue;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.PropertiesValue;
import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.Value;
import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.ArrayValue;
import org.xml.sax.InputSource;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class Parser {

    public static final String BLUEPRINT_NAMESPACE = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    public static final String BLUEPRINT_COMPENDIUM_NAMESPACE = "http://www.osgi.org/xmlns/blueprint-compendium/v1.0.0";

    public static final String COMPONENTS_ELEMENT = "components";
    public static final String DESCRIPTION_ELEMENT = "description";
    public static final String TYPE_CONVERTERS_ELEMENT = "type-converters";
    public static final String COMPONENT_ELEMENT = "component";
    public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";
    public static final String REF_ELEMENT = "ref";
    public static final String IDREF_ELEMENT = "idref";
    public static final String LIST_ELEMENT = "list";
    public static final String SET_ELEMENT = "set";
    public static final String MAP_ELEMENT = "map";
    public static final String ARRAY_ELEMENT = "array";
    public static final String PROPS_ELEMENT = "props";
    public static final String PROP_ELEMENT = "prop";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String NULL_ELEMENT = "null";
    public static final String VALUE_ELEMENT = "value";
    public static final String SERVICE_ELEMENT = "service";
    public static final String REFERENCE_ELEMENT = "reference";
    public static final String REFLIST_ELEMENT = "ref-list";
    public static final String REFSET_ELEMENT = "ref-set";
    public static final String INTERFACES_ELEMENT = "interfaces";
    public static final String LISTENER_ELEMENT = "listener";
    public static final String SERVICE_PROPERTIES_ELEMENT = "service-properties";
    public static final String REGISTRATION_LISTENER_ELEMENT = "registration-listener";
    public static final String ENTRY_ELEMENT = "entry";
    public static final String KEY_ELEMENT = "key";
    public static final String COMPARATOR_ELEMENT = "comparator";
    public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";
    public static final String DEFAULT_INIT_METHOD_ATTRIBUTE = "default-init-method";
    public static final String DEFAULT_DESTROY_METHOD_ATTRIBUTE = "default-destroy-method";
    public static final String DEFAULT_TIMEOUT_ATTRIBUTE = "default-timeout";
    public static final String DEFAULT_AVAILABILITY_ATTRIBUTE = "default-availability";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String ID_ATTRIBUTE = "id";
    public static final String CLASS_ATTRIBUTE = "class";
    public static final String PARENT_ATTRIBUTE = "parent";
    public static final String INDEX_ATTRIBUTE = "index";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String VALUE_REF_ATTRIBUTE = "value-ref";
    public static final String KEY_ATTRIBUTE = "key";
    public static final String KEY_REF_ATTRIBUTE = "key-ref";
    public static final String REF_ATTRIBUTE = "ref";
    public static final String COMPONENT_ATTRIBUTE = "component";
    public static final String INTERFACE_ATTRIBUTE = "interface";
    public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";
    public static final String AUTO_EXPORT_ATTRIBUTE = "auto-export";
    public static final String RANKING_ATTRIBUTE = "ranking";
    public static final String TIMEOUT_ATTRIBUTE = "timeout";
    public static final String FILTER_ATTRIBUTE = "filter";
    public static final String COMPONENT_NAME_ATTRIBUTE = "component-name";
    public static final String AVAILABILITY_ATTRIBUTE = "availability";
    public static final String REGISTRATION_METHOD_ATTRIBUTE = "registration-method";
    public static final String UNREGISTRATION_METHOD_ATTRIBUTE = "unregistration-method";
    public static final String BIND_METHOD_ATTRIBUTE = "bind-method";
    public static final String UNBIND_METHOD_ATTRIBUTE = "unbind-method";
    public static final String KEY_TYPE_ATTRIBUTE = "key-type";
    public static final String VALUE_TYPE_ATTRIBUTE = "value-type";
    public static final String COMPARATOR_REF_ATTRIBUTE = "comparator-ref";
    public static final String MEMBER_TYPE_ATTRIBUTE = "member-type";
    public static final String ORDERING_BASIS_ATTRIBUTE = "order-basis";
    public static final String SCOPE_ATTRIBUTE = "scope";
    public static final String INIT_METHOD_ATTRIBUTE = "init-method";
    public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
    public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

    public static final String BOOLEAN_DEFAULT = "default";
    public static final String BOOLEAN_TRUE = "true";
    public static final String BOOLEAN_FALSE = "false";

    public static final String AUTO_EXPORT_DISABLED = "disabled";
    public static final String AUTO_EXPORT_INTERFACES = "interfaces";
    public static final String AUTO_EXPORT_CLASS_HIERARCHY = "class-hierachy";
    public static final String AUTO_EXPORT_ALL = "all";
    public static final String AUTO_EXPORT_DEFAULT = AUTO_EXPORT_DISABLED;
    public static final String RANKING_DEFAULT = "0";
    public static final String AVAILABILITY_MANDATORY = "mandatory";
    public static final String AVAILABILITY_OPTIONAL = "optional";
    public static final String AVAILABILITY_DEFAULT = AVAILABILITY_MANDATORY;
    public static final String TIMEOUT_DEFAULT = "30000";
    public static final String MEMBER_TYPE_SERVICES = "service-instance";
    public static final String MEMBER_TYPE_SERVICE_REFERENCE = "service-reference";
    public static final String ORDERING_BASIS_SERVICES = "services";
    public static final String ODERING_BASIS_SERVICE_REFERENCES = "service-reference";
    public static final String LAZY_INIT_DEFAULT = BOOLEAN_FALSE;

    private DocumentBuilderFactory documentBuilderFactory;
    private ComponentDefinitionRegistryImpl registry;
    private ConversionService conversionService;
    private NamespaceHandlerRegistry namespaceHandlerRegistry;
    private int nameCounter;
    private String defaultTimeout;
    private String defaultAvailability;
    private String defaultLazyInit;
    private String defaultInitMethod;
    private String defaultDestroyMethod;

    public Parser() {
        registry = new ComponentDefinitionRegistryImpl();
        conversionService = new ConversionServiceImpl();
        // TODO: Register conversionService, bundle, bundleContext, moduleContext in the registry
    }

    public ComponentDefinitionRegistryImpl getRegistry() {
        return registry;
    }

    public void parse(URL[] urls) throws Exception {
        List<Document> documents = new ArrayList<Document>();
        // Load documents
        for (URL url : urls) {
            InputStream inputStream = url.openStream();
            try {
                InputSource inputSource = new InputSource(inputStream);
                DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
                Document doc = builder.parse(inputSource);
                documents.add(doc);
            } finally {
                inputStream.close();
            }
        }
        // Look for namespace handler requirements
        Set<String> namespaces = new HashSet<String>();
        for (Document doc : documents) {
            findNamespaces(namespaces, doc);
        }
        // TODO: check namespaces and wait for dependencies
        // Parse components
        for (Document doc : documents) {
            loadComponents(doc);
        }
    }

    private void findNamespaces(Set<String> namespaces, Node node) {
        if (node instanceof Element || node instanceof Attr) {
            String ns = node.getNamespaceURI();
            if (ns != null) {
                namespaces.add(ns);
            }
        }
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            findNamespaces(namespaces, nl.item(i));
        }
    }

    private void loadComponents(Document doc) {
        defaultTimeout = TIMEOUT_DEFAULT;
        defaultAvailability = AVAILABILITY_DEFAULT;
        defaultLazyInit = LAZY_INIT_DEFAULT;
        defaultInitMethod = null;
        defaultDestroyMethod = null;
        Element root = doc.getDocumentElement();
        if (!isBlueprintNamespace(root.getNamespaceURI()) ||
                !nodeNameEquals(root, COMPONENTS_ELEMENT)) {
            throw new ComponentDefinitionException("Root element must be {" + BLUEPRINT_NAMESPACE + "}" + COMPONENTS_ELEMENT + " element");
        }
        // Parse global attributes
        if (root.hasAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE)) {
            defaultLazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
        }
        if (root.hasAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE)) {
            defaultInitMethod = root.getAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE);
        }
        if (root.hasAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE)) {
            defaultDestroyMethod = root.getAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE);
        }
        if (root.hasAttribute(DEFAULT_TIMEOUT_ATTRIBUTE)) {
            defaultTimeout = root.getAttribute(DEFAULT_TIMEOUT_ATTRIBUTE);
        }
        if (root.hasAttribute(DEFAULT_AVAILABILITY_ATTRIBUTE)) {
            defaultAvailability = root.getAttribute(DEFAULT_AVAILABILITY_ATTRIBUTE);
        }
        // Parse custom attributes
        NodeList nl = root.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Attr && !isBlueprintNamespace(node.getNamespaceURI())) {
                decorateCustomNode(node, null);
            }
        }
        // Parse elements
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                String namespaceUri = element.getNamespaceURI();
                if (isBlueprintNamespace(namespaceUri)) {
                    parseBlueprintElement(element);
                } else if (isBlueprintCompendiumNamespace(namespaceUri)) {
                    parseBlueprintCompendiumElement(element);
                } else {
                    parseCustomElement(element, null);
                }
            }
        }
    }

    private void parseBlueprintElement(Element element) {
        if (nodeNameEquals(element, DESCRIPTION_ELEMENT)) {
            // Ignore description
        } else if (nodeNameEquals(element, TYPE_CONVERTERS_ELEMENT)) {
            parseTypeConverters(element);
        } else if (nodeNameEquals(element, COMPONENT_ELEMENT)) {
            LocalComponentMetadataImpl component = parseComponentMetadata(element);
            generateBeanNameIfNeeded(component);
            registry.registerComponentDefinition(component);
        } else if (nodeNameEquals(element, SERVICE_ELEMENT)) {
            ServiceExportComponentMetadataImpl service = parseService(element);
            generateBeanNameIfNeeded(service);
            registry.registerComponentDefinition(service);
        } else if (nodeNameEquals(element, REFERENCE_ELEMENT)) {
            ServiceReferenceComponentMetadataImpl reference = parseUnaryReference(element);
            generateBeanNameIfNeeded(reference);
            registry.registerComponentDefinition(reference);
        } else if (nodeNameEquals(element, REFLIST_ELEMENT) ) {
            CollectionBasedServiceReferenceComponentMetadataImpl references = parseReferenceCollection(element);
            generateBeanNameIfNeeded(references);
            registry.registerComponentDefinition(references);
        } else if (nodeNameEquals(element, REFSET_ELEMENT)) {
            CollectionBasedServiceReferenceComponentMetadataImpl references = parseReferenceCollection(element);
            generateBeanNameIfNeeded(references);
            registry.registerComponentDefinition(references);
        } else {
            throw new ComponentDefinitionException("Unknown element " + element.getNodeName() + " in namespace " + BLUEPRINT_NAMESPACE);
        }
    }

    private void parseBlueprintCompendiumElement(Element element) {
        // TODO
        throw new ComponentDefinitionException("Unknown element " + element.getNodeName() + " in namespace " + BLUEPRINT_COMPENDIUM_NAMESPACE);
    }

    private void parseTypeConverters(Element element) {
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, COMPONENT_ELEMENT)) {
                    LocalComponentMetadata metadata = parseComponentMetadata(e);
                    registry.registerTypeConverter(new ComponentValueImpl(metadata));
                } else if (nodeNameEquals(e, REF_ELEMENT)) {
                    String componentName = e.getAttribute(COMPONENT_ATTRIBUTE);
                    registry.registerTypeConverter(new ReferenceValueImpl(componentName));
                }
            }
        }
    }

    private LocalComponentMetadataImpl parseComponentMetadata(Element element) {
        LocalComponentMetadataImpl metadata = new LocalComponentMetadataImpl();
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            metadata.setName(element.getAttribute(ID_ATTRIBUTE));
        }
        if (element.hasAttribute(CLASS_ATTRIBUTE)) {
            metadata.setClassName(element.getAttribute(CLASS_ATTRIBUTE));
        }
        if (element.hasAttribute(SCOPE_ATTRIBUTE)) {
            metadata.setScope(element.getAttribute(SCOPE_ATTRIBUTE));
        }
        String lazy = element.hasAttribute(LAZY_INIT_ATTRIBUTE) ? element.getAttribute(LAZY_INIT_ATTRIBUTE) : defaultLazyInit;
        if (BOOLEAN_DEFAULT.equals(lazy)) {
            if (BOOLEAN_DEFAULT.equals(defaultLazyInit)) {
                lazy = BOOLEAN_FALSE;
            }
        }
        if (BOOLEAN_TRUE.equals(lazy)) {
            metadata.setLazy(true);
        } else if (BOOLEAN_FALSE.equals(lazy)) {
            metadata.setLazy(false);
        } else {
            throw new ComponentDefinitionException("Attribute " + LAZY_INIT_ATTRIBUTE + " must be equals to " + BOOLEAN_DEFAULT + ", " + BOOLEAN_TRUE + " or " + BOOLEAN_FALSE);
        }
        if (element.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            metadata.setDependsOn(element.getAttribute(DEPENDS_ON_ATTRIBUTE));
        }
        if (element.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
            metadata.setInitMethodName(element.getAttribute(INIT_METHOD_ATTRIBUTE));
        } else {
            metadata.setInitMethodName(defaultInitMethod);
        }
        if (element.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
            metadata.setInitMethodName(element.getAttribute(DESTROY_METHOD_ATTRIBUTE));
        } else {
            metadata.setInitMethodName(defaultDestroyMethod);
        }
        // Parse custom elements and attributes
        int indexConstructor = 0;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isBlueprintNamespace(node.getNamespaceURI())) {
                if (node instanceof Element) {
                    if (nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
                        // TODO: all indexes attributes must be set or none according to the spec
                        Element e = (Element) node;
                        int index = e.hasAttribute(INDEX_ATTRIBUTE) ? Integer.parseInt(e.getAttribute(INDEX_ATTRIBUTE)) : indexConstructor;
                        String type = e.hasAttribute(TYPE_ATTRIBUTE) ? e.getAttribute(TYPE_ATTRIBUTE) : null;
                        Value value = parseValue(e, metadata);
                        metadata.addConsuctorArg(new ParameterSpecificationImpl(value, type, index));
                        indexConstructor++;
                    } else if (nodeNameEquals(node, PROPERTY_ELEMENT)) {
                        Element e = (Element) node;
                        String name = e.hasAttribute(NAME_ATTRIBUTE) ? e.getAttribute(NAME_ATTRIBUTE) : null;
                        Value value = parseValue(e, metadata);
                        metadata.addProperty(new PropertyInjectionMetadataImpl(name, value));
                    }
                }
            } else {
                if (node instanceof Element) {
                    decorateCustomNode(node, metadata);
                } else if (node instanceof Attr) {
                    decorateCustomNode(node, metadata);
                }
            }
        }
        return metadata;
    }

    private ServiceExportComponentMetadataImpl parseService(Element element) {
        ServiceExportComponentMetadataImpl service = new ServiceExportComponentMetadataImpl();
        // Parse attributes
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            service.setName(element.getAttribute(ID_ATTRIBUTE));
        }
        if (element.hasAttribute(INTERFACE_ATTRIBUTE)) {
            service.setInterfaceNames(Collections.singleton(element.getAttribute(INTERFACE_ATTRIBUTE)));
        }
        if (element.hasAttribute(REF_ATTRIBUTE)) {
            service.setExportedComponent(new ReferenceValueImpl(element.getAttribute(REF_ATTRIBUTE)));
        }
        if (element.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            service.setDependsOn(element.getAttribute(DEPENDS_ON_ATTRIBUTE));
        }
        String autoExport = element.hasAttribute(AUTO_EXPORT_ATTRIBUTE) ? element.getAttribute(AUTO_EXPORT_ATTRIBUTE) : AUTO_EXPORT_DEFAULT;
        if (AUTO_EXPORT_DISABLED.equals(autoExport)) {
            service.setAutoExportMode(ServiceExportComponentMetadata.EXPORT_MODE_DISABLED);
        } else if (AUTO_EXPORT_INTERFACES.equals(autoExport)) {
            service.setAutoExportMode(ServiceExportComponentMetadata.EXPORT_MODE_INTERFACES);
        } else if (AUTO_EXPORT_CLASS_HIERARCHY.equals(autoExport)) {
            service.setAutoExportMode(ServiceExportComponentMetadata.EXPORT_MODE_CLASS_HIERARCHY);
        } else if (AUTO_EXPORT_ALL.equals(autoExport)) {
            service.setAutoExportMode(ServiceExportComponentMetadata.EXPORT_MODE_ALL);
        } else {
            throw new ComponentDefinitionException("Illegal value for " + AUTO_EXPORT_ATTRIBUTE + " attribute");
        }
        String ranking = element.hasAttribute(RANKING_ATTRIBUTE) ? element.getAttribute(RANKING_ATTRIBUTE) : RANKING_DEFAULT;
        try {
            service.setRanking(Integer.parseInt(ranking));
        } catch (NumberFormatException e) {
            throw new ComponentDefinitionException("Attribute " + RANKING_ATTRIBUTE + " must be a valid integer (was: " + ranking + ")");
        }
        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, INTERFACES_ELEMENT)) {
                        if (service.getInterfaceNames() != null) {
                            throw new ComponentDefinitionException("Only one of " + INTERFACE_ATTRIBUTE + " attribute or " + INTERFACES_ELEMENT + " element must be used");
                        }
                        service.setInterfaceNames(parseInterfaceNames(e));
                    } else if (nodeNameEquals(e, SERVICE_PROPERTIES_ELEMENT)) {
                        service.setServicePropertiesValue(parseMap(e, service));
                    } else if (nodeNameEquals(e, REGISTRATION_LISTENER_ELEMENT)) {
                        service.addRegistrationListener(parseRegistrationListener(e));
                    } else if (nodeNameEquals(e, COMPONENT_ELEMENT)) {
                        if (service.getExportedComponent() != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + COMPONENT_ELEMENT + " element or custom inner element can be set");
                        }
                        service.setExportedComponent(new ComponentValueImpl(parseComponentMetadata(e)));
                    }
                } else {
                    if (service.getExportedComponent() != null) {
                        throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + COMPONENT_ELEMENT + " element or custom inner element can be set");
                    }
                    service.setExportedComponent(new ComponentValueImpl(parseCustomElement(e, service)));
                }
            }
        }
        // Check service
        if (service.getExportedComponent() == null) {
            throw new ComponentDefinitionException("One of " + REF_ATTRIBUTE + " attribute, " + COMPONENT_ELEMENT + " element or custom inner element must be set");
        }
        return service;
    }

    private ArrayValue parseArray(Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String valueType = element.hasAttribute(VALUE_TYPE_ATTRIBUTE) ? element.getAttribute(VALUE_TYPE_ATTRIBUTE) : null;
        // Parse elements
        List<Value> list = new ArrayList<Value>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Value val = parseValueElement((Element) node, enclosingComponent);
                list.add(val);
            }
        }
        return new ArrayValueImpl(valueType, list.toArray(new Value[list.size()]));
    }

    private ListValue parseList(Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String valueType = element.hasAttribute(VALUE_TYPE_ATTRIBUTE) ? element.getAttribute(VALUE_TYPE_ATTRIBUTE) : null;
        // Parse elements
        List<Value> list = new ArrayList<Value>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Value val = parseValue((Element) node, enclosingComponent);
                list.add(val);
            }
        }
        return new ListValueImpl(valueType, list);
    }

    private SetValue parseSet(Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String valueType = element.hasAttribute(VALUE_TYPE_ATTRIBUTE) ? element.getAttribute(VALUE_TYPE_ATTRIBUTE) : null;
        // Parse elements
        Set<Value> set = new HashSet<Value>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Value val = parseValue((Element) node, enclosingComponent);
                set.add(val);
            }
        }
        return new SetValueImpl(valueType, set);
    }

    private PropertiesValue parseProps(Element element) {
        // Parse elements
        Properties props = new Properties();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, PROP_ELEMENT)) {
                    String key = e.getAttribute(KEY_ATTRIBUTE);
                    String val;
                    if (e.hasAttribute(VALUE_ATTRIBUTE)) {
                        val = e.getAttribute(VALUE_ATTRIBUTE);
                    } else {
                        val = getTextValue(e);
                    }
                    props.setProperty(key, val);
                } else {
                    throw new ComponentDefinitionException("Unknown element " + e.getNodeName());
                }

            }
        }
        return new PropertiesValueImpl(props);
    }

    private MapValue parseMap(Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String keyType = element.hasAttribute(KEY_TYPE_ATTRIBUTE) ? element.getAttribute(KEY_TYPE_ATTRIBUTE) : null;
        String valueType = element.hasAttribute(VALUE_TYPE_ATTRIBUTE) ? element.getAttribute(VALUE_TYPE_ATTRIBUTE) : null;
        // Parse elements
        Map<Object, Object> map = new HashMap<Object, Object>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, ENTRY_ELEMENT)) {
                    parseMapEntry(map, e, enclosingComponent);
                }
            }
        }
        return new MapValueImpl(keyType, valueType, map);
    }

    private void parseMapEntry(Map<Object, Object> map, Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String key = element.hasAttribute(KEY_ATTRIBUTE) ? element.getAttribute(KEY_ATTRIBUTE) : null;
        String keyRef = element.hasAttribute(KEY_REF_ATTRIBUTE) ? element.getAttribute(KEY_REF_ATTRIBUTE) : null;
        String value = element.hasAttribute(VALUE_ATTRIBUTE) ? element.getAttribute(VALUE_ATTRIBUTE) : null;
        String valueRef = element.hasAttribute(VALUE_REF_ATTRIBUTE) ? element.getAttribute(VALUE_REF_ATTRIBUTE) : null;
        // Parse elements
        Value keyValue = null;
        Value valValue = null;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, KEY_ELEMENT)) {
                    keyValue = parseValueElement(e, enclosingComponent);
                } else {
                    valValue = parseValueElement(e, enclosingComponent);
                }
            }
        }
        // Check key
        if (keyValue != null && (key != null || keyRef != null) || (keyValue == null && key == null && keyRef == null)) {
            throw new ComponentDefinitionException("Only and only one of " + KEY_ATTRIBUTE + " attribute, " + KEY_REF_ATTRIBUTE + " attribute or " + KEY_ELEMENT + " element must be set");
        } else if (keyValue == null && key != null) {
            keyValue = new TypedStringValueImpl(key);
        } else if (keyValue == null /*&& keyRef != null*/) {
            keyValue = new ReferenceValueImpl(keyRef);
        }
        // Check value
        if (valValue != null && (value != null || valueRef != null) || (valValue == null && value == null && valueRef == null)) {
            throw new ComponentDefinitionException("Only and only one of " + VALUE_ATTRIBUTE + " attribute, " + VALUE_REF_ATTRIBUTE + " attribute or sub element must be set");
        } else if (valValue == null && value != null) {
            valValue = new TypedStringValueImpl(value);
        } else if (valValue == null /*&& valueRef != null*/) {
            valValue = new ReferenceValueImpl(valueRef);
        }
        map.put(keyValue, valValue);
    }

    private RegistrationListenerMetadata parseRegistrationListener(Element element) {
        // Parse attributes
        String ref = element.hasAttribute(REF_ATTRIBUTE) ? element.getAttribute(REF_ATTRIBUTE) : null;
        String registrationMethod = element.getAttribute(REGISTRATION_METHOD_ATTRIBUTE);
        String unregistrationMethod = element.getAttribute(UNREGISTRATION_METHOD_ATTRIBUTE);
        // Parse elements
        Value listenerComponent = null;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, REF_ELEMENT)) {
                    String component = e.getAttribute(COMPONENT_ATTRIBUTE);
                    if (component == null || component.length() == 0) {
                        throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ATTRIBUTE + " attribute");
                    }
                    listenerComponent = new ReferenceValueImpl(component);
                } else if (nodeNameEquals(e, COMPONENT_ELEMENT)) {
                    LocalComponentMetadataImpl component = parseComponentMetadata(e);
                    generateBeanNameIfNeeded(component);
                    listenerComponent = new ComponentValueImpl(component);
                } else if (nodeNameEquals(e, REFERENCE_ELEMENT)) {
                    ServiceReferenceComponentMetadataImpl reference = parseUnaryReference(e);
                    generateBeanNameIfNeeded(reference);
                    listenerComponent = new ComponentValueImpl(reference);
                } else if (nodeNameEquals(e, SERVICE_ELEMENT)) {
                    ServiceExportComponentMetadataImpl service = parseService(e);
                    generateBeanNameIfNeeded(service);
                    listenerComponent = new ComponentValueImpl(service);
                }
            }
        }
        if (listenerComponent != null && ref != null) {
            throw new ComponentDefinitionException("Attribute " + REF_ATTRIBUTE + " can not be set in addition to a child element");
        } else if (listenerComponent == null && ref != null) {
            listenerComponent = new ReferenceValueImpl(ref);
        }
        RegistrationListenerMetadataImpl listener = new RegistrationListenerMetadataImpl();
        listener.setListenerComponent(listenerComponent);
        if (registrationMethod == null || registrationMethod.length() == 0) {
            throw new ComponentDefinitionException("Attribute " + REGISTRATION_METHOD_ATTRIBUTE + " must be set");
        }
        listener.setRegistrationMethodName(registrationMethod);
        if (unregistrationMethod == null || unregistrationMethod.length() == 0) {
            throw new ComponentDefinitionException("Attribute " + UNREGISTRATION_METHOD_ATTRIBUTE + " must be set");
        }
        listener.setUnregistrationMethodName(unregistrationMethod);
        return listener;
    }

    private ServiceReferenceComponentMetadataImpl parseUnaryReference(Element element) {
        UnaryServiceReferenceComponentMetadataImpl reference = new UnaryServiceReferenceComponentMetadataImpl();
        parseReference(element, reference);
        String timeout = element.hasAttribute(TIMEOUT_ATTRIBUTE) ? element.getAttribute(TIMEOUT_ATTRIBUTE) : this.defaultTimeout;
        try {
            reference.setTimeout(Long.parseLong(timeout));
        } catch (NumberFormatException e) {
            throw new ComponentDefinitionException("Attribute " + TIMEOUT_ATTRIBUTE + " must be a valid long (was: " + timeout + ")");
        }
        return reference;
    }

    private CollectionBasedServiceReferenceComponentMetadataImpl parseReferenceCollection(Element element) {
        CollectionBasedServiceReferenceComponentMetadataImpl references = new CollectionBasedServiceReferenceComponentMetadataImpl();
        if (nodeNameEquals(element, REFLIST_ELEMENT)) {
            references.setCollectionType(List.class);
        } else /*if (nodeNameEquals(element, REFSET_ELEMENT))*/ {
            references.setCollectionType(Set.class);
        }
        if (element.hasAttribute(COMPARATOR_REF_ATTRIBUTE)) {
            references.setComparator(new ReferenceValueImpl(element.getAttribute(COMPARATOR_REF_ATTRIBUTE)));
        }
        if (element.hasAttribute(MEMBER_TYPE_ATTRIBUTE)) {
            String memberType = element.getAttribute(MEMBER_TYPE_ATTRIBUTE);
            if (MEMBER_TYPE_SERVICES.equals(memberType)) {
                references.setMemberType(CollectionBasedServiceReferenceComponentMetadata.MEMBER_TYPE_SERVICES);
            } else if (MEMBER_TYPE_SERVICE_REFERENCE.equals(memberType)) {
                references.setMemberType(CollectionBasedServiceReferenceComponentMetadata.MEMBER_TYPE_SERVICE_REFERENCES);
            }
        }
        if (element.hasAttribute(ORDERING_BASIS_ATTRIBUTE)) {
            String ordering = element.getAttribute(ORDERING_BASIS_ATTRIBUTE);
            if (ORDERING_BASIS_SERVICES.equals(ordering)) {
                references.setOrderingComparisonBasis(CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICES);
            } else if (ODERING_BASIS_SERVICE_REFERENCES.equals(ordering)) {
                references.setOrderingComparisonBasis(CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICE_REFERENCES);
            }
        }
        parseReference(element, references);
        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, COMPARATOR_ELEMENT)) {
                        parseComparator(e, references);
                    }
                }
            }
        }
        return references;
    }

    private void parseComparator(Element element, CollectionBasedServiceReferenceComponentMetadataImpl references) {
        Value comparator = null;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, REF_ELEMENT)) {
                        String component = e.getAttribute(COMPONENT_ATTRIBUTE);
                        if (component == null || component.length() == 0) {
                            throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ATTRIBUTE + " attribute");
                        }
                        comparator = new ReferenceValueImpl(component);
                    } else if (nodeNameEquals(e, COMPONENT_ELEMENT)) {
                        LocalComponentMetadataImpl component = parseComponentMetadata(e);
                        generateBeanNameIfNeeded(component);
                        comparator = new ComponentValueImpl(component);
                    } else if (nodeNameEquals(e, REFERENCE_ELEMENT)) {
                        ServiceReferenceComponentMetadataImpl reference = parseUnaryReference(e);
                        generateBeanNameIfNeeded(reference);
                        comparator = new ComponentValueImpl(reference);
                    } else if (nodeNameEquals(e, SERVICE_ELEMENT)) {
                        ServiceExportComponentMetadataImpl service = parseService(e);
                        generateBeanNameIfNeeded(service);
                        comparator = new ComponentValueImpl(service);
                    }
                } else {
                    ComponentMetadata custom = parseCustomElement(e, references);
                    comparator = new ComponentValueImpl(custom);
                }
            }
        }
        if (comparator == null) {
            throw new ComponentDefinitionException("One of " + REF_ELEMENT + ", " + COMPONENTS_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element is required");
        }
        references.setComparator(comparator);
    }

    private void parseReference(Element element, ServiceReferenceComponentMetadataImpl reference) {
        // Parse attributes
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            reference.setName(element.getAttribute(ID_ATTRIBUTE));
        }
        if (element.hasAttribute(INTERFACE_ATTRIBUTE)) {
            reference.setInterfaceNames(Collections.singleton(element.getAttribute(INTERFACE_ATTRIBUTE)));
        }
        if (element.hasAttribute(FILTER_ATTRIBUTE)) {
            reference.setFilter(element.getAttribute(FILTER_ATTRIBUTE));
        }
        if (element.hasAttribute(COMPONENT_NAME_ATTRIBUTE)) {
            reference.setComponentName(element.getAttribute(COMPONENT_NAME_ATTRIBUTE));
        }
        String availability = element.hasAttribute(AVAILABILITY_ATTRIBUTE) ? element.getAttribute(AVAILABILITY_ATTRIBUTE) : defaultAvailability;
        if (AVAILABILITY_MANDATORY.equals(availability)) {
            reference.setServiceAvailabilitySpecification(ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY);
        } else if (AVAILABILITY_OPTIONAL.equals(availability)) {
            reference.setServiceAvailabilitySpecification(ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY);
        } else {
            throw new ComponentDefinitionException("Illegal value for " + AVAILABILITY_ATTRIBUTE + " attribute: " + availability);
        }
        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, INTERFACES_ELEMENT)) {
                        if (reference.getInterfaceNames() != null) {
                            throw new ComponentDefinitionException("Only one of " + INTERFACE_ATTRIBUTE + " attribute or " + INTERFACES_ELEMENT + " element must be used");
                        }
                        reference.setInterfaceNames(parseInterfaceNames(e));
                    } else if (nodeNameEquals(e, LISTENER_ELEMENT)) {
                        reference.addBindingListener(parseBindingListener(e, reference));
                    }
                }
            }
        }
    }

    private BindingListenerMetadata parseBindingListener(Element element, ComponentMetadata enclosingComponent) {
        BindingListenerMetadataImpl listener = new BindingListenerMetadataImpl();
        // Parse attributes
        if (element.hasAttribute(REF_ATTRIBUTE)) {
            listener.setListenerComponent(new ReferenceValueImpl(element.getAttribute(REF_ATTRIBUTE)));
        }
        listener.setBindMethodName(element.getAttribute(BIND_METHOD_ATTRIBUTE));
        listener.setUnbindMethodName(element.getAttribute(UNBIND_METHOD_ATTRIBUTE));
        // Parse elements
        Value listenerComponent = null;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, REF_ELEMENT)) {
                        if (listener.getListenerComponent() != null) {
                            throw new ComponentDefinitionException("Attribute " + REF_ATTRIBUTE + " can not be set in addition to a child element");
                        }
                        String component = e.getAttribute(COMPONENT_ATTRIBUTE);
                        if (component == null || component.length() == 0) {
                            throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ATTRIBUTE + " attribute");
                        }
                        listener.setListenerComponent(new ReferenceValueImpl(component));
                    } else if (nodeNameEquals(e, COMPONENT_ELEMENT)) {
                        if (listener.getListenerComponent() != null) {
                            throw new ComponentDefinitionException("Attribute " + REF_ATTRIBUTE + " can not be set in addition to a child element");
                        }
                        LocalComponentMetadataImpl component = parseComponentMetadata(e);
                        generateBeanNameIfNeeded(component);
                        listener.setListenerComponent(new ComponentValueImpl(component));
                    } else if (nodeNameEquals(e, REFERENCE_ELEMENT)) {
                        if (listener.getListenerComponent() != null) {
                            throw new ComponentDefinitionException("Attribute " + REF_ATTRIBUTE + " can not be set in addition to a child element");
                        }
                        ServiceReferenceComponentMetadataImpl reference = parseUnaryReference(e);
                        generateBeanNameIfNeeded(reference);
                        listener.setListenerComponent(new ComponentValueImpl(reference));
                    } else if (nodeNameEquals(e, SERVICE_ELEMENT)) {
                        if (listener.getListenerComponent() != null) {
                            throw new ComponentDefinitionException("Attribute " + REF_ATTRIBUTE + " can not be set in addition to a child element");
                        }
                        ServiceExportComponentMetadataImpl service = parseService(e);
                        generateBeanNameIfNeeded(service);
                        listener.setListenerComponent(new ComponentValueImpl(service));
                    }
                } else {
                    if (listener.getListenerComponent() != null) {
                        throw new ComponentDefinitionException("Attribute " + REF_ATTRIBUTE + " can not be set in addition to a child element");
                    }
                    ComponentMetadata custom = parseCustomElement(e, enclosingComponent);
                    listener.setListenerComponent(new ComponentValueImpl(custom));
                }
            }
        }
        return listener;
    }

    private Set<String> parseInterfaceNames(Element element) {
        Set<String> interfaceNames = new HashSet<String>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, VALUE_ELEMENT)) {
                    interfaceNames.add(getTextValue(e));
                } else {
                    // TODO: RFC0124 seems to allow other elements here ... is that a bug ?
                    throw new ComponentDefinitionException("Unsupported element " + e.getNodeName() + " inside an " + INTERFACES_ELEMENT + " element");
                }
            }
        }
        return interfaceNames;
    }

    private Value parseValue(Element element, ComponentMetadata enclosingComponent) {
        if (element.hasAttribute(REF_ATTRIBUTE)) {
            return new ReferenceValueImpl(element.getAttribute(REF_ATTRIBUTE));
        } else if (element.hasAttribute(VALUE_ATTRIBUTE)) {
            return new TypedStringValueImpl(element.getAttribute(VALUE_ATTRIBUTE));
        } else {
            NodeList nl = element.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    if (isBlueprintNamespace(node.getNamespaceURI())) {
                        // Ignore description elements
                        if (!nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
                            return parseValueElement((Element) node, enclosingComponent);
                        }
                    } else {
                        ComponentMetadata innerComponent = parseCustomElement((Element) node, enclosingComponent);
                        return new ComponentValueImpl(innerComponent);
                    }
                }
            }
        }
        throw new ComponentDefinitionException("One of " + REF_ATTRIBUTE + " attribute, " + VALUE_ATTRIBUTE + " attribute or sub element must be set");
    }

    private Value parseValueElement(Element element, ComponentMetadata enclosingComponent) {
        if (nodeNameEquals(element, COMPONENT_ELEMENT)) {
            LocalComponentMetadata inner = parseComponentMetadata(element);
            return new ComponentValueImpl(inner);
        } else if (nodeNameEquals(element, NULL_ELEMENT)) {
            return NullValue.NULL;
        } else if (nodeNameEquals(element, VALUE_ELEMENT)) {
            String type = null;
            if (element.hasAttribute(TYPE_ATTRIBUTE)) {
                type = element.getAttribute(TYPE_ATTRIBUTE);
            }
            return new TypedStringValueImpl(getTextValue(element), type);
        } else if (nodeNameEquals(element, REF_ELEMENT)) {
            String component = element.getAttribute(COMPONENT_ATTRIBUTE);
            if (component == null || component.length() == 0) {
                throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ATTRIBUTE + " attribute");
            }
            return new ReferenceValueImpl(component);
        } else if (nodeNameEquals(element, IDREF_ELEMENT)) {
            String component = element.getAttribute(COMPONENT_ATTRIBUTE);
            if (component == null || component.length() == 0) {
                throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ATTRIBUTE + " attribute");
            }
            return new ReferenceValueImpl(component);
        } else if (nodeNameEquals(element, LIST_ELEMENT)) {
            return parseList(element, enclosingComponent);
        } else if (nodeNameEquals(element, SET_ELEMENT)) {
            return parseSet(element, enclosingComponent);
        } else if (nodeNameEquals(element, MAP_ELEMENT)) {
            return parseMap(element, enclosingComponent);
        } else if (nodeNameEquals(element, PROPS_ELEMENT)) {
            return parseProps(element);
        } else if (nodeNameEquals(element, ARRAY_ELEMENT)) {
            return parseArray(element, enclosingComponent);
        } else {
            throw new ComponentDefinitionException("Unknown element " + element.getNodeName());
        }
    }

    private void decorateCustomNode(Node node, ComponentMetadata enclosingComponent) {
        // TODO: decorate custom nodes
    }

    private ComponentMetadata parseCustomElement(Element element, ComponentMetadata enclosingComponent) {
        // TODO: parse custom elements
        throw new ComponentDefinitionException("Unsupported custom element");
    }

    private boolean isBlueprintNamespace(String ns) {
        return BLUEPRINT_NAMESPACE.equals(ns);
    }

    private boolean isBlueprintCompendiumNamespace(String ns) {
        return BLUEPRINT_COMPENDIUM_NAMESPACE.equals(ns);
    }

    private DocumentBuilderFactory getDocumentBuilderFactory() {
        if (documentBuilderFactory == null) {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
        }
        return documentBuilderFactory;
    }

    private void generateBeanNameIfNeeded(ComponentMetadataImpl component) {
        if (component.getName() == null) {
            component.setName("component-" + ++nameCounter);
        }
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return (name.equals(node.getNodeName()) || name.equals(node.getLocalName()));
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
}
