package org.apache.aries.jndi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

// TODO need to determine if this needs to be thread safe or not
public class ServicePair<T>
{
  private BundleContext ctx;
  private ServiceReference ref;
  private T serviceObject;
  
  public ServicePair(BundleContext context, ServiceReference serviceRef)
  {
    ctx = context;
    ref = serviceRef;
  }
  
  public ServicePair(BundleContext context, ServiceReference serviceRef, T service)
  {
    ctx = context;
    ref = serviceRef;
    serviceObject = service;
  }
  
  @SuppressWarnings("unchecked")
  public T get()
  {
    if (serviceObject == null && ref.getBundle() != null) {
      serviceObject = (T) ctx.getService(ref);
    }
    
    return serviceObject;
  }
  
  public void unget()
  {
    if (serviceObject != null) {
      ctx.ungetService(ref);
      serviceObject = null;
    }
  }
}
