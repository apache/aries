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

/**
 * this is really bundle level declaration
 * It is possible we want to eliminate this annotation and move the configuration to the bundle manifest header.
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Blueprint {
    
    /**
     * Specifies the default activation setting that will be defined
     * for components.  If not specified, the global default is "eager".
     * Individual components may override the default value.
     */
    String defaultActivation() default "eager";
    
    /**
     * Specifies the default timeout value to be used when operations
     * are invoked on unsatisfied service references.  If the
     * reference does not change to a satisfied state within the timeout
     * window, an error is raised on the method invocation.  The
     * default timeout value is 300 seconds and individual
     * <reference> element can override the specified configuration default.
     */   
    int defaultTimeout() default 300;
    
    /**
     * Specifies the default availability value to be used for
     * <reference>, and <reference-list> components.  The
     * normal default is "mandatory", and can be changed by individual
     * service reference components. 
     */
    String defaultAvailability() default "mandatory";
    
}
