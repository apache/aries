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
 * Annotation A allows for enriching blueprint XML or add new bean to context when bean class or factory method is marked with such annotation
 */
public interface BeanAnnotationHandler<A extends Annotation> extends AnnotationHandler<A> {
    /**
     * Handle annotation A on bean or factory method
     * @param annotatedElement bean class or factory method
     * @param id id of bean
     * @param contextEnricher context enricher
     */
    void handleBeanAnnotation(AnnotatedElement annotatedElement, String id, ContextEnricher contextEnricher, BeanEnricher beanEnricher);
}
