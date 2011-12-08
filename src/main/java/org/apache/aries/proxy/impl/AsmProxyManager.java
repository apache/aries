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
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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

    // loop through the classes checking if they are java interfaces
    // if we find any class that isn't an interface we need to use
    // the subclass proxy
    Set<Class<?>> notInterfaces = new HashSet<Class<?>>();
    Set<Class<?>> interfaces = new HashSet<Class<?>>();
    
    for (Class<?> clazz : classes) {
      if (!!!clazz.isInterface()) {
        notInterfaces.add(clazz);
      } else {
        interfaces.add(clazz);
      }
    }

    // if we just have no classes we default to using
    // the interface proxy because we can't dynamically
    // subclass more than one interface
    // unless we have a class
    // that implements all of them
    if (notInterfaces.isEmpty()) {
      proxyObject = InterfaceProxyGenerator.getProxyInstance(clientBundle, null, interfaces, dispatcher, listener);
    } else {
      // if we need to use the subclass proxy then we need to find
      // the most specific class
      Class<?> classToProxy = getLowestSubclass(notInterfaces);
      if(WovenProxy.class.isAssignableFrom(classToProxy)) {
        
        if(isConcrete(classToProxy) && implementsAll(classToProxy, interfaces)) {
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
        } else {
          //We need to generate a class that implements the interfaces (if any) and
          //has the classToProxy as a superclass
          if((classToProxy.getModifiers() & Modifier.FINAL) != 0) {
            throw new UnableToProxyException(classToProxy, "The class " + classToProxy
                + " does not implement all of the interfaces " + interfaces + 
                " and is final. This means that we cannot create a proxy for both the class and all of the requested interfaces.");
          }
          proxyObject = InterfaceProxyGenerator.getProxyInstance(clientBundle, 
              (Class<? extends WovenProxy>)classToProxy, interfaces, dispatcher, listener);
        }
      } 
      if(proxyObject == null){
        proxyObject = ProxySubclassGenerator.newProxySubclassInstance(classToProxy, new ProxyHandler(this, dispatcher, listener));
      }
    }

    return proxyObject;
  }

  private Class<?> getLowestSubclass(Set<Class<?>> notInterfaces) throws
       UnableToProxyException {
    
    Iterator<Class<?>> it = notInterfaces.iterator();
    
    Class<?> classToProxy = it.next();
    
    while(it.hasNext()) {
      Class<?> potential = it.next();
      if(classToProxy.isAssignableFrom(potential)) {
        //potential can be widened to classToProxy, and is therefore
        //a lower subclass
        classToProxy = potential;
      } else if (!!!potential.isAssignableFrom(classToProxy)){
        //classToProxy is not a subclass of potential - This is
        //an error, we can't be part of two hierarchies at once!
        throw new UnableToProxyException(classToProxy, "The requested classes "
            + classToProxy + " and " + potential + " are not in the same type hierarchy");
      }
    }
    return classToProxy;
  }
  
  private boolean isConcrete(Class<?> classToProxy) {
    
    return (classToProxy.getModifiers() & Modifier.ABSTRACT) == 0;
  }

  private boolean implementsAll(Class<?> classToProxy, Set<Class<?>> interfaces) {
    //If we can't widen to one of the interfaces then we need to do some more work
    for(Class<?> iface : interfaces) {
      if(!!!iface.isAssignableFrom(classToProxy))
        return false;
    }
    return true;
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