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
package org.apache.aries.jndi.url;

import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.aries.jndi.services.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ServiceRegistryListContext extends AbstractServiceRegistryContext implements Context
{
  /** The osgi lookup name **/
  private OsgiName parentName;
  
  private interface ThingManager<T>
  {
    public T get(BundleContext ctx, ServiceReference ref);
    public void release(BundleContext ctx, ServiceReference ref);
  }
  
  private static class ServiceNamingEnumeration<T> implements NamingEnumeration<T>
  {
    private BundleContext ctx;
    private ServiceReference[] refs;
    private int position = 0;
    private ThingManager<T> mgr;
    private T last;
    
    private ServiceNamingEnumeration(BundleContext context, ServiceReference[] theRefs, ThingManager<T> manager)
    {
      ctx = context;
      refs = theRefs;
      mgr = manager;
    }
    
    public void close() throws NamingException
    {
      mgr.release(ctx, refs[position - 1]);
      last = null;
    }

    public boolean hasMore() throws NamingException
    {
      return hasMoreElements();
    }

    public T next() throws NamingException
    {
      return nextElement();
    }

    public boolean hasMoreElements()
    {
      return position < refs.length;
    }

    public T nextElement()
    {
      if (!!!hasMoreElements()) throw new NoSuchElementException();
      
      if (position > 0) mgr.release(ctx, refs[position - 1]);
      
      last = mgr.get(ctx, refs[position++]);
      
      return last;
    }
    
  }
  
  public ServiceRegistryListContext(Map<String, Object> env, OsgiName validName)
  {
    super(env);
    parentName = validName;
  }

  public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
  {
    return list(name.toString());
  }

  public NamingEnumeration<NameClassPair> list(String name) throws NamingException
  {
    if (!!!"".equals(name)) throw new NameNotFoundException(name);
    
    final BundleContext ctx = ServiceHelper.getBundleContext(env);
    final ServiceReference[] refs = ServiceHelper.getServiceReferences(parentName.getInterface(), parentName.getFilter(), parentName.getServiceName(), env);
    
    return new ServiceNamingEnumeration<NameClassPair>(ctx, refs, new ThingManager<NameClassPair>() {
      public NameClassPair get(BundleContext ctx, ServiceReference ref)
      {
        String serviceId = String.valueOf(ref.getProperty(Constants.SERVICE_ID));
        String className = null;
        Object service = ctx.getService(ref);
        if (service != null) {
          className = service.getClass().getName();
        }

        ctx.ungetService(ref);
        
        return new NameClassPair(serviceId, className, true);
      }

      public void release(BundleContext ctx, ServiceReference ref)
      {
      }
    });
  }

  public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
  {
    return listBindings(name.toString());
  }

  public NamingEnumeration<Binding> listBindings(String name) throws NamingException
  {
    if (!!!"".equals(name)) throw new NameNotFoundException(name);
    
    final BundleContext ctx = ServiceHelper.getBundleContext(env);
    final ServiceReference[] refs = ServiceHelper.getServiceReferences(parentName.getInterface(), parentName.getFilter(), parentName.getServiceName(), env);

    return new ServiceNamingEnumeration<Binding>(ctx, refs, new ThingManager<Binding>() {
      public Binding get(BundleContext ctx, ServiceReference ref)
      {
        String serviceId = String.valueOf(ref.getProperty(Constants.SERVICE_ID));
        
        Object service = ServiceHelper.getService(ctx, ref);

        return new Binding(serviceId, service, true);
      }

      public void release(BundleContext ctx, ServiceReference ref)
      {
        ctx.ungetService(ref);
      }
    });
  }

  public Object lookup(Name name) throws NamingException
  {
    return lookup(name.toString());
  }

  public Object lookup(String name) throws NamingException
  {
    Object result = null;
    
    result = ServiceHelper.getService(parentName.getInterface(), parentName.getFilter(), parentName.getServiceName(), name, false, env);
    
    if (result == null) {
      throw new NameNotFoundException(name.toString());
    }
    
    return result;
  }
}