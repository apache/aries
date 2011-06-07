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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.gen.ProxySubclassGenerator;
import org.apache.aries.proxy.impl.interfaces.InterfaceProxyGenerator;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.osgi.framework.Bundle;

public final class AsmProxyManager extends AbstractProxyManager implements ProxyManager
{
  public Object createNewProxy(Bundle clientBundle, Collection<Class<?>> classes, 
      Callable<Object> dispatcher, InvocationListener listener) throws UnableToProxyException
  {
    Object proxyObject = null;
    
    // if we just have interfaces and no classes we default to using
    // the interface proxy because we can't dynamically
    // subclass more than one interface
    // unless we have a class
    // that implements all of them

    // default to not subclass
    boolean useSubclassProxy = false;

    // loop through the classes checking if they are java interfaces
    // if we find any class that isn't an interface we need to use
    // the subclass proxy
    Set<Class<?>> notInterfaces = new HashSet<Class<?>>();
    for (Class<?> clazz : classes) {
      if (!!!clazz.isInterface()) {
        useSubclassProxy = true;
        notInterfaces.add(clazz);
      }
    }

    if (useSubclassProxy) {
      // if we need to use the subclass proxy then we need to find
      // the most specific class
      Class<?> classToProxy = null;
      int deepest = 0;
      // for each of the classes find out how deep it is in the
      // hierarchy
      for (Class<?> clazz : notInterfaces) {
        Class<?> nextHighestClass = clazz;
        int depth = 0;
        do {
          nextHighestClass = nextHighestClass.getSuperclass();
          depth++;
        } while (nextHighestClass != null);
        if (depth > deepest) {
          // if we find a class deeper than the one we already
          // had
          // it becomes the new most specific
          deepest = depth;
          classToProxy = clazz;
        }
      }
      if(WovenProxy.class.isAssignableFrom(classToProxy)) {
        try {
          Constructor<?> c = classToProxy.getDeclaredConstructor(Callable.class, 
              InvocationListener.class);
          c.setAccessible(true);
          proxyObject = c.newInstance(dispatcher, listener);
        } catch (Exception e) {
          //We will have to subclass this one, but we should always have a constructor
          //to use
          //TODO log that performance would be improved by using a non-null template
        }
      } 
      if(proxyObject == null){
        proxyObject = ProxySubclassGenerator.newProxySubclassInstance(classToProxy, new ProxyHandler(this, dispatcher, listener));
      }
    } else {
      proxyObject = InterfaceProxyGenerator.getProxyInstance(clientBundle, classes, dispatcher, listener);
    }

    return proxyObject;
  }
  
  @Override
  protected boolean isProxyClass(Class<?> clazz)
  {
    return WovenProxy.class.isAssignableFrom(clazz) || ProxySubclassGenerator.isProxySubclass(clazz) || Proxy.isProxyClass(clazz);
  }

  @Override
  protected InvocationHandler getInvocationHandler(Object proxy) 
  {
    Class<?> type = proxy.getClass();
    InvocationHandler ih = null;
    
    if (ProxySubclassGenerator.isProxySubclass(type)) {
      ih = ProxySubclassGenerator.getInvocationHandler(proxy);
    } else if (Proxy.isProxyClass(type)) {
      ih = Proxy.getInvocationHandler(proxy);
    }
    
    return ih;
  }
}