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
package org.apache.aries.blueprint.annotation.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate bean which should be registered as a service
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {

    /**
     * @return the list of classes or interfaces with which the service will be registered
     * When the list is empty than the service will be registered according to {@link #autoExport()}
     */
    Class<?>[] classes() default {};

    /**
     * @return auto export policy (used when {@link #classes()} are not provided)
     */
    AutoExport autoExport() default AutoExport.INTERFACES;

    /**
     * @return service.ranking property of service
     * this parameter override service.ranking property (in {@link #properties()}) if it has value not equal to 0
     */
    int ranking() default 0;

    /**
     * @return the list of service properties
     */
    ServiceProperty[] properties() default {};
}
