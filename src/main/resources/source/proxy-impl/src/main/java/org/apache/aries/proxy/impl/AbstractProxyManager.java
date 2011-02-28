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
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationHandlerWrapper;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.aries.util.nls.MessageUtil;
import org.osgi.framework.Bundle;

public abstract class AbstractProxyManager implements ProxyManager
{
  public final Object createProxy(Bundle clientBundle, Collection<Class<?>> classes,
      Callable<Object> dispatcher) 
    throws UnableToProxyException
  {
    return createProxy(clientBundle, classes, dispatcher, null);
  }

  public final Object createProxy(Bundle clientBundle, Collection<Class<?>> classes,
      Callable<Object> dispatcher, InvocationHandlerWrapper wrapper)
      throws UnableToProxyException 
  {
    InvocationHandler ih = new ProxyHandler(this, dispatcher, wrapper);
    Object proxyObject = duplicateProxy(classes, ih);
    
    if (proxyObject == null) {
      proxyObject = createNewProxy(clientBundle, classes, ih);
    }
    
    return proxyObject;
  }
  
  public final Callable<Object> unwrap(Object proxy) 
  {
    Callable<Object> target = null;
    
    if (isProxy(proxy)) {
      InvocationHandler ih = getInvocationHandler(proxy);
      
      if (ih instanceof ProxyHandler) {
        target = ((ProxyHandler)ih).getTarget();
      }
    }
    
    return target;
  }
  
  public final boolean isProxy(Object proxy)
  {
    return (getInvocationHandler(proxy) instanceof ProxyHandler);
  }
  
  protected abstract Object createNewProxy(Bundle clientBundle, Collection<Class<?>> classes,
      InvocationHandler ih) throws UnableToProxyException;
  protected abstract InvocationHandler getInvocationHandler(Object proxy);
  protected abstract boolean isProxyClass(Class<?> clazz);

  protected synchronized ClassLoader getClassLoader(final Bundle clientBundle, Collection<Class<?>> classes) 
  {
    if (clientBundle.getState() == Bundle.UNINSTALLED) {
      throw new IllegalStateException(NLS.MESSAGES.getMessage("bundle.uninstalled", clientBundle.getSymbolicName(), clientBundle.getVersion(), clientBundle.getBundleId()));
    }
    
    ClassLoader cl = null;
    
    if (classes.size() == 1) cl = classes.iterator().next().getClassLoader();

    if (cl == null) {
      // First of all see if the AriesFrameworkUtil can get the classloader, if it can we go with that.
      cl = AriesFrameworkUtil.getClassLoaderForced(clientBundle);
    }
    
    return cl;
  }

  private Object duplicateProxy(Collection<Class<?>> classes, InvocationHandler handler)
  {
    Object proxyObject = null;
    
    if (classes.size() == 1) {

      Class<?> classToProxy = classes.iterator().next();

      boolean isProxy = isProxyClass(classToProxy);

      if (isProxy) {
        try {
          /*
           * the class is already a proxy, we should just invoke
           * the constructor to get a new instance of the proxy
           * with a new Collaborator using the specified delegate
           */
          proxyObject = classToProxy.getConstructor(InvocationHandler.class).newInstance(handler);
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalArgumentException e) {
        } catch (SecurityException e) {
        } catch (IllegalAccessException e) {
        }
      }
    }
    
    return proxyObject;
  }
}