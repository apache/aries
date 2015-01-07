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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.web.converter.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class PackageFinder extends ClassVisitor//AnnotationVisitor, SignatureVisitor, ClassVisitor,
    //FieldVisitor, MethodVisitor
{
  private static int asmVersion = Opcodes.ASM4;
  private PackageFinderSignatureVisitor pfsv;
  private PackageFinderAnnotationVisitor pfav;
  private PackageFinderFieldVisitor pffv;
  private PackageFinderMethodVisitor pfmv;
  
  public PackageFinder()
  {
    super(asmVersion);
    this.pfsv = new PackageFinderSignatureVisitor();
    this.pfav = new PackageFinderAnnotationVisitor();
    this.pffv = new PackageFinderFieldVisitor();
    this.pfmv = new PackageFinderMethodVisitor();
  }

  private Set<String> packages = new HashSet<String>();
  private Set<String> exemptPackages = new HashSet<String>();

  // stored value of the signature class name
  private String signatureOuterClass = null;

  public Set<String> getImportPackages()
  {
    // Remove entries that will be imported by default
    for (Iterator<String> i = packages.iterator(); i.hasNext();) {
      if (i.next().startsWith("java.")) i.remove();
    }
    
    return packages;
  }
  
  public Set<String> getExemptPackages()
  {
    return exemptPackages;
  }

  private String getPackageName(String name)
  {
    String packageName = null;
    if (name != null) {
      int index = name.lastIndexOf('/');
      if (index > 0) packageName = name.substring(0, index);
    }
    return packageName;
  }

  private String canonizePackage(String rawPackage)
  {
    String result = rawPackage.replace('/', '.');

    // handle arrays
    return result.replaceFirst("^\\[+L", "");
  }
  
  private void addPackage(String packageName)
  {
    if (packageName != null) {
      packages.add(canonizePackage(packageName));
    }
  }
  
  private void addExemptPackage(String packageName)
  {
    if (packageName != null) 
      exemptPackages.add(canonizePackage(packageName));
  }

  private void addPackages(String[] packageNames)
  {
    if (packageNames != null) {
      for (String s : packageNames)
        if (s != null) {
          packages.add(canonizePackage(s));
        }
    }
  }

  private String getResolvedPackageName(String name)
  {
    String resolvedName = null;
    if (name != null) resolvedName = getPackageName(name);
    return resolvedName;
  }

  private String[] getResolvedPackageNames(String[] names)
  {
    String[] resolvedNames = null;
    if (names != null) {
      resolvedNames = new String[names.length];
      int i = 0;
      for (String s : names)
        resolvedNames[i++] = getResolvedPackageName(s);
    }
    return resolvedNames;
  }

  private String getDescriptorInfo(String descriptor)
  {
    String type = null;
    if (descriptor != null) type = getType(Type.getType(descriptor));
    return type;
  }

  private String[] getMethodDescriptorInfo(String descriptor)
  {
    String[] descriptors = null;
    if (descriptor != null) {
      Type[] types = Type.getArgumentTypes(descriptor);
      descriptors = new String[types.length + 1];
      descriptors[0] = getType(Type.getReturnType(descriptor));
      int i = 1;
      for (Type t : types)
        descriptors[i++] = getType(t);
    }
    return descriptors;
  }

  private String getType(Type t)
  {
    String type = null;
    switch (t.getSort())
    {
      case Type.ARRAY:
        type = getType(t.getElementType());
        break;
      case Type.OBJECT:
        type = getPackageName(t.getInternalName());
        break;
    }
    return type;
  }

  private void addSignaturePackages(String signature)
  {
    if (signature != null) new SignatureReader(signature).accept(pfsv);
  }

  private void addResolvedSignaturePackages(String signature)
  {
    if (signature != null) new SignatureReader(signature).acceptType(pfsv);
  }

  //
  // ClassVisitor methods
  //

  public void visit(int arg0, int arg1, String name, String signature, String parent,
      String[] interfaces)
  {
    // We dont want to import our own packages so we add this classes package name to the
    // list of exempt packages.
    addExemptPackage(getPackageName(name));

    if (signature == null) {
      addPackage(getResolvedPackageName(parent));
      addPackages(getResolvedPackageNames(interfaces));
    } else addSignaturePackages(signature);
  }

  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
  {
    addPackage(getDescriptorInfo(descriptor));
    return pfav;
  }

  public void visitAttribute(Attribute arg0)
  {
    // No-op
  }

  public FieldVisitor visitField(int access, String name, String descriptor, String signature,
      Object value)
  {
    if (signature == null) addPackage(getDescriptorInfo(descriptor));
    else addResolvedSignaturePackages(signature);

    if (value instanceof Type) addPackage(getType((Type) value));
    return pffv;
  }

  public void visitInnerClass(String arg0, String arg1, String arg2, int arg3)
  {
    // no-op
  }

  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
      String[] exceptions)
  {
    if (signature == null) addPackages(getMethodDescriptorInfo(descriptor));
    else addSignaturePackages(signature);

    addPackages(getResolvedPackageNames(exceptions));
    return pfmv;

  }

  public void visitOuterClass(String arg0, String arg1, String arg2)
  {
    // no-op
  }

  public void visitSource(String arg0, String arg1)
  {
    // no-op
  }

  public void visitEnd()
  {
    // no-op
  }







  public class PackageFinderSignatureVisitor extends SignatureVisitor {

    public PackageFinderSignatureVisitor()
    {
      super(asmVersion);
    }
    
    // 
    // SignatureVisitor methods
    //

    public SignatureVisitor visitArrayType()
    {
      return pfsv;
    }

    public void visitBaseType(char arg0)
    {
      // no-op
    }

    public SignatureVisitor visitClassBound()
    {
      return pfsv;
    }

    public void visitClassType(String name)
    {
      signatureOuterClass = name;
      addPackage(getResolvedPackageName(name));
    }

    public void visitInnerClassType(String name)
    {
      addPackage(getResolvedPackageName(signatureOuterClass + "$" + name));
    }

    public SignatureVisitor visitExceptionType()
    {
      return pfsv;
    }

    public void visitFormalTypeParameter(String arg0)
    {
      // no-op
    }

    public SignatureVisitor visitInterface()
    {
      return pfsv;
    }

    public SignatureVisitor visitParameterType()
    {
      return pfsv;
    }

    public SignatureVisitor visitReturnType()
    {
      return pfsv;
    }

    public SignatureVisitor visitSuperclass()
    {
      return pfsv;
    }

    public void visitTypeArgument()
    {
      // no-op
    }

    public SignatureVisitor visitTypeArgument(char arg0)
    {
      return pfsv;
    }

    public void visitTypeVariable(String arg0)
    {
      // no-op
    }

    public SignatureVisitor visitInterfaceBound()
    {
      return pfsv;
    }
  }
  
  
  
  
  public class PackageFinderAnnotationVisitor extends AnnotationVisitor {

    public PackageFinderAnnotationVisitor()
    {
      super(asmVersion);
    }
    
    //
    // AnnotationVisitor Methods
    //

    public void visit(String arg0, Object value)
    {
      if (value instanceof Type) {
        addPackage(getType((Type) value));
      }
    }

    public AnnotationVisitor visitAnnotation(String arg0, String descriptor)
    {
      addPackage(getDescriptorInfo(descriptor));
      return pfav;
    }

    public AnnotationVisitor visitArray(String arg0)
    {
      return pfav;
    }

    public void visitEnum(String name, String desc, String value)
    {
      addPackage(getDescriptorInfo(desc));
    }
  }
  
  
  
  
  public class PackageFinderFieldVisitor extends FieldVisitor {

    public PackageFinderFieldVisitor()
    {
      super(asmVersion);
    }
  }
  
  
  
  
  public class PackageFinderMethodVisitor extends MethodVisitor {

    public PackageFinderMethodVisitor()
    {
      super(asmVersion);
    }
    
    // 
    // MethodVisitor methods
    //

    public AnnotationVisitor visitAnnotationDefault()
    {
      return pfav;
    }

    public void visitCode()
    {
      // no-op
    }

    public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4)
    {
      // no-op
    }

    public void visitIincInsn(int arg0, int arg1)
    {
      // no-op
    }

    public void visitInsn(int arg0)
    {
      // no-op
    }

    public void visitIntInsn(int arg0, int arg1)
    {
      // no-op
    }

    public void visitJumpInsn(int arg0, Label arg1)
    {
      // no-op
    }

    public void visitLabel(Label arg0)
    {
      // no-op
    }

    public void visitLdcInsn(Object type)
    {
      if (type instanceof Type) addPackage(getType((Type) type));
    }

    public void visitLineNumber(int arg0, Label arg1)
    {
      // no-op
    }

    public void visitLocalVariable(String name, String descriptor, String signature, Label start,
        Label end, int index)
    {
      addResolvedSignaturePackages(signature);
    }

    public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2)
    {
      // no-op
    }

    public void visitMaxs(int arg0, int arg1)
    {
      // no-op
    }

    public void visitMethodInsn(int opcode, String owner, String name, String descriptor)
    {
      addPackage(getResolvedPackageName(owner));
      addPackages(getMethodDescriptorInfo(descriptor));
    }

    public void visitMultiANewArrayInsn(String descriptor, int arg1)
    {
      addPackage(getDescriptorInfo(descriptor));
    }

    public AnnotationVisitor visitParameterAnnotation(int arg0, String descriptor, boolean arg2)
    {
      addPackage(getDescriptorInfo(descriptor));
      return pfav;
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels)
    {
      //no-op
    }

    public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String type)
    {
      addPackage(getResolvedPackageName(type));
    }

    public void visitTypeInsn(int arg0, String type)
    {
      addPackage(getResolvedPackageName(type));
    }

    public void visitVarInsn(int arg0, int arg1)
    {
      // no-op
    }
    
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor)
    {
      addPackage(getResolvedPackageName(owner));
      addPackage(getDescriptorInfo(descriptor));
    }
  }
}
