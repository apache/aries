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
import org.apache.aries.blueprint.plugin.model.BeanFromFactory;
import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.model.Property;
import org.apache.aries.blueprint.plugin.model.PropertyWriter;
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;


public class Generator implements PropertyWriter, ArgumentWriter {
    private static final String NS_BLUEPRINT = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    private static final String NS_EXT = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0";

    private final Context context;
    private final BlueprintConfiguration blueprintConfiguration;
    private final XMLStreamWriter writer;
    private final OutputStream os;
    private final ByteArrayOutputStream temp = new ByteArrayOutputStream();

    public Generator(Context context, OutputStream os, BlueprintConfigurationImpl blueprintConfiguration) throws XMLStreamException {
        this.context = context;
        this.blueprintConfiguration = blueprintConfiguration;
        this.writer = XMLOutputFactory.newFactory().createXMLStreamWriter(temp);
        this.os = os;
    }

    public void generate() {
        generateXml();
        printFormatted();
    }

    private void generateXml() {
        try {
            writer.writeStartDocument();
            writeBlueprint();

            for (Bean bean : context.getBeans()) {
                writeBeanStart(bean);
                bean.writeArguments(this);
                bean.writeProperties(this);
                writer.writeEndElement();
            }

            for (XmlWriter bw : context.getBlueprintWriters().values()) {
                bw.write(writer);
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void printFormatted() {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");


            transformer.transform(
                    new StreamSource(new ByteArrayInputStream(temp.toByteArray())),
                    new StreamResult(os));
        } catch (TransformerException e) {
            throw new RuntimeException("Cannot print file", e);
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
        if (bean instanceof BeanFromFactory) {
            writeFactory((BeanFromFactory) bean);
        }
        for (XmlWriter xmlWriter : bean.beanContentWriters.values()) {
            xmlWriter.write(writer);
        }
    }

    private void writeFactory(BeanFromFactory bean) throws XMLStreamException {
        writer.writeAttribute("factory-ref", bean.factoryBean.id);
        writer.writeAttribute("factory-method", bean.factoryMethod);
    }

    @Override
    public void writeProperty(Property property) {
        try {
            writer.writeEmptyElement("property");
            writer.writeAttribute("name", property.name);
            if (property.ref != null) {
                writer.writeAttribute("ref", property.ref);
            } else if (property.value != null) {
                writer.writeAttribute("value", property.value);
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void writeArgument(Argument argument) {
        try {
            writer.writeEmptyElement("argument");
            if (argument.getRef() != null) {
                writer.writeAttribute("ref", argument.getRef());
            } else if (argument.getValue() != null) {
                writer.writeAttribute("value", argument.getValue());
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
