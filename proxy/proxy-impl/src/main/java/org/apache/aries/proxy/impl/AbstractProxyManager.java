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

import static java.lang.String.format;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public abstract class AbstractProxyManager implements ProxyManager
{
  public final Object createDelegatingProxy(Bundle clientBundle, Collection<Class<?>> classes,
      Callable<Object> dispatcher, Object template) 
    throws UnableToProxyException
  {
    return createDelegatingInterceptingProxy(clientBundle, classes, dispatcher, template, null);
  }
  
  public Object createInterceptingProxy(Bundle clientBundle,
      Collection<Class<?>> classes, Object delegate, InvocationListener listener)
      throws UnableToProxyException {
    
    if (delegate instanceof WovenProxy) {
      WovenProxy proxy = ((WovenProxy) delegate).
              org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance(
              new SingleInstanceDispatcher(delegate), listener);
      return proxy;
    } else {
      return createDelegatingInterceptingProxy(clientBundle, classes, 
          new SingleInstanceDispatcher(delegate), delegate, listener);
    }
  }

  public final Object createDelegatingInterceptingProxy(Bundle clientBundle, Collection<Class<?>> classes,
      Callable<Object> dispatcher, Object template, InvocationListener listener)
      throws UnableToProxyException 
  {
    if(dispatcher == null)
      throw new NullPointerException("You must specify a dipatcher");
    
    if (template instanceof WovenProxy) {
      WovenProxy proxy = ((WovenProxy) template).
             org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance(
             dispatcher, listener);
      return proxy;
    }
    
    Object proxyObject = duplicateProxy(classes, dispatcher, template, listener);
    
    if (proxyObject == null) {
      proxyObject = createNewProxy(clientBundle, classes, dispatcher, listener);
    }
    
    return proxyObject;
  }
   
  public final Callable<Object> unwrap(Object proxy) 
  {
    Callable<Object> target = null;
    
    if(proxy instanceof WovenProxy) {
      //Woven proxies are a bit different, they can be proxies without
      //having a dispatcher, so we fake one up if we need to 
      
      WovenProxy wp = (WovenProxy) proxy;
      if(wp.org_apache_aries_proxy_weaving_WovenProxy_isProxyInstance()) {
        target = wp.org_apache_aries_proxy_weaving_WovenProxy_unwrap();
        if(target == null) {
          target = new SingleInstanceDispatcher(proxy);
        }
      }
    } else {
      InvocationHandler ih = getInvocationHandler(proxy);
      
      if (ih instanceof ProxyHandler) {
        target = ((ProxyHandler)ih).getTarget();
      }
    }
    return target;
  }
  
  public final boolean isProxy(Object proxy)
  {
    return (proxy != null && 
        ((proxy instanceof WovenProxy && ((WovenProxy)proxy).org_apache_aries_proxy_weaving_WovenProxy_isProxyInstance()) || 
        getInvocationHandler(proxy) instanceof ProxyHandler));
  }
  
  protected abstract Object createNewProxy(Bundle clientBundle, Collection<Class<?>> classes,
      Callable<Object> dispatcher, InvocationListener listener) throws UnableToProxyException;
  protected abstract InvocationHandler getInvocationHandler(Object proxy);
  protected abstract boolean isProxyClass(Class<?> clazz);

  protected synchronized ClassLoader getClassLoader(final Bundle clientBundle, Collection<Class<?>> classes) 
  {
    if (clientBundle != null && clientBundle.getState() == Bundle.UNINSTALLED) {
      throw new IllegalStateException(format("The bundle %s at version %s with id %d has been uninstalled.", 
                                             clientBundle.getSymbolicName(), clientBundle.getVersion(), clientBundle.getBundleId()));
    }
    
    ClassLoader cl = null;
    
    if (classes.size() == 1) cl = classes.iterator().next().getClassLoader();

    if (cl == null) {
        cl = getWiringClassloader(clientBundle);
    }
    
    return cl;
  }

  private ClassLoader getWiringClassloader(final Bundle bundle) {
    BundleWiring wiring = bundle != null ? bundle.adapt(BundleWiring.class) : null;
    return wiring != null ? wiring.getClassLoader() : null;
  }

  private Object duplicateProxy(Collection<Class<?>> classes, Callable<Object> dispatcher, 
      Object template, InvocationListener listener)
  {
    Object proxyObject = null;
    Class<?> classToProxy = null;
    
    if (template != null) {
      if(isProxyClass(template.getClass()))
        classToProxy = template.getClass();
    } else if (classes.size() == 1) {

      classToProxy = classes.iterator().next();

      if(!!!isProxyClass(classToProxy))
        classToProxy = null;
    }

    if (classToProxy != null) {
      try {
        /*
         * the class is already a proxy, we should just invoke
         * the constructor to get a new instance of the proxy
         * with a new Collaborator using the specified delegate
         */
        if(WovenProxy.class.isAssignableFrom(classToProxy)) {
          Constructor<?> c = classToProxy.getDeclaredConstructor(Callable.class, 
              InvocationListener.class);
          c.setAccessible(true);
          proxyObject = c.newInstance(dispatcher, listener);
        } else {
          proxyObject = classToProxy.getConstructor(InvocationHandler.class).
          newInstance(new ProxyHandler(this, dispatcher, listener));
        }
      } catch (InvocationTargetException e) {
      } catch (NoSuchMethodException e) {
      } catch (InstantiationException e) {
      } catch (IllegalArgumentException e) {
      } catch (SecurityException e) {
      } catch (IllegalAccessException e) {
      }
    }
    
    return proxyObject;
  }
}