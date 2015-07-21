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

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.aries.jpa.blueprint.supplier.impl.ServiceProxy;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaBeanProcessor implements BeanProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaInterceptor.class);
    public static final String JPA_PROCESSOR_BEAN_NAME = "org_apache_aries_jpan";
    private Map<Object, Collection<Closeable>> serviceProxies;
    private ComponentDefinitionRegistry cdr;
    private final List<Class<?>> managedJpaClasses;

    public JpaBeanProcessor() {
        serviceProxies = new ConcurrentHashMap<Object, Collection<Closeable>>();
        managedJpaClasses = new ArrayList<Class<?>>();

        managedJpaClasses.add(EntityManagerFactory.class);
        managedJpaClasses.add(EntityManager.class);
        managedJpaClasses.add(EmSupplier.class);
    }

    public void setCdr(ComponentDefinitionRegistry cdr) {
        this.cdr = cdr;
    }

    public void afterDestroy(Object bean, String beanName) {
        Collection<Closeable> proxies = serviceProxies.remove(bean);
        if (proxies == null) {
            return;
        }
        for (Closeable closeable : proxies) {
            safeClose(closeable);
        }
        proxies.clear();
    }

    private void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
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
        Collection<Closeable> beanProxies = getBeanProxies(bean);
        BundleContext context = FrameworkUtil.getBundle(bean.getClass()).getBundleContext();
        LOGGER.info("context bundle " + context.getBundle());
        JpaAnnotatedMemberHandler jpaAnnotatedMember = new JpaAnnotatedMemberHandler(bean);
        for (AccessibleObject member : jpaAnnotated) {
            member.setAccessible(true);
            PersistenceContext pcAnn = member.getAnnotation(PersistenceContext.class);
            if (pcAnn != null) {
                LOGGER.info("Adding jpa/jta interceptor bean {} with class {}", beanName, bean.getClass());
                String filter = getFilter(EmSupplier.class, pcAnn.unitName());
                EmSupplier supplierProxy = ServiceProxy.create(context, EmSupplier.class, filter);
                jpaAnnotatedMember.handleSupplierMember(member, pcAnn.unitName(), supplierProxy);
                beanProxies.add((Closeable)supplierProxy);

                Interceptor interceptor = new JpaInterceptor(supplierProxy);
                cdr.registerInterceptorWithComponent(beanData, interceptor);
            } else {
                PersistenceUnit puAnn = member.getAnnotation(PersistenceUnit.class);
                if (puAnn != null) {
                    LOGGER.debug("Adding emf proxy");
                    String filter = getFilter(EntityManagerFactory.class, puAnn.unitName());
                    EntityManagerFactory emfProxy = ServiceProxy.create(context, EntityManagerFactory.class, filter);
                    jpaAnnotatedMember.handleEmFactoryMethod(member, puAnn.unitName(), emfProxy);
                    beanProxies.add((Closeable)emfProxy);

                }
            }
        }
    }

    private Collection<Closeable> getBeanProxies(Object bean) {
        Collection<Closeable> beanProxies = serviceProxies.get(bean);
        if (beanProxies == null) {
            beanProxies = new ArrayList<>();
            serviceProxies.put(bean, beanProxies);
        }
        return beanProxies;
    }

    private String getFilter(Class<?> clazz, String unitName) {
        return String.format("(&(objectClass=%s)(%s=%s))", clazz.getName(), JPA_UNIT_NAME, unitName);
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
