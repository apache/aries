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

import java.io.OutputStream;
import java.lang.reflect.Field;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.aries.blueprint.plugin.model.Bean;
import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.model.OsgiServiceRef;
import org.apache.aries.blueprint.plugin.model.ProducedBean;
import org.apache.aries.blueprint.plugin.model.Property;
import org.apache.aries.blueprint.plugin.model.PropertyWriter;
import org.apache.aries.blueprint.plugin.model.TransactionalDef;

public class Generator implements PropertyWriter {
    private static final String NS_BLUEPRINT = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    private static final String NS_EXT = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0";
    private static final String NS_JPA = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    private static final String NS_JPA2 = "http://aries.apache.org/xmlns/jpa/v2.0.0";
    private static final String NS_TX = "http://aries.apache.org/xmlns/transactions/v1.2.0";

    private Context context;
    private XMLStreamWriter writer;
    private boolean persistenceAnnotated;

    public Generator(Context context, OutputStream os, boolean persistenceAnnotated) throws XMLStreamException {
        this.context = context;
        this.persistenceAnnotated = persistenceAnnotated;
        
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        writer = factory.createXMLStreamWriter(os);
    }

    public void generate() {
        try {
            writer.writeStartDocument();
            writer.writeCharacters("\n");
            writeBlueprint();
            writer.writeCharacters("\n");
            
            if (persistenceAnnotated) {
                if (isJpaUsed()) {
                    writer.writeEmptyElement(NS_JPA2, "enable");
                    writer.writeCharacters("\n");
                }
                writer.writeEmptyElement(NS_TX, "enable-annotations");
                writer.writeCharacters("\n");
                //if (isJtaUsed()) {
//                    writer.writeEmptyElement(NS_TX, "enable-annotations");
                //}
            }
            for (Bean bean : context.getBeans()) {
                writeBeanStart(bean);
                bean.writeProperties(this);
                writer.writeEndElement();
                writer.writeCharacters("\n");
            }
            
            new OsgiServiceRefWriter(writer).write(context.getServiceRefs());
            new OsgiServiceProviderWriter(writer).write(context.getBeans());
            
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndDocument();
            writer.writeCharacters("\n");
            writer.close();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean isJpaUsed() {
        boolean jpaUsed = false;
        for (Bean bean : context.getBeans()) {
        if (bean.persistenceFields.length > 0) {
            jpaUsed = true;
        }
        }
        return jpaUsed;
    }

    private boolean isJtaUsed() {
        boolean jtaUsed = false;
        for (Bean bean : context.getBeans()) {
            if (bean.transactionDef != null) {
                jtaUsed = true;
            }

        }
        return jtaUsed;
    }

    private void writeBlueprint() throws XMLStreamException {
        writer.writeStartElement("blueprint");
        writer.writeDefaultNamespace(NS_BLUEPRINT);
        writer.writeNamespace("ext", NS_EXT);
        if (persistenceAnnotated) {
            writer.writeNamespace("jpa", NS_JPA2);
        } else {
            writer.writeNamespace("jpa", NS_JPA);
        }
        writer.writeNamespace("tx", NS_TX);
    }
    
    public void writeBeanStart(Bean bean) throws XMLStreamException {
        writer.writeStartElement("bean");
        writer.writeAttribute("id", bean.id);
        writer.writeAttribute("class", bean.clazz.getName());
        writer.writeAttribute("ext", NS_EXT, "field-injection", "true");
        if (bean instanceof ProducedBean) {
            writeFactory((ProducedBean)bean);
        }
        if (bean.initMethod != null) {
            writer.writeAttribute("init-method", bean.initMethod);
        }
        if (bean.destroyMethod != null) {
            writer.writeAttribute("destroy-method", bean.destroyMethod);
        }
        writer.writeCharacters("\n");
        
        if (!persistenceAnnotated) {
            writeTransactional(bean.transactionDef);
            writePersistenceFields(bean.persistenceFields);
        }
    }
    
    private void writeFactory(ProducedBean bean) throws XMLStreamException {
        writer.writeAttribute("factory-ref", bean.factoryBean.id);
        writer.writeAttribute("factory-method", bean.factoryMethod);
    }

    private void writeTransactional(TransactionalDef transactionDef)
            throws XMLStreamException {
        if (transactionDef != null) {
            writer.writeCharacters("    ");
            writer.writeEmptyElement("tx", "transaction", NS_TX);
            writer.writeAttribute("method", transactionDef.getMethod());
            writer.writeAttribute("value", transactionDef.getType());
            writer.writeCharacters("\n");
        }
    }

    
    private void writePersistenceFields(Field[] fields) throws XMLStreamException {
        for (Field field : fields) {
            writePersistenceField(field);
        }
    }

    private void writePersistenceField(Field field) throws XMLStreamException {
        PersistenceContext persistenceContext = field.getAnnotation(PersistenceContext.class);
        if (persistenceContext != null) {
            writer.writeCharacters("    ");
            writer.writeEmptyElement("jpa", "context", NS_JPA);
            writer.writeAttribute("unitname", persistenceContext.unitName());
            writer.writeAttribute("property", field.getName());
            writer.writeCharacters("\n");
        }
        PersistenceUnit persistenceUnit = field.getAnnotation(PersistenceUnit.class);
        if (persistenceUnit != null) {
            writer.writeCharacters("    ");
            writer.writeEmptyElement("jpa", "unit", NS_JPA);
            writer.writeAttribute("unitname", persistenceUnit.unitName());
            writer.writeAttribute("property", field.getName());
            writer.writeCharacters("\n");
        }
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

}
