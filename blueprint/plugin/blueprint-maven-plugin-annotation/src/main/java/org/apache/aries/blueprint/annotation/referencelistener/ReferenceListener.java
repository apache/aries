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
package org.apache.aries.blueprint.annotation.referencelistener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating any class with this will create a
 * reference or referenclist element in blueprint
 * with annotated bean as reference-listener.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceListener {

    /**
     * @return interface class of referenced service
     */
    Class<?> referenceInterface();

    /**
     * @return filter on reference or reference-list
     */
    String filter() default "";

    /**
     * @return component-name on reference or reference-list
     */
    String componentName() default "";

    /**
     * @return if existence of at least one service is necessary
     */
    Availability availability() default Availability.OPTIONAL;

    /**
     * @return id of reference or reference-list
     */
    String referenceName() default "";

    /**
     * @return should generate reference or reference-list
     */
    Cardinality cardinality() default Cardinality.MULTIPLE;

    /**
     * @return bind method of reference-listener
     * if not provided then method annotated with {{@link Bind}}
     * will be found and used
     */
    String bindMethod() default "";

    /**
     * @return unbind method of reference-listener
     * if not provided then method annotated with {{@link Unbind}}
     * will be found and used
     */
    String unbindMethod() default "";

}
