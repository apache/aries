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

import java.lang.reflect.AccessibleObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaComponentProcessor implements ComponentDefinitionRegistryProcessor {
    private static final String JPA_COORDINATOR = "jpa_Coordinator";
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaComponentProcessor.class);
    private AnnotationScanner annotationScanner;
    private ParserContext pc;

    public JpaComponentProcessor() {
        annotationScanner = new AnnotationScanner();
    }
    
    public void setPc(ParserContext pc) {
        this.pc = pc;
    }

    @Override
    public void process(ComponentDefinitionRegistry cdr) {
        BlueprintContainer container = getComponent(cdr, "blueprintContainer");
        Bundle bundle = getComponent(cdr, "blueprintBundle");
        cdr.registerComponentDefinition(createServiceRef(JPA_COORDINATOR, Coordinator.class));

        Set<String> components = new HashSet<String>(cdr.getComponentDefinitionNames());
        for (String component : components) {
            ComponentMetadata compDef = cdr.getComponentDefinition(component);
            if (compDef instanceof MutableBeanMetadata && !((MutableBeanMetadata)compDef).isProcessor()) {
                handleComponent((MutableBeanMetadata)compDef, bundle, cdr, container);
            }
        }
    }

    private void handleComponent(MutableBeanMetadata compDef, Bundle bundle, ComponentDefinitionRegistry cdr, BlueprintContainer container) {
        final String compName = compDef.getId();
        if (compDef.getClassName() == null) {
            LOGGER.debug("No classname for " + compDef.getId());
            return;
        }
        Class<?> compClass;
        try {
            compClass = bundle.loadClass(compDef.getClassName());
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Bean class not found " + compDef.getClassName());
        }
        compDef.setFieldInjection(true);
        List<AccessibleObject> pcMembers = annotationScanner.getJpaAnnotatedMembers(compClass, PersistenceContext.class);
        for (AccessibleObject member : pcMembers) {
            PersistenceContext pcAnn = member.getAnnotation(PersistenceContext.class);
            
            String propName = annotationScanner.getName(member);
            Class<?> iface = annotationScanner.getType(member);
            LOGGER.debug("Injecting {} into prop {} of bean {} with class {}", iface.getSimpleName(), propName, compName, compClass);
            MutableRefMetadata ref = getServiceRef(cdr, pcAnn.unitName(), iface);
            compDef.addProperty(propName, ref);
            
            MutableRefMetadata emRef = getServiceRef(cdr, pcAnn.unitName(), EntityManager.class);
            Interceptor interceptor = new JpaInterceptor(container, JPA_COORDINATOR, emRef.getComponentId());
            cdr.registerInterceptorWithComponent(compDef, interceptor);
        }
        
        List<AccessibleObject> puMembers = annotationScanner.getJpaAnnotatedMembers(compClass, PersistenceUnit.class);
        for (AccessibleObject member : puMembers) {
            PersistenceUnit puAnn = member.getAnnotation(PersistenceUnit.class);
            String propName = annotationScanner.getName(member);
            Class<?> iface = annotationScanner.getType(member);
            LOGGER.debug("Injecting {} into prop {} of bean {} with class {}", iface.getSimpleName(), propName, compName, compClass);
            MutableRefMetadata ref = getServiceRef(cdr, puAnn.unitName(), iface);
            compDef.addProperty(propName, ref);
        }
    }

    private MutableRefMetadata getServiceRef(ComponentDefinitionRegistry cdr, String unitName, Class<?> iface) {
        ComponentMetadata serviceRef = cdr.getComponentDefinition(getId(unitName, iface));
        if (serviceRef == null)  {
            serviceRef = createJPAServiceRef(unitName, iface);
            cdr.registerComponentDefinition(serviceRef);
        }
        MutableRefMetadata ref = pc.createMetadata(MutableRefMetadata.class);
        ref.setComponentId(serviceRef.getId());
        return ref;
    }
    
    @SuppressWarnings("unchecked")
    ComponentMetadata createServiceRef(String id, Class<?> iface) {
        final MutableReferenceMetadata refMeta = pc.createMetadata(MutableReferenceMetadata.class);
        refMeta.setActivation(getDefaultActivation(pc));
        refMeta.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);
        refMeta.setRuntimeInterface(iface);
        refMeta.setTimeout(Integer.parseInt(pc.getDefaultTimeout()));
        refMeta.setDependsOn((List<String>)Collections.EMPTY_LIST);
        refMeta.setId(id);
        return refMeta;
    }

    @SuppressWarnings("unchecked")
    ComponentMetadata createJPAServiceRef(String unitName, Class<?> iface) {
        final MutableReferenceMetadata refMeta = pc.createMetadata(MutableReferenceMetadata.class);
        refMeta.setActivation(getDefaultActivation(pc));
        refMeta.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);
        refMeta.setRuntimeInterface(iface);
        refMeta.setFilter(String.format("(%s=%s)", JPA_UNIT_NAME, unitName));
        refMeta.setTimeout(Integer.parseInt(pc.getDefaultTimeout()));
        refMeta.setDependsOn((List<String>)Collections.EMPTY_LIST);
        refMeta.setId(getId(unitName, iface));
        return refMeta;
    }
    
    public String getId(String unitName, Class<?> iface) {
        return unitName + "-" + iface.getSimpleName();
    }
    
    private int getDefaultActivation(ParserContext ctx) {
        return "ACTIVATION_EAGER".equalsIgnoreCase(ctx.getDefaultActivation())
            ? ReferenceMetadata.ACTIVATION_EAGER : ReferenceMetadata.ACTIVATION_LAZY;
    }
    
    @SuppressWarnings("unchecked")
    private <T>T getComponent(ComponentDefinitionRegistry cdr, String id) {
        return (T)((PassThroughMetadata) cdr.getComponentDefinition(id)).getObject();
    }

}
