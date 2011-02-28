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

import org.apache.aries.proxy.InvocationHandlerWrapper;

public final class ProxyHandler implements InvocationHandler {
  private final Callable<Object> target;
  private final InvocationHandler core;
  private final InvocationHandlerWrapper wrapper;
  private final AbstractProxyManager proxyManager;

  public ProxyHandler(AbstractProxyManager abstractProxyManager, Callable<Object> dispatcher, InvocationHandlerWrapper wrapper)
  {
    target = dispatcher;
    proxyManager = abstractProxyManager;
    if (wrapper == null) {
      this.wrapper = new DefaultWrapper();
    } else {
      this.wrapper = wrapper;
    }
    
    core = new InvocationHandler() {
      public Object invoke(Object proxy, Method method, Object[] args)
          throws Throwable 
      {
          Object result;
          try {
              result = method.invoke(target.call(), args);
          } catch (InvocationTargetException ite) {
              // We are invisible, so unwrap and throw the cause as
              // though we called the method directly.
              throw ite.getCause();
          } catch (IllegalAccessException e) {
              throw new IllegalAccessError(e.getMessage());
          }

          return result;
      }
    };
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
  {
    // Unwrap calls for equals
    if (method.getName().equals("equals")
            && method.getDeclaringClass() == Object.class) {
        Object targetObject = args[0];
        if (proxyManager.isProxy(targetObject)) {
          args[0] = proxyManager.unwrap(targetObject).call();
        }
    } else if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
        // special case finalize, don't route through to delegate because that will get its own call
        return null;
    }
    
    return wrapper.invoke(proxy, method, args, core);
  }

  public Callable<Object> getTarget() 
  {
    return target;
  }
}