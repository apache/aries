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
package org.apache.aries.transaction.parsing;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TxNamespaceHandler implements NamespaceHandler {
    public static final String ANNOTATION_PARSER_BEAN_NAME = ".org_apache_aries_transaction_annotations";
    private static final Logger LOGGER = LoggerFactory.getLogger(TxNamespaceHandler.class);
    private TransactionManager tm;
    private Coordinator coordinator;

    private final Map<String, String> schemaMap;
    
    public TxNamespaceHandler() {
        schemaMap = new HashMap<String, String>();
        schemaMap.put("http://aries.apache.org/xmlns/transactions/v1.0.0", "transactionv10.xsd");
        schemaMap.put("http://aries.apache.org/xmlns/transactions/v1.1.0", "transactionv11.xsd");
        schemaMap.put("http://aries.apache.org/xmlns/transactions/v1.2.0", "transactionv12.xsd");
    }

    private void parseElement(Element elt, ComponentMetadata cm, ParserContext pc)
    {
        LOGGER.debug("parser asked to parse element {} ", elt.getNodeName());

        ComponentDefinitionRegistry cdr = pc.getComponentDefinitionRegistry();
        if ("enable-annotations".equals(elt.getLocalName())) {
            Node n = elt.getChildNodes().item(0);
            if (n == null || Boolean.parseBoolean(n.getNodeValue())) {
                //We need to register a bean processor to add annotation-based config
                if (!cdr.containsComponentDefinition(ANNOTATION_PARSER_BEAN_NAME)) {
                    LOGGER.debug("Enabling annotation based transactions");
                    MutableBeanMetadata meta = createAnnotationParserBean(pc, cdr);
                    cdr.registerComponentDefinition(meta);
                }
            }
        }
    }

    private MutableBeanMetadata createAnnotationParserBean(ParserContext pc, ComponentDefinitionRegistry cdr) {
        MutableBeanMetadata meta = pc.createMetadata(MutableBeanMetadata.class);
        meta.setId(ANNOTATION_PARSER_BEAN_NAME);
        meta.setRuntimeClass(AnnotationProcessor.class);
        meta.setProcessor(true);
        meta.addArgument(passThrough(pc, cdr), ComponentDefinitionRegistry.class.getName(), 0);
        meta.addArgument(passThrough(pc, tm), TransactionManager.class.getName(), 1);
        meta.addArgument(passThrough(pc, coordinator), Coordinator.class.getName(), 1);
        return meta;
    }

    private MutablePassThroughMetadata passThrough(ParserContext pc, Object o) {
        MutablePassThroughMetadata meta = pc.createMetadata(MutablePassThroughMetadata.class);
        meta.setObject(o);
        return meta;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata cm, ParserContext pc)
    {
        if (node instanceof Element) {
            Element elt = (Element) node;
            parseElement(elt, cm, pc);
        }
        return cm;
    }

    public Metadata parse(Element elt, ParserContext pc)
    {
        parseElement(elt, pc.getEnclosingComponent(), pc);
        return null;
    }

    public URL getSchemaLocation(String namespaceUri)
    {
        String xsdPath = schemaMap.get(namespaceUri);
        return xsdPath != null ? this.getClass().getResource(xsdPath) : null;
    }
    
    public void setTm(TransactionManager tm) {
        this.tm = tm;
    }
    
    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses()
    {
        return null;
    }
    
}
