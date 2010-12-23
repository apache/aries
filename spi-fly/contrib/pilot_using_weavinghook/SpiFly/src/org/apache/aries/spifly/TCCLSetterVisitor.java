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
import java.util.ServiceLoader;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This class implements an ASM ClassVisitor which puts the appropriate ThreadContextClassloader
 * calls around applicable method invocations. It does the actual bytecode weaving.
 */
public class TCCLSetterVisitor extends ClassAdapter implements ClassVisitor, Opcodes {
    private static final String GENERATED_METHOD_NAME = "$$FCCL$$";

    private static final String UTIL_CLASS = Util.class.getName().replace('.', '/'); 
    private static final String VOID_RETURN_TYPE = "()V";
    
    private final String targetClass;
    private final WeavingData [] weavingData;

    // Set to true when the weaving code has changed the client such that an additional import 
    // (to the Util.class.getPackage()) is needed.
    private boolean additionalImportRequired = false;

    public TCCLSetterVisitor(ClassVisitor cv, String className, WeavingData [] weavingData) {
        super(cv);
        this.targetClass = className.replace('.', '/');
        this.weavingData = weavingData;
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

        for (WeavingData wd : weavingData) {
            /* Equivalent to:
             * private static void $$FCCL$$<className>$<methodName>(Class<?> cls) {
             *   Util.fixContextClassLoader("java.util.ServiceLoader", "load", cls, WovenClass.class.getClassLoader());
             * }
             */
             MethodVisitor mv = cv.visitMethod(ACC_PRIVATE + ACC_STATIC, getGeneratedMethodName(wd), 
                     "(Ljava/lang/Class;)V", "(Ljava/lang/Class<*>;)V", null);
             mv.visitCode();
             mv.visitLdcInsn(wd.getClassName());
             mv.visitLdcInsn(wd.getMethodName());
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
        }

        super.visitEnd();
    }

    private String getGeneratedMethodName(WeavingData wd) {
        StringBuilder name = new StringBuilder(GENERATED_METHOD_NAME);
        name.append(wd.getClassName().replace('.', '#'));
        name.append("$");
        name.append(wd.getMethodName());
        if (wd.getArgClasses() != null) {
            for (String cls : wd.getArgClasses()) {
                name.append("$");
                name.append(cls.replace('.', '#'));
            }
        }
        return name.toString();
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
                        
            WeavingData wd = findWeavingData(owner, name, desc);            
            if (opcode == INVOKESTATIC && wd != null) {
                System.out.println("+++ Gotcha!");
          
                additionalImportRequired = true;

                // Add: Util.storeContextClassloader();                
                mv.visitMethodInsn(INVOKESTATIC, UTIL_CLASS,
                        "storeContextClassloader", VOID_RETURN_TYPE);

                // Add: MyClass.$$FCCL$$<classname>$<methodname>(<class>);                
                if (ServiceLoader.class.getName().equals(wd.getClassName()) &&
                    "load".equals(wd.getMethodName()) &&
                    (wd.getArgClasses() == null || Arrays.equals(new String [] {Class.class.getName()}, wd.getArgClasses()))) {
                    // ServiceLoader.load() is a special case because it's a general-purpose service loader, 
                    // therefore, the target class it the class being passed in to the ServiceLoader.load() 
                    // call itself.
                    mv.visitLdcInsn(lastLDCType);
                } else {
                    // In any other case, we're not dealing with a general-purpose service loader, but rather
                    // with a specific one, such as DocumentBuilderFactory.newInstance(). In that case the 
                    // target class is the class that is being invoked on (i.e. DocumentBuilderFactory).
                    Type type = Type.getObjectType(owner);
                    mv.visitLdcInsn(type);
                }
                mv.visitMethodInsn(INVOKESTATIC, targetClass,
                        getGeneratedMethodName(wd), "(Ljava/lang/Class;)V");

                super.visitMethodInsn(opcode, owner, name, desc);

                // Add: Util.restoreContextClassloader();
                mv.visitMethodInsn(INVOKESTATIC, UTIL_CLASS,
                        "restoreContextClassloader", VOID_RETURN_TYPE);
            } else {                
                super.visitMethodInsn(opcode, owner, name, desc);
            }
        }

        private WeavingData findWeavingData(String owner, String methodName, String methodDesc) {
            owner = owner.replace('/', '.');

            Type[] argTypes = Type.getArgumentTypes(methodDesc);
            String [] argClassNames = new String[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                argClassNames[i] = argTypes[i].getClassName();
            }

            for (WeavingData wd : weavingData) {
                if (wd.getClassName().equals(owner) &&
                    wd.getMethodName().equals(methodName) &&
                    (wd.getArgClasses() != null ? Arrays.equals(argClassNames, wd.getArgClasses()) : true)) {
                    return wd;
                }
            }
            return null;
        }
    }

    public boolean additionalImportRequired() {
        return additionalImportRequired ;
    }
}
