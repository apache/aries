package org.apache.aries.jpa.blueprint.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.blueprint.supplier.impl.EmProxyFactory;
import org.apache.aries.jpa.blueprint.supplier.impl.EmSupplierProxy;
import org.apache.aries.jpa.blueprint.supplier.impl.EmfProxyFactory;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class JpaAnnotatedMemberHandler {
    private Object bean;

    private BundleContext context;

    public JpaAnnotatedMemberHandler(Object bean) {
        this.bean = bean;
        context = FrameworkUtil.getBundle(bean.getClass()).getBundleContext();
    }

    public EmSupplierProxy handleSupplierMember(AccessibleObject member, String unitName) {
        EmSupplierProxy supplierProxy = new EmSupplierProxy(context, unitName);
        if (member instanceof Field) {
            Field field = (Field)member;
            try {
                field.set(bean, getEmProxy(field.getType(), supplierProxy));
            } catch (Exception e) {
                throw new IllegalStateException("Error setting field " + field, e);
            }
        } else {
            Method method = (Method)member;
            try {
                method.invoke(bean, getEmProxy(method.getParameterTypes()[0], supplierProxy));
            } catch (Exception e) {
                throw new IllegalStateException("Error invoking method " + method, e);
            }
        }
        return supplierProxy;
    }

    public EntityManagerFactory handleEmFactoryMethod(AccessibleObject member, String unitName) {
        EntityManagerFactory emfProxy = EmfProxyFactory.create(context, unitName);

        if (member instanceof Field) {
            Field field = (Field)member;
            try {
                field.set(bean, getEmfProxy(field.getType(), emfProxy));
            } catch (Exception e) {
                throw new IllegalStateException("Error setting field " + field, e);
            }
        } else {
            Method method = (Method)member;
            try {
                method.invoke(bean, getEmfProxy(method.getParameterTypes()[0], emfProxy));
            } catch (Exception e) {
                throw new IllegalStateException("Error invoking method " + method, e);
            }
        }
        return emfProxy;
    }

    private Object getEmProxy(Class<?> clazz, EmSupplierProxy supplierProxy) {
        if (clazz == EmSupplier.class) {
            return supplierProxy;
        } else if (clazz == EntityManager.class) {
            return EmProxyFactory.create(supplierProxy);
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
                                            "Field or setter Mthod with @PersistenceUnit has class not supported "
                                                + clazz);
        }
    }
}
