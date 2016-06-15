/**
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
package org.apache.aries.blueprint.plugin.model.service;

import com.google.common.collect.Iterables;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collection;
import java.util.Map;

public class ServiceProviderWriter {
    private final XMLStreamWriter writer;

    public ServiceProviderWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }

    public void write(Collection<ServiceProvider> serviceProviders) throws XMLStreamException {
        for (ServiceProvider serviceProvider : serviceProviders) {
            write(serviceProvider);
        }
    }

    private void write(ServiceProvider serviceProvider) throws XMLStreamException {

        // If there are no properties to write and only one service attribute (either
        // interface="MyServiceInterface" or auto-export="interfaces") then create an
        // empty element
        boolean writeEmptyElement = serviceProvider.serviceProperties.isEmpty() && serviceProvider.interfaces.size() < 2;
        if (writeEmptyElement) {
            writer.writeEmptyElement("service");
        } else {
            writer.writeStartElement("service");
        }
        writer.writeAttribute("ref", serviceProvider.beanRef);

        if (serviceProvider.interfaces.size() == 0) {
            writer.writeAttribute("auto-export", "interfaces");
        } else if (serviceProvider.interfaces.size() == 1) {
            writer.writeAttribute("interface", Iterables.getOnlyElement(serviceProvider.interfaces));
        } else {
            writeInterfacesElement(serviceProvider.interfaces);
        }

        writer.writeCharacters("\n");

        if (!serviceProvider.serviceProperties.isEmpty()) {
            writeProperties(serviceProvider.serviceProperties);
        }

        if (!writeEmptyElement) {
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }
    }

    private void writeInterfacesElement(Iterable<String> interfaceNames) throws XMLStreamException
    {
        writer.writeCharacters("\n");
        writer.writeCharacters("    ");
        writer.writeStartElement("interfaces");
        writer.writeCharacters("\n");
        for (String interfaceName : interfaceNames) {
            writer.writeCharacters("        ");
            writer.writeStartElement("value");
            writer.writeCharacters(interfaceName);
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }
        writer.writeCharacters("    ");
        writer.writeEndElement();
    }

    private void writeProperties(Map<String,String> properties) throws XMLStreamException {
        writer.writeCharacters("    ");
        writer.writeStartElement("service-properties");
        writer.writeCharacters("\n");
        for (Map.Entry<String,String> property : properties.entrySet()) {
            writer.writeCharacters("        ");
            writer.writeEmptyElement("entry");
            writer.writeAttribute("key", property.getKey());
            writer.writeAttribute("value", property.getValue());
            writer.writeCharacters("\n");
        }
        writer.writeCharacters("    ");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }


}
