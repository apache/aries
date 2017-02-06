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
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findName;
import static org.apache.aries.blueprint.plugin.model.NamingHelper.getBeanName;

class Property implements Comparable<Property>, XmlWriter {
    public final String name;
    public final String ref;
    public final String value;
    final boolean isField;

    private Property(String name, String ref, String value, boolean isField) {
        this.name = name;
        this.ref = ref;
        this.value = value;
        this.isField = isField;
    }

    static Property create(BlueprintRegistry blueprintRegistry, Field field) {
        if (needsInject(field)) {
            String value = AnnotationHelper.findValue(field.getAnnotations());
            if (value != null) {
                return new Property(field.getName(), null, value, true);
            }
            String ref = getForcedRefName(field);
            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Handlers.CUSTOM_DEPENDENCY_ANNOTATION_HANDLERS) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(field.getAnnotations(), customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(field, ref, blueprintRegistry);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }
            if (ref != null) {
                return new Property(field.getName(), ref, null, true);
            }
            BeanRef matching = blueprintRegistry.getMatching(new BeanTemplate(field));
            ref = (matching == null) ? getDefaultRefName(field) : matching.id;
            return new Property(field.getName(), ref, null, true);
        } else {
            // Field is not a property
            return null;
        }
    }

    static Property create(BlueprintRegistry blueprintRegistry, Method method) {
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
                ref = findName(method.getParameterAnnotations()[0]);
            }
            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Handlers.CUSTOM_DEPENDENCY_ANNOTATION_HANDLERS) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(method.getAnnotations(), customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(method, ref, blueprintRegistry);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }
            if (ref != null) {
                return new Property(propertyName, ref, null, false);
            }

            for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Handlers.CUSTOM_DEPENDENCY_ANNOTATION_HANDLERS) {
                Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(method.getParameterAnnotations()[0], customDependencyAnnotationHandler.getAnnotation());
                if (annotation != null) {
                    String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(method.getParameterTypes()[0], annotation, ref, blueprintRegistry);
                    if (generatedRef != null) {
                        ref = generatedRef;
                        break;
                    }
                }
            }
            if (ref != null) {
                return new Property(propertyName, ref, null, false);
            }

            BeanTemplate template = new BeanTemplate(method);
            BeanRef matching = blueprintRegistry.getMatching(template);
            ref = (matching == null) ? getBeanName(method.getParameterTypes()[0]) : matching.id;
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
        return getBeanName(field.getType());
    }

    private static String getForcedRefName(Field field) {
        return getForcedRefName(field.getType(), field);
    }

    private static String getForcedRefName(Method method) {
        return getForcedRefName(method.getParameterTypes()[0], method);
    }

    private static String getForcedRefName(Class<?> clazz, AnnotatedElement annotatedElement) {
        for (NamedLikeHandler namedLikeHandler : Handlers.NAMED_LIKE_HANDLERS) {
            if (annotatedElement.getAnnotation(namedLikeHandler.getAnnotation()) != null) {
                String name = namedLikeHandler.getName(clazz, annotatedElement);
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

    @Override
    public void write(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEmptyElement("property");
        writer.writeAttribute("name", name);
        if (ref != null) {
            writer.writeAttribute("ref", ref);
        } else if (value != null) {
            writer.writeAttribute("value", value);
        }
    }
}
