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
package org.apache.aries.jpa.blueprint.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.jpa.blueprint.supplier.impl.EmSupplierProxy;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaBeanProcessor implements BeanProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaInterceptor.class);
    public static final String JPA_PROCESSOR_BEAN_NAME = "org_apache_aries_jpan";
    private Map<Object, EmSupplierProxy> emProxies;
    private Map<Object, EntityManagerFactory> emfProxies;
    private ComponentDefinitionRegistry cdr;
    private final List<Class<?>> managedJpaClasses;

    public JpaBeanProcessor() {
        emProxies = new ConcurrentHashMap<Object, EmSupplierProxy>();
        emfProxies = new ConcurrentHashMap<Object, EntityManagerFactory>();
        managedJpaClasses = new ArrayList<Class<?>>();

        managedJpaClasses.add(EntityManagerFactory.class);
        managedJpaClasses.add(EntityManager.class);
        managedJpaClasses.add(EmSupplier.class);
    }

    public void setCdr(ComponentDefinitionRegistry cdr) {
        this.cdr = cdr;
    }

    public void afterDestroy(Object bean, String beanName) {
        EmSupplierProxy emProxy = emProxies.get(bean);
        if (emProxy != null) {
            emProxy.close();
        }
        EntityManagerFactory emfProxy = emfProxies.get(bean);
        if (emfProxy != null) {
            emfProxy.close();
        }
    }

    public Object afterInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        return bean;
    }

    public void beforeDestroy(Object bean, String beanName) {
    }

    public Object beforeInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        List<AccessibleObject> jpaAnnotatedMember = getJpaAnnotatedMembers(bean.getClass());
        managePersistenceMembers(jpaAnnotatedMember, bean, beanName, beanData);
        return bean;
    }

    private void managePersistenceMembers(List<AccessibleObject> jpaAnnotated, Object bean, String beanName,
                                          BeanMetadata beanData) {

        JpaAnnotatedMemberHandler jpaAnnotatedMember = new JpaAnnotatedMemberHandler(bean);
        for (AccessibleObject member : jpaAnnotated) {
            member.setAccessible(true);
            PersistenceContext pcAnn = member.getAnnotation(PersistenceContext.class);
            if (pcAnn != null) {
                LOGGER.debug("Adding jpa/jta interceptor bean {} with class {}", beanName, bean.getClass());

                EmSupplierProxy supplierProxy = jpaAnnotatedMember.handleSupplierMember(member,
                                                                                        pcAnn.unitName());

                emProxies.put(bean, supplierProxy);

                Interceptor interceptor = new JpaInterceptor(supplierProxy);
                cdr.registerInterceptorWithComponent(beanData, interceptor);
            } else {
                PersistenceUnit puAnn = member.getAnnotation(PersistenceUnit.class);
                if (puAnn != null) {
                    LOGGER.debug("Adding emf proxy");

                    EntityManagerFactory emfProxy = jpaAnnotatedMember
                        .handleEmFactoryMethod(member, puAnn.unitName());
                    emfProxies.put(bean, emfProxy);

                }
            }
        }
    }

    private List<AccessibleObject> getJpaAnnotatedMembers(Class<?> c) {
        final List<AccessibleObject> jpaAnnotated = new ArrayList<AccessibleObject>();

        Class<?> cl = c;
        if (c != Object.class) {
            while (cl != Object.class) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(PersistenceContext.class) != null
                        || field.getAnnotation(PersistenceUnit.class) != null) {
                        jpaAnnotated.add(field);
                    }
                }

                for (Method method : cl.getDeclaredMethods()) {
                    if (method.getAnnotation(PersistenceContext.class) != null
                        || method.getAnnotation(PersistenceUnit.class) != null) {

                        Class<?>[] pType = method.getParameterTypes();
                        if (method.getName().startsWith("set") && pType.length == 1
                            && managedJpaClasses.contains(pType[0])) {
                            jpaAnnotated.add(method);
                        }
                    }
                }

                cl = cl.getSuperclass();
            }
        }

        return jpaAnnotated;
    }
}
