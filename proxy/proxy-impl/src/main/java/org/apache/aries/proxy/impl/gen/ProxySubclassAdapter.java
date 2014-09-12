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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;

import org.apache.aries.proxy.impl.NLS;
import org.apache.aries.proxy.impl.ProxyUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxySubclassAdapter extends ClassVisitor implements Opcodes
{

  private static final Type STRING_TYPE = Type.getType(String.class);
  private static final Type CLASS_TYPE = Type.getType(Class.class);
  private static final Type CLASSLOADER_TYPE = Type.getType(ClassLoader.class);
  private static final Type OBJECT_TYPE = Type.getType(Object.class);
  private static final Type METHOD_TYPE = Type.getType(java.lang.reflect.Method.class);
  private static final Type IH_TYPE = Type.getType(InvocationHandler.class);
  private static final Type[] NO_ARGS = new Type[] {};

  private static final String IH_FIELD = "ih";

  private static Logger LOGGER = LoggerFactory.getLogger(ProxySubclassAdapter.class);

  private String newClassName = null;
  private String superclassBinaryName = null;
  private Class<?> superclassClass = null;
  private ClassLoader loader = null;
  private Type newClassType = null;
  private GeneratorAdapter staticAdapter = null;
  private String currentlyAnalysedClassName = null;
  private Class<?> currentlyAnalysedClass = null;
  private String currentClassFieldName = null;

  public ProxySubclassAdapter(ClassVisitor writer, String newClassName, ClassLoader loader)
  {
    // call the superclass constructor
    super(Opcodes.ASM4, writer);
    // the writer is now the cv in the superclass of ClassAdapter

    LOGGER.debug(Constants.LOG_ENTRY, "ProxySubclassAdapter", new Object[] { this, writer,
        newClassName });

    // set the newClassName field
    this.newClassName = newClassName;
    // set the newClassType descriptor
    newClassType = Type.getType("L" + newClassName + ";");

    // set the classloader
    this.loader = loader;

    LOGGER.debug(Constants.LOG_EXIT, "ProxySubclassAdapter", this);
  }

  /*
   * This method visits the class to generate the new subclass.
   * 
   * The following things happen here: 1. The class is renamed to a dynamic
   * name 2. The existing class name is changed to be the superclass name so
   * that the generated class extends the original class. 3. A private field
   * is added to store an invocation handler 4. A constructor is added that
   * takes an invocation handler as an argument 5. The constructor method
   * instantiates an instance of the superclass 6. The constructor method sets
   * the invocation handler so the invoke method can be called from all the
   * subsequently rewritten methods 7. Add a getInvocationHandler() method 8.
   * store a static Class object of the superclass so we can reflectively find
   * methods later
   */
  public void visit(int version, int access, String name, String signature, String superName,
      String[] interfaces)
  {
    LOGGER.debug(Constants.LOG_ENTRY, "visit", new Object[] { version, access, name,
        signature, superName, interfaces });

    // store the superclass binary name
    this.superclassBinaryName = name.replaceAll("/", "\\.");

    try {
      this.superclassClass = Class.forName(superclassBinaryName, false, loader);
    } catch (ClassNotFoundException cnfe) {
      throw new TypeNotPresentException(superclassBinaryName, cnfe);
    }


    // keep the same access and signature as the superclass (unless it's abstract)
    // remove all the superclass interfaces because they will be inherited
    // from the superclass anyway
    if((access & ACC_ABSTRACT) != 0) {
      //If the super was abstract the subclass should not be!
      access &= ~ACC_ABSTRACT;
    }
    cv.visit(ProxyUtils.getWeavingJavaVersion(), access, newClassName, signature, name, null);

    // add a private field for the invocation handler
    // this isn't static in case we have multiple instances of the same
    // proxy
    cv.visitField(ACC_PRIVATE, IH_FIELD, Type.getDescriptor(InvocationHandler.class), null, null);

    // create a static adapter for generating a static initialiser method in
    // the generated subclass
    staticAdapter = new GeneratorAdapter(ACC_STATIC,
        new Method("<clinit>", Type.VOID_TYPE, NO_ARGS), null, null, cv);

    // add a zero args constructor method
    Method m = new Method("<init>", Type.VOID_TYPE, NO_ARGS);
    GeneratorAdapter methodAdapter = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cv);
    // loadthis
    methodAdapter.loadThis();
    // List the constructors in the superclass.
    Constructor<?>[] constructors = superclassClass.getDeclaredConstructors();
    // Check that we've got at least one constructor, and get the 1st one in the list.
    if (constructors.length > 0) {
      // We now need to construct the proxy class as though it is going to invoke the superclasses constructor.
      // We do this because we can no longer call the java.lang.Object() zero arg constructor as the JVM now throws a VerifyError.
      // So what we do is build up the calling of the superclasses constructor using nulls and default values. This means that the 
      // class bytes can be verified by the JVM, and then in the ProxySubclassGenerator, we load the class without invoking the 
      // constructor. 
      String constructorString = constructors[0].toGenericString();
      Method constructor = null;
      if (constructorString.indexOf(")") != -1) {
        //If constructor throws two or more exceptions, getMethod(String) will report a StringIndexOutOfBounds exception,
        //so attempt to remove exceptions
        constructor = Method.getMethod(constructorString.substring(0, constructorString.indexOf(")") + 1));
      } else {
	//As a backup, just pass in the generic string as before
        constructor = Method.getMethod(constructorString);
      }
      
      Type[] argTypes = constructor.getArgumentTypes();
      if (argTypes.length == 0) {
        methodAdapter.invokeConstructor(Type.getType(superclassClass), new Method("<init>", Type.VOID_TYPE, NO_ARGS));
      } else {
        for (Type type : argTypes) {
          switch (type.getSort())
          {
            case Type.ARRAY:
              // We need to process any array or multidimentional arrays.
              String elementDesc = type.getElementType().getDescriptor();
              String typeDesc = type.getDescriptor();
              
              // Iterate over the number of arrays and load 0 for each one. Keep a count of the number of 
              // arrays as we will need to run different code fo multi dimentional arrays.
              int index = 0;
              while (! elementDesc.equals(typeDesc)) {
                typeDesc = typeDesc.substring(1);
                methodAdapter.visitInsn(Opcodes.ICONST_0);
                index++;
              }
              // If we're just a single array, then call the newArray method, otherwise use the MultiANewArray instruction.
              if (index == 1) {
                methodAdapter.newArray(type.getElementType());
              } else {
                methodAdapter.visitMultiANewArrayInsn(type.getDescriptor(), index);
              }
              break;
            case Type.BOOLEAN:
              methodAdapter.push(true);
              break;
            case Type.BYTE:
              methodAdapter.push(Type.VOID_TYPE);
              break;
            case Type.CHAR:
              methodAdapter.push(Type.VOID_TYPE);
              break;
            case Type.DOUBLE:
              methodAdapter.push(0.0);
              break;
            case Type.FLOAT:
              methodAdapter.push(0.0f);
              break;
            case Type.INT:
              methodAdapter.push(0);
              break;
            case Type.LONG:
              methodAdapter.push(0l);
              break;
            case Type.SHORT:
              methodAdapter.push(0);
              break;
            default:
            case Type.OBJECT:
              methodAdapter.visitInsn(Opcodes.ACONST_NULL);
              break;
          }
        }
        
        methodAdapter.invokeConstructor(Type.getType(superclassClass), new Method("<init>", Type.VOID_TYPE, argTypes));
      }
    }
    methodAdapter.returnValue();
    methodAdapter.endMethod();

    // add a method for getting the invocation handler
    Method setter = new Method("setInvocationHandler", Type.VOID_TYPE, new Type[] { IH_TYPE });
    m = new Method("getInvocationHandler", IH_TYPE, NO_ARGS);
    methodAdapter = new GeneratorAdapter(ACC_PUBLIC | ACC_FINAL, m, null, null, cv);
    // load this to get the field
    methodAdapter.loadThis();
    // get the ih field and return
    methodAdapter.getField(newClassType, IH_FIELD, IH_TYPE);
    methodAdapter.returnValue();
    methodAdapter.endMethod();

    // add a method for setting the invocation handler
    methodAdapter = new GeneratorAdapter(ACC_PUBLIC | ACC_FINAL, setter, null, null, cv);
    // load this to put the field
    methodAdapter.loadThis();
    // load the method arguments (i.e. the invocation handler) to the stack
    methodAdapter.loadArgs();
    // set the ih field using the method argument
    methodAdapter.putField(newClassType, IH_FIELD, IH_TYPE);
    methodAdapter.returnValue();
    methodAdapter.endMethod();

    // loop through the class hierarchy to get any needed methods off the
    // supertypes
    // start by finding the methods declared on the class of interest (the
    // superclass of our dynamic subclass)
    java.lang.reflect.Method[] observedMethods = superclassClass.getDeclaredMethods();
    // add the methods to a set of observedMethods
    ProxySubclassMethodHashSet<String> setOfObservedMethods = new ProxySubclassMethodHashSet<String>(
        observedMethods.length);
    setOfObservedMethods.addMethodArray(observedMethods);
    // get the next superclass in the hierarchy
    Class<?> nextSuperClass = superclassClass.getSuperclass();
    while (nextSuperClass != null) {
      // set the fields for the current class
      setCurrentAnalysisClassFields(nextSuperClass);

      // add a static field and static initializer code to the generated
      // subclass
      // for each of the superclasses in the hierarchy
      addClassStaticField(currentlyAnalysedClassName);

      LOGGER.debug("Class currently being analysed: {} {}", currentlyAnalysedClassName,
          currentlyAnalysedClass);

      // now find the methods declared on the current class and add them
      // to a set of foundMethods
      java.lang.reflect.Method[] foundMethods = currentlyAnalysedClass.getDeclaredMethods();
      ProxySubclassMethodHashSet<String> setOfFoundMethods = new ProxySubclassMethodHashSet<String>(
          foundMethods.length);
      setOfFoundMethods.addMethodArray(foundMethods);
      // remove from the set of foundMethods any methods we saw on a
      // subclass
      // because we want to use the lowest level declaration of a method
      setOfFoundMethods.removeAll(setOfObservedMethods);
      try {
        // read the current class and use a
        // ProxySubclassHierarchyAdapter
        // to process only methods on that class that are in the list
        ClassLoader loader = currentlyAnalysedClass.getClassLoader();
        if (loader == null) {
          loader = this.loader;
        }
        ClassReader cr = new ClassReader(loader.getResourceAsStream(currentlyAnalysedClass
            .getName().replaceAll("\\.", "/")
            + ".class"));
        ClassVisitor hierarchyAdapter = new ProxySubclassHierarchyAdapter(this, setOfFoundMethods);
        cr.accept(hierarchyAdapter, ClassReader.SKIP_DEBUG);
      } catch (IOException e) {
        throw new TypeNotPresentException(currentlyAnalysedClassName, e);
      }
      // now add the foundMethods to the overall list of observed methods
      setOfObservedMethods.addAll(setOfFoundMethods);
      // get the next class up in the hierarchy and go again
      nextSuperClass = currentlyAnalysedClass.getSuperclass();
    }

    // we've finished looking at the superclass hierarchy
    // set the fields for the immediate superclass of our dynamic subclass
    setCurrentAnalysisClassFields(superclassClass);

    // add the class static field
    addClassStaticField(currentlyAnalysedClassName);
    // we do the lowest class last because we are already visiting the class
    // when in this adapter code
    // now we are ready to visit all the methods on the lowest class
    // which will happen by the ASM ClassVisitor implemented in this adapter

    LOGGER.debug(Constants.LOG_EXIT, "visit");
  }

  public void visitSource(String source, String debug)
  {
    LOGGER.debug(Constants.LOG_ENTRY, "visitSource", new Object[] { source, debug });

    // set the source to null since the class is generated on the fly and
    // not compiled
    cv.visitSource(null, null);

    LOGGER.debug(Constants.LOG_EXIT, "visitSource");
  }

  public void visitEnd()
  {
    LOGGER.debug(Constants.LOG_ENTRY, "visitEnd");

    // this method is called when we reach the end of the class
    // so it is time to make sure the static initialiser method is closed
    staticAdapter.returnValue();
    staticAdapter.endMethod();
    // now delegate to the cv
    cv.visitEnd();

    LOGGER.debug(Constants.LOG_EXIT, "visitEnd");
  }

  /*
   * This method is called on each method of the superclass (and all parent
   * classes up to Object) Each of these methods is visited in turn and the
   * code here generates the byte code for the InvocationHandler to call the
   * methods on the superclass.
   */
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exceptions)
  {
    LOGGER.debug(Constants.LOG_ENTRY, "visitMethod", new Object[] { access, name, desc,
        signature, exceptions });

    /*
     * Check the method access and handle the method types we don't want to
     * copy: final methods (issue warnings if these are not methods from
     * java.* classes) static methods (initialiser and others) private
     * methods, constructors (for now we don't copy any constructors)
     * everything else we process to proxy. Abstract methods should be made
     * non-abstract so that they can be proxied.
     */
    
    if((access & ACC_ABSTRACT) != 0) {
      //If the method is abstract then it should not be in the concrete subclass!
      access &= ~ACC_ABSTRACT;
    }
    
    LOGGER.debug("Method name: {} with descriptor: {}", name, desc);

    MethodVisitor methodVisitorToReturn = null;

    if (name.equals("<init>")) {
      // we may need to do something extra with constructors later
      // e.g. include bytecode for calling super with the same args
      // since we currently rely on the super having a zero args
      // constructor
      // we need to issue an error if we don't find one

      // for now we return null to ignore them
      methodVisitorToReturn = null;
    } else if (name.equals("<clinit>")) {
      // don't copy static initialisers from the superclass into the new
      // subclass
      methodVisitorToReturn = null;
    } else if ((access & ACC_FINAL) != 0) {
      // since we check for final methods in the ProxySubclassGenerator we
      // should never get here
      methodVisitorToReturn = null;
    } else if ((access & ACC_SYNTHETIC) != 0) {
      // synthetic methods are generated by the compiler for covariance
      // etc
      // we shouldn't copy them or we will have duplicate methods
      methodVisitorToReturn = null;
    } else if ((access & ACC_PRIVATE) != 0) {
      // don't copy private methods from the superclass
      methodVisitorToReturn = null;
    } else if ((access & ACC_STATIC) != 0) {
      // don't copy static methods
      methodVisitorToReturn = null;
    } else if (!(((access & ACC_PUBLIC) != 0) || ((access & ACC_PROTECTED) != 0) || ((access & ACC_PRIVATE) != 0))) {
      // the default (package) modifier value is 0, so by using & with any
      // of the other
      // modifier values and getting a result of zero means that we have
      // default accessibility

      // check the package in which the method is declared against the
      // package
      // where the generated subclass will be
      // if they are the same process the method otherwise ignore it
      if (currentlyAnalysedClass.getPackage().equals(superclassClass.getPackage())) {
        processMethod(access, name, desc, signature, exceptions);
        methodVisitorToReturn = null;
      } else {
        methodVisitorToReturn = null;
      }
    } else {
      processMethod(access, name, desc, signature, exceptions);
      // return null because we don't want the original method code from
      // the superclass
      methodVisitorToReturn = null;
    }

    LOGGER.debug(Constants.LOG_EXIT, "visitMethod", methodVisitorToReturn);

    return methodVisitorToReturn;

  }

  private void processMethod(int access, String name, String desc, String signature,
      String[] exceptions)
  {
    LOGGER.debug(Constants.LOG_ENTRY, "processMethod", new Object[] { access, name, desc,
        signature, exceptions });

    LOGGER.debug("Processing method: {} with descriptor {}", name, desc);

    // identify the target method parameters and return type
    Method currentTransformMethod = new Method(name, desc);
    Type[] targetMethodParameters = currentTransformMethod.getArgumentTypes();
    Type returnType = currentTransformMethod.getReturnType();

    // we create a static field for each method we encounter with a name
    // like method_parm1_parm2...
    StringBuilder methodStaticFieldNameBuilder = new StringBuilder(name);
    // for each a parameter get the name and add it to the field removing
    // the dots first
    for (Type t : targetMethodParameters) {
      methodStaticFieldNameBuilder.append("_");
      methodStaticFieldNameBuilder.append(t.getClassName().replaceAll("\\[\\]", "Array")
          .replaceAll("\\.", ""));
    }
    String methodStaticFieldName = methodStaticFieldNameBuilder.toString();

    // add a private static field for the method
    cv.visitField(ACC_PRIVATE | ACC_STATIC, methodStaticFieldName, METHOD_TYPE.getDescriptor(),
        null, null);

    // visit the method using the class writer, delegated through the method
    // visitor and generator
    // modify the method access so that any native methods aren't
    // described as native
    // since they won't be native in proxy form
    // also stop methods being marked synchronized on the proxy as they will
    // be sync
    // on the real object
    int newAccess = access & (~ACC_NATIVE) & (~ACC_SYNCHRONIZED);
    MethodVisitor mv = cv.visitMethod(newAccess, name, desc, signature, exceptions);
    // use a GeneratorAdapter to build the invoke call directly in byte code
    GeneratorAdapter methodAdapter = new GeneratorAdapter(mv, newAccess, name, desc);

    /*
     * Stage 1 creates the bytecode for adding the reflected method of the
     * superclass to a static field in the subclass: private static Method
     * methodName_parm1_parm2... = null; static{ methodName_parm1_parm2... =
     * superClass.getDeclaredMethod(methodName,new Class[]{method args}; }
     * 
     * Stage 2 is to call the ih.invoke(this,methodName_parm1_parm2,args) in
     * the new subclass methods Stage 3 is to cast the return value to the
     * correct type
     */

    /*
     * Stage 1 use superClass.getMethod(methodName,new Class[]{method args}
     * from the Class object on the stack
     */

    // load the static superclass Class onto the stack
    staticAdapter.getStatic(newClassType, currentClassFieldName, CLASS_TYPE);

    // push the method name string arg onto the stack
    staticAdapter.push(name);

    // create an array of the method parm class[] arg
    staticAdapter.push(targetMethodParameters.length);
    staticAdapter.newArray(CLASS_TYPE);
    int index = 0;
    for (Type t : targetMethodParameters) {
      staticAdapter.dup();
      staticAdapter.push(index);
      switch (t.getSort())
      {
        case Type.BOOLEAN:
          staticAdapter.getStatic(Type.getType(java.lang.Boolean.class), "TYPE", CLASS_TYPE);
          break;
        case Type.BYTE:
          staticAdapter.getStatic(Type.getType(java.lang.Byte.class), "TYPE", CLASS_TYPE);
          break;
        case Type.CHAR:
          staticAdapter.getStatic(Type.getType(java.lang.Character.class), "TYPE", CLASS_TYPE);
          break;
        case Type.DOUBLE:
          staticAdapter.getStatic(Type.getType(java.lang.Double.class), "TYPE", CLASS_TYPE);
          break;
        case Type.FLOAT:
          staticAdapter.getStatic(Type.getType(java.lang.Float.class), "TYPE", CLASS_TYPE);
          break;
        case Type.INT:
          staticAdapter.getStatic(Type.getType(java.lang.Integer.class), "TYPE", CLASS_TYPE);
          break;
        case Type.LONG:
          staticAdapter.getStatic(Type.getType(java.lang.Long.class), "TYPE", CLASS_TYPE);
          break;
        case Type.SHORT:
          staticAdapter.getStatic(Type.getType(java.lang.Short.class), "TYPE", CLASS_TYPE);
          break;
        default:
        case Type.OBJECT:
          staticAdapter.push(t);
          break;
      }
      staticAdapter.arrayStore(CLASS_TYPE);
      index++;
    }

    // invoke the getMethod
    staticAdapter.invokeVirtual(CLASS_TYPE, new Method("getDeclaredMethod", METHOD_TYPE,
        new Type[] { STRING_TYPE, Type.getType(java.lang.Class[].class) }));

    // store the reflected method in the static field
    staticAdapter.putStatic(newClassType, methodStaticFieldName, METHOD_TYPE);

    /*
     * Stage 2 call the ih.invoke(this,supermethod,parms)
     */

    // load this to get the ih field
    methodAdapter.loadThis();
    // load the invocation handler from the field (the location of the
    // InvocationHandler.invoke)
    methodAdapter.getField(newClassType, IH_FIELD, IH_TYPE);
    // loadThis (the first arg of the InvocationHandler.invoke)
    methodAdapter.loadThis();
    // load the method to invoke (the second arg of the
    // InvocationHandler.invoke)
    methodAdapter.getStatic(newClassType, methodStaticFieldName, METHOD_TYPE);
    // load all the method arguments onto the stack as an object array (the
    // third arg of the InvocationHandler.invoke)
    methodAdapter.loadArgArray();
    // generate the invoke method
    Method invocationHandlerInvokeMethod = new Method("invoke", OBJECT_TYPE, new Type[] {
        OBJECT_TYPE, METHOD_TYPE, Type.getType(java.lang.Object[].class) });
    // call the invoke method of the invocation handler
    methodAdapter.invokeInterface(IH_TYPE, invocationHandlerInvokeMethod);

    /*
     * Stage 3 the returned object is now on the top of the stack We need to
     * check the type and cast as necessary
     */
    switch (returnType.getSort())
    {
      case Type.BOOLEAN:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Boolean.class));
        methodAdapter.unbox(Type.BOOLEAN_TYPE);
        break;
      case Type.BYTE:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Byte.class));
        methodAdapter.unbox(Type.BYTE_TYPE);
        break;
      case Type.CHAR:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Character.class));
        methodAdapter.unbox(Type.CHAR_TYPE);
        break;
      case Type.DOUBLE:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Double.class));
        methodAdapter.unbox(Type.DOUBLE_TYPE);
        break;
      case Type.FLOAT:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Float.class));
        methodAdapter.unbox(Type.FLOAT_TYPE);
        break;
      case Type.INT:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Integer.class));
        methodAdapter.unbox(Type.INT_TYPE);
        break;
      case Type.LONG:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Long.class));
        methodAdapter.unbox(Type.LONG_TYPE);
        break;
      case Type.SHORT:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Short.class));
        methodAdapter.unbox(Type.SHORT_TYPE);
        break;
      case Type.VOID:
        methodAdapter.cast(OBJECT_TYPE, Type.getType(Void.class));
        methodAdapter.unbox(Type.VOID_TYPE);
        break;
      default:
      case Type.OBJECT:
        // in this case check the cast and cast the object to the return
        // type
        methodAdapter.checkCast(returnType);
        methodAdapter.cast(OBJECT_TYPE, returnType);
        break;
    }
    // return the (appropriately cast) result of the invocation from the
    // stack
    methodAdapter.returnValue();
    // end the method
    methodAdapter.endMethod();

    LOGGER.debug(Constants.LOG_EXIT, "processMethod");
  }

  private void addClassStaticField(String classBinaryName)
  {
    LOGGER.debug(Constants.LOG_ENTRY, "addClassStaticField",
        new Object[] { classBinaryName });

    currentClassFieldName = classBinaryName.replaceAll("\\.", "_");

    /*
     * use Class.forName on the superclass so we can reflectively find
     * methods later
     * 
     * produces bytecode for retrieving the superclass and storing in a
     * private static field: private static Class superClass = null; static{
     * superClass = Class.forName(superclass, true, TYPE_BEING_PROXIED.class.getClassLoader()); }
     */

    // add a private static field for the superclass Class
    cv.visitField(ACC_PRIVATE | ACC_STATIC, currentClassFieldName, CLASS_TYPE.getDescriptor(),
        null, null);

    // push the String arg for the Class.forName onto the stack
    staticAdapter.push(classBinaryName);
    //push the boolean arg for the Class.forName onto the stack
    staticAdapter.push(true);
    //get the classloader
    staticAdapter.push(newClassType);
    staticAdapter.invokeVirtual(CLASS_TYPE, new Method("getClassLoader", CLASSLOADER_TYPE, NO_ARGS));

    // invoke the Class forName putting the Class on the stack
    staticAdapter.invokeStatic(CLASS_TYPE, new Method("forName", CLASS_TYPE,
        new Type[] { STRING_TYPE, Type.BOOLEAN_TYPE, CLASSLOADER_TYPE }));

    // put the Class in the static field
    staticAdapter.putStatic(newClassType, currentClassFieldName, CLASS_TYPE);

    LOGGER.debug(Constants.LOG_ENTRY, "addClassStaticField");
  }

  private void setCurrentAnalysisClassFields(Class<?> aClass)
  {
    LOGGER.debug(Constants.LOG_ENTRY, "setCurrentAnalysisClassFields",
        new Object[] { aClass });

    currentlyAnalysedClassName = aClass.getName();
    currentlyAnalysedClass = aClass;

    LOGGER.debug(Constants.LOG_EXIT, "setCurrentAnalysisClassFields");
  }

  // we don't want to copy fields from the class into the proxy
  public FieldVisitor visitField(int access, String name, String desc, String signature,
      Object value)
  {
    return null;
  }

  // for now we don't do any processing in these methods
  public AnnotationVisitor visitAnnotation(String desc, boolean visible)
  {
    return null;
  }

  public void visitAttribute(Attribute attr)
  {
    // no-op
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access)
  {
    // no-op
  }

  public void visitOuterClass(String owner, String name, String desc)
  {
    // no-op
  }
}
