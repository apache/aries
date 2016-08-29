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
package org.apache.aries.blueprint.plugin.javax;

import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.FieldAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.persistence.PersistenceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.Field;
import java.util.List;

public class PersistenceContextHandler implements FieldAnnotationHandler<PersistenceContext> {
    public static final String NS_JPA = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    public static final String NS_JPA2 = "http://aries.apache.org/xmlns/jpa/v2.0.0";

    @Override
    public Class<PersistenceContext> getAnnotation() {
        return PersistenceContext.class;
    }

    @Override
    public void handleMethodAnnotation(Class<?> clazz, List<Field> fields, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        if (contextEnricher.getBlueprintConfiguration().getNamespaces().contains(NS_JPA)) {
            for (final Field field : fields) {
                final String name = field.getName();
                final PersistenceContext persistenceContext = field.getAnnotation(PersistenceContext.class);
                beanEnricher.addBeanContentWriter("javax.persistence.field.context/" + name, new XmlWriter() {
                    @Override
                    public void write(XMLStreamWriter writer) throws XMLStreamException {
                        writer.writeEmptyElement("context");
                        writer.writeDefaultNamespace(NS_JPA);
                        writer.writeAttribute("unitname", persistenceContext.unitName());
                        writer.writeAttribute("property", name);
                    }
                });
            }
        }
        if (contextEnricher.getBlueprintConfiguration().getNamespaces().contains(NS_JPA2)) {
            contextEnricher.addBlueprintContentWriter("javax.persistence.enableJpa2", new XmlWriter() {
                @Override
                public void write(XMLStreamWriter writer) throws XMLStreamException {
                    writer.writeEmptyElement("enable");
                    writer.writeDefaultNamespace(NS_JPA2);
                }
            });
        }
    }
}
