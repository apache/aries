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
package org.apache.aries.jpa.container.context.impl;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;

import org.apache.aries.jpa.container.context.transaction.impl.JTAEntityManager;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
/**
 * A service factory that can lazily create persistence contexts
 */
public class ManagedPersistenceContextServiceFactory implements ServiceFactory {

  private final ServiceReference emf;
  private final Map<String, Object> properties;
  private final JTAPersistenceContextRegistry registry;
  
  private EntityManager em;
  
  public ManagedPersistenceContextServiceFactory(ServiceReference unit,
      Map<String, Object> props, JTAPersistenceContextRegistry contextRegistry) {

      emf = unit;
      properties = props;
      registry = contextRegistry;
      
  }

  public Object getService(Bundle bundle, ServiceRegistration registration) {
    
    if(em == null) {
      EntityManagerFactory factory = (EntityManagerFactory) emf.getBundle().getBundleContext().getService(emf);
      
      synchronized(this) {
        if (em == null) {
          PersistenceContextType type = (PersistenceContextType) properties.get(PersistenceContextManager.PERSISTENCE_CONTEXT_TYPE);
          if(type == PersistenceContextType.TRANSACTION || type == null)
            em = new JTAEntityManager(factory, properties, registry);
          else {
            //TODO add support, or log the failure
          }
        }
      }
    }
    return em;
  }

  public void ungetService(Bundle bundle, ServiceRegistration registration,
      Object service) {
    //No-op

  }

}
