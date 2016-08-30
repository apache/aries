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
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;

import java.lang.annotation.Annotation;
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

    public static Property create(BlueprintRegister blueprintRegister, Field field) {
        if (needsInject(field)) {
            String value = AnnotationHelper.findValue(field.getAnnotations());
            if (value != null) {
                return new Property(field.getName(), null, value, true);
            }
            String ref = getForcedRefName(field);
            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Extensions.customDependencyAnnotationHandlers) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(field.getAnnotations(), customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(field, ref, blueprintRegister);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }
            if (ref != null) {
                return new Property(field.getName(), ref, null, true);
            }
            BeanRef matching = blueprintRegister.getMatching(new BeanRef(field));
            ref = (matching == null) ? getDefaultRefName(field) : matching.id;
            return new Property(field.getName(), ref, null, true);
        } else {
            // Field is not a property
            return null;
        }
    }

    public static Property create(BlueprintRegister blueprintRegister, Method method) {
        String propertyName = resolveProperty(method);
        if (propertyName == null) {
            return null;
        }

        String value = AnnotationHelper.findValue(method.getAnnotations());
        if (value != null) {
            return new Property(propertyName, null, value, false);
        }

        if (needsInject(method)) {
            String ref = getForcedRefName(method);
            if (ref == null) {
                ref = getForcedRefName(method.getParameterAnnotations()[0]);
            }
            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Extensions.customDependencyAnnotationHandlers) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(method.getAnnotations(), customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(method, ref, blueprintRegister);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }
            if (ref != null) {
                return new Property(propertyName, ref, null, false);
            }

            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Extensions.customDependencyAnnotationHandlers) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(method.getParameterAnnotations()[0], customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(method.getParameterTypes()[0], annotation, ref, blueprintRegister);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }
            if (ref != null) {
                return new Property(propertyName, ref, null, false);
            }

            BeanRef beanRef = new BeanRef(method);
            BeanRef matching = blueprintRegister.getMatching(beanRef);
            ref = (matching == null) ? beanRef.id : matching.id;
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
    private static String getDefaultRefName(Field field) {
        return Bean.getBeanName(field.getType());
    }

    private static String getForcedRefName(Field field) {
        for (NamedLikeHandler namedLikeHandler : Extensions.namedLikeHandlers) {
            if (field.getAnnotation(namedLikeHandler.getAnnotation()) != null) {
                String name = namedLikeHandler.getName(field.getType(), field);
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
    }

    private static String getForcedRefName(Method method) {
        for (NamedLikeHandler namedLikeHandler : Extensions.namedLikeHandlers) {
            if (method.getAnnotation(namedLikeHandler.getAnnotation()) != null) {
                String name = namedLikeHandler.getName(method.getParameterTypes()[0], method);
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
    }

    private static String getForcedRefName(Annotation[] annotations) {
        for (NamedLikeHandler namedLikeHandler : Extensions.namedLikeHandlers) {
            Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(annotations, namedLikeHandler.getAnnotation());
            if (annotation != null) {
                String name = namedLikeHandler.getName(annotation);
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
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
