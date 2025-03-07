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

import org.apache.aries.blueprint.annotation.service.MemberType;
import org.apache.aries.blueprint.annotation.service.ReferenceList;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.apache.aries.blueprint.plugin.handlers.blueprint.service.ReferenceParameters.needAvailability;

public class ReferenceListHandler implements CustomDependencyAnnotationHandler<ReferenceList> {
    @Override
    public Class<ReferenceList> getAnnotation() {
        return ReferenceList.class;
    }

    @Override
    public String handleDependencyAnnotation(AnnotatedElement annotatedElement, String name, ContextEnricher contextEnricher) {
        ReferenceList referenceList = annotatedElement.getAnnotation(ReferenceList.class);
        final Class<?> clazz = getClass(annotatedElement);
        return handleDependencyAnnotation(clazz, referenceList, name, contextEnricher);
    }

    @Override
    public String handleDependencyAnnotation(final Class<?> clazz, ReferenceList referenceList, String name, ContextEnricher contextEnricher) {
        if (clazz != List.class) {
            throw new ReferenceListInvalidInterface(clazz);
        }
        final String id = name != null ? name : ReferenceId.generateReferenceListId(referenceList, contextEnricher);
        contextEnricher.addBean(id, clazz);
        contextEnricher.addBlueprintContentWriter(getWriterId(id, referenceList.referenceInterface()), getXmlWriter(id, referenceList, contextEnricher));
        return id;
    }

    private XmlWriter getXmlWriter(final String id, final ReferenceList referenceList, final ContextEnricher contextEnricher) {
        return new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeEmptyElement("reference-list");
                writer.writeAttribute("id", id);
                writer.writeAttribute("interface", referenceList.referenceInterface().getName());
                if (!"".equals(referenceList.filter())) {
                    writer.writeAttribute("filter", referenceList.filter());
                }
                if (!"".equals(referenceList.componentName())) {
                    writer.writeAttribute("component-name", referenceList.componentName());
                }
                if (needAvailability(contextEnricher, referenceList.availability())) {
                    writer.writeAttribute("availability", referenceList.availability().name().toLowerCase());
                }
                if (referenceList.memberType() == MemberType.SERVICE_REFERENCE) {
                    writer.writeAttribute("member-type", "service-reference");
                }
            }
        };
    }

    private String getWriterId(String id, Class<?> clazz) {
        return "referenceList/" + clazz.getName() + "/" + id;
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
