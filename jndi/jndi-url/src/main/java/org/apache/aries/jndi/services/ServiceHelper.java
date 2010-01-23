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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

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
  /** The bundle context used for service registry queries */
  private static BundleContext context;
  /** A cache of what service was returned last time the query was performed */
  private static ConcurrentMap<ServiceKey, Set<ServiceReference>> cache = new ConcurrentHashMap<ServiceKey, Set<ServiceReference>>();

  public static void setBundleContext(BundleContext ctx)
  {
    context = ctx;
  }
  
  /**
   * This class is used as the key into the cache. It holds information to identify 
   * who performed the query, along with the className and filter used. The thread context
   * class loader is used in the key, so two different modules will potentially get different
   * services.
   */
  private static final class ServiceKey
  {
    /** The class loader of the invoking application */
    private ClassLoader classLoader;
    /** The name of the class being queried from the registry */
    private String className;
    /** the registry filter, this may be null */
    private String filter;
    /** The cached hashCode */
    private final int hashCode;

    /**
     * Boring unimportant comment.
     * 
     * @param cl
     * @param cn
     * @param f
     */
    public ServiceKey(ClassLoader cl, String cn, String f)
    {
      classLoader = cl;
      className = cn;
      filter = f;
      
      int classNameHash = (className == null) ? 0 : className.hashCode();
      hashCode = System.identityHashCode(classLoader) * 1000003 + classNameHash;
    }

    @Override
    public int hashCode()
    {
      return hashCode;
    }

    @Override
    public boolean equals(Object other)
    {
      if (other == this) return true;
      if (other == null) return false;

      if (other instanceof ServiceKey) {
        ServiceKey otherKey = (ServiceKey) other;
        if (hashCode != otherKey.hashCode) return false;

        if (classLoader != otherKey.classLoader) return false;
        if (!!!comparePossiblyNullObjects(className, otherKey.className)) return false;
        return comparePossiblyNullObjects(filter, otherKey.filter);
      }

      return false;
    }
    
    /**
     * Compares two objects where one or other (or both) may be null.
     * 
     * @param a the first object to compare.
     * @param b the second object to compare.
     * @return true if they are ==, both null or identity equals, false otherwise.
     */
    public boolean comparePossiblyNullObjects(Object a, Object b) {
      if (a == b) return true;
      else if (a == null) return false;
      else return a.equals(b);
    }
  }

  /**
   * This method is used to obtain a single instance of a desired service from the OSGi
   * service registry. If the filter and class name identify multiple services the first
   * one is returned. If no service is found null will be returned.
   * 
   * @param className The class name used to register the desired service. If null is provided
   *                  then all services are eligible to be returned.
   * @param filter    An RFC 1960 query into the properties of the registered services. e.g.
   *                  (service.description=really useful)
   * @return          The desired service
   * 
   * @throws IllegalArgumentException If the filter is not valid. See RFC 1960 to work out what 
   *                                  it should be.
   */
  public static Object getService(String className, String filter) throws IllegalArgumentException
  {
    Object service = null;
    try {
      BundleContext callerCtx = getBundleContext();
      ServiceReference[] refs = callerCtx.getServiceReferences(className, filter);
      
      if (refs != null) {
        // we need to sort the references returned in case they are out of order
        // we need to sort in the reverse natural order, services with higher 
        // ranking or lower id should be processed first so should be earlier in the array.
        Arrays.sort(refs, new Comparator<ServiceReference>() {
          public int compare(ServiceReference o1, ServiceReference o2)
          {
            return o2.compareTo(o1);
          }
        });
        
        for (ServiceReference ref : refs) {
          List<Object> services = getServices(callerCtx, className, filter, ref);
          if (!!!services.isEmpty()) {
            service = services.get(0);
            break;
          }
        }
      }      
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
    
    return service;
  }
  
  /**
   * This method is used to obtain a list of service instances from the OSGi
   * service registry. If no service is found an empty list will be returned.
   * 
   * @param className The class name used to register the desired service. If null is provided
   *                  then all services are eligible to be returned.
   * @param filter    An RFC 1960 query into the properties of the registered services. e.g.
   *                  (service.description=really useful)
   * @return          A list of matching services.
   * 
   * @throws IllegalArgumentException If the filter is not valid. See RFC 1960 to work out what 
   *                                  it should be.
   */
  public static List<?> getServices(String className, String filter)
      throws IllegalArgumentException
  {
    List<Object> services;
    try {
      BundleContext callerCtx = getBundleContext();
      ServiceReference[] refs = callerCtx.getAllServiceReferences(className, filter);
      
      services = getServices(callerCtx, className, filter, refs);
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
    
    return services;
  }
  
  /**
   * @return the bundle context for the caller.
   */
  private static BundleContext getBundleContext()
  {
    BundleContext result = null;
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    while (result == null && cl != null) {
      if (cl instanceof BundleReference) {
        result = ((BundleReference)cl).getBundle().getBundleContext();
      } else if (cl != null) {
        cl = cl.getParent();
      }
    } 
    
    if (result == null) result = context;
    return result;
  }

  /**
   * This worker method obtains the requested service(s) and if the service(s) 
   * exist updates the cache and releases the previous service(s).
   * 
   * @param callerCtx The caller context.
   * @param className The class name used to query for the service.
   * @param filter    The filter name used to query for the service.
   * @param refs      The references to get.
   * @return          The service, if one was found, or null.
   */
  private static List<Object> getServices(BundleContext callerCtx, String className, String filter, ServiceReference...refs)
  {
    List<Object> data = new LinkedList<Object>();
    
    if (refs != null) {
      Set<ServiceReference> refSet = new HashSet<ServiceReference>();
      for (ServiceReference ref : refs) {
        Object service = callerCtx.getService(ref);
        if (service != null) {
          data.add(service);
          refSet.add(ref);
        }
      }
      
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      ServiceKey key = new ServiceKey(cl, className, filter);
      
      // we do not need any synchronization around this. The map is concurrent
      // and until here we do not touch any shared state.
      refSet = cache.put(key, refSet);
      
      if (refSet != null) {
        for (ServiceReference ref : refSet) {
          callerCtx.ungetService(ref);
        }
      }
    }
    
    return data;
  }
}
