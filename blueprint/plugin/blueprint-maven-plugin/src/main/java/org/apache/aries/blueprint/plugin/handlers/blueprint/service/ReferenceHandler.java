/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.handlers.blueprint.service;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.apache.aries.blueprint.plugin.handlers.blueprint.service.ReferenceParameters.needAvailability;
import static org.apache.aries.blueprint.plugin.handlers.blueprint.service.ReferenceParameters.needTimeout;

public class ReferenceHandler implements CustomDependencyAnnotationHandler<Reference> {
    @Override
    public Class<Reference> getAnnotation() {
        return Reference.class;
    }

    @Override
    public String handleDependencyAnnotation(AnnotatedElement annotatedElement, String name, ContextEnricher contextEnricher) {
        Reference reference = annotatedElement.getAnnotation(Reference.class);
        final Class<?> clazz = getClass(annotatedElement);
        return handleDependencyAnnotation(clazz, reference, name, contextEnricher);
    }

    @Override
    public String handleDependencyAnnotation(final Class<?> clazz, Reference reference, String name, ContextEnricher contextEnricher) {
        final String id = name != null ? name : ReferenceId.generateReferenceId(clazz, reference, contextEnricher);
        contextEnricher.addBean(id, clazz);
        contextEnricher.addBlueprintContentWriter(getWriterId(id, clazz), getXmlWriter(id, clazz, reference, contextEnricher));
        return id;
    }

    private XmlWriter getXmlWriter(final String id, final Class<?> clazz, final Reference reference, final ContextEnricher contextEnricher) {
        return new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeEmptyElement("reference");
                writer.writeAttribute("id", id);
                writer.writeAttribute("interface", clazz.getName());
                if (!"".equals(reference.filter())) {
                    writer.writeAttribute("filter", reference.filter());
                }
                if (!"".equals(reference.componentName())) {
                    writer.writeAttribute("component-name", reference.componentName());
                }
                if (needTimeout(reference)) {
                    writer.writeAttribute("timeout", String.valueOf(reference.timeout()));
                }
                if (needAvailability(contextEnricher, reference)) {
                    writer.writeAttribute("availability", reference.availability().name().toLowerCase());
                }
            }
        };
    }

    private String getWriterId(String id, Class<?> clazz) {
        return "reference/" + clazz.getName() + "/" + id;
    }

    private Class<?> getClass(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Class<?>) {
            return (Class<?>) annotatedElement;
        }
        if (annotatedElement instanceof Method) {
            return ((Method) annotatedElement).getParameterTypes()[0];
        }
        if (annotatedElement instanceof Field) {
            return ((Field) annotatedElement).getType();
        }
        throw new RuntimeException("Unknown annotated element");
    }

}
