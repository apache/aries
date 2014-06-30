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

import java.lang.reflect.Modifier;

import static org.objectweb.asm.Opcodes.ASM4;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

class SyntheticSerialVerUIDAdder extends SerialVersionUIDAdder {

  private WovenProxyAdapter wpa;

  //copied from superclass, they are now private
  /**
   * Flag that indicates if we need to compute SVUID.
   */
  private boolean computeSVUID;

  /**
   * Set to true if the class already has SVUID.
   */
  private boolean hasSVUID;

  public SyntheticSerialVerUIDAdder(WovenProxyAdapter cv) {
    super(ASM4, cv);
    wpa = cv;
  }

  // The following visit and visitField methods are workaround since ASM4 does not supply the javadoced method isHasSVUID() by mistake. 
  // When the method isHasSVUId() or similar methods available, we can remove the following two methods.

  @Override
  public void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    computeSVUID = (access & Opcodes.ACC_INTERFACE) == 0;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public FieldVisitor visitField(
      final int access,
      final String name,
      final String desc,
      final String signature,
      final Object value) {
    if (computeSVUID) {
      if ("serialVersionUID".equals(name) && Modifier.isFinal(access) && Modifier.isStatic(access) && Type.LONG_TYPE.equals(Type.getType(desc))) {
        // since the class already has SVUID, we won't be computing it.
        computeSVUID = false;
        hasSVUID = true;
      }
    }

    return super.visitField(access, name, desc, signature, value);
  }

  @Override
  public void visitEnd() {

    wpa.setSVUIDGenerated(!!!hasSVUID);
    super.visitEnd();
  }
}
