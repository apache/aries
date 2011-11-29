

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
package org.apache.aries.versioning.utils;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Opcodes;

public abstract class GenericDeclaration
{

  private final int access;
  private final String name; 
  private final String signature;
  
  public GenericDeclaration(int access, String name, String signature) {
    int updatedAccess = access;
    // ignore the native or synchronized modifier as they do not affect binary compatibility
    if (Modifier.isNative(access)) {
      updatedAccess = updatedAccess - Opcodes.ACC_NATIVE;
    }
    if (Modifier.isSynchronized(access)) {
      updatedAccess = updatedAccess - Opcodes.ACC_SYNCHRONIZED;
    }
    this.access = access;
    this.name = name;
    this.signature = signature;
  }
  public int getAccess()
  {
    return access;
  }

  public String getName()
  {
    return name;
  }

 

  public String getSignature()
  {
    return signature;
  }
  
  public boolean isFinal() {
    return Modifier.isFinal(access);
  }
  
  public boolean isStatic() {
    return Modifier.isStatic(access);
  }
  
  public boolean isPublic() {
    return Modifier.isPublic(access);
  }
  
  public boolean isProtected() {
    return Modifier.isProtected(access);
  }
  public boolean isPrivate() {
    return Modifier.isPrivate(access);
  }
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + access;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    
    return result;
  }
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    GenericDeclaration other = (GenericDeclaration) obj;
    if (access != other.access) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    
    return true;
  }

}