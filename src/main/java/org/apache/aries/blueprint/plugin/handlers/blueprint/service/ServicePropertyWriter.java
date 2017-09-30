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

import org.apache.aries.blueprint.annotation.service.ServiceProperty;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;

class ServicePropertyWriter {
    private final List<ServiceProperty> serviceProperties;

    ServicePropertyWriter(ServiceProperty[] serviceProperties, int ranking) {
        this.serviceProperties = filterProperties(serviceProperties, ranking != 0);
    }

    private List<ServiceProperty> filterProperties(ServiceProperty[] properties, boolean rankingAlreadyProvided) {
        List<ServiceProperty> filtered = new ArrayList<>();
        for (ServiceProperty sp : properties) {
            if (rankingAlreadyProvided && sp.name().equals("service.ranking")) {
                continue;
            }
            if (sp.values().length == 0) {
                continue;
            }
            filtered.add(sp);
        }
        return filtered;
    }

    void writeProperties(XMLStreamWriter writer) throws XMLStreamException {
        if (!serviceProperties.isEmpty()) {
            writer.writeStartElement("service-properties");
            for (ServiceProperty serviceProperty : serviceProperties) {
                writeServiceProperty(writer, serviceProperty);
            }
            writer.writeEndElement();
        }
    }

    private void writeServiceProperty(XMLStreamWriter writer, ServiceProperty serviceProperty) throws XMLStreamException {
        writer.writeStartElement("entry");
        writer.writeAttribute("key", serviceProperty.name());
        if (isSingleValue(serviceProperty)) {
            writeOneValueProperty(writer, serviceProperty);
        } else {
            writeMultiValueProperty(writer, serviceProperty);
        }
        writer.writeEndElement();
    }

    private boolean isSingleValue(ServiceProperty serviceProperty) {
        return serviceProperty.values().length == 1;
    }

    private boolean isStringProperty(ServiceProperty serviceProperty) {
        return serviceProperty.type().equals(String.class);
    }

    private void writeOneValueProperty(XMLStreamWriter writer, ServiceProperty serviceProperty) throws XMLStreamException {
        if (isStringProperty(serviceProperty)) {
            writer.writeAttribute("value", serviceProperty.values()[0]);
        } else {
            writer.writeStartElement("value");
            writer.writeAttribute("type", serviceProperty.type().getName());
            writer.writeCharacters(serviceProperty.values()[0]);
            writer.writeEndElement();
        }
    }

    private void writeMultiValueProperty(XMLStreamWriter writer, ServiceProperty serviceProperty) throws XMLStreamException {
        writer.writeStartElement("array");
        if (!isStringProperty(serviceProperty)) {
            writer.writeAttribute("value-type", serviceProperty.type().getName());
        }
        for (String value : serviceProperty.values()) {
            writer.writeStartElement("value");
            writer.writeCharacters(value);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
