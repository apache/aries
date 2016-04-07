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

import org.ops4j.pax.cdi.api.OsgiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Bean extends BeanRef {
    public String initMethod;
    public String destroyMethod;
    public SortedSet<Property> properties = new TreeSet<>();
    public List<Argument> constructorArguments = new ArrayList<>();
    public Set<OsgiServiceRef> serviceRefs = new HashSet<>();
    public List<Field> persistenceFields;
    public Set<TransactionalDef> transactionDefs = new HashSet<>();
    public boolean isPrototype;

    public Bean(Class<?> clazz) {
        super(clazz, BeanRef.getBeanName(clazz));
        Introspector introspector = new Introspector(clazz);

        // Init method
        Method initMethod = introspector.methodWith(PostConstruct.class);
        if (initMethod != null) {
            this.initMethod = initMethod.getName();
        }

        // Destroy method
        Method destroyMethod = introspector.methodWith(PreDestroy.class);
        if (destroyMethod != null) {
            this.destroyMethod = destroyMethod.getName();
        }

        // Transactional methods
        transactionDefs.addAll(new JavaxTransactionFactory().create(clazz));
        transactionDefs.addAll(new SpringTransactionFactory().create(clazz));
        this.isPrototype = isPrototype(clazz);
        this.persistenceFields = introspector.fieldsWith(PersistenceContext.class, PersistenceUnit.class);
        setQualifiersFromAnnotations(clazz.getAnnotations());
    }

    private boolean isPrototype(Class<?> clazz) {
        return clazz.getAnnotation(Singleton.class) == null && clazz.getAnnotation(Component.class) == null;
    }

    public void resolve(Matcher matcher) {
        resolveConstructorArguments(matcher);
        for (Field field : new Introspector(clazz).fieldsWith(Value.class, Autowired.class, Inject.class)) {
            Property prop = Property.create(matcher, field);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    private void resolveConstructorArguments(Matcher matcher) {
        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        for (Constructor constructor : declaredConstructors) {
            Annotation inject = constructor.getAnnotation(Inject.class);
            Annotation autowired = constructor.getAnnotation(Autowired.class);
            if (inject != null || autowired != null || declaredConstructors.length == 1) {
                Class[] parameterTypes = constructor.getParameterTypes();
                Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
                for (int i = 0; i < parameterTypes.length; ++i) {
                    Annotation[] annotations = parameterAnnotations[i];
                    String ref = null;
                    String value = null;
                    Value valueAnnotation = findAnnotation(annotations, Value.class);
                    OsgiService osgiServiceAnnotation = findAnnotation(annotations, OsgiService.class);

                    if (valueAnnotation != null) {
                        value = valueAnnotation.value();
                    }

                    if (osgiServiceAnnotation != null) {
                        Named namedAnnotation = findAnnotation(annotations, Named.class);
                        ref = namedAnnotation != null ? namedAnnotation.value() : getBeanNameFromSimpleName(parameterTypes[i].getSimpleName());
                        OsgiServiceRef osgiServiceRef = new OsgiServiceRef(parameterTypes[i], osgiServiceAnnotation, ref);
                        serviceRefs.add(osgiServiceRef);
                    }

                    if (ref == null && value == null && osgiServiceAnnotation == null) {
                        BeanRef template = new BeanRef(parameterTypes[i]);
                        template.setQualifiersFromAnnotations(annotations);
                        BeanRef bean = matcher.getMatching(template);
                        if (bean != null) {
                            ref = bean.id;
                        } else {
                            Named namedAnnotation = findAnnotation(annotations, Named.class);
                            if (namedAnnotation != null) {
                                ref = namedAnnotation.value();
                            } else {
                                ref = getBeanName(parameterTypes[i]);
                            }
                        }
                    }

                    constructorArguments.add(new Argument(ref, value));
                }
                break;
            }
        }
    }

    private static <T> T findAnnotation(Annotation[] annotations, Class<T> annotation) {
        for (Annotation a : annotations) {
            if (a.annotationType() == annotation) {
                return annotation.cast(a);
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


}
