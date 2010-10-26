package org.apache.aries.proxy;

import java.util.concurrent.Callable;

import org.osgi.framework.Bundle;

public interface ProxyFactory 
{
  public Object createProxy(Bundle clientBundle, Class[] classes, Callable<Object> dispatcher);
  public Object createProxy(Bundle clientBundle, Class[] classes, Callable<Object> dispatcher, InvocationHandlerWrapper wrapper);
  public boolean isProxy(Object proxy);
}