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

import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.PassThroughMetadataImpl;
import org.apache.aries.blueprint.reflect.RefMetadataImpl;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.NullSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Blueprint NamespaceHandler wrapper for a spring NamespaceHandler
 */
public class BlueprintNamespaceHandler implements NamespaceHandler {

    public static final String SPRING_CONTEXT_ID = "." + org.springframework.beans.factory.xml.ParserContext.class.getName();
    public static final String SPRING_BEAN_PROCESSOR_ID = "." + SpringBeanProcessor.class.getName();
    public static final String SPRING_APPLICATION_CONTEXT_ID = "." + ApplicationContext.class.getName();
    public static final String SPRING_BEAN_FACTORY_ID = "." + BeanFactory.class.getName();

    private final Bundle bundle;
    private final Properties schemas;
    private final org.springframework.beans.factory.xml.NamespaceHandler springHandler;

    public BlueprintNamespaceHandler(Bundle bundle, Properties schemas, org.springframework.beans.factory.xml.NamespaceHandler springHandler) {
        this.bundle = bundle;
        this.schemas = schemas;
        this.springHandler = springHandler;
        springHandler.init();
    }

    public org.springframework.beans.factory.xml.NamespaceHandler getSpringHandler() {
        return springHandler;
    }

    @Override
    public URL getSchemaLocation(String s) {
        if (schemas.containsKey(s)) {
            return bundle.getResource(schemas.getProperty(s));
        }
        return null;
    }

    @Override
    public Set<Class> getManagedClasses() {
        return Collections.<Class>singleton(BeanDefinition.class);
    }

    @Override
    public Metadata parse(Element element, ParserContext parserContext) {
        try {
            // Get the spring context
            org.springframework.beans.factory.xml.ParserContext springContext
                    = getOrCreateParserContext(parserContext);
            // Parse spring bean
            springHandler.parse(element, springContext);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext parserContext) {
        throw new UnsupportedOperationException();
    }

    private org.springframework.beans.factory.xml.ParserContext getOrCreateParserContext(ParserContext parserContext) {
        ComponentDefinitionRegistry registry = parserContext.getComponentDefinitionRegistry();
        org.springframework.beans.factory.xml.ParserContext springContext;
        ComponentMetadata contextMetadata = registry.getComponentDefinition(SPRING_CONTEXT_ID);
        if (contextMetadata == null) {
            ExtendedBlueprintContainer container = getBlueprintContainer(parserContext);
            // Create spring application context
            SpringApplicationContext applicationContext = new SpringApplicationContext(container);
            registry.registerComponentDefinition(new PassThroughMetadataImpl(
                    SPRING_APPLICATION_CONTEXT_ID, applicationContext
            ));
            // Create registry
            DefaultListableBeanFactory beanFactory = applicationContext.getBeanFactory();
            registry.registerComponentDefinition(new PassThroughMetadataImpl(
                    SPRING_BEAN_FACTORY_ID, beanFactory
            ));
            // Create spring context
            springContext = createSpringParserContext(parserContext, beanFactory);
            registry.registerComponentDefinition(new PassThroughMetadataImpl(
                    SPRING_CONTEXT_ID, springContext
            ));
            // Create processor
            BeanMetadataImpl bm = new BeanMetadataImpl();
            bm.setId(SPRING_BEAN_PROCESSOR_ID);
            bm.setProcessor(true);
            bm.setScope(BeanMetadata.SCOPE_SINGLETON);
            bm.setRuntimeClass(SpringBeanProcessor.class);
            bm.setActivation(BeanMetadata.ACTIVATION_EAGER);
            bm.addArgument(new RefMetadataImpl("blueprintBundleContext"), null, 0);
            bm.addArgument(new RefMetadataImpl("blueprintContainer"), null, 0);
            bm.addArgument(new RefMetadataImpl(SPRING_APPLICATION_CONTEXT_ID), null, 0);
            registry.registerComponentDefinition(bm);
        } else {
            PassThroughMetadata ptm = (PassThroughMetadata) contextMetadata;
            springContext = (org.springframework.beans.factory.xml.ParserContext) ptm.getObject();
        }
        return springContext;
    }

    private ExtendedBlueprintContainer getBlueprintContainer(ParserContext parserContext) {
        return (ExtendedBlueprintContainer) ((PassThroughMetadata) parserContext.getComponentDefinitionRegistry().getComponentDefinition("blueprintContainer")).getObject();
    }

    private org.springframework.beans.factory.xml.ParserContext createSpringParserContext(ParserContext parserContext, DefaultListableBeanFactory registry) {
        try {
            XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(registry);
            Resource resource = new UrlResource(parserContext.getSourceNode().getOwnerDocument().getDocumentURI());
            ProblemReporter problemReporter = new FailFastProblemReporter();
            ReaderEventListener listener = new EmptyReaderEventListener();
            SourceExtractor extractor = new NullSourceExtractor();
            NamespaceHandlerResolver resolver = new SpringNamespaceHandlerResolver(parserContext);
            XmlReaderContext xmlReaderContext = new XmlReaderContext(resource, problemReporter, listener, extractor, xbdr, resolver);
            BeanDefinitionParserDelegate bdpd = new BeanDefinitionParserDelegate(xmlReaderContext);
            return new org.springframework.beans.factory.xml.ParserContext(xmlReaderContext, bdpd);
        } catch (Exception e) {
            throw new RuntimeException("Error creating spring parser context", e);
        }
    }


}
