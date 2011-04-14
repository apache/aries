/**
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
package org.apache.aries.proxy;

import java.lang.reflect.Method;

/**
 * An {@link InvocationListener} is used in conjunction with the {@link ProxyManager}
 * to intercept method calls on the proxy object
 */
public interface InvocationListener 
{
  public Object preInvoke(Object proxy, Method m, Object[] args) throws Throwable;
  
  public void postInvoke(Object token, Object proxy, Method m, Object returnValue) throws Throwable;
  
  public void postInvokeExceptionalReturn(Object token, Object proxy, Method m, Throwable exception) throws Throwable;
}