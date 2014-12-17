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
package org.apache.aries.proxy.impl.common;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
/**
 * We need to override ASM's default behaviour in {@link #getCommonSuperClass(String, String)}
 * so that it doesn't load classes (which it was doing on the wrong {@link ClassLoader}
 * anyway...)
 */
public final class OSGiFriendlyClassVisitor extends ClassVisitor {

 
  private final boolean inlineJSR;
  
  public OSGiFriendlyClassVisitor(ClassVisitor cv, int arg1) {
   
    super(Opcodes.ASM5, cv);

    inlineJSR = arg1 == ClassWriter.COMPUTE_FRAMES;
  }
  
  

  
  @Override
  public MethodVisitor visitMethod(int arg0, String arg1, String arg2,
      String arg3, String[] arg4) {
    MethodVisitor mv =  cv.visitMethod(arg0, arg1, arg2, arg3, arg4);
    
    if(inlineJSR)
      mv = new JSRInlinerAdapter(mv, arg0, arg1, arg2, arg3, arg4);
    
    return mv;
  }

}
