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

import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.DISPATCHER_FIELD;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.DISPATCHER_TYPE;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.OBJECT_TYPE;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public final class WovenProxyConcreteMethodAdapter extends AbstractWovenProxyMethodAdapter {

  /** Jump here to start executing the original method body **/
  private final Label executeDispatch = new Label();
  
  public WovenProxyConcreteMethodAdapter(MethodVisitor mv, int access, String name,
      String desc, String[] exceptions, String methodStaticFieldName, Method currentTransformMethod,
      Type typeBeingWoven, Type methodDeclaringType, boolean isMethodDeclaringTypeInterface) {
    //If we're running on Java 6+ We need to inline any JSR instructions because we're computing stack frames.
    //otherwise we can save the overhead
    super(mv, access, name, desc, methodStaticFieldName, currentTransformMethod, typeBeingWoven,
        methodDeclaringType, isMethodDeclaringTypeInterface, false);
  }

  /**
   * We weave instructions before the normal method body. We must be careful not
   * to violate the "rules" of Java (e.g. that try blocks cannot intersect, but
   * can be nested). We must also not violate the ordering that ASM expects, so
   * we must not call visitMaxs, or define labels before a try/catch that uses 
   * them!
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
    
    //Check if we have a dispatcher, if so then we need to dispatch!
    loadThis();
    getField(typeBeingWoven, DISPATCHER_FIELD, DISPATCHER_TYPE);
    ifNonNull(executeDispatch);
  }

  @Override
  public final void visitMaxs(int stack, int locals) {
    //Mark this location for continuing execution when a dispatcher is set
    mark(executeDispatch);
    //Write the dispatcher code in here
    writeDispatcher();
    mv.visitMaxs(stack, locals);
  }
}