/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.spring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A spring namespace handler to handle spring bean elements.
 */
public class BeansNamespaceHandler implements org.springframework.beans.factory.xml.NamespaceHandler {

    @Override
    public void init() {
    }

    @Override
    public BeanDefinition parse(Element ele, ParserContext parserContext) {
        if (BeanDefinitionParserDelegate.BEAN_ELEMENT.equals(ele.getLocalName())) {
            BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
            BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
            if (bdHolder != null) {
                bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
                try {
                    // Register the final decorated instance.
                    BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, parserContext.getReaderContext().getRegistry());
                } catch (BeanDefinitionStoreException ex) {
                    parserContext.getReaderContext().error("Failed to register bean definition with name '" +
                            bdHolder.getBeanName() + "'", ele, ex);
                }
                // Send registration event.
                parserContext.getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
            }
        } else if (DefaultBeanDefinitionDocumentReader.NESTED_BEANS_ELEMENT.equals(ele.getLocalName())) {
            MyDefaultBeanDefinitionDocumentReader reader = new MyDefaultBeanDefinitionDocumentReader();
            reader.registerBeanDefinitions(ele, parserContext.getReaderContext());
        } else {
            throw new UnsupportedOperationException();
        }
        return null;
    }

    @Override
    public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext) {
        return definition;
    }

    private static class MyDefaultBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

        public void registerBeanDefinitions(final Element ele, XmlReaderContext readerContext) {
            Document doc = (Document) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Document.class}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("getDocumentElement")) {
                        return ele;
                    }
                    throw new UnsupportedOperationException();
                }
            });
            registerBeanDefinitions(doc, readerContext);
        }

    }
}
