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
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.FieldAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.InjectLikeHandler;
import org.apache.aries.blueprint.plugin.spi.MethodAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findSingleton;
import static org.apache.aries.blueprint.plugin.model.NamingHelper.getBeanName;

class Bean implements BeanEnricher, XmlWriter {

    private static final String NS_EXT = "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0";

    String id;
    final Class<?> clazz;
    SortedSet<Property> properties = new TreeSet<>();
    List<Argument> constructorArguments = new ArrayList<>();
    final Map<String, String> attributes = new HashMap<>();
    final Map<String, XmlWriter> beanContentWriters = new HashMap<>();
    protected final ContextEnricher contextEnricher;
    private final Introspector introspector;

    Bean(Class<?> clazz, ContextEnricher contextEnricher) {
        this.clazz = clazz;
        this.id = getBeanName(clazz);
        this.contextEnricher = contextEnricher;
        introspector = new Introspector(clazz);

        setScope(clazz);
        handleCustomBeanAnnotations();
        handleFieldsAnnotation(introspector);
        handleMethodsAnnotation(introspector);
    }

    private void setScope(Class<?> clazz) {
        attributes.put("scope", findSingleton(clazz) ? "singleton" : "prototype");
    }

    void resolveDependency(BlueprintRegistry blueprintRegistry) {
        resolveArguments(blueprintRegistry);
        resolveFields(blueprintRegistry);
        resolveMethods(blueprintRegistry);
    }

    private void handleMethodsAnnotation(Introspector introspector) {
        for (MethodAnnotationHandler methodAnnotationHandler : Handlers.METHOD_ANNOTATION_HANDLERS) {
            List<Method> methods = introspector.methodsWith(methodAnnotationHandler.getAnnotation());
            if (methods.size() > 0) {
                methodAnnotationHandler.handleMethodAnnotation(clazz, methods, contextEnricher, this);
            }
        }
    }

    private void handleFieldsAnnotation(Introspector introspector) {
        for (FieldAnnotationHandler fieldAnnotationHandler : Handlers.FIELD_ANNOTATION_HANDLERS) {
            List<Field> fields = introspector.fieldsWith(fieldAnnotationHandler.getAnnotation());
            if (fields.size() > 0) {
                fieldAnnotationHandler.handleFieldAnnotation(clazz, fields, contextEnricher, this);
            }
        }
    }

    private void handleCustomBeanAnnotations() {
        for (BeanAnnotationHandler beanAnnotationHandler : Handlers.BEAN_ANNOTATION_HANDLERS) {
            Object annotation = AnnotationHelper.findAnnotation(clazz.getAnnotations(), beanAnnotationHandler.getAnnotation());
            if (annotation != null) {
                beanAnnotationHandler.handleBeanAnnotation(clazz, id, contextEnricher, this);
            }
        }
    }

    private void resolveMethods(BlueprintRegistry blueprintRegistry) {
        for (Method method : introspector.methodsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(blueprintRegistry, method);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    private void resolveFields(BlueprintRegistry matcher) {
        for (Field field : introspector.fieldsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(matcher, field);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    protected void resolveArguments(BlueprintRegistry matcher) {
        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        for (Constructor constructor : declaredConstructors) {
            if (declaredConstructors.length == 1 || shouldInject(constructor)) {
                resolveArguments(matcher, constructor.getParameterTypes(), constructor.getParameterAnnotations());
                break;
            }
        }
    }

    private boolean shouldInject(AnnotatedElement annotatedElement) {
        for (InjectLikeHandler injectLikeHandler : Handlers.BEAN_INJECT_LIKE_HANDLERS) {
            if (annotatedElement.getAnnotation(injectLikeHandler.getAnnotation()) != null) {
                return true;
            }
        }
        return false;
    }

    void resolveArguments(BlueprintRegistry blueprintRegistry, Class[] parameterTypes, Annotation[][] parameterAnnotations) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            constructorArguments.add(new Argument(blueprintRegistry, parameterTypes[i], parameterAnnotations[i]));
        }
    }

    private void writeProperties(XMLStreamWriter writer) throws XMLStreamException {
        for (Property property : properties) {
            property.write(writer);
        }
    }

    private void writeArguments(XMLStreamWriter writer) throws XMLStreamException {
        for (Argument argument : constructorArguments) {
            argument.write(writer);
        }
    }

    private boolean needFieldInjection() {
        for (Property property : properties) {
            if (property.isField) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public void addBeanContentWriter(String id, XmlWriter blueprintWriter) {
        beanContentWriters.put(id, blueprintWriter);
    }

    private void writeCustomContent(XMLStreamWriter writer) throws XMLStreamException {
        for (XmlWriter xmlWriter : beanContentWriters.values()) {
            xmlWriter.write(writer);
        }
    }

    private void writeAttributes(XMLStreamWriter writer) throws XMLStreamException {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if ("scope".equals(entry.getKey()) && "singleton".equals(entry.getValue())) {
                continue;
            }
            writer.writeAttribute(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void write(XMLStreamWriter writer) throws XMLStreamException {
        writeBeanStart(writer);
        writeCustomContent(writer);
        writeArguments(writer);
        writeProperties(writer);
        writer.writeEndElement();
    }

    private void writeBeanStart(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("bean");
        writer.writeAttribute("id", id);
        writer.writeAttribute("class", clazz.getName());
        if (needFieldInjection()) {
            writer.writeNamespace("ext", NS_EXT);
            writer.writeAttribute("ext", NS_EXT, "field-injection", "true");
        }
        writeAttributes(writer);
    }

    BeanRef toBeanRef() {
        return new BeanRef(clazz, id, clazz.getAnnotations());
    }
}
