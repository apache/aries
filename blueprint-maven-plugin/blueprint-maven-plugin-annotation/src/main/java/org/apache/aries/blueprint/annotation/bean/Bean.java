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
package org.apache.aries.blueprint.annotation.bean;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating any class or method will create a bean.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {

    /**
     * @return id or auto generated name from class name when id is empty
     */
    String id() default "";

    /**
     * @return activation of bean: eager, lazy or defaut for whole blueprint file
     */
    Activation activation() default Activation.DEFAULT;

    /**
     * @return array of bean ids on which this bean depends
     */
    String[] dependsOn() default {};

    /**
     * @return bean scope
     */
    Scope scope() default Scope.SINGLETON;

    /**
     * @return init method name, if empty then bean has no init method.
     * Warning: if bean has another annotation which selects another init-method then cannot determine which method will be selected.
     */
    String initMethod() default "";

    /**
     * @return destroy method name, if empty then bean has no destroy method
     * * Warning: if bean has another annotation which selects another destroy-method then cannot determine which method will be selected.
     */
    String destroyMethod() default "";
}
