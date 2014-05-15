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

import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.OBJECT_TYPE;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * Used to create a delegating method implementation for methods with no body
 */
public final class WovenProxyAbstractMethodAdapter extends AbstractWovenProxyMethodAdapter {
 
  public WovenProxyAbstractMethodAdapter(MethodVisitor mv, int access, String name,
      String desc, String methodStaticFieldName, Method currentTransformMethod,
      Type typeBeingWoven, Type methodDeclaringType, boolean isMethodDeclaringTypeInterface, boolean isDefaultMethod) {
    super(mv, access, name, desc, methodStaticFieldName, currentTransformMethod,
        typeBeingWoven, methodDeclaringType, isMethodDeclaringTypeInterface, isDefaultMethod);
  }

  /**
   * We write dispatch code here because we have no real method body
   */
  @Override
  public final void visitCode()
  {
    //Notify our parent that the method code is starting. This must happen first
    mv.visitCode();
    
    //unwrap for equals if we need to
    if(currentTransformMethod.getName().equals("equals") && 
        currentTransformMethod.getArgumentTypes().length == 1 && 
        currentTransformMethod.getArgumentTypes()[0].equals(OBJECT_TYPE)) {
      unwrapEqualsArgument();
    }
    //No null-check needed
    //Write the dispatcher code in here
    writeDispatcher();
  }

  @Override
  public final void visitMaxs(int stack, int locals) {
    mv.visitMaxs(stack, locals);
  }
  
  /**
   * We don't get the code and maxs calls for interfaces, so we add them here
   */
  @Override
  public final void visitEnd() {
    visitCode();
    visitMaxs(0, 0);
    mv.visitEnd();
  }
}
