/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint;

import java.lang.reflect.Method;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * An Interceptor interface provides support for custom interceptor implementation.
 */
public interface Interceptor {
    /**
     * This is called just before the method m is invocation.
     * @param cm : the component's metada
     * @param m: the method to be invoked
     * @param parameters: method parameters
     * @return token which will subsequently be passed to postCall
     * @throws Throwable
     */
    Object preCall(ComponentMetadata cm, Method m, Object... parameters) throws Throwable;
    
    /**
     * This method is called after the method m is invoked and returned normally.
     * @param cm: the component metadata
     * @param m: the method invoked
     * @param returnType : the return object
     * @param preCallToken token returned by preCall
     * @throws Throwable
     */
    void postCallWithReturn(ComponentMetadata cm, Method m, Object returnType, Object preCallToken) throws Throwable;
    
    /**
     * The method is called after the method m is invoked and causes an exception.
     * @param cm : the component metadata
     * @param m : the method invoked
     * @param ex : the <code>Throwable</code> thrown
     * @param preCallToken token returned by preCall
     * @throws Throwable
     */
    void postCallWithException(ComponentMetadata cm, Method m, Throwable ex, Object preCallToken) throws Throwable;
    
    
    /**
     * Return the rank of the interceptor, which is used to determine the order of the interceptors to be invoked
     * Rank is between Integer.MIN_VALUE and Integer.MAX_VALUE, interceptors are called in the order of highest value
     * rank first to lowest value rank last i.e. an interceptor with rank Integer.MAX_VALUE will be called before 
     * all others (except of the same rank).
     * @return the rank of the interceptor
     */
    int getRank();
}
