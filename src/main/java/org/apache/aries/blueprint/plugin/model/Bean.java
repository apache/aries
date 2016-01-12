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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

public class Bean extends BeanRef {
    public String initMethod;
    public String destroyMethod;
    public SortedSet<Property> properties;
    public Field[] persistenceFields;
    public Set<TransactionalDef> transactionDefs = new HashSet<TransactionalDef>();
    public boolean isPrototype;

    public Bean(Class<?> clazz) {
        super(clazz, BeanRef.getBeanName(clazz));

        // Init method
        Method initMethod = getMethodWithAnnotation(clazz, PostConstruct.class);
        if (initMethod != null) {
            this.initMethod = initMethod.getName();
        }

        // Destroy method
        Method destroyMethod = getMethodWithAnnotation(clazz, PreDestroy.class);
        if (destroyMethod != null) {
            this.destroyMethod = destroyMethod.getName();
        }

        // Transactional methods
        transactionDefs.addAll(new JavaxTransactionFactory().create(clazz));
        transactionDefs.addAll(new SpringTransactionFactory().create(clazz));
        this.isPrototype = isPrototype(clazz);
        this.persistenceFields = getPersistenceFields();
        properties = new TreeSet<Property>();
    }

    private boolean isPrototype(Class<?> clazz)
    {
        return clazz.getAnnotation(Singleton.class) == null && clazz.getAnnotation(Component.class) == null;
    }

    private Field[] getPersistenceFields() {
        List<Field> persistenceFields = new ArrayList<Field>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            PersistenceContext persistenceContext = field.getAnnotation(PersistenceContext.class);
            PersistenceUnit persistenceUnit = field.getAnnotation(PersistenceUnit.class);
            if (persistenceContext !=null || persistenceUnit != null) {
                 persistenceFields.add(field);
            }
        }
        return persistenceFields.toArray(new Field[]{});
    }

    public void resolve(Matcher matcher) {
        Class<?> curClass = this.clazz;
        while (curClass != null && curClass != Object.class) {
            resolveProperties(matcher, curClass);
            curClass = curClass.getSuperclass();
        }
    }

    private void resolveProperties(Matcher matcher, Class<?> curClass) {
        for (Field field : curClass.getDeclaredFields()) {
            Property prop = Property.create(matcher, field);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }

    private static <T extends Annotation> Method getMethodWithAnnotation(Class<?> classToSearch,
                                                                         Class<T> annotationClass) {
        List<Method> methods = getMethodsWithAnnotation(classToSearch, annotationClass);
        Preconditions.checkArgument(methods.size() <= 1,
                                    "Found %d methods annotated with %s in class %s, but only 1 allowed",
                                    methods.size(), annotationClass.getName(), classToSearch.getName());
        return Iterables.getOnlyElement(methods, null);
    }

    private static <T extends Annotation> List<Method> getMethodsWithAnnotation(Class<?> classToSearch,
                                                                                Class<T> annotationClass) {
        List<Method> methods = new ArrayList<>();
        for (Method method : classToSearch.getMethods()) {
            T annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                methods.add(method);
            }
        }
        return methods;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.getName().hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return clazz.getName();
    }

    public void writeProperties(PropertyWriter writer) {
        for (Property property : properties) {
            writer.writeProperty(property);
        }
    }

}
