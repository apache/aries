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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.context.transaction.impl.JTAEntityManager;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A factory that can lazily create managed persistence contexts.
 * This is registered in the Service registry to be looked up by blueprint.
 * The EntityManagerFactory interface is used to ensure a shared class space
 * with the client. Only the createEntityManager() method is supported.
 */
public class ManagedPersistenceContextFactory implements EntityManagerFactory {
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container.context");
  
  private final ServiceReference emf;
  private final Map<String, Object> properties;
  private final JTAPersistenceContextRegistry registry;
  private final PersistenceContextType type;
    
  public ManagedPersistenceContextFactory(ServiceReference unit,
      Map<String, Object> props, JTAPersistenceContextRegistry contextRegistry) {

      emf = unit;
      //Take a copy of the Map so that we don't modify the original
      properties = new HashMap<String, Object>(props);
      registry = contextRegistry;
      //Remove our internal property so that it doesn't get passed on the createEntityManager call
      type = (PersistenceContextType) properties.remove(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE);
  }

  public EntityManager createEntityManager() {
    if(_logger.isDebugEnabled()) {
      _logger.debug("Creating a container managed entity manager for the perstence unit {} with the following properties {}",
          new Object[] {emf, properties});
    }
    EntityManagerFactory factory = (EntityManagerFactory) emf.getBundle().getBundleContext().getService(emf);
    
    if(type == PersistenceContextType.TRANSACTION || type == null)
      return new JTAEntityManager(factory, properties, registry);
    else {
      _logger.error("There is currently no support for extended scope EntityManagers");
      return null;
    }

  }

  public void close() {
    throw new UnsupportedOperationException();
  }
  
  public EntityManager createEntityManager(Map arg0) {
    throw new UnsupportedOperationException();
  }

  public Cache getCache() {
    throw new UnsupportedOperationException();
  }

  public CriteriaBuilder getCriteriaBuilder() {
    throw new UnsupportedOperationException();
  }

  public Metamodel getMetamodel() {
    throw new UnsupportedOperationException();
  }

  public PersistenceUnitUtil getPersistenceUnitUtil() {
    throw new UnsupportedOperationException();
  }

  public Map<String, Object> getProperties() {
    throw new UnsupportedOperationException();
  }

  public boolean isOpen() {
    throw new UnsupportedOperationException();
  }

}
