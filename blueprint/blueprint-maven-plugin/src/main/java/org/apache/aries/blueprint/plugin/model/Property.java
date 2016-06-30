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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Property implements Comparable<Property> {
    public final String name;
    public final String ref;
    public final String value;
    public final boolean isField;

    public Property(String name, String ref, String value, boolean isField) {
        this.name = name;
        this.ref = ref;
        this.value = value;
        this.isField = isField;
    }

    public static Property create(Matcher matcher, Field field) {
        if (needsInject(field)) {
            String value = AnnotationHelper.findValue(field.getAnnotations());
            if (value != null) {
                return new Property(field.getName(), null, value, true);
            }
            BeanRef matching = matcher.getMatching(new BeanRef(field));
            String ref = (matching == null) ? getRefName(field) : matching.id;
            return new Property(field.getName(), ref, null, true);
        } else {
            // Field is not a property
            return null;
        }
    }

    public static Property create(Matcher matcher, Method method) {
        String propertyName = resolveProperty(method);
        if (propertyName == null) {
            return null;
        }

        String value = AnnotationHelper.findValue(method.getAnnotations());
        if (value != null) {
            return new Property(propertyName, null, value, false);
        }

        if (needsInject(method)) {
            BeanRef beanRef = new BeanRef(method);
            BeanRef matching = matcher.getMatching(beanRef);
            String ref = (matching == null) ? beanRef.id : matching.id;
            return new Property(propertyName, ref, null, false);
        }

        return null;
    }

    private static String resolveProperty(Method method) {
        if (method.getParameterTypes().length != 1) {
            return null;
        }
        String propertyName = method.getName().substring(3);
        return makeFirstLetterLower(propertyName);
    }

    /**
     * Assume it is defined in another manually created blueprint context with default name
     *
     * @param field
     * @return
     */
    private static String getRefName(Field field) {
        for (NamedLikeHandler namedLikeHandler : Extensions.namedLikeHandlers) {
            if (field.getAnnotation(namedLikeHandler.getAnnotation()) != null) {
                String name = namedLikeHandler.getName(field.getType(), field);
                if (name != null) {
                    return name;
                }
            }
        }
        return Bean.getBeanName(field.getType());
    }

    private static boolean needsInject(AnnotatedElement annotatedElement) {
        for (Class injectDependencyAnnotation : AnnotationHelper.injectDependencyAnnotations) {
            if (annotatedElement.getAnnotation(injectDependencyAnnotation) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Property other) {
        return name.compareTo(other.name);
    }

    private static String makeFirstLetterLower(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
    }
}
