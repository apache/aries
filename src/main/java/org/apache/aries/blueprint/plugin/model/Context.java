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

public class Context implements BlueprintRegister, ContextEnricher, XmlWriter {
    private static final String NS_BLUEPRINT = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    private static final String NS_EXT = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0";

    SortedSet<BeanRef> reg = new TreeSet<BeanRef>();
    private final Map<String, XmlWriter> blueprintWriters = new HashMap<>();
    private final BlueprintConfiguration blueprintConfiguration;

    Context(BlueprintConfiguration blueprintConfiguration, Class<?>... beanClasses) {
        this(blueprintConfiguration, Arrays.asList(beanClasses));
    }

    public Context(BlueprintConfiguration blueprintConfiguration, Collection<Class<?>> beanClasses) {
        this.blueprintConfiguration = blueprintConfiguration;
        initContext();
        addBeans(beanClasses);
        resolve();
    }

    private void initContext() {
        for (ContextInitializationHandler contextInitializationHandler : Handlers.contextInitializationHandlers) {
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
        reg.add(bean);
        reg.addAll(bean.refs);
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
            reg.add(beanFromFactory);
        }
    }

    private boolean isFactoryMethod(Method method) {
        boolean isFactoryMethod = false;
        for (Class<? extends Annotation> factoryMethodAnnotationClass : Handlers.factoryMethodAnnotationClasses) {
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
        for (BeanRef bean : reg) {
            if (bean.matches(template)) {
                return bean;
            }
        }
        return null;
    }

    public SortedSet<Bean> getBeans() {
        TreeSet<Bean> beans = new TreeSet<Bean>();
        for (BeanRef ref : reg) {
            if (ref instanceof Bean) {
                beans.add((Bean) ref);
            }
        }
        return beans;
    }

    public Map<String, XmlWriter> getBlueprintWriters() {
        return blueprintWriters;
    }

    @Override
    public void addBean(String id, Class<?> clazz) {
        reg.add(new BeanRef(clazz, id));

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
            writeBeanStart(writer, bean);
            bean.writeArguments(writer);
            bean.writeProperties(writer);
            writer.writeEndElement();
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

    private void writeBeanStart(XMLStreamWriter writer, Bean bean) throws XMLStreamException {
        writer.writeStartElement("bean");
        writer.writeAttribute("id", bean.id);
        writer.writeAttribute("class", bean.clazz.getName());
        if (bean.needFieldInjection()) {
            writer.writeAttribute("ext", NS_EXT, "field-injection", "true");
        }
        if (bean.isPrototype) {
            writer.writeAttribute("scope", "prototype");
        }

        Map<String, String> attributes = bean.attributes;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            writer.writeAttribute(entry.getKey(), entry.getValue());
        }
        if (bean instanceof BeanFromFactory) {
            writeFactory(writer, (BeanFromFactory) bean);
        }
        for (XmlWriter xmlWriter : bean.beanContentWriters.values()) {
            xmlWriter.write(writer);
        }
    }

    private void writeFactory(XMLStreamWriter writer, BeanFromFactory bean) throws XMLStreamException {
        writer.writeAttribute("factory-ref", bean.factoryBean.id);
        writer.writeAttribute("factory-method", bean.factoryMethod);
    }
}
