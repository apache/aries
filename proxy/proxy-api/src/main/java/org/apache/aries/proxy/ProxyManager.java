package org.apache.aries.proxy;

public interface ProxyManager 
{
  public ProxyFactory createProxyFactory();
  public ProxyFactory createProxyFactory(boolean interfaceProxyingOnly);
  public Object unwrap(Object proxy);
}