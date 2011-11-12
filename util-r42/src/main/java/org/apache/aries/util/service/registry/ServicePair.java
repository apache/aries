/**
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
package org.apache.aries.util.service.registry;

import java.security.AccessController;
import java.security.PrivilegedAction;

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
      serviceObject = AccessController.doPrivileged(new PrivilegedAction<T>() {
          public T run()
          {
            return serviceObject = (T) ctx.getService(ref);
          }
        });
    }
    
    return serviceObject;
  }
  
  public boolean isValid() {
    return (ref.getBundle() != null);
  }

  public void unget()
  {
    if (serviceObject != null) {
      ctx.ungetService(ref);
      serviceObject = null;
    }
  }

  public ServiceReference getReference() 
  {
    return ref;
  }
}
