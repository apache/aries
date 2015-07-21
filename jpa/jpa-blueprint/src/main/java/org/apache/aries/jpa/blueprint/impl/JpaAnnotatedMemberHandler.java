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
