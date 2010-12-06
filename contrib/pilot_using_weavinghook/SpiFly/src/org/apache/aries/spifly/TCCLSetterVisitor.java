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
package org.apache.aries.spifly;

import java.util.Arrays;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This class implements an ASM ClassVisitor which puts the appropriate ThreadContextClassloader
 * calls around applicable method invocations. 
 */
public class TCCLSetterVisitor extends ClassAdapter implements ClassVisitor, Opcodes {
    private static final String GENERATED_METHOD_NAME = "$$FCCL$$";

    private static final String UTIL_CLASS = Util.class.getName().replace('.', '/'); 
    private static final String VOID_RETURN_TYPE = "()V";
    
    private final String targetClass;

    public TCCLSetterVisitor(ClassVisitor cv, String className) {
        super(cv);
        this.targetClass = className.replace('.', '/');
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        System.out.println("@@@ " + access + ": " + name + "#" + desc + "#" + signature + "~" + Arrays.toString(exceptions));

        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new TCCLSetterMethodVisitor(mv);
    }

    @Override
    public void visitEnd() {
        // Add generated static method

        /* Equivalent to:
         * private static void SomeMethodName(Class<?> cls) {
         *   Util.fixContextClassLoader("java.util.ServiceLoader", "load", cls, WovenClass.class.getClassLoader());
         * }
         */
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE + ACC_STATIC, GENERATED_METHOD_NAME, 
                "(Ljava/lang/Class;)V", "(Ljava/lang/Class<*>;)V", null);
        mv.visitCode();
        mv.visitLdcInsn("java.util.ServiceLoader");
        mv.visitLdcInsn("load");
        mv.visitVarInsn(ALOAD, 0);
        String typeIdentifier = "L" + targetClass + ";";
        mv.visitLdcInsn(Type.getType(typeIdentifier));
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class",
                "getClassLoader", "()Ljava/lang/ClassLoader;");
        mv.visitMethodInsn(
                INVOKESTATIC,
                "org/apache/aries/spifly/Util",
                "fixContextClassloader",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/ClassLoader;)V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();

        super.visitEnd();
    }
    
    private class TCCLSetterMethodVisitor extends MethodAdapter implements MethodVisitor
    {
        Type lastLDCType;
        
        public TCCLSetterMethodVisitor(MethodVisitor mv) {
            super(mv);
        }

        
        /**
         * Store the last LDC call. When ServiceLoader.load(Class cls) is called
         * the last LDC call before the ServiceLoader.load() visitMethodInsn call
         * contains the class being passed in. We need to pass this class to $$FCCL$$ as well
         * so we can copy the value found in here.
         */
        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Type) {
                lastLDCType = ((Type) cst);
            }
            super.visitLdcInsn(cst);
        }

        /**
         * Wrap selected method calls with
         *  Util.storeContextClassloader();
         *  $$FCCL$$(<class>)
         *  Util.restoreContextClassloader(); 
         */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            System.out.println("### " + opcode + ": " + owner + "#" + name + "#" + desc);
            
            if (opcode == INVOKESTATIC &&
                "java/util/ServiceLoader".equals(owner) &&
                "load".equals(name)) {
                System.out.println("+++ Gotcha!");
          
                // Add: Util.storeContextClassloader();                
                mv.visitMethodInsn(INVOKESTATIC, UTIL_CLASS,
                        "storeContextClassloader", VOID_RETURN_TYPE);
                // Add: MyClass.$$FCCL$$(<class>);
                // The class is the same class as the one passed into the ServiceLoader.load() api.
                mv.visitLdcInsn(lastLDCType);
                mv.visitMethodInsn(INVOKESTATIC, targetClass,
                        GENERATED_METHOD_NAME, "(Ljava/lang/Class;)V");

                super.visitMethodInsn(opcode, owner, name, desc);

                // Add: Util.restoreContextClassloader();
                mv.visitMethodInsn(INVOKESTATIC, UTIL_CLASS,
                        "restoreContextClassloader", VOID_RETURN_TYPE);
            } else {                
                super.visitMethodInsn(opcode, owner, name, desc);
            }
        }
    }
}
