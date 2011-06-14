package org.apache.aries.proxy.impl.interfaces;

import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.common.AbstractWovenProxyAdapter;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;

/** An implementation of ClassLoader that will be used to define our proxy class */
final class ProxyClassLoader extends ClassLoader {
  
  private static final ProtectionDomain PROXY_PROTECTION_DOMAIN;
  
  static {
    PermissionCollection pc = new Permissions();
    pc.add(new AllPermission());
    PROXY_PROTECTION_DOMAIN = new ProtectionDomain(null, pc);
  }
  
  /** A {@link Map} of classes we already know */
  private final ConcurrentMap<HashSet<Class<?>>, String> classes = 
                new ConcurrentHashMap<HashSet<Class<?>>, String>();
  
  private final ConcurrentMap<String, Class<?>> locatedClasses = 
                new ConcurrentHashMap<String, Class<?>>();
  
  private final Set<Class<?>> ifaces = new HashSet<Class<?>>();
  
  private final ReadWriteLock ifacesLock = new ReentrantReadWriteLock();
  
  public ProxyClassLoader(Bundle bundle) {
    super(AriesFrameworkUtil.getClassLoader(bundle));
  }

  @Override
  protected Class<?> findClass(String className) {
    
    if(WovenProxy.class.getName().equals(className))
      return WovenProxy.class;
    else if (InvocationListener.class.getName().equals(className))
      return InvocationListener.class;
    else {
      Class<?> c = locatedClasses.get(className);
      if(c != null)
        return c;
      Lock rLock = ifacesLock.readLock();
      rLock.lock();
      try {
        Set<ClassLoader> cls = new HashSet<ClassLoader>();
        for(Class<?> iface : ifaces) {
          if(cls.add(iface.getClassLoader())) {
            try {
              c = Class.forName(className, false, iface.getClassLoader());
              locatedClasses.put(className, c);
              return c;
            } catch (ClassNotFoundException e) {
              // This is a no-op
            }
          }
        }
      } finally {
        rLock.unlock();
      }
    }
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
    
    Lock wLock = ifacesLock.writeLock();
    wLock.lock();
    try {
      ifaces.addAll(createSet);
    } finally {
      wLock.unlock();
    }
    
    className = "Proxy" + AbstractWovenProxyAdapter.getSanitizedUUIDString();
    
    InterfaceCombiningClassAdapter icca = new InterfaceCombiningClassAdapter(
        className, this, createSet);
    
    //Use a special protection domain that grants AllPermission to our Proxy
    //object. This is important so that we never get in the way of any security
    //checks. This isn't unsafe because we only add simple dispatch/listener code
    
    try {
      byte[] bytes = icca.generateBytes();
      Class<?> c = defineClass(className, bytes, 0, bytes.length, 
          PROXY_PROTECTION_DOMAIN);
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