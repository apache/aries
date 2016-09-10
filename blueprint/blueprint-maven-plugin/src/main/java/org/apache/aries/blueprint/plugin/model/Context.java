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
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextInitializationHandler;
import org.apache.aries.blueprint.plugin.spi.CustomFactoryMethodAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class Context implements BlueprintRegister, ContextEnricher {

    SortedSet<BeanRef> reg = new TreeSet<BeanRef>();
    private final Map<String, XmlWriter> blueprintWriters = new HashMap<>();
    private final BlueprintConfiguration blueprintConfiguration;

    public Context(BlueprintConfiguration blueprintConfiguration, Class<?>... beanClasses) {
        this(blueprintConfiguration, Arrays.asList(beanClasses));
    }

    public Context(BlueprintConfiguration blueprintConfiguration, Collection<Class<?>> beanClasses) {
        this.blueprintConfiguration = blueprintConfiguration;
        initContext();
        addBeans(beanClasses);
    }

    private void initContext() {
        for (ContextInitializationHandler contextInitializationHandler : Extensions.contextInitializationHandlers) {
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
            for (CustomFactoryMethodAnnotationHandler customFactoryMethodAnnotationHandler : Extensions.customFactoryMethodAnnotationHandlers) {
                if (AnnotationHelper.findAnnotation(method.getAnnotations(), customFactoryMethodAnnotationHandler.getAnnotation()) != null) {
                    customFactoryMethodAnnotationHandler.handleFactoryMethodAnnotation(method, beanFromFactory.id, this);
                }
            }
        }
    }

    private boolean isFactoryMethod(Method method) {
        boolean isFactoryMethod = false;
        for (Class<? extends Annotation> factoryMethodAnnotationClass : Extensions.factoryMethodAnnotationClasses) {
            Annotation annotation = AnnotationHelper.findAnnotation(method.getAnnotations(), factoryMethodAnnotationClass);
            if (annotation != null) {
                isFactoryMethod = true;
                break;
            }
        }
        return isFactoryMethod;
    }

    public void resolve() {
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
}
