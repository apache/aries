/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceList
{    
    /**
     * the description property of the service reference
     */
    String description() default "";
    
    /**
     * the interface type that a matching service must support.
     */
    Class<?> serviceInterface() default Object.class;
    
    /**
     * the filter expression that a matching service must match.
     */
    String filter() default "";
    
    /**
     * the <code>component-name</code> attribute of the service reference.
     */
    String componentName() default "";
    
    /**
     * whether or not a matching service is required at all times.  either optional or mandatory.
     */
    String availability() default "";
    
    /**
     * the reference listeners for the service reference, to receive bind and unbind events.
     */
    ReferenceListener[] referenceListeners() default {};
    
    /**
     * the value of the memberType property.
     */
    String memberType() default "service-object";
    
    /**
     * the id for the referencelist
     */
    String id() default "";
}