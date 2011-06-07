package org.apache.aries.proxy.impl.interfaces;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.aries.util.BundleToClassLoaderAdapter;
import org.osgi.framework.Bundle;

/** An implementation of ClassLoader that will be used to define our proxy class */
final class ProxyClassLoader extends ClassLoader {
  
  /** A {@link Map} of classes we already know */
  private final ConcurrentMap<HashSet<Class<?>>, String> classes = 
                new ConcurrentHashMap<HashSet<Class<?>>, String>();
  
  public ProxyClassLoader(Bundle bundle) {
    super(AriesFrameworkUtil.getClassLoader(bundle));
  }

  @Override
  protected Class<?> findClass(String className) {
    
    if(WovenProxy.class.getName().equals(className))
      return WovenProxy.class;
    else if (InvocationListener.class.getName().equals(className))
      return InvocationListener.class;
    else 
      return null;
  }

  public Class<?> createProxyClass(HashSet<Class<?>> createSet) throws UnableToProxyException {
    
    String className = classes.get(createSet);
    
    if(className != null) {
      try {
        return Class.forName(className, false, this);
      } catch (ClassNotFoundException cnfe) {
        //This is odd, but we should be able to recreate the class, continue
        classes.remove(createSet);
      }
    }
    
    className = "Proxy" + AbstractWovenProxyAdapter.getSanitizedUUIDString();
    
    InterfaceCombiningClassAdapter icca = new InterfaceCombiningClassAdapter(
        className, this, createSet);
    
    //Use the protection domain of the first interface in the set
    try {
      byte[] bytes = icca.generateBytes();
      ProtectionDomain pd = createSet.iterator().next().getProtectionDomain();
      Class<?> c = defineClass(className, bytes, 0, bytes.length, pd);
      String old = classes.putIfAbsent(createSet, className);
      if(old != null) {
        c = Class.forName(className, false, this);
      }
      return c;
    } catch (ClassFormatError cfe) {
      throw new UnableToProxyException(createSet.iterator().next(), cfe);
    } catch (ClassNotFoundException e) {
      throw new UnableToProxyException(createSet.iterator().next(), e);
    }
  }
}