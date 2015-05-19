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

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class JpaNsHandler implements NamespaceHandler {

    private void parseElement(Element elt, ComponentMetadata cm, ParserContext pc) {
        ComponentDefinitionRegistry cdr = pc.getComponentDefinitionRegistry();

        if ("enable".equals(elt.getLocalName())) {
            if (!cdr.containsComponentDefinition(JpaBeanProcessor.JPA_PROCESSOR_BEAN_NAME)) {
                MutableBeanMetadata meta = pc.createMetadata(MutableBeanMetadata.class);
                meta.setId(JpaBeanProcessor.JPA_PROCESSOR_BEAN_NAME);
                meta.setRuntimeClass(JpaBeanProcessor.class);
                meta.setProcessor(true);
                meta.addProperty("cdr", passThrough(pc, cdr));
                cdr.registerComponentDefinition(meta);
            }
        }
    }

    private MutablePassThroughMetadata passThrough(ParserContext pc, Object obj) {
        MutablePassThroughMetadata cdrMeta = pc.createMetadata(MutablePassThroughMetadata.class);
        cdrMeta.setObject(obj);
        return cdrMeta;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata cm, ParserContext pc) {
        System.out.println(cm.getId());
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
        return this.getClass().getResource("/jpa10.xsd");
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return null;
    }

    @SuppressWarnings("unchecked")
    ComponentMetadata createEmSupplierRef(ParserContext pc, String unitName) {
        final MutableReferenceMetadata refMetadata = pc.createMetadata(MutableReferenceMetadata.class);
        refMetadata.setActivation(getDefaultActivation(pc));
        refMetadata.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);
        refMetadata.setRuntimeInterface(EmSupplier.class);
        refMetadata.setInterface(EmSupplier.class.getName());
        refMetadata.setFilter(String.format("(%s=%s)", JPA_UNIT_NAME, unitName));
        refMetadata.setTimeout(Integer.parseInt(pc.getDefaultTimeout()));
        refMetadata.setDependsOn((List<String>)Collections.EMPTY_LIST);
        refMetadata.setId(pc.generateId());
        return refMetadata;
    }
    
    @SuppressWarnings("unchecked")
    ComponentMetadata createTransactionManagerRef(ParserContext pc) {
        final MutableReferenceMetadata refMetadata = pc.createMetadata(MutableReferenceMetadata.class);
        refMetadata.setActivation(getDefaultActivation(pc));
        refMetadata.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);
        refMetadata.setRuntimeInterface(TransactionManager.class);
        refMetadata.setInterface(TransactionManager.class.getName());
        refMetadata.setTimeout(Integer.parseInt(pc.getDefaultTimeout()));
        refMetadata.setDependsOn((List<String>)Collections.EMPTY_LIST);
        refMetadata.setId(pc.generateId());
        return refMetadata;
    }

    private int getDefaultActivation(ParserContext ctx) {
        return "ACTIVATION_EAGER".equalsIgnoreCase(ctx.getDefaultActivation())
            ? ReferenceMetadata.ACTIVATION_EAGER : ReferenceMetadata.ACTIVATION_LAZY;
    }
}
