/**
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
package org.apache.aries.blueprint.authorization.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

/**
 * Evaluates JEE security annotations 
 * @see PermitAll
 * @see DenyAll
 * @see RolesAllowed
 */
class SecurityAnotationParser {

    /**
     * Get the effective annotation regarding method annotations override class annotations.
     * DenyAll has highest priority then RolesAllowed and in the end PermitAll. 
     * So the most restrictive annotation is pereferred.
     * 
     * @param m Method to check
     * @return effective annotation (either DenyAll, PermitAll or RolesAllowed)
     */
    Annotation getEffectiveAnnotation(Method m) {
        Annotation classLevel = getAuthAnnotation(m.getDeclaringClass());
        Annotation methodLevel = getAuthAnnotation(m);
        return (methodLevel != null) ? methodLevel : classLevel;
    }

    private Annotation getAuthAnnotation(AnnotatedElement element) {
        Annotation ann = null;
        ann = element.getAnnotation(DenyAll.class);
        if (ann == null) {
            ann = element.getAnnotation(RolesAllowed.class);
        }
        if (ann == null) {
            ann = element.getAnnotation(PermitAll.class);
        }
        return ann;
    }

}
