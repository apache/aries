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
 * Annotation A on class provides id of bean in blueprint XML created from this class. Annotation could be also used to inject bean with provided id in constructor, setter or field.
 */
public interface NamedLikeHandler<A extends Annotation> extends AnnotationHandler<A> {
    /**
     * @param clazz depends on annotated element - if it is class then clazz is class itself, if setter then class of method argument and if field then class of field
     * @param annotatedElement class, method, field annotated with A
     * @return name of bean
     */
    String getName(Class clazz, AnnotatedElement annotatedElement);

    /**
     * Using to get name of bean based only on annotation when:
     * - inject via constructor
     * - inject via setter
     * @param annotation instance of A annotation
     * @return name of bean
     */
    String getName(Object annotation);
}
