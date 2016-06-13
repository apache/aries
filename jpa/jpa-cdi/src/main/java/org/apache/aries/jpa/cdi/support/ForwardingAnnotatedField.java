/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.jpa.cdi.support;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;

public class ForwardingAnnotatedField<X> implements AnnotatedField<X> {

    private final AnnotatedField<X> delegate;

    public ForwardingAnnotatedField(AnnotatedField<X> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Field getJavaMember() {
        return delegate.getJavaMember();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public AnnotatedType<X> getDeclaringType() {
        return delegate.getDeclaringType();
    }

    @Override
    public Type getBaseType() {
        return delegate.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return delegate.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return delegate.getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return delegate.getAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return delegate.isAnnotationPresent(annotationType);
    }
}
