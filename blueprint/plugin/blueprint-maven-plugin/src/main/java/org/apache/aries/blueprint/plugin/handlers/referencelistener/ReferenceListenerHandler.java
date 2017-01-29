/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.handlers.referencelistener;

import org.apache.aries.blueprint.annotation.referencelistener.Bind;
import org.apache.aries.blueprint.annotation.referencelistener.Cardinality;
import org.apache.aries.blueprint.annotation.referencelistener.ReferenceListener;
import org.apache.aries.blueprint.annotation.referencelistener.Unbind;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;

public class ReferenceListenerHandler implements BeanAnnotationHandler<ReferenceListener> {
    @Override
    public void handleBeanAnnotation(AnnotatedElement annotatedElement, String id, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        ReferenceListener annotation = annotatedElement.getAnnotation(ReferenceListener.class);
        Class<?> referenceListenerClass = getClass(annotatedElement);
        String bindMethod = annotation.bindMethod().isEmpty() ? getAnnotatedMethodName(referenceListenerClass, Bind.class) : annotation.bindMethod();
        String unbindMethod = annotation.unbindMethod().isEmpty() ? getAnnotatedMethodName(referenceListenerClass, Unbind.class) : annotation.unbindMethod();
        ReferenceListenerDefinition referenceListenerDefinition = new ReferenceListenerDefinition(
                id, bindMethod, unbindMethod
        );
        String referenceBeanName = annotation.referenceName().isEmpty() ? createReferenceBeanName(annotation) : annotation.referenceName();
        if (annotation.cardinality() == Cardinality.SINGLE) {
            contextEnricher.addBean(referenceBeanName, annotation.referenceInterface());
            createReference(referenceBeanName, referenceListenerDefinition, annotation, contextEnricher);
        } else {
            contextEnricher.addBean(referenceBeanName, List.class);
            createReferenceList(referenceBeanName, referenceListenerDefinition, annotation, contextEnricher);
        }
    }

    private void createReference(final String referenceBeanName,
                                 final ReferenceListenerDefinition referenceListenerDefinition,
                                 final ReferenceListener annotation,
                                 ContextEnricher contextEnricher) {
        contextEnricher.addBlueprintContentWriter("referenceWithReferenceListener/" + referenceBeanName + "/" + referenceListenerDefinition.ref, new XmlWriter() {
            @Override
            public void write(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
                xmlStreamWriter.writeStartElement("reference");
                xmlStreamWriter.writeAttribute("interface", annotation.referenceInterface().getName());
                xmlStreamWriter.writeAttribute("availability", annotation.availability().name().toLowerCase());
                xmlStreamWriter.writeAttribute("id", referenceBeanName);
                if (!annotation.filter().isEmpty()) {
                    xmlStreamWriter.writeAttribute("filter", annotation.filter());
                }
                if (!annotation.componentName().isEmpty()) {
                    xmlStreamWriter.writeAttribute("component-name", annotation.componentName());
                }
                writeReferenceListner(xmlStreamWriter, referenceListenerDefinition);
                xmlStreamWriter.writeEndElement();
            }
        });
    }

    private void createReferenceList(final String referenceBeanName,
                                     final ReferenceListenerDefinition referenceListenerDefinition,
                                     final ReferenceListener annotation,
                                     ContextEnricher contextEnricher) {
        contextEnricher.addBlueprintContentWriter("referenceListWithReferenceListener/" + referenceBeanName + "/" + referenceListenerDefinition.ref, new XmlWriter() {
            @Override
            public void write(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
                xmlStreamWriter.writeStartElement("reference-list");
                xmlStreamWriter.writeAttribute("interface", annotation.referenceInterface().getName());
                xmlStreamWriter.writeAttribute("availability", annotation.availability().name().toLowerCase());
                xmlStreamWriter.writeAttribute("id", referenceBeanName);
                if (!annotation.filter().isEmpty()) {
                    xmlStreamWriter.writeAttribute("filter", annotation.filter());
                }
                if (!annotation.componentName().isEmpty()) {
                    xmlStreamWriter.writeAttribute("component-name", annotation.componentName());
                }
                writeReferenceListner(xmlStreamWriter, referenceListenerDefinition);
                xmlStreamWriter.writeEndElement();
            }
        });
    }

    private void writeReferenceListner(XMLStreamWriter xmlStreamWriter, ReferenceListenerDefinition referenceListenerDefinition) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("reference-listener");
        xmlStreamWriter.writeAttribute("ref", referenceListenerDefinition.ref);
        if (referenceListenerDefinition.bind != null) {
            xmlStreamWriter.writeAttribute("bind-method", referenceListenerDefinition.bind);
        }
        if (referenceListenerDefinition.unbind != null) {
            xmlStreamWriter.writeAttribute("unbind-method", referenceListenerDefinition.unbind);
        }
        xmlStreamWriter.writeEndElement();
    }

    private String createReferenceBeanName(ReferenceListener annotation) {
        String prefix = getBeanNameFromSimpleName(annotation.referenceInterface().getSimpleName());
        String listPart = annotation.cardinality() == Cardinality.SINGLE ? "" : "List";
        String suffix = createIdSuffix(annotation);
        return prefix + listPart + suffix;
    }

    private static String getBeanNameFromSimpleName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
    }

    private String createIdSuffix(ReferenceListener listener) {
        return createComponentNamePart(listener) + createFilterPart(listener);
    }

    private String createComponentNamePart(ReferenceListener listener) {
        if (!listener.componentName().isEmpty()) {
            return "-" + listener.componentName();
        }
        return "";
    }

    private String createFilterPart(ReferenceListener listener) {
        if (!listener.filter().isEmpty()) {
            return "-" + getId(listener.filter());
        }
        return "";
    }

    private String getId(String raw) {
        StringBuilder builder = new StringBuilder();
        for (int c = 0; c < raw.length(); c++) {
            char ch = raw.charAt(c);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String getAnnotatedMethodName(Class<?> referenceListenerClass, Class annotation) {
        for (Method method : referenceListenerClass.getMethods()) {
            if (method.getAnnotation(annotation) != null) {
                return method.getName();
            }
        }
        return null;
    }

    @Override
    public Class<ReferenceListener> getAnnotation() {
        return ReferenceListener.class;
    }

    private Class<?> getClass(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Class<?>) {
            return (Class<?>) annotatedElement;
        }
        if (annotatedElement instanceof Method) {
            return ((Method) annotatedElement).getReturnType();
        }
        throw new RuntimeException("Unknown annotated element");
    }
}
