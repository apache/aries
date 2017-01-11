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
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.osgi.framework.Bundle;

public final class JdkProxyManager extends AbstractProxyManager implements ProxyManager
{
  public Object createNewProxy(Bundle clientBundle, Collection<Class<?>> classes, 
      Callable<Object> dispatcher, InvocationListener listener) throws UnableToProxyException 
  {
    return Proxy.newProxyInstance(getClassLoader(clientBundle, classes), getInterfaces(classes), new ProxyHandler(this, dispatcher, listener));
  }

  private static final Class<?>[] getInterfaces(Collection<Class<?>> classes) throws UnableToProxyException
  {
    for (Class<?> clazz : classes) {
        if (!!!clazz.isInterface()) {
          throw new UnableToProxyException(clazz, String.format("The class %s is not an interface and therefore a proxy cannot be generated.", clazz.getName()));
        } 
    }
    return (Class[]) classes.toArray(new Class[classes.size()]);
  }

  @Override
  protected boolean isProxyClass(Class<?> clazz) 
  {
    return Proxy.isProxyClass(clazz);
  }

  @Override
  protected InvocationHandler getInvocationHandler(Object proxy) 
  {
    Class<?> clazz = proxy.getClass();
    if (isProxyClass(clazz)) {
      return Proxy.getInvocationHandler(proxy);
    }
    return null;
  }
}