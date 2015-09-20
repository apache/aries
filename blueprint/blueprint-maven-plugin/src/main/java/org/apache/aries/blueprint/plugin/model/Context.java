/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.enterprise.inject.Produces;

import org.ops4j.pax.cdi.api.OsgiService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

public class Context implements Matcher {

    SortedSet<Bean> beans;
    SortedSet<OsgiServiceRef> serviceRefs;

    public Context(Class<?>... beanClasses) {
        this(Arrays.asList(beanClasses));
    }

    public Context(Collection<Class<?>> beanClasses) {
        this.beans = new TreeSet<Bean>();
        this.serviceRefs = new TreeSet<OsgiServiceRef>();
        addBeans(beanClasses);
    }

    private void addBeans(Collection<Class<?>> beanClasses) {
        for (Class<?> clazz : beanClasses) {
            addBean(clazz);
        }
    }

    private void addBean(Class<?> clazz) {
        Bean bean = new Bean(clazz);
        beans.add(bean);
        addServiceRefs(clazz);
        addProducedBeans(clazz, bean.id);
    }

    private void addProducedBeans(Class<?> clazz, String factoryBeanId) {
        for (Method method : clazz.getMethods()) {
            Produces produces = method.getAnnotation(Produces.class);
            if (produces != null) {
                Class<?> producedClass = method.getReturnType();
                ProducedBean producedBean = new ProducedBean(producedClass, factoryBeanId, method.getName());
                beans.add(producedBean);
            }
        }
    }

    private void addServiceRefs(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            OsgiService osgiService = field.getAnnotation(OsgiService.class);
            if (osgiService != null) {
                serviceRefs.add(new OsgiServiceRef(field));
            }
        }
    }

    public void resolve() {
        for (Bean bean : beans) {
            bean.resolve(this);
        }
    }
    
    public BeanRef getMatching(BeanRef template) {
        for (Bean bean : beans) {
            if (bean.matches(template)) {
                return bean;
            }
        }
        for (BeanRef bean : serviceRefs) {
            if (bean.matches(template)) {
                return bean;
            }
        }
        if (template.clazz == BundleContext.class) {
            return new BeanRef(BundleContext.class, "blueprintBundleContext");
        }
        if (template.clazz == Bundle.class) {
            return new BeanRef(Bundle.class, "blueprintBundle");
        }
        if (template.clazz == BlueprintContainer.class) {
            return new BeanRef(BlueprintContainer.class, "blueprintContainer");
        }
        if (template.clazz == Converter.class) {
            return new BeanRef(Converter.class, "blueprintConverter");
        }
        
        return null;
    }

    public SortedSet<Bean> getBeans() {
        return beans;
    }

    public SortedSet<OsgiServiceRef> getServiceRefs() {
        return serviceRefs;
    }

}
