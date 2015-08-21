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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.blueprint.supplier.impl.EmProxy;
import org.apache.aries.jpa.supplier.EmSupplier;

public class JpaAnnotatedMemberHandler {
    private Object bean;

    public JpaAnnotatedMemberHandler(Object bean) {
        this.bean = bean;
    }

    public void handleSupplierMember(AccessibleObject member, String unitName, EmSupplier emSupplier) {
        if (member instanceof Field) {
            Field field = (Field)member;
            try {
                field.set(bean, getEmProxy(field.getType(), emSupplier));
            } catch (Exception e) {
                throw new IllegalStateException("Error setting field " + field, e);
            }
        } else {
            Method method = (Method)member;
            try {
                method.invoke(bean, getEmProxy(method.getParameterTypes()[0], emSupplier));
            } catch (Exception e) {
                throw new IllegalStateException("Error invoking method " + method, e);
            }
        }
    }

    public void handleEmFactoryMethod(AccessibleObject member, String unitName, EntityManagerFactory emf) {
        if (member instanceof Field) {
            Field field = (Field)member;
            try {
                field.set(bean, getEmfProxy(field.getType(), emf));
            } catch (Exception e) {
                throw new IllegalStateException("Error setting field " + field, e);
            }
        } else {
            Method method = (Method)member;
            try {
                method.invoke(bean, getEmfProxy(method.getParameterTypes()[0], emf));
            } catch (Exception e) {
                throw new IllegalStateException("Error invoking method " + method, e);
            }
        }
    }

    private Object getEmProxy(Class<?> clazz, EmSupplier emSupplier) {
        if (clazz == EmSupplier.class) {
            return emSupplier;
        } else if (clazz == EntityManager.class) {
            return EmProxy.create(emSupplier);
        } else {
            throw new IllegalStateException(
                                            "Field or setter Method with @PersistenceContext has class not supported "
                                                + clazz.getName());
        }
    }

    private Object getEmfProxy(Class<?> clazz, EntityManagerFactory supplierProxy) {
        if (clazz == EntityManagerFactory.class) {
            return supplierProxy;
        } else {
            throw new IllegalStateException(
                                            "Field or setter Method with @PersistenceUnit has class not supported "
                                                + clazz);
        }
    }
}
