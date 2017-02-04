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

import org.apache.aries.blueprint.plugin.handlers.Handlers;
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextInitializationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.apache.aries.blueprint.plugin.model.Bean.NS_EXT;

public class Blueprint implements BlueprintRegistry, ContextEnricher, XmlWriter {
    private static final String NS_BLUEPRINT = "http://www.osgi.org/xmlns/blueprint/v1.0.0";

    private Registry registry = new Registry();
    private final Map<String, XmlWriter> blueprintWriters = new HashMap<>();
    private final BlueprintConfiguration blueprintConfiguration;

    Blueprint(BlueprintConfiguration blueprintConfiguration, Class<?>... beanClasses) {
        this(blueprintConfiguration, Arrays.asList(beanClasses));
    }

    public Blueprint(BlueprintConfiguration blueprintConfiguration, Collection<Class<?>> beanClasses) {
        this.blueprintConfiguration = blueprintConfiguration;
        initContext();
        addBeans(beanClasses);
        resolve();
    }

    private void initContext() {
        for (ContextInitializationHandler contextInitializationHandler : Handlers.CONTEXT_INITIALIZATION_HANDLERS) {
            contextInitializationHandler.initContext(this);
        }
    }

    private void addBeans(Collection<Class<?>> beanClasses) {
        for (Class<?> clazz : beanClasses) {
            addBean(clazz);
        }
    }

    private void addBean(Class<?> clazz) {
        Bean bean = new Bean(clazz, this);
        registry.addBean(bean);
        addBeansFromFactories(bean);
    }

    private void addBeansFromFactories(BeanRef factoryBean) {
        for (Method method : factoryBean.clazz.getMethods()) {
            if (!isFactoryMethod(method)) {
                continue;
            }
            String name = AnnotationHelper.findName(method.getAnnotations());
            Class<?> beanClass = method.getReturnType();
            BeanFromFactory beanFromFactory;
            if (name != null) {
                beanFromFactory = new BeanFromFactory(beanClass, name, factoryBean, method, this);
            } else {
                beanFromFactory = new BeanFromFactory(beanClass, factoryBean, method, this);
            }
            if (AnnotationHelper.findSingletons(method.getAnnotations())) {
                beanFromFactory.setSingleton();
            }
            registry.addBean(beanFromFactory);
        }
    }

    private boolean isFactoryMethod(Method method) {
        boolean isFactoryMethod = false;
        for (Class<? extends Annotation> factoryMethodAnnotationClass : Handlers.FACTORY_METHOD_ANNOTATION_CLASSES) {
            Annotation annotation = AnnotationHelper.findAnnotation(method.getAnnotations(), factoryMethodAnnotationClass);
            if (annotation != null) {
                isFactoryMethod = true;
                break;
            }
        }
        return isFactoryMethod;
    }

    private void resolve() {
        for (Bean bean : getBeans()) {
            bean.resolve(this);
        }
    }

    public BeanRef getMatching(BeanRef template) {
        for (BeanRef bean : registry.getBeans()) {
            if (bean.matches(template)) {
                return bean;
            }
        }
        return null;
    }

    SortedSet<Bean> getBeans() {
        TreeSet<Bean> beans = new TreeSet<Bean>();
        for (BeanRef ref : registry.getBeans()) {
            if (ref instanceof Bean) {
                beans.add((Bean) ref);
            }
        }
        return beans;
    }

    Map<String, XmlWriter> getBlueprintWriters() {
        return blueprintWriters;
    }

    @Override
    public void addBean(String id, Class<?> clazz) {
        registry.addBean(new BeanRef(clazz, id));

    }

    @Override
    public void addBlueprintContentWriter(String id, XmlWriter blueprintWriter) {
        blueprintWriters.put(id, blueprintWriter);
    }

    @Override
    public BlueprintConfiguration getBlueprintConfiguration() {
        return blueprintConfiguration;
    }

    public void write(XMLStreamWriter writer) throws XMLStreamException {
        writeBlueprint(writer);

        for (Bean bean : getBeans()) {
            bean.write(writer);
        }

        for (XmlWriter bw : getBlueprintWriters().values()) {
            bw.write(writer);
        }

        writer.writeEndElement();
    }

    private void writeBlueprint(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("blueprint");
        writer.writeDefaultNamespace(NS_BLUEPRINT);
        writer.writeNamespace("ext", NS_EXT);
        if (blueprintConfiguration.getDefaultActivation() != null) {
            writer.writeAttribute("default-activation", blueprintConfiguration.getDefaultActivation().name().toLowerCase());
        }
    }

    public boolean shouldBeGenerated() {
        return !getBeans().isEmpty();
    }
}
