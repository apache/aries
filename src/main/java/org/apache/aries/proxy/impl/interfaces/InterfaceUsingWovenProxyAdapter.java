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
package org.apache.aries.proxy.impl.interfaces;

import org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter;
import org.apache.aries.proxy.impl.weaving.ProxyWeavingHook;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * Used to copy method signatures into a new class
 */
final class InterfaceUsingWovenProxyAdapter extends AbstractWovenProxyAdapter {

  private Type currentIfaceType;
  
  public InterfaceUsingWovenProxyAdapter(ClassVisitor writer, String className,
      ClassLoader loader) {
    super(writer, className, loader);
  }

  public final void setCurrentInterface(Type type) {
    currentIfaceType = type;
  }
  
  /**
   * Return a {@link MethodVisitor} that copes with interfaces
   */ 
  protected final MethodVisitor getWeavingMethodVisitor(int access, String name,
      String desc, String signature, String[] exceptions, Method currentMethod,
      String methodStaticFieldName) {
    return new InterfaceUsingWovenProxyMethodAdapter(cv.visitMethod(
        access, name, desc, signature, exceptions), access, name, desc,
        methodStaticFieldName, currentMethod, typeBeingWoven, currentIfaceType);
  }

  @Override
  protected final Type getDeclaringTypeForCurrentMethod() {
    return currentIfaceType;
  }
}
