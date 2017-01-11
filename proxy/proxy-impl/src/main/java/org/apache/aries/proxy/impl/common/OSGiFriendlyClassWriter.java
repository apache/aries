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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.aries.proxy.UnableToProxyException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
/**
 * We need to override ASM's default behaviour in {@link #getCommonSuperClass(String, String)}
 * so that it doesn't load classes (which it was doing on the wrong {@link ClassLoader}
 * anyway...)
 */
public final class OSGiFriendlyClassWriter extends ClassWriter {

  private static final String OBJECT_INTERNAL_NAME = "java/lang/Object";
  private final ClassLoader loader;

  
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
   *
   * precondition: arg0 and arg1 are not equal. (checked before this method is called)
   */
  @Override
  protected final String getCommonSuperClass(String arg0, String arg1) {
    //If either is Object, then Object must be the answer
    if(arg0.equals(OBJECT_INTERNAL_NAME) || arg1.equals(OBJECT_INTERNAL_NAME)) {
      return OBJECT_INTERNAL_NAME;
    }
    Set<String> names = new HashSet<String>();
    names.add(arg0);
    names.add(arg1);
    //Try loading the class (in ASM not for real)
    try {
      boolean bRunning = true;
      boolean aRunning = true;
      InputStream is;
      String arg00 = arg0;
      String arg11 = arg1;
      String unable = null;
      while(aRunning || bRunning ) {
        if(aRunning) {
          is = loader.getResourceAsStream(arg00 + ".class");
          if(is != null) {
            ClassReader cr = new ClassReader(is);
            arg00 = cr.getSuperName();
            if(arg00 == null) {
              if (names.size() == 2) {
                return OBJECT_INTERNAL_NAME; //arg0 is an interface
              }
              aRunning = false; //old arg00 was java.lang.Object
            } else if(!!!names.add(arg00)) {
              return arg00;
            }
          } else {
            //The class file isn't visible on this ClassLoader
            unable = arg0;
            aRunning = false;
          }
        }
        if(bRunning) {
          is = loader.getResourceAsStream(arg11 + ".class");
          if(is != null) {
            ClassReader cr = new ClassReader(is);
            arg11 = cr.getSuperName();
            if(arg11 == null) {
              if (names.size() == 3) {
                return OBJECT_INTERNAL_NAME;  //arg1 is an interface
              }
              bRunning = false; //old arg11 was java.lang.Object
            } else if(!!!names.add(arg11)) {
              return arg11;
            }
          } else {
            unable = arg1;
            bRunning = false;
          }
        }
      }

      String msg = String.format("The class %s and %s do not have a common super class.", arg0, arg1);
      if (unable == null) {
          throw new RuntimeException(msg);
      } else {
          throw new RuntimeException(new UnableToProxyException(unable, msg));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
