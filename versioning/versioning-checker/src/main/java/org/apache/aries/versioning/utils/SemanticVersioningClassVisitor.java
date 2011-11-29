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
import java.net.URLClassLoader;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class SemanticVersioningClassVisitor implements ClassVisitor
{

  private ClassDeclaration classDeclaration;
  private boolean needVisit = false;
  private URLClassLoader loader = null;
  private SerialVersionClassVisitor cv = null;
  public SemanticVersioningClassVisitor(URLClassLoader newJarLoader, SerialVersionClassVisitor cv) {
    this.loader = newJarLoader;
    this.cv = cv;
  }

  public SemanticVersioningClassVisitor(URLClassLoader newJarLoader) {
    this.loader = newJarLoader;
  }

  public ClassDeclaration getClassDeclaration()
  {
    return classDeclaration;
  }
  /*
   * (non-Javadoc)
   * 
   * @see org.objectweb.asm.ClassAdapter#visit(int, int,
   * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  // visit the header of the class
  @Override
  public void  visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    // only interested in public class
    if (cv != null) {
      cv.visit(version, access, name, signature, superName, interfaces);
    }
    if (Modifier.isPublic(access) || (Modifier.isProtected(access))) {
      classDeclaration = new ClassDeclaration(access, name, signature, superName, interfaces, loader, cv);
      needVisit = true;

    } 
  }
  /*
   * (non-Javadoc)
   * 
   * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.Object)
   * 
   * Grab all protected or public fields
   */
  @Override
  public FieldVisitor visitField(int access, String name, String desc,
      String signature, Object value) {
    if (cv != null) {
      cv.visitField(access, name, desc, signature, value);
    }
    if (needVisit) {
      FieldDeclaration fd = new FieldDeclaration(access, name, desc, signature, value);
      classDeclaration.addFields(fd);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String[])
   * Get all non-private methods
   */
  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {


    if (cv != null) {
      cv.visitMethod(access, name, desc, signature, exceptions);
    }
    if (needVisit && (!SemanticVersioningUtils.CLINIT.equals(name))) {
      MethodDeclaration md = new MethodDeclaration(access, name, desc, signature, exceptions);
      classDeclaration.addMethods(md);
    }
    return null;
  }
  @Override
  public AnnotationVisitor visitAnnotation(String arg0, boolean arg1)
  {
    return null;
  }
  @Override
  public void visitAttribute(Attribute arg0)
  {
    // no-op    
  }

  @Override
  public void visitEnd()
  {
    //no-op

  }
  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access)
  {
    //no-op
    //The inner class will be scanned on its own. However, the method level class will be excluded, as they won't be public or protected.

  }
  @Override
  public void visitOuterClass(String owner, String name, String desc)
  {
    //no op

  }
  @Override
  public void visitSource(String arg0, String arg1)
  {
    //no-op

  }


}
