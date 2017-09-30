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

import org.apache.aries.blueprint.annotation.service.AutoExport;
import org.apache.aries.blueprint.annotation.service.Service;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;

public class ServiceHandler implements BeanAnnotationHandler<Service> {
    @Override
    public Class<Service> getAnnotation() {
        return Service.class;
    }

    @Override
    public void handleBeanAnnotation(final AnnotatedElement annotatedElement, final String id, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        final Service annotation = annotatedElement.getAnnotation(Service.class);
        contextEnricher.addBlueprintContentWriter("service/" + id, new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeStartElement("service");
                writer.writeAttribute("ref", id);
                writeRanking(writer, annotation);
                writeExportSpecification(writer, annotation);
                new ServicePropertyWriter(annotation.properties(), annotation.ranking()).writeProperties(writer);
                writer.writeEndElement();
            }
        });
    }

    private void writeRanking(XMLStreamWriter writer, Service annotation) throws XMLStreamException {
        if (annotation.ranking() != 0) {
            writer.writeAttribute("ranking", String.valueOf(annotation.ranking()));
        }
    }

    private void writeExportSpecification(XMLStreamWriter writer, Service annotation) throws XMLStreamException {
        if (annotation.classes().length == 0) {
            if (annotation.autoExport() != AutoExport.DISABLED) {
                writer.writeAttribute("auto-export", autoExport(annotation.autoExport()));
            }
        } else if (annotation.classes().length == 1) {
            writer.writeAttribute("interface", annotation.classes()[0].getName());
        } else {
            writeInterfaces(writer, annotation.classes());
        }
    }

    private String autoExport(AutoExport autoExport) {
        switch (autoExport) {
            case INTERFACES:
                return "interfaces";
            case ALL_CLASSES:
                return "all-classes";
            case CLASS_HIERARCHY:
                return "class-hierarchy";
            default:
                throw new IllegalStateException("unkown " + autoExport);
        }
    }

    private void writeInterfaces(XMLStreamWriter writer, Class<?>[] classes) throws XMLStreamException {
        writer.writeStartElement("interfaces");
        for (Class<?> singleClass : classes) {
            writer.writeStartElement("value");
            writer.writeCharacters(singleClass.getName());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
