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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.ProxyUtils;
import org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter;
import org.apache.aries.proxy.impl.common.OSGiFriendlyClassVisitor;
import org.apache.aries.proxy.impl.common.OSGiFriendlyClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * This class is used to aggregate several interfaces into a real class which implements all of them
 */
final class InterfaceCombiningClassAdapter extends ClassVisitor implements Opcodes {

  /** The superclass we should use */
  private final Class<?> superclass;
  /** The interfaces we need to implement */
  private final Collection<Class<?>> interfaces;
  /** The {@link ClassWriter} we use to write our class */
  private final ClassWriter writer;
  /** The adapter we use to weave in our method implementations */
  private final AbstractWovenProxyAdapter adapter;
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
      ClassLoader loader, Class<?> superclass, Collection<Class<?>> interfaces) {
    super(Opcodes.ASM5);
    writer = new OSGiFriendlyClassWriter(ClassWriter.COMPUTE_FRAMES, loader);
    ClassVisitor cv = new OSGiFriendlyClassVisitor(writer, ClassWriter.COMPUTE_FRAMES);
    adapter = new InterfaceUsingWovenProxyAdapter(cv, className, loader);

    this.interfaces = interfaces;
    this.superclass = superclass;
    String[] interfaceNames = new String[interfaces.size()];

    int i = 0;
    for(Class<?> in : interfaces) {
      interfaceNames[i] = Type.getInternalName(in);
      i++;
    }

    adapter.visit(ProxyUtils.getWeavingJavaVersion(), ACC_PUBLIC | ACC_SYNTHETIC, className, null,
        (superclass == null) ? AbstractWovenProxyAdapter.OBJECT_TYPE.getInternalName() :
          Type.getInternalName(superclass), interfaceNames);
  }


  @Override
  public final MethodVisitor visitMethod(int access, String name, String desc,
          String sig, String[] arg4) {
      //If we already implement this method (from another interface) then we don't
      //want a duplicate. We also don't want to copy any static init blocks (these
      //initialize static fields on the interface that we don't copy
      if(adapter.getKnownMethods().contains(new Method(name, desc)) || 
              "<clinit>".equals(name)) {
          return null;
      }
      else if(((access & (ACC_PRIVATE|ACC_SYNTHETIC)) == (ACC_PRIVATE|ACC_SYNTHETIC))) {
          // private, synthetic methods on interfaces don't need to be proxied.       
          return null;
      }
      else {//We're going to implement this method, so make it non abstract!
          return adapter.visitMethod(access, name, desc, null, arg4);
      }
  }

  /**
   * Generate the byte[] for our class
   * @return
   * @throws UnableToProxyException
   */
  final byte[] generateBytes() throws UnableToProxyException {
    if(!!!done) {
      for(Class<?> c : interfaces) {
        adapter.setCurrentMethodDeclaringType(Type.getType(c), true);
        try {
          AbstractWovenProxyAdapter.readClass(c, this);
        } catch (IOException e) {
          throw new UnableToProxyException(c, e);
        }
      }

      Class<?> clazz = superclass;

      while(clazz != null && (clazz.getModifiers() & Modifier.ABSTRACT) != 0) {
        adapter.setCurrentMethodDeclaringType(Type.getType(clazz), false);
        visitAbstractMethods(clazz);
        clazz = clazz.getSuperclass();
      }
      
      adapter.setCurrentMethodDeclaringType(AbstractWovenProxyAdapter.OBJECT_TYPE, false);
      visitObjectMethods();

      adapter.visitEnd();
      done  = true;
    }
    
    return writer.toByteArray();
  }

  private void visitAbstractMethods(Class<?> clazz) {
    for(java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
      int modifiers = m.getModifiers();
      if((modifiers & Modifier.ABSTRACT) != 0) {
        List<String> exceptions = new ArrayList<String>();
        for(Class<?> c : m.getExceptionTypes()) {
          exceptions.add(Type.getInternalName(c));
        }
        MethodVisitor visitor = visitMethod(modifiers, m.getName(), Method.getMethod(m).getDescriptor(), 
            null, exceptions.toArray(new String[exceptions.size()]));
        if (visitor != null) visitor.visitEnd();
      }
    }
  }


  /**
   * Make sure that the three common Object methods toString, equals and hashCode are redirected to the delegate
   * even if they are not on any of the interfaces
   */
  private void visitObjectMethods() {
      MethodVisitor visitor = visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "toString", "()Ljava/lang/String;", null, null);
      if (visitor != null) visitor.visitEnd();
      
      visitor = visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "equals", "(Ljava/lang/Object;)Z", null, null);
      if (visitor != null) visitor.visitEnd();

      visitor = visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "hashCode", "()I", null, null);
      if (visitor != null) visitor.visitEnd();     
  }
}