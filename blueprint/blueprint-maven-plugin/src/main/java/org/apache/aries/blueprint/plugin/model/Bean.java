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

import org.apache.aries.blueprint.plugin.Extensions;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.FieldAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.InjectLikeHandler;
import org.apache.aries.blueprint.plugin.spi.MethodAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findName;
import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findValue;

public class Bean extends BeanRef implements BeanEnricher {
    public SortedSet<Property> properties = new TreeSet<>();
    public List<Argument> constructorArguments = new ArrayList<>();
    public boolean isPrototype;
    public final Map<String, String> attributes = new HashMap<>();
    public final Set<BeanRef> refs = new HashSet<>();
    public final Map<String, XmlWriter> beanContentWriters = new HashMap<>();
    protected final ContextEnricher contextEnricher;

    public Bean(Class<?> clazz, ContextEnricher contextEnricher) {
        super(clazz, BeanRef.getBeanName(clazz));
        this.contextEnricher = contextEnricher;
        Introspector introspector = new Introspector(clazz);

        this.isPrototype = isPrototype(clazz);

        setQualifiersFromAnnotations(clazz.getAnnotations());

        handleCustomBeanAnnotations();

        handleFieldsAnnotation(introspector);

        handleMethodsAnnotation(introspector);
    }

    public void resolve(BlueprintRegister blueprintRegister) {
        resolveArguments(blueprintRegister);
        resolveFields(blueprintRegister);
        resolveMethods(blueprintRegister);
    }

    private void handleMethodsAnnotation(Introspector introspector) {
        for (MethodAnnotationHandler methodAnnotationHandler : Extensions.methodAnnotationHandlers) {
            List<Method> methods = introspector.methodsWith(methodAnnotationHandler.getAnnotation());
            if (methods.size() > 0) {
                methodAnnotationHandler.handleMethodAnnotation(clazz, methods, contextEnricher, this);
            }
        }
    }

    private void handleFieldsAnnotation(Introspector introspector) {
        for (FieldAnnotationHandler fieldAnnotationHandler : Extensions.fieldAnnotationHandlers) {
            List<Field> fields = introspector.fieldsWith(fieldAnnotationHandler.getAnnotation());
            if (fields.size() > 0) {
                fieldAnnotationHandler.handleFieldAnnotation(clazz, fields, contextEnricher, this);
            }
        }
    }

    private void handleCustomBeanAnnotations() {
        for (BeanAnnotationHandler beanAnnotationHandler : Extensions.BEAN_ANNOTATION_HANDLERs) {
            Object annotation = AnnotationHelper.findAnnotation(clazz.getAnnotations(), beanAnnotationHandler.getAnnotation());
            if (annotation != null) {
                beanAnnotationHandler.handleBeanAnnotation(clazz, id, contextEnricher, this);
            }
        }
    }

    private boolean isPrototype(Class<?> clazz) {
        return !findSingleton(clazz);
    }

    private boolean findSingleton(Class clazz) {
        for (Class<?> singletonAnnotation : Extensions.singletons) {
            if (clazz.getAnnotation(singletonAnnotation) != null) {
                return true;
            }
        }
        return false;
    }


    private void resolveMethods(BlueprintRegister blueprintRegister) {
        for (Method method : new Introspector(clazz).methodsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(blueprintRegister, method);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    private void resolveFields(BlueprintRegister matcher) {
        for (Field field : new Introspector(clazz).fieldsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(matcher, field);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    protected void resolveArguments(BlueprintRegister matcher) {
        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        for (Constructor constructor : declaredConstructors) {
            if (declaredConstructors.length == 1 || shouldInject(constructor)) {
                resolveArguments(matcher, constructor.getParameterTypes(), constructor.getParameterAnnotations());
                break;
            }
        }
    }

    private boolean shouldInject(AnnotatedElement annotatedElement) {
        for (InjectLikeHandler injectLikeHandler : Extensions.beanInjectLikeHandlers) {
            if (annotatedElement.getAnnotation(injectLikeHandler.getAnnotation()) != null) {
                return true;
            }
        }
        return false;
    }

    protected void resolveArguments(BlueprintRegister blueprintRegister, Class[] parameterTypes, Annotation[][] parameterAnnotations) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            Annotation[] annotations = parameterAnnotations[i];
            String value = findValue(annotations);
            String ref = findName(annotations);

            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Extensions.customDependencyAnnotationHandlers) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(annotations, customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(parameterTypes[i], annotation, ref, blueprintRegister);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }

            if (ref == null && value == null) {
                BeanRef template = new BeanRef(parameterTypes[i]);
                template.setQualifiersFromAnnotations(annotations);
                BeanRef bean = blueprintRegister.getMatching(template);
                if (bean != null) {
                    ref = bean.id;
                } else {
                    String name = findName(annotations);
                    if (name != null) {
                        ref = name;
                    } else {
                        ref = getBeanName(parameterTypes[i]);
                    }
                }
            }

            constructorArguments.add(new Argument(ref, value));
        }
    }

    @Override
    public String toString() {
        return clazz.getName();
    }

    public void writeProperties(PropertyWriter writer) {
        for (Property property : properties) {
            writer.writeProperty(property);
        }
    }

    public void writeArguments(ArgumentWriter writer) {
        for (Argument argument : constructorArguments) {
            writer.writeArgument(argument);
        }
    }

    public boolean needFieldInjection() {
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
}
