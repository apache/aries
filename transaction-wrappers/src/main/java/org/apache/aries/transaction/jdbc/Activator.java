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
package org.apache.aries.transaction.jdbc;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import java.util.Hashtable;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

public class Activator implements BundleActivator, ServiceTrackerCustomizer, ServiceListener
{
  private TransactionManager tm;
  private ServiceTracker t;
  private ServiceReference ref;
  private BundleContext context;
  
  public void start(BundleContext ctx)
  {
    context = ctx;
    
    t = new ServiceTracker(ctx, javax.sql.XADataSource.class.getName(), this);
    
    try {
      ctx.addServiceListener(this, "(objectClass=javax.transaction.TransactionManager)");
    } catch (InvalidSyntaxException e) {
    }
    ref = ctx.getServiceReference(TransactionManager.class.getName());
    if (ref != null) {
      tm = (TransactionManager) ctx.getService(ref);
    }
    
    if (tm != null) {
      t.open();
    }
  }

  public void stop(BundleContext ctx)
  {
  }

  public Object addingService(ServiceReference ref)
  {
    BundleContext ctx = ref.getBundle().getBundleContext();

    Hashtable<String, Object> map = new Hashtable<String, Object>();
    for (String key : ref.getPropertyKeys()) {
      map.put(key, ref.getProperty(key));
    }
    map.put("aries.xa.aware", "true");

    XADatasourceEnlistingWrapper wrapper = new XADatasourceEnlistingWrapper();
    wrapper.setTxManager(tm);
    wrapper.setDataSource((XADataSource) ctx.getService(ref));

    ServiceRegistration reg = ctx.registerService(DataSource.class.getName(), wrapper, map); 

    return reg;
  }
 
  public void modifiedService(ServiceReference ref, Object service)
  {
    ServiceRegistration reg = (ServiceRegistration) service;
    
    Hashtable<String, Object> map = new Hashtable<String, Object>();
    for (String key : ref.getPropertyKeys()) {
      map.put(key, ref.getProperty(key));
    }
    map.put("aries.xa.aware", "true");

    reg.setProperties(map);
  }

  public void removedService(ServiceReference ref, Object service)
  {
    ((ServiceRegistration)service).unregister();
  }

  public void serviceChanged(ServiceEvent event)
  {
    if (event.getType() == ServiceEvent.REGISTERED && tm == null) {
      ref = event.getServiceReference();
      tm = (TransactionManager) context.getService(ref);
      
      if (tm == null) ref = null;
      else t.open();
    } else if (event.getType() == ServiceEvent.UNREGISTERING && tm != null &&
        ref.getProperty("service.id").equals(event.getServiceReference().getProperty("service.id"))) {
      t.close();
      context.ungetService(ref);
      ref = null;
      tm = null;
    }
  }
}
