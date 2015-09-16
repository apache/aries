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
package org.apache.aries.jpa.blueprint.impl;

import java.net.URL;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class JpaNsHandler implements NamespaceHandler {

    public static final String NAMESPACE_JPA_20 = "http://aries.apache.org/xmlns/jpa/v2.0.0";
    public static final String NAMESPACE_JPAN_10 = "http://aries.apache.org/xmlns/jpan/v1.0.0";

    private void parseElement(Element elt, ComponentMetadata cm, ParserContext pc) {
        ComponentDefinitionRegistry cdr = pc.getComponentDefinitionRegistry();

        if ("enable".equals(elt.getLocalName())) {
            if (!cdr.containsComponentDefinition(JpaComponentProcessor.class.getSimpleName())) {
                MutableBeanMetadata meta = pc.createMetadata(MutableBeanMetadata.class);
                meta.setId(JpaComponentProcessor.class.getSimpleName());
                meta.setRuntimeClass(JpaComponentProcessor.class);
                meta.setProcessor(true);
                meta.addProperty("pc", passThrough(pc, pc));
                cdr.registerComponentDefinition(meta);
            }
        }
    }

    private MutablePassThroughMetadata passThrough(ParserContext pc, Object obj) {
        MutablePassThroughMetadata meta = pc.createMetadata(MutablePassThroughMetadata.class);
        meta.setObject(obj);
        return meta;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata cm, ParserContext pc) {
        if (node instanceof Element) {
            Element elt = (Element)node;
            parseElement(elt, cm, pc);
        }
        return cm;
    }

    public Metadata parse(Element elt, ParserContext pc) {
        parseElement(elt, pc.getEnclosingComponent(), pc);
        return null;
    }

    public URL getSchemaLocation(String namespace) {
        if (NAMESPACE_JPAN_10.equals(namespace)) {
            // deprecated (remove in jpa 3)
            return this.getClass().getResource("/jpan10.xsd");
        } else if (NAMESPACE_JPA_20.equals(namespace)) {
            return this.getClass().getResource("/jpa20.xsd");
        } else {
            throw new IllegalArgumentException("Unknown namespace for jpa: " + namespace);
        }
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return null;
    }

}
