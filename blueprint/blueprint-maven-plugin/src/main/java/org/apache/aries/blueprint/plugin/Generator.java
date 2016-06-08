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
import org.apache.aries.blueprint.plugin.model.TransactionalDef;
import org.apache.aries.blueprint.plugin.model.service.ServiceProviderWriter;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Generator implements PropertyWriter, ArgumentWriter {
    private static final String NS_BLUEPRINT = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    private static final String NS_EXT = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0";
    public static final String NS_JPA = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    public static final String NS_JPA2 = "http://aries.apache.org/xmlns/jpa/v2.0.0";
    public static final String NS_TX = "http://aries.apache.org/xmlns/transactions/v1.2.0";
    public static final String NS_TX2 = "http://aries.apache.org/xmlns/transactions/v2.0.0";

    private final Context context;
    private final XMLStreamWriter writer;
    private final Set<String> namespaces;
    private final Activation defaultActivation;

    public Generator(Context context, OutputStream os, Set<String> namespaces, Activation defaultActivation) throws XMLStreamException {
        this.context = context;
        this.namespaces = namespaces != null ? namespaces :  new HashSet<>(Arrays.asList(NS_TX2, NS_JPA2));
        this.defaultActivation = defaultActivation;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        writer = factory.createXMLStreamWriter(os);
    }

    public void generate() {
        try {
            writer.writeStartDocument();
            writer.writeCharacters("\n");
            writeBlueprint();
            writer.writeCharacters("\n");

            if (namespaces.contains(NS_JPA2) && isJpaUsed()) {
                writer.writeEmptyElement(NS_JPA2, "enable");
                writer.writeCharacters("\n");
            }
            if (namespaces.contains(NS_TX) && isJtaUsed()) {
                writer.writeEmptyElement(NS_TX, "enable-annotations");
                writer.writeCharacters("\n");
            }
            if (namespaces.contains(NS_TX2) && isJtaUsed()) {
                writer.writeEmptyElement(NS_TX2, "enable");
                writer.writeCharacters("\n");
            }
            for (Bean bean : context.getBeans()) {
                writeBeanStart(bean);
                bean.writeArguments(this);
                bean.writeProperties(this);
                writer.writeEndElement();
                writer.writeCharacters("\n");
            }

            new OsgiServiceRefWriter(writer).write(context.getServiceRefs());
            new ServiceProviderWriter(writer).write(context.getServiceProviders());

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
            if (bean.persistenceFields.size() > 0) {
                jpaUsed = true;
            }
        }
        return jpaUsed;
    }

    private boolean isJtaUsed() {
        for (Bean bean : context.getBeans()) {
            if (!bean.transactionDefs.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void writeBlueprint() throws XMLStreamException {
        writer.writeStartElement("blueprint");
        writer.writeDefaultNamespace(NS_BLUEPRINT);
        writer.writeNamespace("ext", NS_EXT);
        for (String namespace : namespaces) {
            String prefix = getPrefixForNamesapace(namespace);
            writer.writeNamespace(prefix, namespace);
        }
        if (defaultActivation != null) {
            writer.writeAttribute("default-activation", defaultActivation.name().toLowerCase());
        }
    }

    private String getPrefixForNamesapace(String namespace) {
        if (namespace.contains("jpa")) {
            return "jpa";
        }
        if (namespace.contains("transactions")) {
            return "tx";
        }
        return "other";
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
        if (bean.activation != null) {
            writer.writeAttribute("activation", bean.activation.toString());
        }
        if (bean.dependsOn != null) {
            writer.writeAttribute("depends-on", bean.dependsOn);
        }
        if (bean instanceof ProducedBean) {
            writeFactory((ProducedBean) bean);
        }
        if (bean.initMethod != null) {
            writer.writeAttribute("init-method", bean.initMethod);
        }
        if (bean.destroyMethod != null) {
            writer.writeAttribute("destroy-method", bean.destroyMethod);
        }
        writer.writeCharacters("\n");

        if (namespaces.contains(NS_TX)) {
            for (TransactionalDef transactionalDef : bean.transactionDefs) {
                writeTransactional(transactionalDef);
            }
        }
        if (namespaces.contains(NS_JPA)) {
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


    private void writePersistenceFields(List<Field> fields) throws XMLStreamException {
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
