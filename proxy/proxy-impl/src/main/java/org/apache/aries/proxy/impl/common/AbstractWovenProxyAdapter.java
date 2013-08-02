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

import static org.apache.aries.proxy.impl.ProxyUtils.JAVA_CLASS_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.NLS;
import org.apache.aries.proxy.impl.gen.Constants;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This abstract superclass is responsible for providing proxy extensions to 
 * classes being written. Classes processed by this adapter will implement 
 * {@link WovenProxy}, and have a static initialiser that populates 
 * {@link java.lang.reflect.Method} fields for use with the 
 * {@link InvocationListener}. Known subclasses are WovenProxyAdapter, 
 * used to weave classes being loaded by the framework, and InterfaceCombiningClassAdapter
 * which is used to dynamically create objects that implement multiple interfaces
 */
public abstract class AbstractWovenProxyAdapter extends ClassVisitor implements Opcodes {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractWovenProxyAdapter.class);

  /** Access modifier for a public generated method */
  private static final int PUBLIC_GENERATED_METHOD_ACCESS = ACC_PUBLIC | ACC_FINAL
      | ACC_SYNTHETIC;
  /** The internal name for Throwable */
  static final String THROWABLE_INAME = Type.getInternalName(Throwable.class);
  /** 
   * A static UUID for adding to our method names. 
   * This must not change over time, otherwise uninstalling
   * and reinstalling the proxy component with a separate
   * API bundle will cause BIG trouble (NoSuchFieldError)
   * with subclasses that get woven by the "new" hook
   */
  private static final String UU_ID = "04df3c80_2877_4f6c_99e2_5a25e11d5535";
  /** A constant for No Args methods */
  static final Type[] NO_ARGS = new Type[0];

  /** The annotation types we should add to generated methods and fields */
  private static final String[] annotationTypeDescriptors = new String[] { "Ljavax/persistence/Transient;" };

  /** the name of the field used to store the {@link InvocationListener} */
  protected static final String LISTENER_FIELD = "org_apache_aries_proxy_InvocationListener_"
      + UU_ID;
  /** the name of the field used to store the dispatcher */
  public static final String DISPATCHER_FIELD = "woven_proxy_dispatcher_" + UU_ID;

  /* Useful ASM types */
  /** The ASM type for the {@link InvocationListener} */
  static final Type LISTENER_TYPE = Type.getType(InvocationListener.class);
  /** The ASM type for the dispatcher */
  public static final Type DISPATCHER_TYPE = Type.getType(Callable.class);
  private static final Type CLASS_TYPE = Type.getType(Class.class);
  private static final Type CLASS_ARRAY_TYPE = Type.getType(Class[].class);
  private static final Type STRING_TYPE = Type.getType(String.class);
  public static final Type OBJECT_TYPE = Type.getType(Object.class);
  static final Type METHOD_TYPE = Type.getType(java.lang.reflect.Method.class);
  /** The {@link Type} of the {@link WovenProxy} interface */
  static final Type WOVEN_PROXY_IFACE_TYPE = Type.getType(WovenProxy.class);
  private static final Type NPE_TYPE = Type.getType(NullPointerException.class);
  
  private static final Type[] DISPATCHER_LISTENER_METHOD_ARGS = new Type[] {
    DISPATCHER_TYPE, LISTENER_TYPE };

  private static final Method ARGS_CONSTRUCTOR = new Method("<init>", Type.VOID_TYPE,
      DISPATCHER_LISTENER_METHOD_ARGS);
  private static final Method NO_ARGS_CONSTRUCTOR = new Method("<init>", Type.VOID_TYPE,
      NO_ARGS);
  private static final Method NPE_CONSTRUCTOR = new Method("<init>", Type.VOID_TYPE,
      new Type[] {STRING_TYPE});

  // other new methods we will need
  static final Method getInovcationTargetMethod = new Method(
      "getInvocationTarget" + UU_ID, OBJECT_TYPE, NO_ARGS);
  static final Method listenerPreInvokeMethod = new Method("getListener"
      + UU_ID, OBJECT_TYPE, new Type[] { OBJECT_TYPE,
      Type.getType(java.lang.reflect.Method.class),
      Type.getType(Object[].class) });

  /* Instance fields */

  /** The type of this class */
  protected final Type typeBeingWoven;
  /** The type of this class's super */
  private Type superType;
  /** The {@link ClassLoader} loading this class */
  private final ClassLoader loader;
  /**
   * A flag to indicate that we need to weave WovenProxy methods into this class
   */
  private boolean implementWovenProxy = false;
  /** 
   * A list of un-woven superclasses between this object and {@link Object}, 
   * only populated for classes which will directly implement {@link WovenProxy}.
   * This list is then used to override any methods that would otherwise be missed
   * by the weaving process. 
   */
  protected final List<Class<?>> nonObjectSupers = new ArrayList<Class<?>>();
  
  /**
   * Methods we have transformed and need to create static fields for.
   * Stored as field name to {@link TypeMethod} so we know which Class to reflect
   * them off
   */
  protected final Map<String, TypeMethod> transformedMethods = new HashMap<String, TypeMethod>();
  
  /**
   *  A set of {@link Method} objects identifying the methods that are in this 
   *  class. This is used to prevent us duplicating methods copied from 
   *  {@link AbstractWovenProxyAdapter#nonObjectSupers} that are already overridden in 
   *  this class.
   */
  private final Set<Method> knownMethods = new HashSet<Method>();
  /** 
   * If our super does not have a no-args constructor then we need to be clever
   * when writing our own constructor.
   */
  private boolean superHasNoArgsConstructor = false;
  /**
   * If we have a no-args constructor then we can delegate there rather than 
   * to a super no-args
   */
  private boolean hasNoArgsConstructor = false;
  /**
   * If we have a no-args constructor then we can delegate there rather than 
   * to a super no-args
   */
  protected boolean isSerializable = false;
  /**
   * The default static initialization method where we will write the proxy init
   * code. If there is an existing <clinit> then we will change this and write a
   * static_init_UUID instead (see the overriden 
   * {@link #visitMethod(int, String, String, String, String[])}
   * for where this swap happens). See also {@link #writeStaticInitMethod()} for
   * where the method is actually written.
   */
  private Method staticInitMethod = new Method("<clinit>", Type.VOID_TYPE, NO_ARGS);
  /**
   * The default access flags for the staticInitMethod. If we find an existing
   * <clinit> then we will write a static_init_UUID method and add the ACC_PRIVATE_FLAG.
   * See the overriden {@link #visitMethod(int, String, String, String, String[])}
   * for where this flag is added. See also {@link #writeStaticInitMethod()} for
   * where the method is actually written.
   */
  private int staticInitMethodFlags = ACC_SYNTHETIC | ACC_PRIVATE | ACC_STATIC;

  protected Type currentMethodDeclaringType;

  protected boolean currentMethodDeclaringTypeIsInterface;
  
 
  public static final boolean IS_AT_LEAST_JAVA_6 = JAVA_CLASS_VERSION >= Opcodes.V1_6;
  
  /**
   * Create a new adapter for the supplied class
   * 
   * @param writer
   *          The ClassWriter to delegate to
   * @param className
   *          The name of this class
   * @param loader
   *          The ClassLoader loading this class
   */
  public AbstractWovenProxyAdapter(ClassVisitor writer, String className,
      ClassLoader loader) {
    super(Opcodes.ASM4, writer);
    typeBeingWoven = Type.getType("L" + className.replace('.', '/') + ";");
    //By default we expect to see methods from a concrete class
    currentMethodDeclaringType = typeBeingWoven;
    currentMethodDeclaringTypeIsInterface = false;
    this.loader = loader;
  }

  public final void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    LOGGER.debug(Constants.LOG_ENTRY, "visit", new Object[] { version, access,
        name, signature, superName, interfaces });

    // always update to the most recent version of the JVM
    version = JAVA_CLASS_VERSION;

    superType = Type.getType("L" + superName + ";");

    try {
      // we only want to implement WovenProxy once in the hierarchy.
      // It's best to do this as high up as possible so we check the
      // super. By loading it we may end up weaving it, but that's a good thing!
      Class<?> superClass = Class.forName(superName.replace('/', '.'), false,
          loader);

      isSerializable = Serializable.class.isAssignableFrom(superClass) || 
                       Arrays.asList(interfaces).contains(Type.getInternalName(Serializable.class)) ||
                       checkInterfacesForSerializability(interfaces);
      
      if (!!!WovenProxy.class.isAssignableFrom(superClass)) {

        // We have found a type we need to add WovenProxy information to

        implementWovenProxy = true;
        
        if(superClass != Object.class) {
          //If our superclass isn't Object, it means we didn't weave all the way
          //to the top of the hierarchy. This means we need to override all the
          //methods defined on our parent so that they can be intercepted!
          nonObjectSupers.add(superClass);
          Class<?> nextSuper = superClass.getSuperclass();
          while(nextSuper != Object.class) {
            nonObjectSupers.add(nextSuper);
            nextSuper = nextSuper.getSuperclass();
          }
          //Don't use reflection - it can be dangerous
          superHasNoArgsConstructor = superHasNoArgsConstructor(superName, name);

        } else {
          superHasNoArgsConstructor = true;
        }

        // re-work the interfaces list to include WovenProxy
        String[] interfacesPlusWovenProxy = new String[interfaces.length + 1];
        System.arraycopy(interfaces, 0, interfacesPlusWovenProxy, 0, interfaces.length);
        interfacesPlusWovenProxy[interfaces.length] = WOVEN_PROXY_IFACE_TYPE.getInternalName();

        // Write the class header including WovenProxy.
        cv.visit(version, access, name, signature, superName, interfacesPlusWovenProxy);

      } else {
        // Already has a woven proxy parent, but we still need to write the
        // header!
        cv.visit(version, access, name, signature, superName, interfaces);
      }
    } catch (ClassNotFoundException e) {
      // If this happens we're about to hit bigger trouble on verify, so we
      // should stop weaving and fail. Make sure we don't cause the hook to
      // throw an error though.
      UnableToProxyException u = new UnableToProxyException(name, e);
      throw new RuntimeException(NLS.MESSAGES.getMessage("cannot.load.superclass", superName.replace('/', '.'), typeBeingWoven.getClassName()), u);
    }
  }

  /**
   * This method allows us to determine whether a superclass has a
   * non-private no-args constructor without causing it to initialize.
   * This avoids a potential ClassCircularityError on Mac VMs if the
   * initialization references the subclass being woven. Odd, but seen
   * in the wild!
   */
  private final boolean superHasNoArgsConstructor(String superName, String name) {
    
    ConstructorFinder cf = new ConstructorFinder();
    
    try {
      InputStream is = loader.getResourceAsStream(superName +".class");
    
      if(is == null)
        throw new IOException();
      
      new ClassReader(is).accept(cf, ClassReader.SKIP_FRAMES + ClassReader.SKIP_DEBUG + ClassReader.SKIP_CODE);
    } catch (IOException ioe) {
      UnableToProxyException u = new UnableToProxyException(name, ioe);
      throw new RuntimeException(NLS.MESSAGES.getMessage("cannot.load.superclass", superName.replace('/', '.'), typeBeingWoven.getClassName()), u);
    }
    return cf.hasNoArgsConstructor();
  }
  
  private boolean checkInterfacesForSerializability(String[] interfaces) throws ClassNotFoundException {
    for(String iface : interfaces)
    {
      if(Serializable.class.isAssignableFrom(Class.forName(
                 iface.replace('/', '.'), false, loader)))
        return true;
    }
    return false;
  }

  /**
   * This method is called on each method implemented on this object (but not
   * for superclass methods) Each of these methods is visited in turn and the
   * code here generates the byte code for the calls to the InovcationListener
   * around the existing method
   */
  public final MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    LOGGER.debug(Constants.LOG_ENTRY, "visitMethod", new Object[] { access,
        name, desc, signature, exceptions });

    
    Method currentMethod = new Method(name, desc);
    
    getKnownMethods().add(currentMethod);
    
    MethodVisitor methodVisitorToReturn = null;

    // Only weave "real" instance methods. Not constructors, initializers or
    // compiler generated ones.
    if ((access & (ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC 
        | ACC_NATIVE | ACC_BRIDGE)) == 0 && !!!name.equals("<init>") && 
        !!!name.equals("<clinit>")) {

      // found a method we should weave

      //Create a field name and store it for later
      String methodStaticFieldName = "methodField" + getSanitizedUUIDString();
      transformedMethods.put(methodStaticFieldName, new TypeMethod(
           currentMethodDeclaringType, currentMethod));

      // Surround the MethodVisitor with our weaver so we can manipulate the code
      methodVisitorToReturn = getWeavingMethodVisitor(access, name, desc,
          signature, exceptions, currentMethod, methodStaticFieldName,
          currentMethodDeclaringType, currentMethodDeclaringTypeIsInterface);
    } else if (name.equals("<clinit>")){
      //there is an existing clinit method, change the fields we use
      //to write our init code to static_init_UUID instead
      staticInitMethod = new Method("static_init_" + UU_ID, Type.VOID_TYPE, NO_ARGS);
      staticInitMethodFlags = staticInitMethodFlags | ACC_FINAL;
      methodVisitorToReturn = new AdviceAdapter(Opcodes.ASM4, cv.visitMethod(access, name, desc, signature,
          exceptions), access, name, desc){
        @Override
        protected void onMethodEnter()
        {
          //add into the <clinit> a call to our synthetic static_init_UUID
          invokeStatic(typeBeingWoven, staticInitMethod);
          super.onMethodEnter();
        }
      };
    } else {
      if(currentMethod.getArgumentTypes().length == 0 && name.equals("<init>"))
        hasNoArgsConstructor = true;
      //This isn't a method we want to weave, so just get the default visitor
      methodVisitorToReturn = cv.visitMethod(access, name, desc, signature,
          exceptions);
    }

    LOGGER.debug(Constants.LOG_EXIT, "visitMethod", methodVisitorToReturn);
    return methodVisitorToReturn;
  }

  /**
   * Our class may claim to implement WovenProxy, but doesn't have any
   * implementations! We should fix this.
   */
  public void visitEnd() {
    LOGGER.debug(Constants.LOG_ENTRY, "visitEnd");

    for(Class<?> c : nonObjectSupers) {
      setCurrentMethodDeclaringType(Type.getType(c), false);
      try {
        readClass(c, new MethodCopyingClassAdapter(this, loader, c, typeBeingWoven, 
            getKnownMethods(), transformedMethods));
      } catch (IOException e) {
        // This should never happen! <= famous last words (not)
        throw new RuntimeException(NLS.MESSAGES.getMessage("unexpected.error.processing.class", c.getName(), typeBeingWoven.getClassName()), e);
      }
    }
    // If we need to implement woven proxy in this class then write the methods
    if (implementWovenProxy) {
      writeFinalWovenProxyMethods();
    }
    
    // this method is called when we reach the end of the class
    // so it is time to make sure the static initialiser method is written
    writeStaticInitMethod();

    // Make sure we add the instance specific WovenProxy method to our class,
    // and give ourselves a constructor to use
    writeCreateNewProxyInstanceAndConstructor();

    // now delegate to the cv
    cv.visitEnd();

    LOGGER.debug(Constants.LOG_EXIT, "visitEnd");
  }
  
  public Set<Method> getKnownMethods() {
    return knownMethods;
  }

  /**
   * Get the {@link MethodVisitor} that will weave a given method
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   * @param currentMethod
   * @param methodStaticFieldName
   * @return
   */
  protected abstract MethodVisitor getWeavingMethodVisitor(int access, String name,
  String desc, String signature, String[] exceptions, Method currentMethod,
  String methodStaticFieldName, Type currentMethodDeclaringType,
  boolean currentMethodDeclaringTypeIsInterface);
  

  /**
   * Write the methods we need for wovenProxies on the highest supertype
   */
  private final void writeFinalWovenProxyMethods() {
    // add private fields for the Callable<Object> dispatcher
    // and InvocationListener. These aren't static because we can have
    // multiple instances of the same proxy class. These should not be
    // serialized, or used in JPA or any other thing we can think of,
    // so we annotate them as necessary

    generateField(DISPATCHER_FIELD, Type.getDescriptor(Callable.class));
    generateField(LISTENER_FIELD, Type.getDescriptor(InvocationListener.class));

    // a general methodAdapter field that we will use to with GeneratorAdapters
    // to create the methods required to implement WovenProxy
    GeneratorAdapter methodAdapter;

    // add a method for unwrapping the dispatcher
    methodAdapter = getMethodGenerator(PUBLIC_GENERATED_METHOD_ACCESS, new Method(
        "org_apache_aries_proxy_weaving_WovenProxy_unwrap", DISPATCHER_TYPE,
        NO_ARGS));

    // /////////////////////////////////////////////////////
    // Implement the method

    // load this to get the field
    methodAdapter.loadThis();
    // get the dispatcher field and return
    methodAdapter.getField(typeBeingWoven, DISPATCHER_FIELD, DISPATCHER_TYPE);
    methodAdapter.returnValue();
    methodAdapter.endMethod();

    // /////////////////////////////////////////////////////

    // add a method for checking if the dispatcher is set
    methodAdapter = getMethodGenerator(PUBLIC_GENERATED_METHOD_ACCESS, new Method(
        "org_apache_aries_proxy_weaving_WovenProxy_isProxyInstance",
        Type.BOOLEAN_TYPE, NO_ARGS));

    // /////////////////////////////////////////////////////
    // Implement the method

    // load this to get the field
    methodAdapter.loadThis();
    // make a label for return true
    Label returnTrueLabel = methodAdapter.newLabel();
    // get the dispatcher field for the stack
    methodAdapter.getField(typeBeingWoven, DISPATCHER_FIELD, DISPATCHER_TYPE);
    // check if the dispatcher was non-null and goto return true if it was
    methodAdapter.ifNonNull(returnTrueLabel);
    methodAdapter.loadThis();
    // get the listener field for the stack
    methodAdapter.getField(typeBeingWoven, LISTENER_FIELD, LISTENER_TYPE);
    // check if the listener field was non-null and goto return true if it was
    methodAdapter.ifNonNull(returnTrueLabel);
    // return false if we haven't jumped anywhere
    methodAdapter.push(false);
    methodAdapter.returnValue();
    // mark the returnTrueLable
    methodAdapter.mark(returnTrueLabel);
    methodAdapter.push(true);
    methodAdapter.returnValue();
    // end the method
    methodAdapter.endMethod();

    // ///////////////////////////////////////////////////////
  }

  /**
   * We write createNewProxyInstance separately because it isn't final, and is
   * overridden on each class, we also write a constructor for this method to
   * use if we don't have one.
   */
  private final void writeCreateNewProxyInstanceAndConstructor() {
    GeneratorAdapter methodAdapter = getMethodGenerator(ACC_PUBLIC, new Method(
        "org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance",
        WOVEN_PROXY_IFACE_TYPE, DISPATCHER_LISTENER_METHOD_ARGS));

    // /////////////////////////////////////////////////////
    // Implement the method

    // Create and instantiate a new instance, then return it
    methodAdapter.newInstance(typeBeingWoven);
    methodAdapter.dup();
    methodAdapter.loadArgs();
    methodAdapter.invokeConstructor(typeBeingWoven, new Method("<init>",
        Type.VOID_TYPE, DISPATCHER_LISTENER_METHOD_ARGS));
    methodAdapter.returnValue();
    methodAdapter.endMethod();
    //////////////////////////////////////////////////////////

    
    // Write a protected no-args constructor for this class
    methodAdapter = getMethodGenerator(ACC_PROTECTED | ACC_SYNTHETIC, ARGS_CONSTRUCTOR);

    // /////////////////////////////////////////////////////
    // Implement the constructor

    // For the top level supertype we need to invoke a no-args super, on object 
    //if we have to
    
    if(implementWovenProxy) {
      methodAdapter.loadThis();

      if (superHasNoArgsConstructor)
        methodAdapter.invokeConstructor(superType, NO_ARGS_CONSTRUCTOR);
      else {
        if(hasNoArgsConstructor)
          methodAdapter.invokeConstructor(typeBeingWoven, NO_ARGS_CONSTRUCTOR);
        else
          throw new RuntimeException(new UnableToProxyException(typeBeingWoven.getClassName(), 
              NLS.MESSAGES.getMessage("type.lacking.no.arg.constructor", typeBeingWoven.getClassName(), superType.getClassName())));
      }
      methodAdapter.loadThis();
      methodAdapter.loadArg(0);
      methodAdapter.putField(typeBeingWoven, DISPATCHER_FIELD, DISPATCHER_TYPE);
      
      methodAdapter.loadThis();
      methodAdapter.loadArg(1);
      methodAdapter.putField(typeBeingWoven, LISTENER_FIELD, LISTENER_TYPE);
    } else {
      //We just invoke the super with args
      methodAdapter.loadThis();
      methodAdapter.loadArgs();
      methodAdapter.invokeConstructor(superType, ARGS_CONSTRUCTOR);
    }
    
    //Throw an NPE if the dispatcher is null, return otherwise
    methodAdapter.loadArg(0);
    Label returnValue = methodAdapter.newLabel();
    methodAdapter.ifNonNull(returnValue);
    methodAdapter.newInstance(NPE_TYPE);
    methodAdapter.dup();
    methodAdapter.push("The dispatcher must never be null!");
    methodAdapter.invokeConstructor(NPE_TYPE, NPE_CONSTRUCTOR);
    methodAdapter.throwException();
    
    methodAdapter.mark(returnValue);
    methodAdapter.returnValue();
    methodAdapter.endMethod();
    //////////////////////////////////////////////////////////
  }

  /**
   * Create fields and an initialiser for {@link java.lang.reflect.Method}
   * objects in our class
   */
  private final void writeStaticInitMethod() {
    // we create a static field for each method we encounter with a *unique*
    // random name
    // since each method needs to be stored individually

    for (String methodStaticFieldName : transformedMethods.keySet()) {
      // add a private static field for the method
      cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
          methodStaticFieldName, METHOD_TYPE.getDescriptor(), null, null)
          .visitEnd();
    }
    GeneratorAdapter staticAdapter = new GeneratorAdapter(staticInitMethodFlags,
        staticInitMethod, null, null, cv);

    for (Entry<String, TypeMethod> entry : transformedMethods.entrySet()) {
      // Add some more code to the static initializer

      TypeMethod m = entry.getValue();
      Type[] targetMethodParameters = m.method.getArgumentTypes();

      String methodStaticFieldName = entry.getKey();

      Label beginPopulate = staticAdapter.newLabel();
      Label endPopulate = staticAdapter.newLabel();
      Label catchHandler = staticAdapter.newLabel();
      staticAdapter.visitTryCatchBlock(beginPopulate, endPopulate,
          catchHandler, THROWABLE_INAME);

      staticAdapter.mark(beginPopulate);
      staticAdapter.push(m.declaringClass);

      // push the method name string arg onto the stack
      staticAdapter.push(m.method.getName());

      // create an array of the method parm class[] arg
      staticAdapter.push(targetMethodParameters.length);
      staticAdapter.newArray(CLASS_TYPE);
      int index = 0;
      for (Type t : targetMethodParameters) {
        staticAdapter.dup();
        staticAdapter.push(index);
        staticAdapter.push(t);
        staticAdapter.arrayStore(CLASS_TYPE);
        index++;
      }

      // invoke the getMethod
      staticAdapter.invokeVirtual(CLASS_TYPE,
          new Method("getDeclaredMethod", METHOD_TYPE, new Type[] {
              STRING_TYPE, CLASS_ARRAY_TYPE}));

      // store the reflected method in the static field
      staticAdapter.putStatic(typeBeingWoven, methodStaticFieldName,
          METHOD_TYPE);

      Label afterCatch = staticAdapter.newLabel();
      staticAdapter.mark(endPopulate);
      staticAdapter.goTo(afterCatch);

      staticAdapter.mark(catchHandler);
      // We don't care about the exception, so pop it off
      staticAdapter.pop();
      // store the reflected method in the static field
      staticAdapter.visitInsn(ACONST_NULL);
      staticAdapter.putStatic(typeBeingWoven, methodStaticFieldName,
          METHOD_TYPE);
      staticAdapter.mark(afterCatch);

    }
    staticAdapter.returnValue();
    staticAdapter.endMethod();
  }

  /**
   * Get a new UUID suitable for use in method and field names
   * 
   * @return
   */
  public static final String getSanitizedUUIDString() {
    return UUID.randomUUID().toString().replace('-', '_');
  }

  /**
   * This method will read the bytes for the supplied {@link Class} using the
   * supplied ASM {@link ClassVisitor}, the reader will skip DEBUG, FRAMES and CODE.
   * @param c
   * @param adapter
   * @throws IOException
   */
  public static void readClass(Class<?> c, ClassVisitor adapter) throws IOException {
    String className = c.getName();
    className = className.substring(className.lastIndexOf('.') + 1) + ".class";
        
    //Load the class bytes and copy methods across
    ClassReader cReader = new ClassReader(c.getResourceAsStream(className));

    cReader.accept(adapter, ClassReader.SKIP_CODE | 
        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
  }

  /**
   * Generate an instance field that should be "invisible" to normal code
   * 
   * @param fieldName
   * @param fieldDescriptor
   */
  private final void generateField(String fieldName, String fieldDescriptor) {
    FieldVisitor fv = cv.visitField(ACC_PROTECTED | ACC_TRANSIENT | ACC_SYNTHETIC
        | ACC_FINAL, fieldName, fieldDescriptor, null, null);
    for (String s : annotationTypeDescriptors)
      fv.visitAnnotation(s, true).visitEnd();
    fv.visitEnd();
  }

  /**
   * Get a generator for a method, this be annotated with the "invisibility"
   * annotations (and ensured synthetic)
   * 
   * @param methodSignature
   * @return
   */
  private final GeneratorAdapter getMethodGenerator(int access, Method method) {
    access = access | ACC_SYNTHETIC;
    GeneratorAdapter ga = new GeneratorAdapter(access, method, null, null, cv);
    for (String s : annotationTypeDescriptors)
      ga.visitAnnotation(s, true).visitEnd();
    ga.visitCode();
    return ga;
  }

  public final void setCurrentMethodDeclaringType(Type type, boolean isInterface) {
    currentMethodDeclaringType = type;
    currentMethodDeclaringTypeIsInterface = isInterface;
  }
}