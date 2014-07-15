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
package org.apache.aries.blueprint.authorization.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.security.auth.Subject;

import org.apache.aries.blueprint.Interceptor;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationInterceptor.class);

    public int getRank() {
        return 0;
    }

    public void postCallWithException(ComponentMetadata cm, Method m, Throwable ex, Object preCallToken) {
    }

    public void postCallWithReturn(ComponentMetadata cm, Method m, Object returnType, Object preCallToken)
        throws Exception {
    }

    public Object preCall(ComponentMetadata cm, Method m, Object... parameters) throws Throwable {
        Annotation ann = new SecurityAnotationParser().getEffectiveAnnotation(m);
        if (ann instanceof PermitAll) {
            return null;
        }
        String[] rolesAr = new String[] {}; // Also applies for @DenyAll
        if (ann instanceof RolesAllowed) {
            rolesAr = ((RolesAllowed) ann).value();
        } 
        Set<String> roles = new HashSet<String>(Arrays.asList(rolesAr));
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        if (subject == null) {
            throw new AccessControlException("Method call " + m.getDeclaringClass() + "." + m.getName() + " denied. No JAAS login present");
        }
        Set<Principal> principals = subject.getPrincipals();


        for (Principal principal : principals) {
            if (roles.contains(principal.getName())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Granting access to Method: " + m + " for " + principal);
                }
                return null;
            }
        }

        throw new AccessControlException("Method call " + m.getDeclaringClass() + "." + m.getName() + " denied. Roles allowed are " + roles + ". " 
                                         + "Your principals are " + getNames(principals) +".");
    }

    private String getNames(Set<Principal> principals) {
        StringBuilder sb = new StringBuilder();
        for (Principal principal : principals) {
            sb.append(principal.getName() + " ");
        }
        return sb.toString();
    }

}
