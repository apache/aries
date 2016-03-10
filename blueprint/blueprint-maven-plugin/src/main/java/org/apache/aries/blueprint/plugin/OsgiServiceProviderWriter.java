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
package org.apache.aries.blueprint.plugin;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.aries.blueprint.plugin.model.Bean;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collection;
import java.util.List;

public class OsgiServiceProviderWriter {
    private final XMLStreamWriter writer;

    public OsgiServiceProviderWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }

    public void write(Collection<Bean> beans) throws XMLStreamException {
        for (Bean bean : beans) {
            write(bean);
        }
    }

    public void write(Bean bean) throws XMLStreamException {
        OsgiServiceProvider serviceProvider = bean.clazz.getAnnotation(OsgiServiceProvider.class);
        if (serviceProvider == null) {
            return;
        }

        Properties properties = bean.clazz.getAnnotation(Properties.class);
        List<String> interfaceNames = Lists.newArrayList();
        for (Class<?> serviceIf : serviceProvider.classes()) {
            interfaceNames.add(serviceIf.getName());
        }

        // If there are no properties to write and only one service attribute (either
        // interface="MyServiceInterface" or auto-export="interfaces") then create an
        // empty element
        boolean writeEmptyElement = properties == null && interfaceNames.size() < 2;
        if (writeEmptyElement) {
            writer.writeEmptyElement("service");
        } else {
            writer.writeStartElement("service");
        }
        writer.writeAttribute("ref", bean.id);

        if (interfaceNames.size() == 0) {
            writer.writeAttribute("auto-export", "interfaces");
        } else if (interfaceNames.size() == 1) {
            writer.writeAttribute("interface", Iterables.getOnlyElement(interfaceNames));
        } else {
            writeInterfacesElement(interfaceNames);
        }

        writer.writeCharacters("\n");
        if (properties != null) {
            writeProperties(properties);
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

    private void writeProperties(Properties properties) throws XMLStreamException {
        writer.writeCharacters("    ");
        writer.writeStartElement("service-properties");
        writer.writeCharacters("\n");
        for (Property property : properties.value()) {
            writer.writeCharacters("        ");
            writer.writeEmptyElement("entry");
            writer.writeAttribute("key", property.name());
            writer.writeAttribute("value", property.value());
            writer.writeCharacters("\n");
        }
        writer.writeCharacters("    ");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }


}
