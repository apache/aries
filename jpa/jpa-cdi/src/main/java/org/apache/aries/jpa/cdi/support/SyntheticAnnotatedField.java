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
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SyntheticAnnotatedField<X> extends ForwardingAnnotatedField<X> {

    private final Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

    public SyntheticAnnotatedField(AnnotatedField<X> delegate) {
        this(delegate, Collections.<Annotation>emptyList());
    }

    public SyntheticAnnotatedField(AnnotatedField<X> delegate, Iterable<? extends Annotation> annotations) {
        super(delegate);
        for (Annotation annotation : annotations) {
            addAnnotation(annotation);
        }
        for (Annotation annotation : delegate.getAnnotations()) {
            addAnnotation(annotation);
        }
    }

    public void addAnnotation(Annotation annotation) {
        this.annotations.put(annotation.annotationType(), annotation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return (T) annotations.get(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return Collections.unmodifiableSet(new HashSet<>(annotations.values()));
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return annotations.containsKey(annotationType);
    }

}
