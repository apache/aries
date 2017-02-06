/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.annotation.Annotation;

import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findName;
import static org.apache.aries.blueprint.plugin.model.AnnotationHelper.findValue;
import static org.apache.aries.blueprint.plugin.model.NamingHelper.getBeanName;

class Argument implements XmlWriter{
    private final String ref;
    private final String value;

    Argument(BlueprintRegistry blueprintRegistry, Class<?> argumentClass, Annotation[] annotations) {
        String value = findValue(annotations);
        String ref = findName(annotations);

        for (CustomDependencyAnnotationHandler customDependencyAnnotationHandler : Handlers.CUSTOM_DEPENDENCY_ANNOTATION_HANDLERS) {
            Annotation annotation = (Annotation) AnnotationHelper.findAnnotation(annotations, customDependencyAnnotationHandler.getAnnotation());
            if (annotation != null) {
                String generatedRef = customDependencyAnnotationHandler.handleDependencyAnnotation(argumentClass, annotation, ref, blueprintRegistry);
                if (generatedRef != null) {
                    ref = generatedRef;
                    break;
                }
            }
        }

        if (ref == null && value == null) {
            BeanTemplate template = new BeanTemplate(argumentClass, annotations);
            BeanRef bean = blueprintRegistry.getMatching(template);
            if (bean != null) {
                ref = bean.id;
            } else {
                String name = findName(annotations);
                if (name != null) {
                    ref = name;
                } else {
                    ref = getBeanName(argumentClass);
                }
            }
        }
        this.value = value;
        this.ref = ref;
    }

    String getRef() {
        return this.ref;
    }

    String getValue() {
        return this.value;
    }

    @Override
    public void write(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEmptyElement("argument");
        if (ref != null) {
            writer.writeAttribute("ref", ref);
        } else if (value != null) {
            writer.writeAttribute("value", value);
        }
    }
}
