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
import org.apache.aries.blueprint.plugin.spi.BeanAttributesResolver;
import org.apache.aries.blueprint.plugin.spi.BlueprintWriter;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.CustomBeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.InjectLikeHandler;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
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

import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findValue;

public class Bean extends BeanRef implements ContextEnricher {
    public final String initMethod;
    public String destroyMethod;
    public SortedSet<Property> properties = new TreeSet<>();
    public List<Argument> constructorArguments = new ArrayList<>();
    public List<Field> persistenceFields;
    public Set<TransactionalDef> transactionDefs = new HashSet<>();
    public boolean isPrototype;
    public final Map<String, String> attributes = new HashMap<>();
    public final Set<BeanRef> refs = new HashSet<>();
    public final Map<String, BlueprintWriter> blueprintWriters = new HashMap<>();

    public Bean(Class<?> clazz) {
        super(clazz, BeanRef.getBeanName(clazz));
        Introspector introspector = new Introspector(clazz);

        initMethod = findMethodAnnotatedWith(introspector, PostConstruct.class);
        destroyMethod = findMethodAnnotatedWith(introspector, PreDestroy.class);

        interpretTransactionalMethods(clazz);

        this.isPrototype = isPrototype(clazz);
        this.persistenceFields = findPersistenceFields(introspector);

        setQualifiersFromAnnotations(clazz.getAnnotations());

        resolveBeanAttributes();

        handleCustomBeanAnnotations();
    }

    private void resolveBeanAttributes() {
        for (BeanAttributesResolver beanAttributesResolver : Extensions.beanAttributesResolvers) {
            if (clazz.getAnnotation(beanAttributesResolver.getAnnotation()) != null) {
                attributes.putAll(beanAttributesResolver.resolveAttributes(clazz, clazz));
            }
        }
    }

    private void handleCustomBeanAnnotations() {
        for (CustomBeanAnnotationHandler customBeanAnnotationHandler : Extensions.customBeanAnnotationHandlers) {
            Object annotation = AnnotationHelper.findAnnotation(clazz.getAnnotations(), customBeanAnnotationHandler.getAnnotation());
            if (annotation != null) {
                customBeanAnnotationHandler.handleBeanAnnotation(clazz, id, this);
            }
        }
    }

    private List<Field> findPersistenceFields(Introspector introspector) {
        return introspector.fieldsWith(PersistenceContext.class, PersistenceUnit.class);
    }

    private void interpretTransactionalMethods(Class<?> clazz) {
        for (AbstractTransactionalFactory transactionalFactory : Extensions.transactionalFactories) {
            transactionDefs.addAll(transactionalFactory.create(clazz));
        }
    }

    private String findMethodAnnotatedWith(Introspector introspector, Class<? extends Annotation> annotation) {
        Method initMethod = introspector.methodWith(annotation);
        if (initMethod == null) {
            return null;
        }
        return initMethod.getName();
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

    public void resolve(BlueprinRegister matcher) {
        resolveArguments(matcher);
        resolveFields(matcher);
        resolveMethods(matcher);
    }

    private void resolveMethods(BlueprinRegister blueprinRegister) {
        for (Method method : new Introspector(clazz).methodsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(blueprinRegister, method);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    private void resolveFields(BlueprinRegister matcher) {
        for (Field field : new Introspector(clazz).fieldsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(matcher, field);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    protected void resolveArguments(BlueprinRegister matcher) {
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

    protected void resolveArguments(BlueprinRegister blueprinRegister, Class[] parameterTypes, Annotation[][] parameterAnnotations) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            Annotation[] annotations = parameterAnnotations[i];
            String value = findValue(annotations);
            String ref = findName(annotations);

            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Extensions.customDependencyAnnotationHandlers) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(annotations, customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(parameterTypes[i], annotation, ref, blueprinRegister);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }

            if (ref == null && value == null) {
                BeanRef template = new BeanRef(parameterTypes[i]);
                template.setQualifiersFromAnnotations(annotations);
                BeanRef bean = blueprinRegister.getMatching(template);
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

    private String findName(Annotation[] annotations) {
        for (NamedLikeHandler namedLikeHandler : Extensions.namedLikeHandlers) {
            Object annotation = AnnotationHelper.findAnnotation(annotations, namedLikeHandler.getAnnotation());
            if (annotation != null) {
                String name = namedLikeHandler.getName(annotation);
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
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
    public void addBean(String id, Class<?> clazz) {
        refs.add(new BeanRef(clazz, id));
    }

    @Override
    public void addBlueprintWriter(String id, BlueprintWriter blueprintWriter) {
        blueprintWriters.put(id, blueprintWriter);
    }
}
