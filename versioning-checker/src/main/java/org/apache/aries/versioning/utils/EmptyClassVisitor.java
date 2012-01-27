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
package org.apache.aries.versioning.utils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class EmptyClassVisitor extends ClassVisitor {

    public EmptyClassVisitor()
  {
    super(SemanticVersioningUtils.ASM4);
    
  }

    @Override
    public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {


    }

    @Override
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {

        return null;
    }

    @Override
    public void visitAttribute(Attribute arg0) {
        //noop

    }

    @Override
    public void visitEnd() {
        // noop

    }

    @Override
    public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
        // noop
        return null;
    }

    @Override
    public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
        // noop

    }

    @Override
    public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
        // noop
        return null;
    }

    @Override
    public void visitOuterClass(String arg0, String arg1, String arg2) {
        // noop
    }

    @Override
    public void visitSource(String arg0, String arg1) {
        // noop

    }

}
