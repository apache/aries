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
package org.apache.aries.blueprint.plugin.handlers.javax;

import org.apache.aries.blueprint.plugin.spi.*;

import javax.transaction.Transactional;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;

public abstract class AbstractTransactionFactory<T extends Annotation> implements BeanAnnotationHandler<T>, MethodAnnotationHandler<T> {
    protected abstract String getTransactionTypeName(AnnotatedElement annotatedElement);

    private static final String ENABLE_ANNOTATION = "transaction.enableAnnotation";
    @Override
    public void handleMethodAnnotation(Class<?> clazz, List<Method> methods, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        final String nsTx1 = Namespaces.getNamespaceByPattern(contextEnricher.getBlueprintConfiguration().getNamespaces(), Namespaces.PATTERN_NS_TX1);
        if (nsTx1 != null) {
            enableAnnotationTx1(contextEnricher, nsTx1);
            for (final Method method : methods) {
                final String transactionTypeName = getTransactionTypeName(method);
                final String name = method.getName();
                beanEnricher.addBeanContentWriter("javax.transactional.method/" + clazz.getName() + "/" + name + "/" + transactionTypeName, new XmlWriter() {
                    @Override
                    public void write(XMLStreamWriter writer) throws XMLStreamException {
                        writer.writeEmptyElement("transaction");
                        writer.writeDefaultNamespace(nsTx1);
                        writer.writeAttribute("method", name);
                        writer.writeAttribute("value", transactionTypeName);
                    }
                });
            }
        }
        final String nsTx2 = Namespaces.getNamespaceByPattern(contextEnricher.getBlueprintConfiguration().getNamespaces(), Namespaces.PATTERN_NS_TX2);
        if (nsTx2 != null) {
            insertEnableAnnotationTx2(contextEnricher, nsTx2);
        }
    }

    @Override
    public void handleBeanAnnotation(AnnotatedElement annotatedElement, String id, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        final String nsTx1 = Namespaces.getNamespaceByPattern(contextEnricher.getBlueprintConfiguration().getNamespaces(), Namespaces.PATTERN_NS_TX1);
        if (nsTx1 != null) {
            enableAnnotationTx1(contextEnricher, nsTx1);
            final String transactionTypeName = getTransactionTypeName(annotatedElement);
            beanEnricher.addBeanContentWriter("javax.transactional.method/" + annotatedElement + "/*/" + transactionTypeName, new XmlWriter() {
                @Override
                public void write(XMLStreamWriter writer) throws XMLStreamException {
                    writer.writeEmptyElement("transaction");
                    writer.writeDefaultNamespace(nsTx1);
                    writer.writeAttribute("method", "*");
                    writer.writeAttribute("value", transactionTypeName);
                }
            });
        }
        final String nsTx2 = Namespaces.getNamespaceByPattern(contextEnricher.getBlueprintConfiguration().getNamespaces(), Namespaces.PATTERN_NS_TX2);
        if (nsTx2 != null) {
            insertEnableAnnotationTx2(contextEnricher, nsTx2);
        }
    }


    private void enableAnnotationTx1(ContextEnricher contextEnricher, final String nsTx1) {
        // TX1 enable-annotation are valid only in 1.2.0 schema
        if (Namespaces.isTX12(nsTx1) && getEnableAnnotationConfig(contextEnricher.getBlueprintConfiguration())) {
            insertEnableAnnotationTx1(contextEnricher, nsTx1);
        }
    }

    private boolean getEnableAnnotationConfig(BlueprintConfiguration blueprintConfig) {
        String enableAnnotation = blueprintConfig.getCustomParameters().get(ENABLE_ANNOTATION);
        return enableAnnotation == null || Boolean.parseBoolean(enableAnnotation);
    }

    private void insertEnableAnnotationTx1(ContextEnricher contextEnricher, final String namespace) {
        contextEnricher.addBlueprintContentWriter("transaction/ennable-annotation", new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeEmptyElement("enable-annotations");
                writer.writeDefaultNamespace(namespace);
            }
        });
    }

    private void insertEnableAnnotationTx2(ContextEnricher contextEnricher, final String namespace) {
        contextEnricher.addBlueprintContentWriter("transaction/ennable-annotation", new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeEmptyElement("enable");
                writer.writeDefaultNamespace(namespace);
            }
        });
    }
}
