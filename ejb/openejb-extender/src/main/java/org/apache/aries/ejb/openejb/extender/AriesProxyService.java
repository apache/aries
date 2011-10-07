/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.openejb.extender;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.util.proxy.InvocationHandler;
import org.apache.openejb.util.proxy.ProxyFactory;
import org.osgi.framework.BundleContext;

public class AriesProxyService implements ProxyFactory, SingleServiceListener {

  private static class NoProxySupportException extends RuntimeException { 
    public NoProxySupportException() {
      super("No Proxy support is available");
    }
  }
  
  private static final class InvocationHandlerProxy implements Callable<Object>, InvocationListener {

    private final InvocationHandler handler;
    
    private final Map<Thread, Class<?>> invocations = new ConcurrentHashMap<Thread, Class<?>>();
    private final ConcurrentMap<Class<?>, Object> proxys = new ConcurrentHashMap<Class<?>, Object>();

    public InvocationHandlerProxy(InvocationHandler handler) {
      this.handler = handler;
    }

    public InvocationHandler getHandler() {
      return handler;
    }

    public void postInvoke(Object arg0, Object arg1, Method arg2, Object arg3)
        throws Throwable {
      // No op
    }

    public void postInvokeExceptionalReturn(Object arg0, Object arg1,
        Method arg2, Throwable arg3) throws Throwable {
      //No op
    }

    public Object preInvoke(Object arg0, Method arg1, Object[] arg2)
        throws Throwable {
      invocations.put(Thread.currentThread(), arg1.getDeclaringClass());
      return null;
    }

    public Object call() throws Exception {
      Class<?> c = invocations.remove(Thread.currentThread());
      if(c == null)
        throw new IllegalStateException("Unable to establish any context");
      else if (c.equals(Object.class)) {
        //This is a toString or similar, just use an interface we know
        //we can see and that doesn't have any methods on it :)
        c = Serializable.class;
      }
      
      Object proxy = proxys.get(c);
      
      if(proxy == null) {
        Object tmp = Proxy.newProxyInstance(c.getClassLoader(), new Class[] {c}, handler);
        proxy = proxys.putIfAbsent(c, tmp);
        if(proxy == null)
          proxy = tmp;
      }
      return proxy;
    }
    
  }
  
  private static class InnerProxyDelegator implements Callable<Object> {

    private final Object delegate;
    
    public InnerProxyDelegator(Object delegate) {
      this.delegate = delegate;
    }
    
    public Object call() {
      return delegate;
    }
  }
  
  private final Map<Class<?>, Object> proxies = Collections.synchronizedMap(
      new WeakHashMap<Class<?>, Object>());
  
  private final SingleServiceTracker<ProxyManager> proxyTracker;
  
  private AriesProxyService(BundleContext ctx) {
    proxyTracker = new SingleServiceTracker<ProxyManager>(ctx, ProxyManager.class, this);
    proxyTracker.open();
  }
  
  private final AtomicReference<ProxyManager> manager = 
    new AtomicReference<ProxyManager>();
  
  private static final AtomicReference<AriesProxyService> INSTANCE = 
    new AtomicReference<AriesProxyService>();
  
  private final ProxyManager getManager() {
    ProxyManager pManager = manager.get();
    
    if(pManager == null) {
      throw new NoProxySupportException();
    }
    return pManager;
  }

  public static AriesProxyService get() {
    return INSTANCE.get();
  }
  
  public static void init(BundleContext ctx) {
    AriesProxyService oTM = new AriesProxyService(ctx);
    if(!!!INSTANCE.compareAndSet(null, oTM))
      oTM.destroy();
  }
  
  public void destroy() {
    INSTANCE.set(null);
    proxyTracker.close();
  }
  
  public void serviceFound() {
    update();
  }

  public void serviceLost() {
    update();
  }

  public void serviceReplaced() {
    update();
  }
  
  private void update() {
    manager.set(proxyTracker.getService());
  }
  
  
  
  public InvocationHandler getInvocationHandler(Object arg0)
      throws IllegalArgumentException {
    Callable<Object> unwrapped = getManager().unwrap(arg0);
    
    if(unwrapped instanceof InnerProxyDelegator) {
      unwrapped = getManager().unwrap(((InnerProxyDelegator)unwrapped).call());
    }
    
    if(unwrapped instanceof InvocationHandlerProxy) {
      return ((InvocationHandlerProxy) unwrapped).getHandler();
    }
    return null;
  }

  public Class getProxyClass(Class iface) throws IllegalArgumentException {
    if(iface == null || !!!iface.isInterface())
      throw new IllegalArgumentException("Not an interface " + iface);
    return newProxyInstance(iface, null).getClass();
  }

  public Class getProxyClass(Class[] ifaces) throws IllegalArgumentException {
    if(ifaces == null || ifaces.length == 0)
      throw new IllegalArgumentException("No interfaces.");
    
    for(Class iface : ifaces) {
      if (!!!iface.isInterface())
        throw new IllegalArgumentException("Not an interface " + iface + " in " + Arrays.toString(ifaces));
    }
    
    return newProxyInstance(ifaces, null).getClass();
  }

  public void init(Properties arg0) throws OpenEJBException {
    //No op
  }

  public boolean isProxyClass(Class arg0) {
    return proxies.containsKey(arg0);
  }

  public Object newProxyInstance(Class iface, InvocationHandler arg1)
      throws IllegalArgumentException {
    return newProxyInstance(new Class[] {iface}, arg1);
  }

  public Object newProxyInstance(Class[] ifaces, InvocationHandler arg1)
      throws IllegalArgumentException {
    InvocationHandlerProxy ihProxy = new InvocationHandlerProxy(arg1);
    List<Class<?>> classes = new ArrayList<Class<?>>();
    for(Class<?> iface : ifaces)
      classes.add(iface);
    try {
      Object inner = getManager().createDelegatingProxy(null, classes, ihProxy, null);
      
      Object proxy = getManager().createDelegatingInterceptingProxy(null, classes, 
          new InnerProxyDelegator(inner), null, ihProxy);
      proxies.put(proxy.getClass(), null);
      return proxy;
    } catch (UnableToProxyException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
