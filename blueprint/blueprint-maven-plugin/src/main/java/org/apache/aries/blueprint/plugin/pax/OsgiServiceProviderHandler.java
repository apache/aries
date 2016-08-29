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
package org.apache.aries.blueprint.plugin.pax;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.aries.blueprint.plugin.spi.BlueprintWriter;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.CustomBeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.CustomFactoryMethodAnnotationHandler;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsgiServiceProviderHandler implements CustomBeanAnnotationHandler<OsgiServiceProvider>, CustomFactoryMethodAnnotationHandler<OsgiServiceProvider> {
    @Override
    public Class<OsgiServiceProvider> getAnnotation() {
        return OsgiServiceProvider.class;
    }

    @Override
    public void handleFactoryMethodAnnotation(AnnotatedElement annotatedElement, String id, ContextEnricher contextEnricher) {
        handleAnnotation(annotatedElement, id, contextEnricher);
    }

    @Override
    public void handleBeanAnnotation(AnnotatedElement annotatedElement, final String id, ContextEnricher contextEnricher) {
        handleAnnotation(annotatedElement, id, contextEnricher);
    }

    private void handleAnnotation(AnnotatedElement annotatedElement, final String id, ContextEnricher contextEnricher) {
        final OsgiServiceProvider serviceProvider = annotatedElement.getAnnotation(OsgiServiceProvider.class);
        Properties properties = annotatedElement.getAnnotation(Properties.class);

        final List<String> interfaceNames = extractServiceInterfaces(serviceProvider);

        final Map<String, String> propertiesAsMap = extractProperties(properties);

        contextEnricher.addBlueprintWriter("OsgiServiceProvider/" + annotatedElement + "/" + id, new BlueprintWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writeService(writer, propertiesAsMap, interfaceNames, id);
            }
        });
    }

    private void writeService(XMLStreamWriter writer, Map<String, String> propertiesAsMap, List<String> interfaceNames, String id) throws XMLStreamException {
        // If there are no properties to write and only one service attribute (either
        // interface="MyServiceInterface" or auto-export="interfaces") then create an
        // empty element
        boolean writeEmptyElement = propertiesAsMap.isEmpty() && interfaceNames.size() < 2;
        if (writeEmptyElement) {
            writer.writeEmptyElement("service");
        } else {
            writer.writeStartElement("service");
        }
        writer.writeAttribute("ref", id);

        if (interfaceNames.size() == 0) {
            writer.writeAttribute("auto-export", "interfaces");
        } else if (interfaceNames.size() == 1) {
            writer.writeAttribute("interface", Iterables.getOnlyElement(interfaceNames));
        } else {
            writeInterfacesElement(writer, interfaceNames);
        }

        writer.writeCharacters("\n");

        if (!propertiesAsMap.isEmpty()) {
            writeProperties(writer, propertiesAsMap);
        }

        if (!writeEmptyElement) {
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }
    }

    private static Map<String, String> extractProperties(Properties properties) {
        Map<String, String> propertiesAsMap = new HashMap<>();
        if (properties != null) {
            for (Property property : properties.value()) {
                propertiesAsMap.put(property.name(), property.value());
            }
        }
        return propertiesAsMap;
    }

    private static List<String> extractServiceInterfaces(OsgiServiceProvider serviceProvider) {
        List<String> interfaceNames = Lists.newArrayList();
        for (Class<?> serviceIf : serviceProvider.classes()) {
            interfaceNames.add(serviceIf.getName());
        }
        return interfaceNames;
    }

    private void writeInterfacesElement(XMLStreamWriter writer, Iterable<String> interfaceNames) throws XMLStreamException {
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

    private void writeProperties(XMLStreamWriter writer, Map<String, String> properties) throws XMLStreamException {
        writer.writeCharacters("    ");
        writer.writeStartElement("service-properties");
        writer.writeCharacters("\n");
        for (Map.Entry<String, String> property : properties.entrySet()) {
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
