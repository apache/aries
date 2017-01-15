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
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsgiServiceProviderHandler implements BeanAnnotationHandler<OsgiServiceProvider> {

    private static final List<String> SPECIAL_PROPERTIES = Collections.singletonList("service.ranking");

    @Override
    public Class<OsgiServiceProvider> getAnnotation() {
        return OsgiServiceProvider.class;
    }

    @Override
    public void handleBeanAnnotation(AnnotatedElement annotatedElement, final String id, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        handleAnnotation(annotatedElement, id, contextEnricher);
    }

    private void handleAnnotation(AnnotatedElement annotatedElement, final String id, ContextEnricher contextEnricher) {
        final OsgiServiceProvider serviceProvider = annotatedElement.getAnnotation(OsgiServiceProvider.class);
        Properties properties = annotatedElement.getAnnotation(Properties.class);

        final List<String> interfaceNames = extractServiceInterfaces(serviceProvider);

        final Map<String, String> propertiesAsMap = extractProperties(properties);

        contextEnricher.addBlueprintContentWriter("OsgiServiceProvider/" + annotatedElement + "/" + id, new XmlWriter() {
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

        if (!propertiesAsMap.isEmpty()) {
            writeRanking(writer, propertiesAsMap);
            writeProperties(writer, propertiesAsMap);
        }

        if (!writeEmptyElement) {
            writer.writeEndElement();
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
        writer.writeStartElement("interfaces");
        for (String interfaceName : interfaceNames) {
            writer.writeStartElement("value");
            writer.writeCharacters(interfaceName);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeRanking(XMLStreamWriter writer, Map<String, String> propertiesAsMap) throws XMLStreamException {
        if (propertiesAsMap.containsKey("service.ranking")) {
            try {
                Integer ranking = Integer.parseInt(propertiesAsMap.get("service.ranking"));
                writer.writeAttribute("ranking", ranking.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("service.ranking property must be an integer!");
            }
        }
    }

    private void writeProperties(XMLStreamWriter writer, Map<String, String> properties) throws XMLStreamException {
        writer.writeStartElement("service-properties");
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (!SPECIAL_PROPERTIES.contains(property.getKey())) {
                writer.writeEmptyElement("entry");
                writer.writeAttribute("key", property.getKey());
                writer.writeAttribute("value", property.getValue());
            }
        }
        writer.writeEndElement();
    }
}
