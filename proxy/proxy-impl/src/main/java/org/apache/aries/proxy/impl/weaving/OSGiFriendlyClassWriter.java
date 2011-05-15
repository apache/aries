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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.aries.proxy.impl.NLS;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
/**
 * We need to override ASM's default behaviour in {@link #getCommonSuperClass(String, String)}
 * so that it doesn't load classes (which it was doing on the wrong {@link ClassLoader}
 * anyway...
 */
final class OSGiFriendlyClassWriter extends ClassWriter {

  private static final String OBJECT_INTERNAL_NAME = "java/lang/Object";
  private final ClassLoader loader;
  private String currentClassInternalName;
  private String currentSuperClassInternalName;
  
  public OSGiFriendlyClassWriter(ClassReader arg0, int arg1, ClassLoader loader) {
    super(arg0, arg1);
    this.loader = loader;
  }
  
  public OSGiFriendlyClassWriter(int arg0, ClassLoader loader) {
    super(arg0);
    this.loader = loader;
  }

  /**
   * We provide an implementation that doesn't cause class loads to occur. It may
   * not be sufficient because it expects to find the common parent using a single
   * classloader, though in fact the common parent may only be loadable by another
   * bundle from which an intermediate class is loaded
   */
  @Override
  protected final String getCommonSuperClass(String arg0, String arg1) {
    //If the two are equal then return either
    if(arg0.equals(arg1))
      return arg0;
    
    //If either is Object, then Object must be the answer
    if(arg0.equals(OBJECT_INTERNAL_NAME) || arg1.equals(OBJECT_INTERNAL_NAME))
      return OBJECT_INTERNAL_NAME;
    
    //We can't load the class being woven, so call again passing in the supertype
    if(arg0.equals(currentClassInternalName))
      getCommonSuperClass(currentSuperClassInternalName, arg1);
    else if (arg1.equals(currentClassInternalName))
      getCommonSuperClass(arg0, currentSuperClassInternalName);
    
    Set<String> names = new HashSet<String>();
    names.add(arg0);
    names.add(arg1);
    //Try loading the class (in ASM)
    try {
      
      boolean bRunning = true;
      boolean aRunning = true;
      InputStream is;
      
      while(aRunning || bRunning ) {
        if(aRunning) {
          is = loader.getResourceAsStream(arg0 + ".class");
          if(is != null) {
            ClassReader cr = new ClassReader(is);
            arg0 = cr.getSuperName();
            if(arg0 == null)
              aRunning = false;
            else if(!!!names.add(arg0))
              return arg0;
          } else {
            aRunning = false;
          }
        }
        if(bRunning) {
          is = loader.getResourceAsStream(arg1 + ".class");
          if(is != null) {
            ClassReader cr = new ClassReader(is);
            arg1 = cr.getSuperName();
            if(arg1 == null)
              bRunning = false;
            else if(!!!names.add(arg1))
              return arg1;
          } else {
            bRunning = false;
          }
        }
      }
      
      throw new RuntimeException(NLS.MESSAGES.getMessage("no.common.superclass", arg0, arg1));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * We need access to the super's name and our class name
   */
  @Override
  public final void visit(int arg0, int arg1, String arg2, String arg3, String arg4,
      String[] arg5) {
    currentClassInternalName = arg2;
    currentSuperClassInternalName = arg4;
    super.visit(arg0, arg1, arg2, arg3, arg4, arg5);
  }

}
