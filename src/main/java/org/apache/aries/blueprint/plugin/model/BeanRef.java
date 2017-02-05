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

import org.apache.aries.blueprint.plugin.handlers.Handlers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

class BeanRef implements Comparable<BeanRef> {
    public String id;
    public final Class<?> clazz;
    private final Set<Annotation> qualifiers = new HashSet<>();

    BeanRef(Class<?> clazz, String id, Annotation[] qualifiers) {
        this.clazz = clazz;
        this.id = id;
        setQualifiersFromAnnotations(qualifiers);
    }

    BeanRef(Class<?> clazz, Annotation[] qualifiers) {
        this.clazz = clazz;
        setQualifiersFromAnnotations(qualifiers);
    }

    BeanRef(Field field) {
        this(field.getType(), field.getAnnotations());
    }

    BeanRef(Method method) {
        this(method.getParameterTypes()[0], method.getAnnotations());
    }

    private void setQualifiersFromAnnotations(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            if (isQualifier(ann) != null) {
                this.qualifiers.add(ann);
            }
        }
    }

    private Object isQualifier(Annotation ann) {
        for (Class<? extends Annotation> qualifingAnnotationClass : Handlers.QUALIFING_ANNOTATION_CLASSES) {
            Object annotation = ann.annotationType().getAnnotation(qualifingAnnotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    boolean matches(BeanRef template) {
        if (template.id != null) {
            return template.id.equals(id);
        }
        boolean assignable = template.clazz.isAssignableFrom(this.clazz);
        return assignable && qualifiers.containsAll(template.qualifiers);
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
        return this.id == null ? other.id == null : this.id.equals(other.id);
    }

    public int hashCode() {
        return 1 * 59 + (this.id == null ? 0 : this.id.hashCode());
    }

    protected boolean canEqual(Object other) {
        return other instanceof BeanRef;
    }
}
