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
package org.apache.aries.util.tracker;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class SingleServiceTracker<T> 
{
  public static interface SingleServiceListener
  {
    public void serviceFound();
    public void serviceLost();
    public void serviceReplaced();
  }
  
  private final BundleContext ctx;
  private final String className;
  private final AtomicReference<T> service = new AtomicReference<T>();
  private final AtomicReference<ServiceReference> ref = new AtomicReference<ServiceReference>();
  private final AtomicBoolean open = new AtomicBoolean(false);
  private final SingleServiceListener serviceListener;
  private String filterString;
  private boolean isCustomFilter;

  private final ServiceListener listener = new ServiceListener()
  {
    public void serviceChanged(ServiceEvent event) 
    {
      if (open.get()) {
        if (event.getType() == ServiceEvent.UNREGISTERING) {
          ServiceReference deadRef = event.getServiceReference();
          if (deadRef.equals(ref.get())) {
            findMatchingReference(deadRef);
          }
        } else if (event.getType() == ServiceEvent.REGISTERED && ref.get() == null) {
          findMatchingReference(null);
        }
      }
    }
  };
  
  public SingleServiceTracker(BundleContext context, Class<T> clazz, SingleServiceListener sl)
  {
    ctx = context;
    this.className = clazz.getName();
    serviceListener = sl;
    this.filterString = '(' + Constants.OBJECTCLASS + '=' + className + ')';
  }
  
  public SingleServiceTracker(BundleContext context, Class<T> clazz, String filterString, SingleServiceListener sl) throws InvalidSyntaxException
  {
    this(context, clazz, sl);
    if (filterString != null) {
    	this.filterString = "(&" + this.filterString + filterString + ')';
    	isCustomFilter = true;
    }
    FrameworkUtil.createFilter(this.filterString);
  }
  
  public T getService()
  {
    return service.get();
  }
  
  public ServiceReference getServiceReference()
  {
    return ref.get();
  }
  
  public void open()
  {
    if (open.compareAndSet(false, true)) {
      try {
        ctx.addServiceListener(listener, filterString);
        findMatchingReference(null);
      } catch (InvalidSyntaxException e) {
        // this can never happen. (famous last words :)
      }
    }
  }

  private void findMatchingReference(ServiceReference original) {
    boolean clear = true;
    ServiceReference ref;
    if(isCustomFilter) {
      try {
        ServiceReference[] refs = ctx.getServiceReferences(className, filterString);
        if(refs == null || refs.length == 0) {
          ref = null;
        } else {
    	  ref = refs[0];
        }
      } catch (InvalidSyntaxException e) {
        //This can't happen, we'd have blown up in the constructor
        ref = null;
      }
    } else {
	  ref = ctx.getServiceReference(className);
    }
    if (ref != null) {
      T service = (T) ctx.getService(ref);
      if (service != null) {
        clear = false;
        
        // We do the unget out of the lock so we don't exit this class while holding a lock.
        if (!!!update(original, ref, service)) {
          ctx.ungetService(ref);
        }
      }
    } else if (original == null){
      clear = false;
    }
    
    if (clear) {
      update(original, null, null);
    }
  }
  
  private boolean update(ServiceReference deadRef, ServiceReference newRef, T service)
  {
    boolean result = false;
    int foundLostReplaced = -1;

    // Make sure we don't try to get a lock on null
    Object lock;
    
    // we have to choose our lock.
    if (newRef != null) lock = newRef;
    else if (deadRef != null) lock = deadRef;
    else lock = this;
    
    // This lock is here to ensure that no two threads can set the ref and service
    // at the same time. 
    synchronized (lock) {
      if (open.get()) {
        result = this.ref.compareAndSet(deadRef, newRef);
        if (result) {
          this.service.set(service);

          if (deadRef == null && newRef != null) foundLostReplaced = 0;
          if (deadRef != null && newRef == null) foundLostReplaced = 1;
          if (deadRef != null && newRef != null) foundLostReplaced = 2;
        }
      }
    }

    if (serviceListener != null) {
      if (foundLostReplaced == 0) serviceListener.serviceFound();
      else if (foundLostReplaced == 1) serviceListener.serviceLost();
      else if (foundLostReplaced == 2) serviceListener.serviceReplaced();
    }

    return result;
  }

  public void close()
  {
    if (open.compareAndSet(true, false)) {
      ctx.removeServiceListener(listener);
      
      synchronized (this) {
        ServiceReference deadRef = ref.getAndSet(null);
        service.set(null);
        if (deadRef != null) ctx.ungetService(deadRef);
      }
    }
  }
}