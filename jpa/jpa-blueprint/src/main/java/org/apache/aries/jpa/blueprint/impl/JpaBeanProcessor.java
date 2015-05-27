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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.jpa.blueprint.supplier.impl.EmProxyFactory;
import org.apache.aries.jpa.blueprint.supplier.impl.EmSupplierProxy;
import org.apache.aries.jpa.blueprint.supplier.impl.EmfProxyFactory;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaBeanProcessor implements BeanProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaInterceptor.class);
    public static final String JPA_PROCESSOR_BEAN_NAME = "org_apache_aries_jpan";
    private Map<Object, EmSupplierProxy> emProxies;
    private Map<Object, EntityManagerFactory> emfProxies;
    private ComponentDefinitionRegistry cdr;

    public JpaBeanProcessor() {
        emProxies = new ConcurrentHashMap<Object, EmSupplierProxy>();
        emfProxies = new ConcurrentHashMap<Object, EntityManagerFactory>();
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
        Class<?> c = bean.getClass();
        Field field = getPersistenceField(c);
        if (field == null) {
            return bean;
        }
        BundleContext context = FrameworkUtil.getBundle(c).getBundleContext();
        field.setAccessible(true);

        PersistenceContext pcAnn = field.getAnnotation(PersistenceContext.class);
        if (pcAnn != null) {
        LOGGER.debug("Adding jpa/jta interceptor bean {} with class {}", beanName, c);

        EmSupplierProxy supplierProxy = new EmSupplierProxy(context, pcAnn.unitName());
	        emProxies.put(bean, supplierProxy);
        try {
	            field.set(bean, getEmProxy(field, supplierProxy));
        } catch (Exception e) {
            throw new IllegalStateException("Error setting field " + field, e);
        }
        Interceptor interceptor = new JpaInterceptor(supplierProxy);
        cdr.registerInterceptorWithComponent(beanData, interceptor);
        } else {
        	PersistenceUnit puAnn = field.getAnnotation(PersistenceUnit.class);
        	if(puAnn != null) {
        		LOGGER.debug("Adding emf proxy");
        		
    	        EntityManagerFactory emfProxy = EmfProxyFactory.create(context, puAnn.unitName()); 
    	        emfProxies.put(bean, emfProxy);
    	        try {
    	            field.set(bean, getEmfProxy(field, emfProxy));
    	        } catch (Exception e) {
    	            throw new IllegalStateException("Error setting field " + field, e);
    	        }	
        	}
        }
        return bean;
    }

    private Object getEmfProxy(Field field, EntityManagerFactory supplierProxy) {
        if (field.getType() == EntityManagerFactory.class) {
            return supplierProxy;
        } else {
            throw new IllegalStateException(
                                            "Field with @PersistenceUnit is not of type EntityManagerFactory "
                                                + field);
        }
    }

    private Object getEmProxy(Field field, EmSupplierProxy supplierProxy) {
        if (field.getType() == EmSupplier.class) {
            return supplierProxy;
        } else if (field.getType() == EntityManager.class) {
            return EmProxyFactory.create(supplierProxy);
        } else {
            throw new IllegalStateException(
                                            "Field with @PersistenceContext is not of type EntityManager or EmSupplier "
                                                + field);
        }
    }

    private Field getPersistenceField(Class<?> c) {
        for (Field field : c.getDeclaredFields()) {
            if (field.getAnnotation(PersistenceContext.class) != null) {
                return field;
            }
            if (field.getAnnotation(PersistenceUnit.class) != null) {
                return field;
            }
        }
        return null;
    }

}
