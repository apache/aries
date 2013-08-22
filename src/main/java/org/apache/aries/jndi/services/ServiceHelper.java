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
package org.apache.aries.jndi.services;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.NamingException;

import org.apache.aries.jndi.url.Activator;
import org.apache.aries.jndi.url.OsgiName;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.util.nls.MessageUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

/**
 * This helper provides access to services registered in the OSGi service registry.
 * If a matching service cannot be located null may be returned. A caller should not
 * expect to get the same object if multiple requests are made to this API. A caller
 * should not expect to get a different object if multiple requests are made to this API.
 * A caller should avoid caching the returned service. OSGi is a dynamic environment and
 * the service may become unavailable while a reference to it is held. To minimize this
 * risk the caller should hold onto the service for the minimum length of time.
 * 
 * <p>This API should not be used from within an OSGi bundle. When in an OSGi environment
 *   the BundleContext for the bundle should be used to obtain the service.
 * </p>
 */
public final class ServiceHelper
{
  public static final class CacheClearoutListener implements BundleListener, ServiceListener
  {
    /** The cache to purge */
    private final ConcurrentMap<ServiceKey, WeakReference<Object>> cache;


    public CacheClearoutListener(ConcurrentMap<ServiceKey, WeakReference<Object>> pc)
    {
      cache = pc;
    }

    public void bundleChanged(BundleEvent event)
    {
      if (event.getType() == BundleEvent.STOPPED) {
        Bundle b = event.getBundle();
        Iterator<ServiceKey> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
          ServiceKey key = keys.next();
          if (key.requesting == b) keys.remove();
        }
      }
    }

    public void serviceChanged(ServiceEvent event)
    {
      if (event.getType() == ServiceEvent.UNREGISTERING) {
        ServiceReference ref = event.getServiceReference();
        Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
        Bundle registeringBundle = ref.getBundle();
        Iterator<ServiceKey> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
          ServiceKey key = keys.next();
          if (key.registering == registeringBundle && serviceId.equals(key.serviceId)) {
            keys.remove();
            break;
          }
        }
      }
    }

    public void add(final BundleContext ctx, ServiceKey k)
    {
      // try to use the system bundle for our listener, if that fails we fall back to the calling context
      BundleContext systemBundle = AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
        public BundleContext run()
        {
        	 Bundle system = ctx.getBundle(0); 
        	 return system == null ? null : system.getBundleContext(); 
        }
      });
      if (systemBundle == null) systemBundle = ctx;
      systemBundle.addBundleListener(cacheClearoutListener);
      systemBundle.addServiceListener(cacheClearoutListener);
    }
  }

  private static final class ServiceKey
  {
    private final Bundle requesting;
    private final Bundle registering;
    private final Long serviceId;
    private final int hash;

    public ServiceKey(Bundle owningBundle, Bundle registeringBundle, Long property)
    {
      requesting = owningBundle;
      registering = registeringBundle;
      serviceId = property;
      hash = serviceId.intValue() * 100003 + System.identityHashCode(requesting);
    }

    public int hashCode()
    {
      return hash;
    }

    public boolean equals(Object other)
    {
      if (other == this) return true;
      if (other == null) return false;

      if (other instanceof ServiceKey) {
        ServiceKey otherKey = (ServiceKey) other;
        return (otherKey.requesting == requesting && otherKey.serviceId.equals(serviceId));
      }

      return false;
    }
  }

  private static class JNDIServiceDamper implements Callable<Object>
  {
    private BundleContext ctx;
    private ServicePair pair;
    private String interfaceName;
    private String filter;
    private boolean dynamic;
    private int rebindTimeout;

    public JNDIServiceDamper(BundleContext context, String i, String f, ServicePair service,
        boolean d, int timeout)
    {
      ctx = context;
      pair = service;
      interfaceName = i;
      filter = f;
      dynamic = d;
      rebindTimeout = timeout;
    }

    public Object call() throws NamingException {
      if (pair == null || pair.ref.getBundle() == null) {
        if (dynamic) {
          pair = findService(ctx, interfaceName, filter);
          if (pair == null && rebindTimeout > 0) {
            long startTime = System.currentTimeMillis();
            try {
              while (pair == null && System.currentTimeMillis() - startTime < rebindTimeout) {
                Thread.sleep(100);
                pair = findService(ctx, interfaceName, filter);
              }
            } catch (InterruptedException e) {
            }
          }
        } else {
          pair = null;
        }
      }

      if (pair == null) {
        throw new ServiceException(interfaceName, ServiceException.UNREGISTERED);
      }
      return pair.service;
    }
  }

  private static class ServicePair
  {
    private ServiceReference ref;
    private Object service;
  }

  /** A cache of proxies returned to the client */
  private static final ConcurrentMap<ServiceKey, WeakReference<Object>> proxyCache = new ConcurrentHashMap<ServiceKey, WeakReference<Object>>();
  private static final CacheClearoutListener cacheClearoutListener = new CacheClearoutListener(proxyCache);
  private static final MessageUtil MESSAGES = MessageUtil.createMessageUtil(ServiceHelper.class, "org.apache.aries.jndi.nls.jndiUrlMessages");

  public static Object getService(BundleContext ctx, OsgiName lookupName, String id,
                                  boolean dynamicRebind, Map<String, Object> env, boolean requireProxy) throws NamingException
  {    
    String interfaceName = lookupName.getInterface();
    String filter = lookupName.getFilter();
    String serviceName = lookupName.getServiceName();

    if (id != null) {
      if (filter == null) {
        filter = '(' + Constants.SERVICE_ID + '=' + id + ')';
      } else {
        filter = "(&(" + Constants.SERVICE_ID + '=' + id + ')' + filter + ')';
      }
    }

    ServicePair pair = null;

    if (!!!lookupName.isServiceNameBased()) {
      pair = findService(ctx, interfaceName, filter);
    }

    if (pair == null) {
      interfaceName = null;
      if (id == null) {
        filter = "(" + JNDIConstants.JNDI_SERVICENAME + "=" + serviceName + ')';
      } else {
        filter = "(&(" + Constants.SERVICE_ID + '=' + id + ")(" + JNDIConstants.JNDI_SERVICENAME
            + "=" + serviceName + "))";
      }
      pair = findService(ctx, interfaceName, filter);
    }

    Object result = null;
    
    if (pair != null) {
      if (requireProxy) {
        Object obj = env.get(org.apache.aries.jndi.api.JNDIConstants.REBIND_TIMEOUT);
        int timeout = 0;
        if (obj instanceof String) {
          timeout = Integer.parseInt((String)obj);
        } else if (obj instanceof Integer) {
          timeout = (Integer)obj;
        }
        
        result = proxy(interfaceName, filter, dynamicRebind, ctx, pair, timeout);
      } else {
        result = pair.service;
      }
    }

    return result;
  }

  private static Object proxy(final String interface1, final String filter, final boolean rebind,
                              final BundleContext ctx, final ServicePair pair, final int timeout)
  {
    Object result = null;
    Bundle owningBundle = ctx.getBundle();
    ServiceKey k = new ServiceKey(owningBundle, pair.ref.getBundle(), (Long) pair.ref.getProperty(Constants.SERVICE_ID));

    WeakReference<Object> proxyRef = proxyCache.get(k);

    if (proxyRef != null) {
      result = proxyRef.get();
      if (result == null) {
        proxyCache.remove(k, proxyRef);
      }
    }

    if (result == null) {
      result = AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run()
        {
          return proxyPrivileged(interface1, filter, rebind, ctx, pair, timeout);
        }
      });

      proxyRef = new WeakReference<Object>(result);
      // if we have two threads doing a put and then clashing we ignore it. The code to ensure only
      // one wins is quite complex to save a few bytes of memory and millis of execution time.
      proxyCache.putIfAbsent(k, proxyRef);
      cacheClearoutListener.add(ctx, k);
    }

    return result;
  }

  private static Object proxyPrivileged(String interface1, String filter, boolean dynamicRebind, BundleContext ctx, ServicePair pair, int timeout)
  {
    String[] interfaces = null;
    if (interface1 != null) {
      interfaces = new String[] { interface1 };
    } else {
      interfaces = (String[]) pair.ref.getProperty(Constants.OBJECTCLASS);
    }

    List<Class<?>> clazz = new ArrayList<Class<?>>(interfaces.length);

    // We load the interface classes the service is registered under using the defining bundle. 
    // This is ok because the service must be able to see the classes to be registered using them. 
    // We then check to see if isAssignableTo on the reference  works for the owning bundle and 
    // the interface name and only use the interface if true is returned there.

    // This might seem odd, but equinox and felix return true for isAssignableTo if the 
    // Bundle provided does not import the package. This is under the assumption the
    // caller will then use reflection. The upshot of doing it this way is that a utility
    // bundle can be created which centralizes JNDI lookups, but the service will be used
    // by another bundle. It is true that class space consistency is less safe, but we
    // are enabling a slightly odd use case anyway.
    
    // August 13th 2013: We've found valid use cases in which a Bundle is exporting 
    // services that the Bundle itself cannot load. We deal with this rare case by
    // noting the classes that we failed to load. If as a result we have no classes 
    // to proxy, we try those classes again but instead pull the Class objects off 
    // the service rather than from the bundle exporting that service. 

    Bundle serviceProviderBundle = pair.ref.getBundle();
    Bundle owningBundle = ctx.getBundle();
    ProxyManager proxyManager = Activator.getProxyManager();

    Collection<String> classesNotFound = new ArrayList<String>();
    for (String interfaceName : interfaces) {
      try {
        Class<?> potentialClass = serviceProviderBundle.loadClass(interfaceName);
        if (pair.ref.isAssignableTo(owningBundle, interfaceName)) {
          clazz.add(potentialClass);
        }
      } catch (ClassNotFoundException e) {
      	classesNotFound.add(interfaceName);
      }
    }
    
    if (clazz.isEmpty() && !classesNotFound.isEmpty()) { 
			Class<?> ifacesOnService[] = ctx.getService(pair.ref).getClass().getInterfaces();
    	for (String interfaceName : classesNotFound) {
    		Class<?> thisClass = null;
    		for (Class<?> c : getAllInterfaces(ifacesOnService)) { 
    			if (c.getName().equals(interfaceName)) { 
    				thisClass = c;
    				break;
    			}
    		}
    		if (thisClass != null) { 
    			if (pair.ref.isAssignableTo(owningBundle, interfaceName)) {
    				clazz.add(thisClass);
    			}
    		}
    	}
    }
    
    if (clazz.isEmpty()) {
      throw new IllegalArgumentException(Arrays.asList(interfaces).toString());
    }

    Callable<Object> ih = new JNDIServiceDamper(ctx, interface1, filter, pair, dynamicRebind, timeout);

    // The ClassLoader needs to be able to load the service interface
    // classes so it needs to be
    // wrapping the service provider bundle. The class is actually defined
    // on this adapter.

    try {
      return proxyManager.createDelegatingProxy(serviceProviderBundle, clazz, ih, null);
    } catch (UnableToProxyException e) {
      throw new IllegalArgumentException(e);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(MESSAGES.getMessage("unable.to.create.proxy", pair.ref), e);
    }
  }

  private static ServicePair findService(BundleContext ctx, String interface1, String filter)
      throws NamingException
  {
    ServicePair p = null;

    try {
      ServiceReference[] refs = ctx.getServiceReferences(interface1, filter);

      if (refs != null) {
        // natural order is the exact opposite of the order we desire.
        Arrays.sort(refs, new Comparator<ServiceReference>() {
          public int compare(ServiceReference o1, ServiceReference o2)
          {
            return o2.compareTo(o1);
          }
        });

        for (ServiceReference ref : refs) {
          Object service = ctx.getService(ref);

          if (service != null) {
            p = new ServicePair();
            p.ref = ref;
            p.service = service;
            break;
          }
        }
      }

    } catch (InvalidSyntaxException e) {
      // If we get an invalid syntax exception we just ignore it. Null
      // will be returned which
      // is valid and that may result in a NameNotFoundException if that
      // is the right thing to do
    }

    return p;
  }

  public static ServiceReference[] getServiceReferences(BundleContext ctx, String interface1,
      String filter, String serviceName, Map<String, Object> env) throws NamingException
  {
    ServiceReference[] refs = null;

    try {
      refs = ctx.getServiceReferences(interface1, filter);

      if (refs == null || refs.length == 0) {
        refs = ctx.getServiceReferences(null, "(" + JNDIConstants.JNDI_SERVICENAME + "="
            + serviceName + ')');
      }
    } catch (InvalidSyntaxException e) {
      throw (NamingException) new NamingException(e.getFilter()).initCause(e);
    }

    if (refs != null) {
      // natural order is the exact opposite of the order we desire.
      Arrays.sort(refs, new Comparator<ServiceReference>() {
        public int compare(ServiceReference o1, ServiceReference o2)
        {
          return o2.compareTo(o1);
        }
      });
    }

    return refs;
  }

  public static Object getService(BundleContext ctx, ServiceReference ref)
  {
    Object service = ctx.getService(ref);

    Object result = null;

    if (service != null) {
      ServicePair pair = new ServicePair();
      pair.ref = ref;
      pair.service = service;

      result = proxy(null, null, false, ctx, pair, 0);
    }

    return result;
  }
 
  static Collection<Class<?>> getAllInterfaces (Class<?>[] baseInterfaces) 
  {
  	Set<Class<?>> result = new HashSet<Class<?>>();
  	for (Class<?> c : baseInterfaces) {
  		if (!c.equals(Object.class)) { 
  			result.add (c);
  			Class<?> ifaces[] = c.getInterfaces();
  			if (ifaces.length != 0) { 
  				result.addAll(getAllInterfaces(ifaces));
  			}
  		}
  	}
  	return result;
  }
}
