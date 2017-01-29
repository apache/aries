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

import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;

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

class BlueprintFileWriter {

    private final XMLStreamWriter writer;
    private final OutputStream os;
    private final ByteArrayOutputStream temp = new ByteArrayOutputStream();

    BlueprintFileWriter(OutputStream os) throws XMLStreamException {
        this.writer = XMLOutputFactory.newFactory().createXMLStreamWriter(temp);
        this.os = os;
    }

    void generate(Context context) {
        generateXml(context);
        printFormatted();
    }

    private void generateXml(Context context) {
        try {
            writer.writeStartDocument();
            context.write(writer);
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
}
