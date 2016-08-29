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
package org.apache.aries.blueprint.plugin;

import org.apache.aries.blueprint.plugin.model.Argument;
import org.apache.aries.blueprint.plugin.model.ArgumentWriter;
import org.apache.aries.blueprint.plugin.model.Bean;
import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.model.ProducedBean;
import org.apache.aries.blueprint.plugin.model.Property;
import org.apache.aries.blueprint.plugin.model.PropertyWriter;
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.Map;

public class Generator implements PropertyWriter, ArgumentWriter {
    private static final String NS_BLUEPRINT = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    private static final String NS_EXT = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0";

    private final Context context;
    private final BlueprintConfiguration blueprintConfiguration;
    private final XMLStreamWriter writer;

    public Generator(Context context, OutputStream os, BlueprintConfiguration blueprintConfiguration) throws XMLStreamException {
        this.context = context;
        this.blueprintConfiguration = blueprintConfiguration;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        writer = factory.createXMLStreamWriter(os);
    }

    public void generate() {
        try {
            writer.writeStartDocument();
            writer.writeCharacters("\n");
            writeBlueprint();
            writer.writeCharacters("\n");

            for (Bean bean : context.getBeans()) {
                writeBeanStart(bean);
                bean.writeArguments(this);
                bean.writeProperties(this);
                writer.writeEndElement();
                writer.writeCharacters("\n");
            }

            for (XmlWriter bw : context.getBlueprintWriters().values()) {
                bw.write(writer);
            }

            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndDocument();
            writer.writeCharacters("\n");
            writer.close();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void writeBlueprint() throws XMLStreamException {
        writer.writeStartElement("blueprint");
        writer.writeDefaultNamespace(NS_BLUEPRINT);
        writer.writeNamespace("ext", NS_EXT);
        if (blueprintConfiguration.getDefaultActivation() != null) {
            writer.writeAttribute("default-activation", blueprintConfiguration.getDefaultActivation().name().toLowerCase());
        }
    }

    public void writeBeanStart(Bean bean) throws XMLStreamException {
        writer.writeStartElement("bean");
        writer.writeAttribute("id", bean.id);
        writer.writeAttribute("class", bean.clazz.getName());
        if (bean.needFieldInjection()) {
            writer.writeAttribute("ext", NS_EXT, "field-injection", "true");
        }
        if (bean.isPrototype) {
            writer.writeAttribute("scope", "prototype");
        }

        Map<String, String> attributes = bean.attributes;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            writer.writeAttribute(entry.getKey(), entry.getValue());
        }
        if (bean instanceof ProducedBean) {
            writeFactory((ProducedBean) bean);
        }
        writer.writeCharacters("\n");

        for (XmlWriter xmlWriter : bean.beanContentWriters.values()) {
            xmlWriter.write(writer);
        }
    }

    private void writeFactory(ProducedBean bean) throws XMLStreamException {
        writer.writeAttribute("factory-ref", bean.factoryBean.id);
        writer.writeAttribute("factory-method", bean.factoryMethod);
    }

    @Override
    public void writeProperty(Property property) {
        try {
            writer.writeCharacters("    ");
            writer.writeEmptyElement("property");
            writer.writeAttribute("name", property.name);
            if (property.ref != null) {
                writer.writeAttribute("ref", property.ref);
            } else if (property.value != null) {
                writer.writeAttribute("value", property.value);
            }
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void writeArgument(Argument argument) {
        try {
            writer.writeCharacters("    ");
            writer.writeEmptyElement("argument");
            if (argument.getRef() != null) {
                writer.writeAttribute("ref", argument.getRef());
            } else if (argument.getValue() != null) {
                writer.writeAttribute("value", argument.getValue());
            }
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
