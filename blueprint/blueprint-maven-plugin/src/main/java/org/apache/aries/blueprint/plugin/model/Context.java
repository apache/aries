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

import org.apache.aries.blueprint.plugin.model.service.ServiceProvider;
import org.ops4j.pax.cdi.api.OsgiService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Context implements Matcher {

    SortedSet<BeanRef> reg = new TreeSet<BeanRef>();
    private final List<ServiceProvider> serviceProviders = new ArrayList<>();

    public Context(Class<?>... beanClasses) {
        this(Arrays.asList(beanClasses));
    }

    public Context(Collection<Class<?>> beanClasses) {
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
        Bean bean = new Bean(clazz);
        reg.add(bean);
        addServiceRefs(clazz);
        addProducedBeans(bean);
        addServiceProviders(bean);
    }

    private void addServiceProviders(Bean bean) {
        serviceProviders.addAll(bean.serviceProviders);
    }

    private void addProducedBeans(BeanRef factoryBean) {
        for (Method method : factoryBean.clazz.getMethods()) {
            Produces produces = method.getAnnotation(Produces.class);
            Named named = method.getAnnotation(Named.class);
            Singleton singleton = method.getAnnotation(Singleton.class);
            if (produces != null) {
                Class<?> producedClass = method.getReturnType();
                ProducedBean producedBean;
                if (named != null) {
                    producedBean = new ProducedBean(producedClass, named.value(), factoryBean, method);
                } else {
                    producedBean = new ProducedBean(producedClass, factoryBean, method);
                }
                if (singleton != null) {
                    producedBean.setSingleton();
                }
                reg.add(producedBean);
                ServiceProvider serviceProvider = ServiceProvider.fromMethod(producedBean, method);
                if (serviceProvider != null) {
                    serviceProviders.add(serviceProvider);
                }
            }
        }
    }

    private void addServiceRefs(Class<?> clazz) {
        for (Field field : new Introspector(clazz).fieldsWith(OsgiService.class)) {
            reg.add(new OsgiServiceRef(field));
        }
    }

    public void resolve() {
        for (Bean bean : getBeans()) {
            bean.resolve(this);
            addServiceRefs(bean);
        }
    }

    private void addServiceRefs(Bean bean) {
        reg.addAll(bean.serviceRefs);
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

    public SortedSet<OsgiServiceRef> getServiceRefs() {
        TreeSet<OsgiServiceRef> serviceRefs = new TreeSet<OsgiServiceRef>();
        for (BeanRef ref : reg) {
            if (ref instanceof OsgiServiceRef) {
                serviceRefs.add((OsgiServiceRef) ref);
            }
        }
        return serviceRefs;
    }

    public List<ServiceProvider> getServiceProviders() {
        return serviceProviders;
    }
}
