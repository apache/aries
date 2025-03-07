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
package org.apache.aries.blueprint.annotation.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate dependency to inject a list of services.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceList {

    /**
     * @return service interface class to inject
     */
    Class<?> referenceInterface();

    /**
     * @return filter on reference-list
     */
    String filter() default "";

    /**
     * @return component-name on reference-list
     */
    String componentName() default "";

    /**
     * @return if existence of at least one service is necessary
     */
    Availability availability() default Availability.MANDATORY;

    /**
     * @return should proxy or service reference be injected
     */
    MemberType memberType() default MemberType.SERVICE_OBJECT;
}
