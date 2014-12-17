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
package org.apache.aries.proxy.impl.gen;

import java.util.Collection;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Although we implement ClassVisitor we are only interested in the methods of
 * the superclasses in the hierarchy.  For this reason although visitMethod is 
 * implemented the other methods of ClassVisitor are currently no-op.
 *
 *
 */
public class ProxySubclassHierarchyAdapter extends ClassVisitor implements Opcodes
{

  private ProxySubclassAdapter adapter = null;
  private Collection<String> methodsToImplement = null;

  private static Logger LOGGER = LoggerFactory.getLogger(ProxySubclassHierarchyAdapter.class);

  ProxySubclassHierarchyAdapter(ProxySubclassAdapter adapter, Collection<String> methodsToImplement)
  {
    super(Opcodes.ASM5);
    LOGGER.debug(Constants.LOG_ENTRY, "ProxySubclassHeirarchyAdapter", new Object[] {
        this, adapter, methodsToImplement });

    this.methodsToImplement = methodsToImplement;
    this.adapter = adapter;

    LOGGER.debug(Constants.LOG_EXIT, "ProxySubclassHeirarchyAdapter", this);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exceptions)
  {
    LOGGER.debug(Constants.LOG_ENTRY, "visitMethod", new Object[] { access, name, desc,
        signature, exceptions });

    // if the method we find in the superclass is one that is available on
    // the class
    // we are dynamically subclassing then we need to implement an
    // invocation for it
    String argDesc = ProxySubclassMethodHashSet.typeArrayToStringArgDescriptor(Type
        .getArgumentTypes(desc));
    if (methodsToImplement.contains(name + argDesc)) {
      // create the method in bytecode
      adapter.visitMethod(access, name, desc, signature, exceptions);
    }

    LOGGER.debug(Constants.LOG_EXIT, "visitMethod");

    // always return null because we don't want to copy any method code
    return null;
  }

  public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5)
  {
    // no-op
  }

  public AnnotationVisitor visitAnnotation(String arg0, boolean arg1)
  {
    // don't process any annotations at this stage
    return null;
  }

  public void visitAttribute(Attribute arg0)
  {
    // no-op
  }

  public void visitEnd()
  {
    // no-op
  }

  public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4)
  {
    // don't process fields
    return null;
  }

  public void visitInnerClass(String arg0, String arg1, String arg2, int arg3)
  {
    // no-op
  }

  public void visitOuterClass(String arg0, String arg1, String arg2)
  {
    // no-op
  }

  public void visitSource(String arg0, String arg1)
  {
    // no-op
  }

}
