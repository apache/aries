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

import java.io.IOException;
import java.util.Collection;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter;
import org.apache.aries.proxy.impl.common.OSGiFriendlyClassWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;

/**
 * This class is used to aggregate several interfaces into a real class which implements all of them
 */
final class InterfaceCombiningClassAdapter extends EmptyVisitor implements Opcodes {
  
  /** The interfaces we need to implement */
  private final Collection<Class<?>> interfaces;
  /** The {@link ClassWriter} we use to write our class */
  private final ClassWriter writer;
  /** The adapter we use to weave in our method implementations */
  private final InterfaceUsingWovenProxyAdapter adapter;
  /** Whether we have already written the class bytes */
  private boolean done = false;
  
  /**
   * Construct an {@link InterfaceCombiningClassAdapter} to combine the supplied
   * interfaces into a class with the supplied name using the supplied classloader
   * @param className
   * @param loader
   * @param interfaces
   */
   InterfaceCombiningClassAdapter(String className,
      ClassLoader loader, Collection<Class<?>> interfaces) {
    writer = new OSGiFriendlyClassWriter(ClassWriter.COMPUTE_FRAMES, loader);
    adapter = new InterfaceUsingWovenProxyAdapter(writer, className, loader);
    
    this.interfaces = interfaces;
    String[] interfaceNames = new String[interfaces.size()];
    
    int i = 0;
    for(Class<?> in : interfaces) {
      interfaceNames[i] = Type.getType(in).getInternalName();
      i++;
    }
    
    adapter.visit(V1_6, ACC_PUBLIC | ACC_SYNTHETIC, className, null,
        AbstractWovenProxyAdapter.OBJECT_TYPE.getInternalName(), interfaceNames);
  }


  @Override
  public final MethodVisitor visitMethod(int access, String name, String desc,
      String sig, String[] arg4) {
    //We're going to implement this method, so make it non abstract!
    access &= ~ACC_ABSTRACT;
    //If we already implement this method (from another interface) then we don't
    //want a duplicate. We also don't want to copy any static init blocks (these
    //initialize static fields on the interface that we don't copy
    if(adapter.getKnownMethods().contains(new Method(name, desc)) || 
        "<clinit>".equals(name))
      return null;
    else 
      return adapter.visitMethod(access, name, desc, null, arg4);
  }
  
  /**
   * Generate the byte[] for our class
   * @return
   * @throws UnableToProxyException
   */
  final byte[] generateBytes() throws UnableToProxyException {
    if(!!!done) {
      for(Class<?> c : interfaces) {
        adapter.setCurrentInterface(Type.getType(c));
        try {
          AbstractWovenProxyAdapter.readClass(c, this);
        } catch (IOException e) {
          throw new UnableToProxyException(c, e);
        }
      }
      
      adapter.setCurrentInterface(Type.getType(Object.class));
      visitObjectMethods();
      
      adapter.visitEnd();
      done  = true;
    }
    return writer.toByteArray();
  }
  
  /**
   * Make sure that the three common Object methods toString, equals and hashCode are redirected to the delegate
   * even if they are not on any of the interfaces
   */
  private void visitObjectMethods() {
      MethodVisitor visitor = visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
      if (visitor != null) visitor.visitEnd();
      
      visitor = visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
      if (visitor != null) visitor.visitEnd();

      visitor = visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
      if (visitor != null) visitor.visitEnd();      
  }
}