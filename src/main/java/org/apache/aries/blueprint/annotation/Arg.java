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


/**
 * used to describe argument of the bean constructor
 * or the argument of the factory method for the bean
 * 
 * this is mapped to Targument for the Tbean
 *
 */
public @interface Arg {

    /**
     * the value of the argument
     */
    String value() default "";
    
    /**
     * the value of the ref attribute of the argument
     */
    String ref() default "";
    
    /**
     * the description of the argument
     */
    String description() default "";
    
    /**
     *  the zero-based index into the parameter list of the factory method
     *  or constructor to be invoked for this argument. This is determined by
     *  specifying the <code>index</code> attribute for the bean. If not
     *  explicitly set, this will return -1 and the initial ordering is defined
     *  by its position in the args[] list.
     */
    int index() default -1;
    
}
