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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.impl;


import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.apache.aries.jpa.container.impl.EntityManagerFactoryManager.NamedCallback;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * An {@link EntityManagerFactory} that keeps track of the number of active instances
 * so that it can be quiesced
 */
public class CountingEntityManagerFactory implements EntityManagerFactory, DestroyCallback {
  /** Number of open EntityManagers */
  private final AtomicLong count = new AtomicLong(0);
  /** The real EMF */
  private final EntityManagerFactory delegate;
  /** The name of this unit */
  private final String name;
  /** A quiesce callback to call */
  private final AtomicReference<NamedCallback> callback = new AtomicReference<NamedCallback>();
  /** The service registration to unregister if we can quiesce */
  private final AtomicReference<ServiceRegistration> reg = new AtomicReference<ServiceRegistration>();
  
  
  public CountingEntityManagerFactory(
      EntityManagerFactory containerEntityManagerFactory, String name) {
    delegate = containerEntityManagerFactory;
    this.name = name;
  }

  public void close() {
    delegate.close();
  }

  public EntityManager createEntityManager() {
    EntityManager em = delegate.createEntityManager();
    count.incrementAndGet();
    return new EntityManagerWrapper(em, this);
  }

  public EntityManager createEntityManager(Map arg0) {
    EntityManager em = delegate.createEntityManager(arg0);
    count.incrementAndGet();
    return new EntityManagerWrapper(em, this);
  }

  public Cache getCache() {
    return delegate.getCache();
  }

  public CriteriaBuilder getCriteriaBuilder() {
    return delegate.getCriteriaBuilder();
  }

  public Metamodel getMetamodel() {
    return delegate.getMetamodel();
  }

  public PersistenceUnitUtil getPersistenceUnitUtil() {
    return delegate.getPersistenceUnitUtil();
  }

  public Map<String, Object> getProperties() {
    return delegate.getProperties();
  }

  public boolean isOpen() {
    return delegate.isOpen();
  }

  public void quiesce(NamedCallback callback, ServiceRegistration reg) {
    this.reg.compareAndSet(null, reg);
    this.callback.compareAndSet(null, callback);
    if(count.get() == 0) {
      AriesFrameworkUtil.safeUnregisterService(this.reg.getAndSet(null));
      this.callback.set(null);
      callback.callback(name);
    }
  }

  public void callback() {
    
    if(count.decrementAndGet() == 0) {
      NamedCallback c = callback.getAndSet(null);
      if(c != null) {
        AriesFrameworkUtil.safeUnregisterService(reg.getAndSet(null));
        c.callback(name);
      }
    }
      
  }

  public void clearQuiesce() {
    //We will already be unregistered
    reg.set(null);
    NamedCallback c = callback.getAndSet(null);
    //If there was a callback then call it in case time hasn't run out.
    if(c != null) {
      c.callback(name);
    }
  }

}
