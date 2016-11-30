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
package org.apache.aries.blueprint.plugin.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Annotation A allows for enriching blueprint XML or add new bean to context when injecting bean, e. g. for generating in bean which could be injected
 */
public interface CustomDependencyAnnotationHandler<A extends Annotation> extends AnnotationHandler<A> {
    /**
     * @param annotatedElement field or setter method
     * @param name name of bean to inject (null if bean name is not provided)
     * @param contextEnricher context enricher
     * @return name of generated bean which should be injected or null
     */
    String handleDependencyAnnotation(AnnotatedElement annotatedElement, String name, ContextEnricher contextEnricher);

    /**
     * @param clazz class of constructor parameter or setter parameter
     * @param annotation instance of annotation A
     * @param name name of bean to inject (null if bean name is not provided)
     * @param contextEnricher context enricher
     * @return name of generated bean which should be injected or null
     */
    String handleDependencyAnnotation(Class<?> clazz, A annotation, String name, ContextEnricher contextEnricher);
}
