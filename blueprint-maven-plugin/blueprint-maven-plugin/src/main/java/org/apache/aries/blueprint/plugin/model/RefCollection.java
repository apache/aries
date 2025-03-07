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
import org.apache.aries.blueprint.plugin.spi.CollectionDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class RefCollection implements XmlWriter {
    private final String type;
    private final List<String> refs;

    private RefCollection(String type, List<String> refs) {
        this.type = type;
        this.refs = refs;
    }

    static RefCollection getRefCollection(BlueprintRegistry blueprintRegistry, Class<?> injectedType, Annotation[] annotations) {
        List<String> refCollection = getMatchingRefs(blueprintRegistry, annotations);
        if (refCollection == null) {
            return null;
        }
        String collectionType = recognizeCollectionType(injectedType);
        return new RefCollection(collectionType, refCollection);
    }

    static RefCollection getRefCollection(BlueprintRegistry blueprintRegistry, Field field) {
        return getRefCollection(blueprintRegistry, field.getType(), field.getAnnotations());
    }

    static RefCollection getRefCollection(BlueprintRegistry blueprintRegistry, Method method) {
        return getRefCollection(blueprintRegistry, method.getParameterTypes()[0], method.getAnnotations());
    }

    private static String recognizeCollectionType(Class<?> type) {
        if (type.isAssignableFrom(List.class)) {
            return "list";
        }
        if (type.isAssignableFrom(Set.class)) {
            return "set";
        }
        if (type.isArray()) {
            return "array";
        }
        throw new IllegalStateException("Expecting that class " + type.getName() + " will be Set, List or Array");
    }

    private static List<String> getMatchingRefs(BlueprintRegistry blueprintRegistry, Annotation[] annotations) {
        for (CollectionDependencyAnnotationHandler<? extends Annotation> collectionDependencyAnnotationHandler : Handlers.COLLECTION_DEPENDENCY_ANNOTATION_HANDLERS) {
            Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(annotations, collectionDependencyAnnotationHandler.getAnnotation());
            if (annotation != null) {
                Class<?> classCollection = collectionDependencyAnnotationHandler.getBeanClass(annotation);
                List<BeanRef> refs = blueprintRegistry.getAllMatching(new BeanTemplate(classCollection, annotations));
                List<String> refList = new ArrayList<>();
                for (BeanRef ref : refs) {
                    refList.add(ref.id);
                }
                return refList;
            }
        }
        return null;
    }

    @Override
    public void write(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(type);
        for (String componentId : refs) {
            writer.writeEmptyElement("ref");
            writer.writeAttribute("component-id", componentId);
        }
        writer.writeEndElement();
    }
}
