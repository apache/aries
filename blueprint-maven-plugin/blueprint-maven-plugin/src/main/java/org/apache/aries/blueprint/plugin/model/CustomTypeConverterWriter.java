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
package org.apache.aries.blueprint.plugin.model;

import org.apache.aries.blueprint.plugin.spi.XmlWriter;
import org.osgi.service.blueprint.container.Converter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.annotation.Annotation;
import java.util.List;

class CustomTypeConverterWriter implements XmlWriter {

    private static final String defaultBlueprintConverter = "blueprintConverter";
    private final BeanRefStore beanRefStore;

    CustomTypeConverterWriter(BeanRefStore beanRefStore) {
        this.beanRefStore = beanRefStore;
    }

    public void write(XMLStreamWriter writer) throws XMLStreamException {
        List<BeanRef> typeConverters = beanRefStore.getAllMatching(new BeanTemplate(Converter.class, new Annotation[0]));
        if (hasCustomTypeConverters(typeConverters)) {
            return;
        }
        writer.writeStartElement("type-converters");
        for (BeanRef typeConverter : typeConverters) {
            if (defaultBlueprintConverter.equals(typeConverter.id)) {
                continue;
            }
            writer.writeEmptyElement("ref");
            writer.writeAttribute("component-id", typeConverter.id);
        }
        writer.writeEndElement();
    }

    private boolean hasCustomTypeConverters(List<BeanRef> typeConverters) {
        return typeConverters.isEmpty() || typeConverters.size() == 1 && "blueprintConverter".equals(typeConverters.get(0).id);
    }
}
