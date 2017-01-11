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

import static java.lang.String.format;

import java.util.Map;
import java.util.Set;

import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.UnableToProxyException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * This class is used to copy concrete methods from a super-class into a sub-class, 
 * but then delegate up to the super-class implementation. We expect to be called
 * with {@link ClassReader#SKIP_CODE}. This class is used when we can't weave
 * all the way up the Class hierarchy and need to override methods on the first
 * subclass we can weave.
 */
final class MethodCopyingClassAdapter extends ClassVisitor implements Opcodes {
  /** The super-class to copy from */
  private final Class<?> superToCopy;
  /** Is the sub-class in the same package as the super */
  private final boolean samePackage;
  /** The ASM {@link Type} of the sub-class */
  private final Type overridingClassType;
  /** 
   * The Set of {@link Method}s that exist in the sub-class. This set must be
   * live so modifications will be reflected in the parent and prevent clashes 
   */
  private final Set<Method> knownMethods;
  /**
   * The map of field names to methods being added
   */
  private final Map<String, TypeMethod> transformedMethods;
  
  private final AbstractWovenProxyAdapter wovenProxyAdapter;
  
  public MethodCopyingClassAdapter(AbstractWovenProxyAdapter awpa, ClassLoader definingLoader,
      Class<?> superToCopy, Type overridingClassType, Set<Method> knownMethods, 
      Map<String, TypeMethod> transformedMethods) {
    super(Opcodes.ASM5);
    this.wovenProxyAdapter = awpa;
    this.superToCopy = superToCopy;
    this.overridingClassType = overridingClassType;
    this.knownMethods = knownMethods;
    this.transformedMethods = transformedMethods;
    
    //To be in the same package they must be loaded by the same classloader and be in the same package!
    if(definingLoader != superToCopy.getClassLoader()) {
    	samePackage = false;
    } else {
    
      String overridingClassName = overridingClassType.getClassName();
      int lastIndex1 = superToCopy.getName().lastIndexOf('.');
      int lastIndex2 = overridingClassName.lastIndexOf('.');
      
      if(lastIndex1 != lastIndex2) {
        samePackage = false;
      } else if (lastIndex1 == -1) {
        samePackage = true;
      } else {
        samePackage = superToCopy.getName().substring(0, lastIndex1)
         .equals(overridingClassName.substring(0, lastIndex2));
      }
    }
  }
  
  @Override
  public final MethodVisitor visitMethod(final int access, String name, String desc,
      String sig, String[] exceptions) {
    
    MethodVisitor mv = null;
    //As in WovenProxyAdapter, we only care about "real" methods, but also not
    //abstract ones!.
    if (!!!name.equals("<init>") && !!!name.equals("<clinit>")
        && (access & (ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC | ACC_ABSTRACT
            | ACC_NATIVE | ACC_BRIDGE)) == 0) {

      // identify the target method parameters and return type
      Method currentTransformMethod = new Method(name, desc);
      // We don't want to duplicate a method we already overrode! 
      if(!!!knownMethods.add(currentTransformMethod))
        return null;
      
      // found a method we should weave
      // We can't override a final method
      if((access & ACC_FINAL) != 0)
        throw new RuntimeException(new FinalModifierException(
            superToCopy, name));
      // We can't call up to a default access method if we aren't in the same
      // package
      if((access & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) == 0) {
        if(!!!samePackage) {
            methodHiddenException(name);
        }
      }
      //Safe to copy a call to this method!
      Type superType = Type.getType(superToCopy);
      
      // identify the target method parameters and return type
      String methodStaticFieldName = "methodField" + AbstractWovenProxyAdapter.getSanitizedUUIDString();
      transformedMethods.put(methodStaticFieldName, new TypeMethod(
          superType, currentTransformMethod));  
      
      //Remember we need to copy the fake method *and* weave it, use a 
      //WovenProxyMethodAdapter as well as a CopyingMethodAdapter
      
      MethodVisitor weaver = wovenProxyAdapter.getWeavingMethodVisitor(
              access, name, desc, sig, exceptions, currentTransformMethod, 
              methodStaticFieldName, superType, false);
      
      if(weaver instanceof AbstractWovenProxyMethodAdapter) {
        //If we are weaving this method then we might have a problem. If it's a protected method and we
        //aren't in the same package then we can't dispatch the call to another object. This may sound
        //odd, but if class Super has a protected method foo(), then class Sub, that extends Super, cannot
        //call ((Super)o).foo() in code (it can call super.foo()). If we are in the same package then this
    	//gets around the problem, but if not the class will fail verification.
        if(!samePackage && (access & ACC_PROTECTED) != 0) {
            methodHiddenException(name);
        }
        mv = new CopyingMethodAdapter((GeneratorAdapter) weaver, superType, currentTransformMethod);
      }
      else {
        //For whatever reason we aren't weaving this method. The call to super.xxx() will always work
        mv = new CopyingMethodAdapter(new GeneratorAdapter(access, currentTransformMethod, mv), 
             superType, currentTransformMethod);
      }
    }
    
    return mv;
  }

private void methodHiddenException(String name) {
    String msg = format("The method %s in class %s cannot be called by %s because it is in a different package.",
                        name, superToCopy.getName(), overridingClassType.getClassName());
    throw new RuntimeException(msg,
                                 new UnableToProxyException(superToCopy));
}
  
  /**
   * This class is used to prevent any method body being copied, instead replacing
   * the body with a call to the super-types implementation. The original annotations
   * attributes etc are all copied.
   */
  private static final class CopyingMethodAdapter extends MethodVisitor {
    /** The visitor to delegate to */
    private final GeneratorAdapter mv;
    /** The type that declares this method (not the one that will override it) */
    private final Type superType;
    /** The method we are weaving */
    private final Method currentTransformMethod;
    
    public CopyingMethodAdapter(GeneratorAdapter mv, Type superType, 
        Method currentTransformMethod) {
      super(Opcodes.ASM5);
      this.mv = mv;
      this.superType = superType;
      this.currentTransformMethod = currentTransformMethod;
    }

    //TODO might not work for attributes
    @Override
    public final AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
      return mv.visitAnnotation(arg0, arg1);
    }

    @Override
    public final AnnotationVisitor visitAnnotationDefault() {
      return mv.visitAnnotationDefault();
    }

    @Override
    public final AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
        boolean arg2) {
      return mv.visitParameterAnnotation(arg0, arg1, arg2);
    }
    
    @Override
    public final void visitAttribute(Attribute attr) {
      mv.visitAttribute(attr);
    }

    /**
     * We skip code for speed when processing super-classes, this means we
     * need to manually drive some methods here!
     */
    @Override
    public final void visitEnd() {
      mv.visitCode();
      
      //Equivalent to return super.method(args);
      mv.loadThis();
	  mv.loadArgs();
	  mv.visitMethodInsn(INVOKESPECIAL, superType.getInternalName(),
	      currentTransformMethod.getName(), currentTransformMethod.getDescriptor());
	  mv.returnValue();

	  mv.visitMaxs(currentTransformMethod.getArgumentTypes().length + 1, 0);
      mv.visitEnd();
    }
  }
}