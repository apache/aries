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
import org.apache.aries.blueprint.plugin.spi.CustomFactoryMethodAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

import javax.enterprise.inject.Produces;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class Context implements BlueprinRegister, ContextEnricher {

    SortedSet<BeanRef> reg = new TreeSet<BeanRef>();
    private final Map<String, XmlWriter> blueprintWriters = new HashMap<>();
    private final BlueprintConfiguration blueprintConfiguration;

    public Context(BlueprintConfiguration blueprintConfiguration, Class<?>... beanClasses) {
        this(blueprintConfiguration, Arrays.asList(beanClasses));
    }

    public Context(BlueprintConfiguration blueprintConfiguration, Collection<Class<?>> beanClasses) {
        this.blueprintConfiguration = blueprintConfiguration;
        addBlueprintRefs();
        addBeans(beanClasses);
    }

    private void addBlueprintRefs() {
        reg.add(new BeanRef(BundleContext.class, "blueprintBundleContext"));
        reg.add(new BeanRef(Bundle.class, "blueprintBundle"));
        reg.add(new BeanRef(BlueprintContainer.class, "blueprintContainer"));
        reg.add(new BeanRef(Converter.class, "blueprintConverter"));
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
        addProducedBeans(bean);
    }

    private void addProducedBeans(BeanRef factoryBean) {
        for (Method method : factoryBean.clazz.getMethods()) {
            Produces produces = method.getAnnotation(Produces.class);
            String name = AnnotationHelper.findName(method.getAnnotations());
            if (produces != null) {
                Class<?> producedClass = method.getReturnType();
                ProducedBean producedBean;
                if (name != null) {
                    producedBean = new ProducedBean(producedClass, name, factoryBean, method, this);
                } else {
                    producedBean = new ProducedBean(producedClass, factoryBean, method, this);
                }
                if (AnnotationHelper.findSingletons(method.getAnnotations())) {
                    producedBean.setSingleton();
                }
                reg.add(producedBean);
                for (CustomFactoryMethodAnnotationHandler customFactoryMethodAnnotationHandler : Extensions.customFactoryMethodAnnotationHandlers) {
                    if (AnnotationHelper.findAnnotation(method.getAnnotations(), customFactoryMethodAnnotationHandler.getAnnotation()) != null) {
                        customFactoryMethodAnnotationHandler.handleFactoryMethodAnnotation(method, producedBean.id, this);
                    }
                }
            }
        }
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
