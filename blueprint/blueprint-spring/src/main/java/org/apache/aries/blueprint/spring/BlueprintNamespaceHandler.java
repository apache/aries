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
import org.apache.aries.blueprint.NamespaceHandler2;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
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
public class BlueprintNamespaceHandler implements NamespaceHandler, NamespaceHandler2 {

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
    public boolean usePsvi() {
        return true;
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
            BeanDefinition bd = springHandler.parse(element, springContext);
            for (String name : springContext.getRegistry().getBeanDefinitionNames()) {
                if (springContext.getRegistry().getBeanDefinition(name) == bd) {
                    ComponentDefinitionRegistry registry = parserContext.getComponentDefinitionRegistry();
                    if (registry.containsComponentDefinition(name)) {
                        // Hack: we can't really make the difference between a top level bean
                        // and an inlined bean when using custom (eventually nested) namespaces.
                        // To work around the problem, the BlueprintBeanFactory will always register
                        // a BeanMetadata for each bean, but here, we unregister it and return it instead
                        // so that the caller is responsible for registering the metadata.
                        ComponentMetadata metadata = registry.getComponentDefinition(name);
                        registry.removeComponentDefinition(name);
                        return metadata;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext parserContext) {
        return componentMetadata;
    }

    private org.springframework.beans.factory.xml.ParserContext getOrCreateParserContext(ParserContext parserContext) {
        ComponentDefinitionRegistry registry = parserContext.getComponentDefinitionRegistry();
        ExtendedBlueprintContainer container = getBlueprintContainer(parserContext);
        // Create spring application context
        SpringApplicationContext applicationContext = getPassThrough(parserContext,
                SPRING_APPLICATION_CONTEXT_ID, SpringApplicationContext.class);
        if (applicationContext == null) {
            applicationContext = new SpringApplicationContext(container);
            registry.registerComponentDefinition(createPassThrough(parserContext,
                    SPRING_APPLICATION_CONTEXT_ID, applicationContext, "destroy"
            ));
        }
        // Create registry
        DefaultListableBeanFactory beanFactory = getPassThrough(parserContext,
                SPRING_BEAN_FACTORY_ID, DefaultListableBeanFactory.class);
        if (beanFactory == null) {
            beanFactory = applicationContext.getBeanFactory();
            registry.registerComponentDefinition(createPassThrough(parserContext,
                    SPRING_BEAN_FACTORY_ID, beanFactory
            ));
        }
        // Create spring parser context
        org.springframework.beans.factory.xml.ParserContext springParserContext
                = getPassThrough(parserContext, SPRING_CONTEXT_ID, org.springframework.beans.factory.xml.ParserContext.class);
        if (springParserContext == null) {
            // Create spring context
            springParserContext = createSpringParserContext(parserContext, beanFactory);
            registry.registerComponentDefinition(createPassThrough(parserContext,
                    SPRING_CONTEXT_ID, springParserContext
            ));
        }
        // Create processor
        if (!parserContext.getComponentDefinitionRegistry().containsComponentDefinition(SPRING_BEAN_PROCESSOR_ID)) {
            MutableBeanMetadata bm = parserContext.createMetadata(MutableBeanMetadata.class);
            bm.setId(SPRING_BEAN_PROCESSOR_ID);
            bm.setProcessor(true);
            bm.setScope(BeanMetadata.SCOPE_SINGLETON);
            bm.setRuntimeClass(SpringBeanProcessor.class);
            bm.setActivation(BeanMetadata.ACTIVATION_EAGER);
            bm.addArgument(createRef(parserContext, "blueprintBundleContext"), null, 0);
            bm.addArgument(createRef(parserContext, "blueprintContainer"), null, 0);
            bm.addArgument(createRef(parserContext, SPRING_APPLICATION_CONTEXT_ID), null, 0);
            registry.registerComponentDefinition(bm);
        }
        // Add the namespace handler's bundle to the application context classloader
        applicationContext.addSourceBundle(bundle);
        return springParserContext;
    }

    private ComponentMetadata createPassThrough(ParserContext parserContext, String id, Object o) {
        MutablePassThroughMetadata pt = parserContext.createMetadata(MutablePassThroughMetadata.class);
        pt.setId(id);
        pt.setObject(o);
        return pt;
    }

    private ComponentMetadata createPassThrough(ParserContext parserContext, String id, Object o, String destroy) {
        MutablePassThroughMetadata pt = parserContext.createMetadata(MutablePassThroughMetadata.class);
        pt.setId(id + ".factory");
        pt.setObject(new Holder(o));
        MutableBeanMetadata b = parserContext.createMetadata(MutableBeanMetadata.class);
        b.setId(id);
        b.setFactoryComponent(pt);
        b.setFactoryMethod("getObject");
        b.setDestroyMethod(destroy);
        return b;
    }

    public static class Holder {
        private final Object object;

        public Holder(Object object) {
            this.object = object;
        }

        public Object getObject() {
            return object;
        }
    }

    private Metadata createRef(ParserContext parserContext, String id) {
        MutableRefMetadata ref = parserContext.createMetadata(MutableRefMetadata.class);
        ref.setComponentId(id);
        return ref;
    }

    private ExtendedBlueprintContainer getBlueprintContainer(ParserContext parserContext) {
        ExtendedBlueprintContainer container = getPassThrough(parserContext, "blueprintContainer", ExtendedBlueprintContainer.class);
        if (container == null) {
            throw new IllegalStateException();
        }
        return container;
    }

    @SuppressWarnings("unchecked")
    private <T> T getPassThrough(ParserContext parserContext, String name, Class<T> clazz) {
        Metadata metadata = parserContext.getComponentDefinitionRegistry().getComponentDefinition(name);
        if (metadata instanceof BeanMetadata) {
            BeanMetadata bm = (BeanMetadata) metadata;
            if (bm.getFactoryComponent() instanceof PassThroughMetadata
                    && "getObject".equals(bm.getFactoryMethod())) {
                metadata = bm.getFactoryComponent();
            }
        }
        if (metadata instanceof PassThroughMetadata) {
            Object o = ((PassThroughMetadata) metadata).getObject();
            if (o instanceof Holder) {
                o = ((Holder) o).getObject();
            }
            return (T) o;
        } else {
            return null;
        }
    }

    private org.springframework.beans.factory.xml.ParserContext createSpringParserContext(ParserContext parserContext, DefaultListableBeanFactory registry) {
        try {
            XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(registry);
            Resource resource = new UrlResource(parserContext.getSourceNode().getOwnerDocument().getDocumentURI());
            ProblemReporter problemReporter = new FailFastProblemReporter();
            ReaderEventListener listener = new EmptyReaderEventListener();
            SourceExtractor extractor = new NullSourceExtractor();
            NamespaceHandlerResolver resolver = new SpringNamespaceHandlerResolver(parserContext);
            xbdr.setProblemReporter(problemReporter);
            xbdr.setEventListener(listener);
            xbdr.setSourceExtractor(extractor);
            xbdr.setNamespaceHandlerResolver(resolver);
            XmlReaderContext xmlReaderContext = xbdr.createReaderContext(resource);
            BeanDefinitionParserDelegate bdpd = new BeanDefinitionParserDelegate(xmlReaderContext);
            return new org.springframework.beans.factory.xml.ParserContext(xmlReaderContext, bdpd);
        } catch (Exception e) {
            throw new RuntimeException("Error creating spring parser context", e);
        }
    }


}
