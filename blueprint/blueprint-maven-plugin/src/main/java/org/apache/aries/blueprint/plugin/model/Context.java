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

    SortedSet<BeanRef> reg;

    public Context(Class<?>... beanClasses) {
        this(Arrays.asList(beanClasses));
    }

    public Context(Collection<Class<?>> beanClasses) {
        this.reg = new TreeSet<BeanRef>();
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
        addProducedBeans(clazz, bean);
    }

    private void addProducedBeans(Class<?> clazz, BeanRef factoryBean) {
        for (Method method : clazz.getMethods()) {
            Produces produces = method.getAnnotation(Produces.class);
            if (produces != null) {
                Class<?> producedClass = method.getReturnType();
                ProducedBean producedBean = new ProducedBean(producedClass, factoryBean, method.getName());
                reg.add(producedBean);
            }
        }
    }

    private void addServiceRefs(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            OsgiService osgiService = field.getAnnotation(OsgiService.class);
            if (osgiService != null) {
                reg.add(new OsgiServiceRef(field));
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
                beans.add((Bean)ref);
            }
        }
        return beans;
    }

    public SortedSet<OsgiServiceRef> getServiceRefs() {
        TreeSet<OsgiServiceRef> serviceRefs = new TreeSet<OsgiServiceRef>();
        for (BeanRef ref : reg) {
            if (ref instanceof OsgiServiceRef) {
                serviceRefs.add((OsgiServiceRef)ref);
            }
        }
        return serviceRefs;
    }

}
