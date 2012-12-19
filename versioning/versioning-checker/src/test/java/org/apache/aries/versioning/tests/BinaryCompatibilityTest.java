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
package org.apache.aries.versioning.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_NATIVE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED;
import static org.objectweb.asm.Opcodes.ACC_TRANSIENT;
import static org.objectweb.asm.Opcodes.V1_5;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.aries.versioning.utils.BinaryCompatibilityStatus;
import org.apache.aries.versioning.utils.MethodDeclaration;
import org.apache.aries.versioning.utils.SemanticVersioningClassVisitor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;


/**
 * Test the jdk chap 13 -binary compatibility implementation
 *
 * @author emily
 */
public class BinaryCompatibilityTest {

    private static final int ACC_ABASTRACT = 0;
    private static URLClassLoader loader = null;

    @BeforeClass
    public static void setup() throws Exception {
        Collection<URL> urls = new HashSet<URL>();
        urls.add(new File("unitest/").toURI().toURL());
        loader = new URLClassLoader(urls.toArray(new URL[0]));
    }

    /**
     * Test of binary incompatibility where a class was not abstract is changed to be declared abstract,
     */
    @Test
    public void test_jdk_chap13_4_1_1() {
        // construct original class
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertTrue(
                "When a class is changed from non abstract to abstract, this should break binary compatibility.",
                bcs.size() == 1);

        assertEquals(" The class pkg/Test was not abstract but is changed to be abstract.", bcs.get(0));
    }

    /**
     * Test of binary compatibility where a class was abstract is changed to be declared non-abstract,
     */
    @Test
    public void test_jdk_chap13_4_1_2() {
        // construct original class
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();

        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertTrue(
                "When a class is changed from static to non-static, this should not break binary compatibility.",
                bcs.isCompatible());


    }

    /**
     * Test a binary incompatibility where a class was not final is changed to be final.
     */
    @Test
    public void test_jdk_chap13_4_2_1() {
        // construct original class
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_FINAL, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertTrue(
                "When a class is changed from non final to final, this should break binary compatibility.",
                bcs.size() == 1);

        assertEquals(" The class pkg/Test was not final but is changed to be final.", bcs.get(0));
    }

    /**
     * Test a binary compatibility where a class was final is changed to not final.
     */
    @Test
    public void test_jdk_chap13_4_2_2() {
        // construct original class
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_FINAL, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertTrue(
                "When a class is changed from final to not final, this should not break binary compatibility.",
                bcs.isCompatible());

    }

    /**
     * Test a binary incompatibility where a class was public is changed to not public.
     */
    @Test
    public void test_jdk_chap13_4_3_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_FINAL, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_FINAL, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertNull(
                "When a class is changed from public to non-public, this should break binary compatibility.",
                newCV.getClassDeclaration());

    }

    /**
     * Test a binary incompatibility where a class was not public is changed to public.
     */
    @Test
    public void test_jdk_chap13_4_3_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_FINAL, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_FINAL, "pkg/Test", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "When a class is changed from non-public to public, this should break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Changing the direct superclass or the set of direct superinterfaces of a class type will not break compatibility
     * with pre-existing binaries, provided that the total set of superclasses or superinterfaces, respectively, of the class type loses no members.
     */
    @Test
    public void test_jdk_chap13_4_4_1() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestBChild", null);

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Changing the direct superclass or the set of direct superinterfaces of a class type will not breake binary compatibility if not losing any members.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Changing field type breaks binary compatibility
     * with pre-existing binaries, provided that the total set of superclasses or superinterfaces, respectively, of the class type loses no members but the one of the fields has changed type.
     */
    @Test
    public void test_jdk_chap13_4_7_4() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertTrue(
                "Changing the direct superclass or the set of direct superinterfaces of a class type results fields changes. This should breake binary compatibility if not losing any members.",
                bcs.size() == 2);
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {"The public field bar was static but is changed to be non static or vice versa.",
                "The public field bar has changed its type."})), new HashSet<String>(bcs));
    }

    /**
     * Change the signature of the field from Colllection<AA> to Collection<String>
     */
    @Test
    public void test_jdk_chap13_4_7_5() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitField(ACC_PUBLIC, "more", "Ljava/util/Collection;", "Lcom/bim/AA;", null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitField(ACC_PROTECTED, "more", "Ljava/util/Collection;", "Ljava/lang/String;", null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {
                "The public field more becomes less accessible."})), new HashSet<String>(bcs));
        assertTrue(
                "Changing the declared access of a field to permit less access  , this should break binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * If a change to the direct superclass or the set of direct superinterfaces results in any class or interface no longer being a superclass or superinterface, respectively, it will break binary compatibility.
     */
    @Test
    public void test_jdk_chap13_4_4_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertFalse(
                "If a change to the direct superclass or the set of direct superinterfaces results in any class or interface no longer being a superclass or superinterface, respectively, it will break binary compatibility.",
                bcs.isCompatible());
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {"The method int getFooLen(java.lang.String) has been deleted or its return type or parameter list has changed.",
                "The method java.lang.String getFoo() has changed from non abstract to abstract.",
                "The method int getBarLen(java.lang.String) has been deleted or its return type or parameter list has changed.",
                "The method int getBooLen(java.lang.String) has been deleted or its return type or parameter list has changed.",
                "The superclasses or superinterfaces have stopped being super: [versioning/java/files/TestC, versioning/java/files/TestA].",
                "The protected field c has been deleted.",
                "The public field bar was not final but has been changed to be final.",
                "The public field bar was static but is changed to be non static or vice versa.",
                "The public field bar has changed its type."})), new HashSet<String>(bcs));
    }

    /**
     * Test deleting a class member or constructor that is not declared private/default breaks binary compatibility.
     */
    @Test
    public void test_jdk_chap13_4_5_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitMethod(ACC_PROTECTED + ACC_ABSTRACT, "convert", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int convert(java.lang.Object) has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "deleting a class member or constructor that is not declared private breaks binary compatibility.",
                bcs.size() == 1);

    }

    /**
     * Test deleting a class member or constructor that is declared private should not break binary compatibility.
     */
    @Test
    public void test_jdk_chap13_4_5_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitMethod(ACC_PRIVATE, "convert", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Deleting a class member or constructor that is declared private should not break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Test adding a class member or constructor should not break binary compatibility.
     */
    @Test
    public void test_jdk_chap13_4_5_3() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);

        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitMethod(ACC_PROTECTED, "convert", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Adding a class member or constructor should not break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }


    /**
     * Test changing the declared access of a method to permit less access, this should break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_6_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "convert", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitMethod(ACC_STATIC, "convert", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int convert(java.lang.Object) is less accessible.", bcs.get(0));
        assertTrue(
                "Changing the declared access of a member or contructor to permit less access  , this should break binary compatibility.",
                bcs.size() == 1);
    }

    @Test
    public void test_jdk_chap13_4_6_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitField(ACC_PUBLIC, "lESS", "I", null, new Integer(-1)).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitField(ACC_PROTECTED, "lESS", "I", null, new Integer(-1)).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The public field lESS becomes less accessible.", bcs.get(0));
        assertTrue(
                "Changing the declared access of a field to permit less access  , this should break binary compatibility.",
                bcs.size()  == 1);
    }

    /**
     * Test deleting a private/default field, this should not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_7_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitField(ACC_PRIVATE, "lESS", "I", null, new Integer(-1)).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();

        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", null);
        cw.visitMethod(ACC_PROTECTED, "convert", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Deleting a private field should not break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * If a field is added but is less accessible than the old one or change to static from non-static or from non-static to static.
     */
    @Test
    public void test_jdk_chap13_4_7_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PROTECTED, "bar", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The public field bar becomes less accessible.", bcs.get(0));
        assertTrue(
                "The new field conflicts with a field in the super class. Check chapter 13.4.7 java spec for more info.",
                bcs.size() == 1);
    }

    /**
     * If a field was not final is changed to be final, then it can break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_8_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_FINAL, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The public field aa was not final but has been changed to be final.", bcs.get(0));
        assertTrue(
                "Change that a public or protected field was final but is changed to be not final will break binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * If a field was final is changed to be non-final, then it does not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_8_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_FINAL, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "If a change to the direct superclass or the set of direct superinterfaces results in any class or interface no longer being a superclass or superinterface, respectively, it will break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * If a field was static is changed to be non-static, then it will break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_9_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_STATIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The public field aa was static but is changed to be non static or vice versa.", bcs.get(0));
        assertTrue(
                "If a field was static is changed to be non-static, then it will break compatibility.",
                bcs.size() == 1);
    }

    /**
     * If a field was non-static is changed to be static, then it will break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_9_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_STATIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The public field aa was static but is changed to be non static or vice versa.", bcs.get(0));
        assertTrue(
                "If a field was non-static is changed to be static, then it will break compatibility.",
                bcs.size() == 1);
    }

    /**
     * If a field was transient is changed to non-transient, then it will not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_10_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));

        assertTrue(
                "If a change to the direct superclass or the set of direct superinterfaces results in any class or interface no longer being a superclass or superinterface, respectively, it will break binary compatibility.",
                bcs.isCompatible());
    }

    /**
     * If a field was non-transient is changed to transient, then it will not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_10_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));

        assertTrue(
                "If a change to the direct superclass or the set of direct superinterfaces results in any class or interface no longer being a superclass or superinterface, respectively, it will break binary compatibility.",
                bcs.isCompatible());
    }

    /**
     * Testing deleting a public/protected method when there is no such a method in the superclass breaks binary compatibility.
     */
    @Test
    public void test_jdk_chap13_4_11_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getFooLen", "(Ljava/lang/STring;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getFooLen(java.lang.STring) has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "Deleting a public/protected method when there is no such a method in the superclass breaks binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * Testing deleting a public/protected method when there is such a method in the superclass does not break binary compatibility.
     */
    @Test
    public void test_jdk_chap13_4_11_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getBarLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertTrue(
                "If a change to the direct superclass or the set of direct superinterfaces results in any class or interface no longer being a superclass or superinterface, respectively, it will break binary compatibility.",
                bcs.isCompatible());
    }

    /**
     * Testing deleting a public/protected method when there is less accessible method in the superclass does break binary compatibility.
     */
    @Test
    public void test_jdk_chap13_4_11_3() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getFooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getFooLen(java.lang.String) is less accessible.", bcs.get(0));
        assertTrue(
                "If a change to the direct superclass or the set of direct superinterfaces results in any class or interface no longer being a superclass or superinterface, respectively, it will break binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * Adding a parameter is a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_12_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;I)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getCooLen(java.lang.String) has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "Changing a parameter list will break binary compatibility.",
                bcs.size() == 1);
    }


    /**
     * Changing a method parameter type is a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_12_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getCooLen(java.lang.String) has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "Changing a method paramether type will break binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * Changing a method formal type parameter is not a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_12_3() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/util/List;)I", "Ljava/lang/String;", null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);

        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/util/List;)I", "Lcome/ibm/blah;", null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertTrue(
                "Changing a method formal type parameter should not break binary compatibility.",
                bcs.isCompatible());
    }

    /**
     * Changing a method return type is a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_13_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)[I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getCooLen(java.lang.String) has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "Changing a method return type will break binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * Changing a method to be abstract is a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_14_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getCooLen(java.lang.String) has changed from non abstract to abstract.", bcs.get(0));
        assertTrue(
                "Changing a method to be abstract will break binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * Changing a method to not to be abstract is not a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_14_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_ABASTRACT, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Changing a method not to be abstract will not break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }


    /**
     * Changing an instance method that is not final to be final is a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_15_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC + ACC_FINAL, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getCooLen(java.lang.String) was not final but has been changed to be final.", bcs.get(0));
        assertTrue(
                "Changing an instance method from non-final to final will break binary compatibility.",
                bcs.size() == 1);
    }

    /**
     * Changing an instance method that is final to be non-final is a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_15_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_FINAL, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Changing an instance method from final to non-final will not break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Changing a static method that is not final to final is not a binary compatibility change.
     */
    @Test
    public void test_jdk_chap13_4_15_3() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Changing a static method from non-final to final will not break binary compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Adding a native modifier of a method does not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_16_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_NATIVE, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Adding a native modifier of a method does not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Deleting a native modifier of a method does not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_16_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_NATIVE, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Adding a native modifier of a method does not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * If a method is not private was not declared static and is changed to be declared static, this breaks compatibility.
     */
    @Test
    public void test_jdk_chap13_4_17_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getCooLen(java.lang.String) has changed from static to non-static or vice versa.", bcs.get(0));
        assertTrue(
                "If a method is not private was not declared static and is changed to be decalared static, this should break compatibility.",
                bcs.size() == 1);
    }

    /**
     * If a method is not private was declared static and is changed to not be declared static, this breaks compatibility.
     */
    @Test
    public void test_jdk_chap13_4_17_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getCooLen(java.lang.String) has changed from static to non-static or vice versa.", bcs.get(0));
        assertTrue(
                "If a method is not private was declared static and is changed to not be decalared static, this should break compatibility.",
                bcs.size() == 1);
    }

    /**
     * If a method is  private was declared static and is changed to not be declared static, this does not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_17_3() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PRIVATE + ACC_STATIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC + ACC_TRANSIENT, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PRIVATE, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));

        assertTrue(
                "If a method is private was declared static and is changed to not be decalared static, this should not break compatibility.",
                bcs.isCompatible());
    }

    /**
     * Adding a synchronized modifier of a method does not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_18_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_SYNCHRONIZED, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Adding a synchronized modifier of a method does not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Deleting a synchronized modifier of a method does not break compatibility.
     */
    @Test
    public void test_jdk_chap13_4_18_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitMethod(ACC_PUBLIC + ACC_SYNCHRONIZED, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "versioning/java/files/TestA", null);
        cw.visitField(ACC_PUBLIC, "aa", "Ljava/lang/String;", null, new String("newBar")).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getCooLen", "(Ljava/lang/String;)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Adding a synchronized modifier of a method does not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Changing an interface that is not declared public to be declared public does not break compatibility.
     */
    @Test
    public void test_jdk_chap13_5_1_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});

        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertTrue(
                "Changing an interface that is not declared public to be declared public should not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration())).isCompatible());
    }

    /**
     * Changing an interface that is declared public to not be declared public should break compatibility.
     */
    @Test
    public void test_jdk_chap13_5_1_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});

        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);
        assertNull(
                "Changing an interface that is declared public to not be declared public should break compatibility.",
                newCV.getClassDeclaration());
    }

    /**
     * Changes to the interface hierarchy resulting an interface not being a super interface should break compatibility.
     */
    @Test
    public void test_jdk_chap13_5_2_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});

        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", null);
        cw.visitMethod(ACC_PUBLIC, "getFoo", "I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {"The public field bar has been deleted.",
                "The superclasses or superinterfaces have stopped being super: [versioning/java/files/TestB].",
                "The method java.lang.String getFoo() has been deleted or its return type or parameter list has changed."})), new HashSet<String>(bcs));
        assertFalse(
                "Changes to the interface hierarchy resulting an interface not being a super interface should break compatibility.",
                bcs.isCompatible());
    }

    /**
     * Deleting a method in an interface should break compatibility.
     */
    @Test
    public void test_jdk_chap13_5_3_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC, "getFoo", "()I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getFoo() has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "Deleting a method in an interface should break compatibility.",
                bcs.size() == 1);
    }

    /**
     * Adding a method in an interface should not break compatibility.
     */
    @Test
    public void test_jdk_chap13_5_3_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC, "getFoo", "()I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC, "getFoo", "()I", null, null).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getMoo", "()I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        assertTrue(
                "Adding a method in an interface should not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus(oldCV.getClassDeclaration()).isCompatible());
    }

    /**
     * Changing a method return type in an interface should  break compatibility.
     */
    @Test
    public void test_jdk_chap13_5_5_1() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC, "getFoo", "()I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC, "getFoo", "(I)I", null, null).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getMoo", "()I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getFoo() has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "Changing a method return type in an interface should  break compatibility.",
                bcs.size() == 1);
    }

    /**
     * Changing a method parameter in an interface should  break compatibility.
     */
    @Test
    public void test_jdk_chap13_5_5_2() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC, "getFoo", "(I)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC, "getFoo", "(IZ)I", null, null).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "getMoo", "I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        BinaryCompatibilityStatus bcs = newCV.getClassDeclaration().getBinaryCompatibleStatus((oldCV.getClassDeclaration()));
        assertEquals("The method int getFoo(int) has been deleted or its return type or parameter list has changed.", bcs.get(0));
        assertTrue(
                "Changing a method parameter in an interface should  break compatibility.",
                bcs.size() == 1);
    }

    /**
     * Check containing more abstract methods
     */
    @Test
    public void test_containing_more_abstract_methods() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "getFoo", "(I)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "getFoo", "(I)I", null, null).visitEnd();
        cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "getMoo", "()I", null, null).visitEnd();
        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        assertTrue(
                "Adding an abstract methods should not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus(oldCV.getClassDeclaration()).isCompatible());

        Collection<MethodDeclaration> extraMethods = newCV.getClassDeclaration().getExtraMethods(oldCV.getClassDeclaration());
        assertEquals(1, extraMethods.size());
        for (MethodDeclaration md : extraMethods) {
            assertEquals(
                    "getMoo", md.getName());
        }
    }

    /**
     * Check not containing more abstract methods
     */
    @Test
    public void test_not_cotaining_more_abstract_methods() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "getFoo", "(I)I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "getFoo", "(I)I", null, null).visitEnd();

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        assertTrue(
                "No change should not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus(oldCV.getClassDeclaration()).isCompatible());


        assertEquals(
                "Containing more abstract methods should return false.", 0,
                newCV.getClassDeclaration().getExtraMethods(oldCV.getClassDeclaration()).size());
    }

    @Test
    public void test_ignore_clinit() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitField(ACC_PUBLIC, "foo", "I", null, null).visitEnd();
        cw.visitEnd();
        byte[] oldBytes = cw.toByteArray();


        cw = new ClassWriter(0);
        cw.visit(V1_5, ACC_PUBLIC, "pkg/Test", null, "java/lang/Object", new String[]{"versioning/java/files/TestB"});
        cw.visitField(ACC_PUBLIC + ACC_STATIC, "bar", "I", null, null).visitEnd();
        cw.visitField(ACC_PUBLIC, "foo", "I", null, null).visitEnd();
        cw.visitMethod(ACC_PUBLIC, "<clinit>", "()V", null, null).visitEnd();

        cw.visitEnd();
        byte[] newBytes = cw.toByteArray();
        SemanticVersioningClassVisitor oldCV = new SemanticVersioningClassVisitor(loader);
        SemanticVersioningClassVisitor newCV = new SemanticVersioningClassVisitor(loader);
        ClassReader newCR = new ClassReader(newBytes);
        ClassReader oldCR = new ClassReader(oldBytes);

        newCR.accept(newCV, 0);
        oldCR.accept(oldCV, 0);

        assertTrue(
                "No change should not break compatibility.",
                newCV.getClassDeclaration().getBinaryCompatibleStatus(oldCV.getClassDeclaration()).isCompatible());


        assertEquals(
                "Containing more abstract methods should return false.", 0,
                newCV.getClassDeclaration().getExtraMethods(oldCV.getClassDeclaration()).size());
    }

}
