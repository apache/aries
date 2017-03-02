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
import java.util.HashSet;
import java.util.Set;

class QualifierHelper {
    static Set<Annotation> getQualifiers(Annotation[] annotations) {
        final Set<Annotation> qualifiers = new HashSet<>();
        for (Annotation ann : annotations) {
            if (isQualifier(ann) != null) {
                qualifiers.add(ann);
            }
        }
        return qualifiers;
    }

    private static Object isQualifier(Annotation ann) {
        for (Class<? extends Annotation> qualifingAnnotationClass : Handlers.QUALIFING_ANNOTATION_CLASSES) {
            Object annotation = ann.annotationType().getAnnotation(qualifingAnnotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

}
