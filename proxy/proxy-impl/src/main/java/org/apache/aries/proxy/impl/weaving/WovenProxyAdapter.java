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
package org.apache.aries.proxy.impl.weaving;

import java.io.IOException;

import org.apache.aries.proxy.impl.NLS;
import org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * Used to weave classes processed by the {@link ProxyWeavingHook}
 */
public final class WovenProxyAdapter extends AbstractWovenProxyAdapter {

  public WovenProxyAdapter(ClassVisitor writer, String className,
      ClassLoader loader) {
    super(writer, className, loader);
  }

  /**
   * Get the weaving visitor used to weave instance methods
   */
  protected final MethodVisitor getWeavingMethodVisitor(int access, String name,
      String desc, String signature, String[] exceptions, Method currentMethod,
      String methodStaticFieldName) {
    MethodVisitor methodVisitorToReturn;
    methodVisitorToReturn = new WovenProxyMethodAdapter(cv.visitMethod(
        access, name, desc, signature, exceptions), access, name, desc,
        exceptions, methodStaticFieldName, currentMethod, typeBeingWoven);
    return methodVisitorToReturn;
  }
  
  @Override
  protected final Type getDeclaringTypeForCurrentMethod() {
    return typeBeingWoven;
  }

  @Override
  public void visitEnd() {
    //first we need to override all the methods that were on non-object parents
    for(Class<?> c : nonObjectSupers) {
      try {
        readClass(c, new MethodCopyingClassAdapter(cv, 
            c, typeBeingWoven, getKnownMethods(), transformedMethods));
      } catch (IOException e) {
        // This should never happen! <= famous last words (not)
        throw new RuntimeException(NLS.MESSAGES.getMessage("unexpected.error.processing.class", c.getName(), typeBeingWoven.getClassName()), e);
      }
    }
    //Now run the normal visitEnd
    super.visitEnd();
  }
  
  
}