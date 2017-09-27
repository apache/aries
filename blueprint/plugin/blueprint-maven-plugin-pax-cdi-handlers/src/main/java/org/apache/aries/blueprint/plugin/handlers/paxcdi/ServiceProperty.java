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
package org.apache.aries.blueprint.plugin.handlers.paxcdi;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Arrays;
import java.util.List;

class ServiceProperty {
    final String name;
    private final String type;
    private final boolean isArray;
    private final List<String> values;

    ServiceProperty(String name, String value) {
        if (hasTypeSignature(name)) {
            String[] splitName = name.split(":");
            this.name = splitName[0];
            this.type = getType(splitName[1]);
            this.isArray = splitName[1].endsWith("[]");
            this.values = isArray ? Arrays.asList(value.split("\\|")) : Arrays.asList(value);
        } else {
            this.name = name;
            this.type = null;
            this.isArray = false;
            this.values = Arrays.asList(value);
        }
    }

    private static boolean hasTypeSignature(String name) {
        return name.contains(":");
    }

    private static String getType(String typeSignature) {
        String rawType;
        if (typeSignature.endsWith("[]")) {
            rawType = typeSignature.substring(0, typeSignature.length() - 2);
        } else {
            rawType = typeSignature;
        }

        if ("".equals(rawType)) {
            return null;
        }
        if (rawType.contains(".")) {
            return rawType;
        } else {
            return "java.lang." + rawType;
        }
    }

    void write(XMLStreamWriter writer) throws XMLStreamException {
        if (type == null && !isArray) {
            writer.writeEmptyElement("entry");
            writer.writeAttribute("key", name);
            writer.writeAttribute("value", values.get(0));
        } else {
            writer.writeStartElement("entry");
            writer.writeAttribute("key", name);
            if (isArray) {
                writer.writeStartElement("array");
                if (type != null) {
                    writer.writeAttribute("value-type", type);
                }
                for (String value : values) {
                    writer.writeStartElement("value");
                    writer.writeCharacters(value);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            } else {
                writer.writeStartElement("value");
                writer.writeAttribute("type", type);
                writer.writeCharacters(values.get(0));
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    String getSingleValue() {
        if (values.size() > 1) {
            throw new IllegalArgumentException("Property has more than one value");
        }
        return values.get(0);
    }
}
