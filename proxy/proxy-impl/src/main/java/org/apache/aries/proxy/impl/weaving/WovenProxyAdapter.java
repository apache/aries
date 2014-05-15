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

import org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter;
import org.apache.aries.proxy.impl.common.WovenProxyConcreteMethodAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * Used to weave classes processed by the {@link ProxyWeavingHook}
 */
final class WovenProxyAdapter extends AbstractWovenProxyAdapter {

  private boolean sVUIDGenerated = false;

  public WovenProxyAdapter(ClassVisitor writer, String className,
      ClassLoader loader) {
    super(writer, className, loader);
  }

  /**
   * Get the weaving visitor used to weave instance methods, or just copy abstract ones
   */
  protected final MethodVisitor getWeavingMethodVisitor(int access, String name,
      String desc, String signature, String[] exceptions, Method currentMethod,
      String methodStaticFieldName, Type currentMethodDeclaringType,
      boolean currentMethodDeclaringTypeIsInterface) {
    MethodVisitor methodVisitorToReturn;
    if((access & ACC_ABSTRACT) == 0) {
      methodVisitorToReturn = new WovenProxyConcreteMethodAdapter(cv.visitMethod(
          access, name, desc, signature, exceptions), access, name, desc,
          exceptions, methodStaticFieldName, currentMethod, typeBeingWoven,
          currentMethodDeclaringType, currentMethodDeclaringTypeIsInterface);
    } else {
      methodVisitorToReturn = cv.visitMethod(access, name, desc, signature, exceptions);
    }
    return methodVisitorToReturn;
  }


  @Override
  public FieldVisitor visitField(int access, String name, String arg2,
      String arg3, Object arg4) {
    
    //If this sVUID is generated then make it synthetic
    if(sVUIDGenerated && "serialVersionUID".equals(name)) {
      
      //If we aren't a serializable class then don't add a generated sVUID
      if(!!!isSerializable) {
        return null;
      }
      
      access |= ACC_SYNTHETIC;
    }
    return super.visitField(access, name, arg2, arg3, arg4);
  }

  public void setSVUIDGenerated(boolean b) {
    sVUIDGenerated  = b;
  }
  
  
}