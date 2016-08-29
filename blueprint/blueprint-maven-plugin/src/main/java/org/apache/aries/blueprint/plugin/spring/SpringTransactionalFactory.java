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
package org.apache.aries.blueprint.plugin.spring;

import com.google.common.base.CaseFormat;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.MethodAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;

public class SpringTransactionalFactory implements BeanAnnotationHandler<Transactional>, MethodAnnotationHandler<Transactional> {
    public static final String NS_TX = "http://aries.apache.org/xmlns/transactions/v1.2.0";
    public static final String NS_TX2 = "http://aries.apache.org/xmlns/transactions/v2.0.0";

    private String getTransactionTypeName(Transactional transactional) {
        Propagation propagation = transactional.propagation();
        if (propagation == Propagation.NESTED) {
            throw new UnsupportedOperationException("Nested transactions not supported");
        }
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, propagation.name());
    }

    @Override
    public Class<Transactional> getAnnotation() {
        return Transactional.class;
    }

    @Override
    public void handleMethodAnnotation(Class<?> clazz, List<Method> methods, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        if (contextEnricher.getBlueprintConfiguration().getNamespaces().contains(NS_TX)) {
            enableAnnotations(contextEnricher);
            for (final Method method : methods) {
                final Transactional transactional = method.getAnnotation(Transactional.class);
                final String transactionTypeName = getTransactionTypeName(transactional);
                final String name = method.getName();
                beanEnricher.addBeanContentWriter("javax.transactional.method/" + clazz.getName() + "/" + name + "/" + transactionTypeName, new XmlWriter() {
                    @Override
                    public void write(XMLStreamWriter writer) throws XMLStreamException {
                        writer.writeEmptyElement(NS_TX, "transaction");
                        writer.writeAttribute("method", name);
                        writer.writeAttribute("value", transactionTypeName);
                        writer.writeCharacters("\n");
                    }
                });
            }
        }
        if (contextEnricher.getBlueprintConfiguration().getNamespaces().contains(NS_TX2)) {
            enableTransactionsTx2(contextEnricher);
        }
    }

    private void enableAnnotations(ContextEnricher contextEnricher) {
        contextEnricher.addBlueprintContentWriter("transaction/ennable-annotation", new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeEmptyElement(NS_TX, "enable-annotations");
            }
        });
    }

    @Override
    public void handleBeanAnnotation(AnnotatedElement annotatedElement, String id, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        if (contextEnricher.getBlueprintConfiguration().getNamespaces().contains(NS_TX)) {
            enableAnnotations(contextEnricher);
            final Transactional transactional = annotatedElement.getAnnotation(Transactional.class);
            final String transactionTypeName = getTransactionTypeName(transactional);
            beanEnricher.addBeanContentWriter("javax.transactional.method/" + annotatedElement + "/*/" + transactionTypeName, new XmlWriter() {
                @Override
                public void write(XMLStreamWriter writer) throws XMLStreamException {
                    writer.writeEmptyElement(NS_TX, "transaction");
                    writer.writeAttribute("method", "*");
                    writer.writeAttribute("value", transactionTypeName);
                    writer.writeCharacters("\n");
                }
            });
        }
        if (contextEnricher.getBlueprintConfiguration().getNamespaces().contains(NS_TX2)) {
            enableTransactionsTx2(contextEnricher);
        }
    }

    private void enableTransactionsTx2(ContextEnricher contextEnricher) {
        contextEnricher.addBlueprintContentWriter("transaction/ennable-annotation", new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeEmptyElement(NS_TX2, "enable");
            }
        });
    }
}
