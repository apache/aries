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

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.apache.aries.blueprint.authorization.impl.test.SecuredClass;
import org.junit.Assert;
import org.junit.Test;

public class SecurityAnnoationParserTest {

    private SecurityAnotationParser annParser;
    
    public SecurityAnnoationParserTest() {
        annParser = new SecurityAnotationParser();
    }
    
    @Test
    public void testIsSecured() {
        Assert.assertTrue(annParser.isSecured(SecuredClass.class));
        Assert.assertFalse(annParser.isSecured(Object.class));
        Assert.assertFalse(annParser.isSecured(Activator.class));
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException, SecurityException {
        Assert.assertTrue(getEffective("admin") instanceof RolesAllowed);
        Assert.assertTrue(getEffective("user") instanceof RolesAllowed);
        Assert.assertTrue(getEffective("anon") instanceof PermitAll);
        Assert.assertTrue(getEffective("closed") instanceof DenyAll);
    }
    
    @Test
    public void testRoles() throws NoSuchMethodException, SecurityException {
        Assert.assertArrayEquals(new String[]{"admin"}, getRoles("admin"));
        Assert.assertArrayEquals(new String[]{"user"}, getRoles("user"));
    }

    private Annotation getEffective(String methodName) throws NoSuchMethodException {
        return annParser.getEffectiveAnnotation(SecuredClass.class.getMethod(methodName));
    }
    
    private String[] getRoles(String methodName) throws NoSuchMethodException {
        Annotation ann = getEffective(methodName);
        Assert.assertTrue(ann instanceof RolesAllowed);
        return ((RolesAllowed)ann).value();
    }

}
