package org.apache.aries.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public interface InvocationHandlerWrapper 
{
  public Object invoke(Object proxy, Method m, Object[] args, InvocationHandler delegate) throws Throwable;
}