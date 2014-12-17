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

import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.DISPATCHER_FIELD;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.DISPATCHER_TYPE;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.LISTENER_FIELD;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.LISTENER_TYPE;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.METHOD_TYPE;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.NO_ARGS;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.OBJECT_TYPE;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.THROWABLE_INAME;
import static org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter.WOVEN_PROXY_IFACE_TYPE;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.ASM5;

import java.util.Arrays;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.impl.NLS;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
/**
 * This class weaves dispatch and listener code into a method, there are two known
 * subclasses {@link WovenProxyConcreteMethodAdapter} is used for weaving instance methods
 * {@link WovenProxyAbstractMethodAdapter} is used to provide a delegating
 * implementation of an interface method.
 * 
 * Roughly (but not exactly because it's easier to write working bytecode
 * if you don't have to exactly recreate the Java!) this is trying to 
 * do the following: <code>
 * 
 *      
    if(dispatcher != null) {
      int returnValue;
      Object token = null;
      boolean inInvoke = false;
      try {
        Object toInvoke = dispatcher.call();
        if(listener != null)
          token = listener.preInvoke(toInvoke, method, args);
        
        inInvoke = true;
        returnValue = ((Template) toInvoke).doStuff(args);
        inInvoke = false;
        
        if(listener != null)
          listener.postInvoke(token, toInvoke, method, args);
        
      } catch (Throwable e){
        // whether the the exception is an error is an application decision
        // if we catch an exception we decide carefully which one to
        // throw onwards
        Throwable exceptionToRethrow = null;
        // if the exception came from a precall or postcall 
        // we will rethrow it
        if (!inInvoke) {
          exceptionToRethrow = e;
        }
        // if the exception didn't come from precall or postcall then it
        // came from invoke
        // we will rethrow this exception if it is not a runtime
        // exception, but we must unwrap InvocationTargetExceptions
        else {
          if (!(e instanceof RuntimeException)) {
            exceptionToRethrow = e;
          }
        }
        try {
          if(listener != null)
            listener.postInvokeExceptionalReturn(token, method, null, e);
        } catch (Throwable f) {
          // we caught an exception from
          // postInvokeExceptionalReturn
          // if we haven't already chosen an exception to rethrow then
          // we will throw this exception
          if (exceptionToRethrow == null) {
            exceptionToRethrow = f;
          }
        }
        // if we made it this far without choosing an exception we
        // should throw e
        if (exceptionToRethrow == null) {
          exceptionToRethrow = e;
        }
        throw exceptionToRethrow;
      }
    }
    
    //...original method body
      </code>
 *  
 *   
 */
public abstract class AbstractWovenProxyMethodAdapter extends GeneratorAdapter
{
  /** The type of a RuntimeException */
  private static final Type RUNTIME_EX_TYPE = Type.getType(RuntimeException.class);
  private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
  
  /** The postInvoke method of an {@link InvocationListener} */
  private static final Method POST_INVOKE_METHOD = getAsmMethodFromClass(InvocationListener.class, "postInvoke", Object.class,
      Object.class, java.lang.reflect.Method.class, Object.class);
  /** The postInvokeExceptionalReturn method of an {@link InvocationListener} */
  private static final Method POST_INVOKE_EXCEPTIONAL_METHOD = getAsmMethodFromClass(InvocationListener.class, 
      "postInvokeExceptionalReturn", Object.class, Object.class,
      java.lang.reflect.Method.class, Throwable.class);
  /** The preInvoke method of an {@link InvocationListener} */
  private static final Method PRE_INVOKE_METHOD = getAsmMethodFromClass(InvocationListener.class, "preInvoke", Object.class,
      java.lang.reflect.Method.class, Object[].class);
  
  
  /** The name of the static field that stores our {@link java.lang.reflect.Method} */
  private final String methodStaticFieldName;
  /** The current method */
  protected final Method currentTransformMethod;
  /** The type of <code>this</code> */
  protected final Type typeBeingWoven;
  /** True if this is a void method */
  private final boolean isVoid;

  //ints for local store
  /** The local we use to store the {@link InvocationListener} token */
  private int preInvokeReturnedToken;
  /** The local we use to note whether we are in the original method body or not */
  private int inNormalMethod;
  /** The local we use to store the invocation target to dispatch to */
  private int dispatchTarget;
  /** The local for storing our method's result */
  private int normalResult;

  //the Labels we need for jumping around the pre/post/postexception and current method code
  /** This marks the start of the try/catch around the pre/postInvoke*/
  private final Label beginTry = new Label();
  /** This marks the end of the try/catch around the pre/postInvoke*/
  private final Label endTry = new Label();

  /** The return type of this method */
  private final Type returnType;
  
  private final Type methodDeclaringType;
  
  private final boolean isMethodDeclaringTypeInterface;
  private boolean isDefaultMethod;
  
  /**
   * Construct a new method adapter
   * @param mv - the method visitor to write to
   * @param access - the access modifiers on this method
   * @param name - the name of this method
   * @param desc - the descriptor of this method
   * @param methodStaticFieldName - the name of the static field that will hold
   *                                the {@link java.lang.reflect.Method} representing
   *                                this method.
   * @param currentTransformMethod - the ASM representation of this method
   * @param proxyType - the type being woven that contains this method
   */
  public AbstractWovenProxyMethodAdapter(MethodVisitor mv, int access, String name, String desc,
      String methodStaticFieldName, Method currentTransformMethod, Type typeBeingWoven,
      Type methodDeclaringType, boolean isMethodDeclaringTypeInterface, boolean isDefaultMethod)
  {
    super(ASM5, mv, access, name, desc);
    this.methodStaticFieldName = methodStaticFieldName;
    this.currentTransformMethod = currentTransformMethod;
    returnType = currentTransformMethod.getReturnType();
    isVoid = returnType.getSort() == Type.VOID;
    this.typeBeingWoven = typeBeingWoven;
    this.methodDeclaringType = methodDeclaringType;
    this.isMethodDeclaringTypeInterface = isMethodDeclaringTypeInterface;
    this.isDefaultMethod = isDefaultMethod;
  }

  @Override
  public abstract void visitCode();

  @Override
  public abstract void visitMaxs(int stack, int locals);
  
  /**
   * Write out the bytecode instructions necessary to do the dispatch.
   * We know the dispatcher is non-null, and we need a try/catch around the
   * invocation and listener calls.
   */
  protected final void writeDispatcher() {
    // Setup locals we will use in the dispatch
    setupLocals();
    
    //Write the try catch block
    visitTryCatchBlock(beginTry, endTry, endTry, THROWABLE_INAME);
    mark(beginTry);
    
    //Start dispatching, get the target object and store it
    loadThis();
    getField(typeBeingWoven, DISPATCHER_FIELD, DISPATCHER_TYPE);
    invokeInterface(DISPATCHER_TYPE, new Method("call", OBJECT_TYPE, NO_ARGS));
    storeLocal(dispatchTarget);
    
    //Pre-invoke, invoke, post-invoke, return
    writePreInvoke();
    //Start the real method
    push(true);
    storeLocal(inNormalMethod);
    
    //Dispatch the method and store the result (null for void)
    loadLocal(dispatchTarget);
    checkCast(methodDeclaringType);
    loadArgs();
    if(isMethodDeclaringTypeInterface && !isDefaultMethod) {
      invokeInterface(methodDeclaringType, currentTransformMethod);
    } else {
      invokeVirtual(methodDeclaringType, currentTransformMethod);
    }
    if(isVoid) {
      visitInsn(ACONST_NULL);
    }
    storeLocal(normalResult);
    
    // finish the real method and post-invoke
    push(false);
    storeLocal(inNormalMethod);
    writePostInvoke();
    
    //Return, with the return value if necessary
    if(!!!isVoid) {
      loadLocal(normalResult);
    }
    returnValue();
    
    //End of our try, start of our catch
    mark(endTry);
    writeMethodCatchHandler();
  }
  
  /**
   * Setup the normalResult, inNormalMethod, preInvokeReturnedToken and
   * dispatch target locals.
   */
  private final void setupLocals() {
    if (isVoid){
      normalResult = newLocal(OBJECT_TYPE);
    } else{
      normalResult = newLocal(returnType);
    }
    
    preInvokeReturnedToken = newLocal(OBJECT_TYPE);
    visitInsn(ACONST_NULL);
    storeLocal(preInvokeReturnedToken);
    
    inNormalMethod = newLocal(Type.BOOLEAN_TYPE);
    push(false);
    storeLocal(inNormalMethod);
    
    dispatchTarget = newLocal(OBJECT_TYPE);
    visitInsn(ACONST_NULL);
    storeLocal(dispatchTarget);
  }

  /**
   * Begin trying to invoke the listener, if the listener is
   * null the bytecode will branch to the supplied label, other
   * otherwise it will load the listener onto the stack.
   * @param l The label to branch to
   */
  private final void beginListenerInvocation(Label l) {
    //If there's no listener then skip invocation
    loadThis();
    getField(typeBeingWoven, LISTENER_FIELD, LISTENER_TYPE);
    ifNull(l);
    loadThis();
    getField(typeBeingWoven, LISTENER_FIELD, LISTENER_TYPE);
  }

  /**
   * Write out the preInvoke. This copes with the listener being null
   */
  private final void writePreInvoke() {
    //The place to go if the listener is null
    Label nullListener = newLabel();
    beginListenerInvocation(nullListener);

    // The listener is on the stack, we need (target, method, args)
    
    loadLocal(dispatchTarget);
    getStatic(typeBeingWoven, methodStaticFieldName, METHOD_TYPE);
    loadArgArray();
    
    //invoke it and store the token returned
    invokeInterface(LISTENER_TYPE, PRE_INVOKE_METHOD);
    storeLocal(preInvokeReturnedToken);
    
    mark(nullListener);
  }
  
  /**
   * Write out the postInvoke. This copes with the listener being null
   */
  private final void writePostInvoke() {
    //The place to go if the listener is null
    Label nullListener = newLabel();
    beginListenerInvocation(nullListener);
    
    // The listener is on the stack, we need (token, target, method, result)
    
    loadLocal(preInvokeReturnedToken);
    loadLocal(dispatchTarget);
    getStatic(typeBeingWoven, methodStaticFieldName, METHOD_TYPE);
    loadLocal(normalResult);
    
    //If the result a primitive then we need to box it
    if (!!!isVoid && returnType.getSort() != Type.OBJECT && returnType.getSort() != Type.ARRAY){
      box(returnType);
    }
    
    //invoke the listener
    invokeInterface(LISTENER_TYPE, POST_INVOKE_METHOD);
    
    mark(nullListener);
  }
  
  /**
   * Write the catch handler for our method level catch, this runs the exceptional
   * post-invoke if there is a listener, and throws the correct exception at the
   * end
   */
  private final void writeMethodCatchHandler() {
    
    //Store the original exception
    int originalException = newLocal(THROWABLE_TYPE);
    storeLocal(originalException);
    
    //Start by initialising exceptionToRethrow
    int exceptionToRethrow = newLocal(THROWABLE_TYPE);
    visitInsn(ACONST_NULL);
    storeLocal(exceptionToRethrow);
    
    //We need another try catch around the postInvokeExceptionalReturn, so here 
    //are some labels and the declaration for it
    Label beforeInvoke = newLabel();
    Label afterInvoke = newLabel();
    visitTryCatchBlock(beforeInvoke, afterInvoke, afterInvoke, THROWABLE_INAME);
    
    //If we aren't in normal flow then set exceptionToRethrow = originalException
    loadLocal(inNormalMethod);
    Label inNormalMethodLabel = newLabel();
    // Jump if not zero (false)
    visitJumpInsn(IFNE, inNormalMethodLabel);
    loadLocal(originalException);
    storeLocal(exceptionToRethrow);
    goTo(beforeInvoke);
    
    mark(inNormalMethodLabel);
    //We are in normal method flow so set exceptionToRethrow = originalException
    //if originalException is not a runtime exception
    loadLocal(originalException);
    instanceOf(RUNTIME_EX_TYPE);
    //If false then store original in toThrow, otherwise go to beforeInvoke
    visitJumpInsn(IFNE, beforeInvoke);
    loadLocal(originalException);
    storeLocal(exceptionToRethrow);
    goTo(beforeInvoke);
    //Setup of variables finished, begin try/catch
       
    //Mark the start of our try
    mark(beforeInvoke);
    //Begin invocation of the listener, jump to throw if null
    Label throwSelectedException = newLabel();
    beginListenerInvocation(throwSelectedException);
    
    //We have a listener, so call it (token, target, method, exception)
    loadLocal(preInvokeReturnedToken);
    loadLocal(dispatchTarget);
    getStatic(typeBeingWoven, methodStaticFieldName, METHOD_TYPE);
    loadLocal(originalException);
    invokeInterface(LISTENER_TYPE, POST_INVOKE_EXCEPTIONAL_METHOD);
    goTo(throwSelectedException);
    
    mark(afterInvoke);
    //catching another exception replaces the original
    storeLocal(originalException);

    //Throw exceptionToRethrow if it isn't null, or the original if it is
    Label throwException = newLabel();
    mark(throwSelectedException);
    loadLocal(exceptionToRethrow);
    ifNonNull(throwException);
    loadLocal(originalException);
    storeLocal(exceptionToRethrow);
    
    mark(throwException);
    loadLocal(exceptionToRethrow);
    throwException();
  }
  
  /**
   * This method unwraps woven proxy instances for use in the right-hand side
   * of equals methods
   */
  protected final void unwrapEqualsArgument() {
    
    //Create and initialize a local for our work
    int unwrapLocal = newLocal(OBJECT_TYPE);
    visitInsn(ACONST_NULL);
    storeLocal(unwrapLocal);
    
    Label startUnwrap = newLabel();
    mark(startUnwrap);
    //Load arg and check if it is a WovenProxy instances
    loadArg(0);
    instanceOf(WOVEN_PROXY_IFACE_TYPE);
    Label unwrapFinished = newLabel();
    //Jump if zero (false)
    visitJumpInsn(Opcodes.IFEQ, unwrapFinished);
    //Arg is a wovenProxy, if it is the same as last time then we're done
    loadLocal(unwrapLocal);
    loadArg(0);
    ifCmp(OBJECT_TYPE, EQ, unwrapFinished);
    //Not the same, store current arg in unwrapLocal for next loop
    loadArg(0);
    storeLocal(unwrapLocal);
    
    //So arg is a WovenProxy, but not the same as last time, cast it and store 
    //the result of unwrap.call in the arg
    loadArg(0);
    checkCast(WOVEN_PROXY_IFACE_TYPE);
    //Now unwrap
    invokeInterface(WOVEN_PROXY_IFACE_TYPE, new Method("org_apache_aries_proxy_weaving_WovenProxy_unwrap",
        DISPATCHER_TYPE, NO_ARGS));
    
    //Now we may have a Callable to invoke
    int callable = newLocal(DISPATCHER_TYPE);
    storeLocal(callable);
    loadLocal(callable);
    ifNull(unwrapFinished);
    loadLocal(callable);
    invokeInterface(DISPATCHER_TYPE, new Method("call",
        OBJECT_TYPE, NO_ARGS));
    //Store the result and we're done (for this iteration)
    storeArg(0);
    goTo(startUnwrap);
    
    mark(unwrapFinished);
  }

  /**
   * A utility method for getting an ASM method from a Class
   * @param clazz the class to search
   * @param name The method name
   * @param argTypes The method args
   * @return
   */
  private static final Method getAsmMethodFromClass(Class<?> clazz, String name, Class<?>... argTypes)
  {
    //get the java.lang.reflect.Method to get the types
    java.lang.reflect.Method ilMethod = null;
    try {
      ilMethod = clazz.getMethod(name, argTypes);
    } catch (Exception e) {
      //Should be impossible!
      throw new RuntimeException(NLS.MESSAGES.getMessage("error.finding.invocation.listener.method", name, Arrays.toString(argTypes)), e);
    }
    //get the ASM method
    return new Method(name, Type.getReturnType(ilMethod), Type.getArgumentTypes(ilMethod));
  }
}
