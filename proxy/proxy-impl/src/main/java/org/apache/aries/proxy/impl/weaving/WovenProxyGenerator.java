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

import static org.objectweb.asm.Opcodes.ACC_ANNOTATION;
import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * This class is used to weave the bytes of a class into a proxyable class
 */
public final class WovenProxyGenerator
{
  public static final byte[] getWovenProxy(byte[] original, String className, ClassLoader loader){
    ClassReader cReader = new ClassReader(original);
    //Don't weave interfaces, enums or annotations
    if((cReader.getAccess() & (ACC_INTERFACE | ACC_ANNOTATION | ACC_ENUM)) != 0)
      return null;
    
    //We need to know the class version, but ASM won't tell us yet!
    int version = ((0xFF & original[6]) << 8) + (0xFF & original[7]);
    
    //If we are Java 1.6 + compiled then we need to compute stack frames, otherwise
    //maxs are fine (and faster)
    ClassWriter cWriter = new ClassWriter(cReader, (version > Opcodes.V1_5) ?
        ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS);
    ClassVisitor weavingAdapter = new WovenProxyAdapter(cWriter, className, loader);
    // If we are Java 1.6 + then we need to skip frames as they will be recomputed
    cReader.accept(weavingAdapter, (version > Opcodes.V1_5) ? ClassReader.SKIP_FRAMES : 0);
    
    return cWriter.toByteArray();
  }
}