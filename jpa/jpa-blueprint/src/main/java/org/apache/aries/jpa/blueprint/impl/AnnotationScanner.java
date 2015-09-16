package org.apache.aries.jpa.blueprint.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.aries.jpa.supplier.EmSupplier;

public class AnnotationScanner {
    private final List<Class<?>> managedJpaClasses;
    
    public AnnotationScanner() {
        managedJpaClasses = Arrays.asList(EntityManagerFactory.class, EntityManager.class, EmSupplier.class);
    }
    
    public List<AccessibleObject> getJpaAnnotatedMembers(Class<?> c) {
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
