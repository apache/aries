/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.model;

import org.apache.aries.blueprint.plugin.Extensions;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BeanRef implements Comparable<BeanRef> {
    public String id;
    public Class<?> clazz;
    public Map<Class<? extends Annotation>, Annotation> qualifiers = new HashMap<>();

    /**
     * @param clazz interface or implementation class
     */
    public BeanRef(Class<?> clazz) {
        this.clazz = clazz;
    }

    public BeanRef(Class<?> clazz, String id) {
        this(clazz);
        this.id = id;
    }

    public BeanRef(Field field) {
        this(field.getType());
        parseQualifiers(field);
    }

    public BeanRef(Method method) {
        this(method.getParameterTypes()[0]);
        parseQualifiers(method);
    }

    private void parseQualifiers(AnnotatedElement annotatedElement) {
        Annotation[] annotations = annotatedElement.getAnnotations();
        setQualifiersFromAnnotations(annotations);
    }

    protected void setQualifiersFromAnnotations(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            if (isQualifier(ann) != null) {
                this.qualifiers.put(ann.annotationType(), ann);
            }
        }
    }

    private Qualifier isQualifier(Annotation ann) {
        return ann.annotationType().getAnnotation(Qualifier.class);
    }

    public static String getBeanName(Class<?> clazz) {
        return getBeanName(clazz, clazz);
    }

    public static String getBeanName(Class<?> clazz, AnnotatedElement annotatedElement) {
        for (NamedLikeHandler namedLikeHandler : Extensions.namedLikeHandlers) {
            if (annotatedElement.getAnnotation(namedLikeHandler.getAnnotation()) != null) {
                String name = namedLikeHandler.getName(clazz, annotatedElement);
                if (name != null) {
                    return name;
                }
            }
        }
        String name = clazz.getSimpleName();
        return getBeanNameFromSimpleName(name);
    }

    protected static String getBeanNameFromSimpleName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
    }

    public boolean matches(BeanRef template) {
        boolean assignable = template.clazz.isAssignableFrom(this.clazz);
        if (template.id != null) {
            return template.id.equals(id);
        }
        return assignable && qualifiers.values().containsAll(template.qualifiers.values());
    }

    @Override
    public int compareTo(BeanRef other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return this.clazz.getSimpleName() + "(" + this.id + ")";
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BeanRef)) return false;
        final BeanRef other = (BeanRef) o;
        if (!other.canEqual(this)) return false;
        if (this.id == null ? other.id != null : !this.id.equals(other.id)) return false;
        if (this.clazz == null ? other.clazz != null : !this.clazz.equals(other.clazz)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.id == null ? 0 : this.id.hashCode());
        result = result * PRIME + (this.clazz == null ? 0 : this.clazz.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof BeanRef;
    }
}
