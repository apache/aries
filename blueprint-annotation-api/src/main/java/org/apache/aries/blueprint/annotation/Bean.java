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

/*
 * To annotate a bean as a blueprint bean, use @Bean
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    
    /**
     * id, activation, dependsOn comes from Tcomponent
     * the id property for the bean
     * should this be auto generated if none is specified?
     */
    String id() default "";
    
    /**
     * the activation property for the bean
     * This can either be "eager" or "lazy".  If not specified, it
     * defaults to default-activation attribute of the enclosing
     * <blueprint> element.
     */
    String activation() default "";
    
    /**
     *  the components that the bean depends on
     */
    String[] dependsOn() default ""; 
    
    
    // TODO:  add the argument for the bean
    
    /**
     * the description property for the bean
     */
    String description() default "";
    
    /**
     * the scope property for the bean. value can be prototype or singleton
     */
    String scope() default "";

    /**
     * the reference to the factory component on which to invoke the
     * factory method for the bean.
     */
    String factoryRef() default "";
  
}
