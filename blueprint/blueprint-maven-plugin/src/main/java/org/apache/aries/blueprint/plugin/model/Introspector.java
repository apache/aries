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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Class to find uniquely-named fields declared in a class hierarchy with specified annotations.
 */
public final class Introspector {
    private Class<?> originalClazz;

    /**
     * @param clazz the class to introspect (including those defined in parent classes).
     */
    public Introspector(Class<?> clazz) {
        this.originalClazz = clazz;
    }

    /**
     * @param requiredAnnotations annotations the fields must have
     * @return fields in the given class (including parent classes) that match this finder's annotations requirements.
     * @throws UnsupportedOperationException if any field matching the annotations requirement shares its name with a
     * field declared elsewhere in the class hierarchy.
     */
    @SafeVarargs
    public final List<Field> fieldsWith(Class<? extends Annotation>... requiredAnnotations) {
        Multimap<String, Field> fieldsByName = HashMultimap.create();
        Set<String> acceptedFieldNames = Sets.newHashSet();
        Class<?> clazz = originalClazz;

        // For each parent class of clazz...
        while(clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                // ...add all declared fields
                fieldsByName.put(field.getName(), field);

                // ...and if it meets the annotation requirement, add the field name to the set of accepted field names
                if (hasAnyRequiredAnnotation(field, requiredAnnotations)) {
                    acceptedFieldNames.add(field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }

        // Add all accepted fields to acceptedFields
        List<Field> acceptedFields = Lists.newArrayList();
        for (String fieldName : acceptedFieldNames) {
            Collection<Field> fields = fieldsByName.get(fieldName);
            validateOnlyOneFieldWithName(fieldName, fields);
            acceptedFields.addAll(fields);
        }
        return acceptedFields;
    }

    /**
     * Check that each field name is defined no more than once
     * @param originalClazz
     * @param acceptedFieldName
     * @param acceptedFieldsWithSameName
     */
    private void validateOnlyOneFieldWithName(String acceptedFieldName,
                                              Collection<Field> acceptedFieldsWithSameName) {
        if (acceptedFieldsWithSameName.size() > 1) {
            String header = String.format("Field '%s' in bean class '%s' has been defined multiple times in:",
                                          acceptedFieldName, originalClazz.getName());
            StringBuilder msgBuilder = new StringBuilder(header);
            for (Field field : acceptedFieldsWithSameName) {
                msgBuilder.append("\n\t- ").append(field.getDeclaringClass().getName());
            }
            throw new UnsupportedOperationException(msgBuilder.toString());
        }
    }

    @SafeVarargs
    private final boolean hasAnyRequiredAnnotation(Field field, Class<? extends Annotation>... requiredAnnotations) {
        if (requiredAnnotations.length == 0) {
            throw new IllegalArgumentException("Must specify at least one annotation");
        }
        for (Class<? extends Annotation> requiredAnnotation : requiredAnnotations) {
            if (field.getAnnotation(requiredAnnotation) != null) {
                return true;
            }
        }
        return false;
    }
    
    public <T extends Annotation> Method methodWith(Class<T> annotationClass) {
        List<Method> methods = methodsWith(annotationClass);
        Preconditions.checkArgument(methods.size() <= 1,
                                    "Found %d methods annotated with %s in class %s, but only 1 allowed",
                                    methods.size(), annotationClass.getName(), originalClazz.getName());
        return Iterables.getOnlyElement(methods, null);
    }

    public <T extends Annotation> List<Method> methodsWith(Class<T> annotationClass) {
        List<Method> methods = new ArrayList<>();
        for (Method method : originalClazz.getMethods()) {
            T annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                methods.add(method);
            }
        }
        return methods;
    }
}
