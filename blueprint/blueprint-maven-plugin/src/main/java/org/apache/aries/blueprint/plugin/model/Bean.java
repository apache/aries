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
import org.apache.aries.blueprint.plugin.model.service.ServiceProvider;
import org.apache.aries.blueprint.plugin.spi.BeanAttributesResolver;
import org.apache.aries.blueprint.plugin.spi.InjectLikeHandler;
import org.ops4j.pax.cdi.api.OsgiService;

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

import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findAnnotation;
import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findName;
import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findValue;

public class Bean extends BeanRef {
    public final String initMethod;
    public String destroyMethod;
    public SortedSet<Property> properties = new TreeSet<>();
    public List<Argument> constructorArguments = new ArrayList<>();
    public Set<OsgiServiceRef> serviceRefs = new HashSet<>();
    public List<Field> persistenceFields;
    public Set<TransactionalDef> transactionDefs = new HashSet<>();
    public boolean isPrototype;
    public List<ServiceProvider> serviceProviders = new ArrayList<>();
    public final Map<String, String> attributes = new HashMap<>();

    public Bean(Class<?> clazz) {
        super(clazz, BeanRef.getBeanName(clazz));
        Introspector introspector = new Introspector(clazz);

        initMethod = findMethodAnnotatedWith(introspector, PostConstruct.class);
        destroyMethod = findMethodAnnotatedWith(introspector, PreDestroy.class);

        interpretTransactionalMethods(clazz);

        this.isPrototype = isPrototype(clazz);
        this.persistenceFields = findPersistenceFields(introspector);

        setQualifiersFromAnnotations(clazz.getAnnotations());

        interpretServiceProvider();

        resolveBeanAttributes();
    }

    private void resolveBeanAttributes() {
        for (BeanAttributesResolver beanAttributesResolver : Extensions.beanAttributesResolvers) {
            if (clazz.getAnnotation(beanAttributesResolver.getAnnotation()) != null) {
                attributes.putAll(beanAttributesResolver.resolveAttributes(clazz, clazz));
            }
        }
    }

    private void interpretServiceProvider() {
        ServiceProvider serviceProvider = ServiceProvider.fromBean(this);
        if (serviceProvider != null) {
            serviceProviders.add(serviceProvider);
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

    public void resolve(Matcher matcher) {
        resolveArguments(matcher);
        resolveFields(matcher);
        resolveMethods(matcher);
    }

    private void resolveMethods(Matcher matcher) {
        for (Method method : new Introspector(clazz).methodsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(matcher, method);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    private void resolveFields(Matcher matcher) {
        for (Field field : new Introspector(clazz).fieldsWith(AnnotationHelper.injectDependencyAnnotations)) {
            Property prop = Property.create(matcher, field);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    protected void resolveArguments(Matcher matcher) {
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

    protected void resolveArguments(Matcher matcher, Class[] parameterTypes, Annotation[][] parameterAnnotations) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            Annotation[] annotations = parameterAnnotations[i];
            String value = findValue(annotations);
            String ref = null;

            OsgiService osgiServiceAnnotation = findAnnotation(annotations, OsgiService.class);
            if (osgiServiceAnnotation != null) {
                String name = findName(annotations);
                ref = name != null ? name : getBeanNameFromSimpleName(parameterTypes[i].getSimpleName());
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
}
