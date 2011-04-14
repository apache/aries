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
package org.apache.aries.proxy.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;

public final class ProxyHandler implements InvocationHandler {
  private final Callable<Object> target;
  private final InvocationHandler core;
  private final AbstractProxyManager proxyManager;

  public ProxyHandler(AbstractProxyManager abstractProxyManager, Callable<Object> dispatcher, InvocationListener listener)
  {
    target = dispatcher;
    proxyManager = abstractProxyManager;
    final InvocationListener nonNullListener;
    if (listener == null) {
      nonNullListener = new DefaultWrapper();
    } else {
      nonNullListener = listener;
    }
    
    core = new InvocationHandler() {
      public Object invoke(Object proxy, Method method, Object[] args)
          throws Throwable 
      {
        Object result = null;
        Object token = null;
        boolean inInvoke = false;
        try {
          token = nonNullListener.preInvoke(proxy, method, args);
          inInvoke = true;
          result = method.invoke(target.call(), args);
          inInvoke = false;
          nonNullListener.postInvoke(token, proxy, method, result);

        } catch (Throwable e) {
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
            if (e instanceof InvocationTargetException) {
              e = ((InvocationTargetException) e).getTargetException();
            }
            
            if (!(e instanceof RuntimeException)) {
              exceptionToRethrow = e;
            }
          }
          try {
            nonNullListener.postInvokeExceptionalReturn(token, proxy, method, e);
          } catch (Exception f) {
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
        return result;
      }
    };
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
  {
    // Unwrap calls for equals
    if (method.getName().equals("equals")
            && method.getParameterTypes().length == 1 &&
            method.getParameterTypes()[0] == Object.class) {
        Object targetObject = args[0];
        if (proxyManager.isProxy(targetObject)) {
          args[0] = proxyManager.unwrap(targetObject).call();
        }
    } else if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
        // special case finalize, don't route through to delegate because that will get its own call
        return null;
    }
    
    return core.invoke(proxy, method, args);
  }

  public Callable<Object> getTarget() 
  {
    return target;
  }
}