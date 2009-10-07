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
package org.apache.aries.unittest.mocks;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * <p>This invocation handler is used by the Skeleton when nothing else is
 *   matched. If the return type is an interface it creates a dynamic proxy
 *   backed by the associated skeleton for return, if it is a class with a
 *   default constructor that will be returned.
 * </p>
 */
public class DefaultInvocationHandler implements InvocationHandler
{
  /** The skeleton this handler is associated with */
  private Skeleton _s;
  
  /* ------------------------------------------------------------------------ */
  /* DefaultInvocationHandler constructor                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Creates an instance called by the specified skeleton.
   *
   * @param s The caller.
   */
  public DefaultInvocationHandler(Skeleton s)
  {
    this._s = s;
  }
  
  /* ------------------------------------------------------------------------ */
  /* invoke method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Invoked when no ReturnType or MethodCall Handlers are defined.
   * 
   * @param target     The target object that was invoked. 
   * @param method     The method that was invoked.
   * @param arguments  The arguments that were passed.
   * @return           A proxy or null.
   * @throws Throwable
   */
  public Object invoke(Object target, Method method, Object[] arguments)
      throws Throwable
  {
    Class<?> returnType = method.getReturnType();
    Object obj = null;
    
    if (returnType.isInterface())
    {
      obj = createProxy(new Class[] { returnType });
    }
    else 
    {
      try
      {
        obj = returnType.newInstance();
      }
      catch (Exception e)
      {
        // if this occurs then assume no default constructor was visible.
      }
    }
    
    return obj;
  }
  
  /* ------------------------------------------------------------------------ */
  /* createProxy method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Creates and returns a proxy backed by the associated skeleton, that 
   * implements the specified interfaces. Null is returned if the return
   * type array contains non interfaces.
   * 
   * @param returnTypes The classes.
   * @return            The proxy or null.
   */
  public Object createProxy(Class<?> ... returnTypes)
  {
    Object result = null;
    
    boolean allInterfaces = true;
    for(int i = 0; (allInterfaces && i<returnTypes.length); i++)
       allInterfaces = returnTypes[i].isInterface();
    
    if (allInterfaces)
    {
      result = _s.createMock(returnTypes);
    }
    return result;
  }
}
