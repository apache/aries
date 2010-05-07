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

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    
    /**
     * the registration listeners to be notified when the service is
     * registered and unregistered with the framework.
     */
    RegistrationListener[] registerationListener();
    
    /**
     *  the ranking value to use when advertising the service.  If the
     *  ranking value is zero, the service must be registered without a
     *  <code>service.ranking</code> service property. 
     */
    int ranking() default 0;
    
    /**
     *  the auto-export mode for the service.  
     *  possible values are disabled, interfaces, class_hierarchy, all_classes
     */
    String autoExport() default "";
    
    /**
     *  the interfaces that the service should be advertised as supporting.
     */
    Class<?>[] interfaces() default {};
    
    /**
     * the user declared properties to be advertised with the service.
     */
    ServiceProperty[] serviceProperties() default {};
}