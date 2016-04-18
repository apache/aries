
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
package org.apache.aries.jpa.blueprint.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.junit.Assert;
import org.junit.Test;


public class AnnotationScannerTest {
    
    private Method setEmMethod;
    private Field emField;
    private Method setEmfMethod;
    private Field emfField;

    public AnnotationScannerTest() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
        setEmMethod = TestClass.class.getMethod("setEm", new Class<?>[] {EntityManager.class});
        emField = TestClass.class.getDeclaredField("em");
        setEmfMethod = TestClass.class.getMethod("setEmf", new Class<?>[] {EntityManagerFactory.class});
        emfField = TestClass.class.getDeclaredField("emf");
    }

    @Test
    public void getNameTest() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
        Assert.assertEquals("em", AnnotationScanner.getName(setEmMethod));
        Assert.assertEquals("em", AnnotationScanner.getName(emField));
        Assert.assertEquals("emf", AnnotationScanner.getName(setEmfMethod));
        Assert.assertEquals("emf", AnnotationScanner.getName(emfField));

    }

    @Test
    public void getTypeTest() {
        Assert.assertEquals(EntityManager.class, AnnotationScanner.getType(setEmMethod));
        Assert.assertEquals(EntityManager.class, AnnotationScanner.getType(emField));
        Assert.assertEquals(EntityManagerFactory.class, AnnotationScanner.getType(setEmfMethod));
        Assert.assertEquals(EntityManagerFactory.class, AnnotationScanner.getType(emfField));

    }
    
    @Test
    public void getPCAnnotatedMembersTest() {
        AnnotationScanner scanner = new AnnotationScanner();
        List<AccessibleObject> members = scanner.getJpaAnnotatedMembers(TestClass.class, PersistenceContext.class);
        Assert.assertEquals(1, members.size());
        AccessibleObject member = members.get(0);
        Assert.assertEquals(Field.class, member.getClass());
        Field field = (Field)member;
        Assert.assertEquals("em", field.getName());
    }

    @Test
    public void getPUAnnotatedMembersTest() {
        AnnotationScanner scanner = new AnnotationScanner();
        List<AccessibleObject> members = scanner.getJpaAnnotatedMembers(TestClass.class, PersistenceUnit.class);
        Assert.assertEquals(1, members.size());
        AccessibleObject member = members.get(0);
        Assert.assertEquals(Method.class, member.getClass());
        Method method = (Method)member;
        Assert.assertEquals("setEmf", method.getName());
    }

    /**
     * When using a factory the class can be an interface. We need to make sure this does not cause a NPE
     */
    @Test
    public void getFactoryTest() {
        AnnotationScanner scanner = new AnnotationScanner();
        List<AccessibleObject> members = scanner.getJpaAnnotatedMembers(TestInterface.class, PersistenceUnit.class);
        Assert.assertEquals(0, members.size());
    }
    
}
