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
package org.apache.aries.jpa.container.unit.impl;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedPersistenceUnitInfoImpl implements
    ManagedPersistenceUnitInfo {
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  
  private static final Boolean useDataSourceFactory;
  
  static {
    boolean b;
    try {
      Class.forName("org.osgi.service.jdbc.DataSourceFactory", false, 
          ManagedPersistenceUnitInfoImpl.class.getClassLoader());
      b = true;
    } catch (ClassNotFoundException cnfe) {
      if(_logger.isInfoEnabled())
        _logger.info(NLS.MESSAGES.getMessage("no.datasourcefactory.integration"));
      b = false;
    }
    useDataSourceFactory = b;
  }
  
  private final PersistenceUnitInfoImpl info;
  
  public ManagedPersistenceUnitInfoImpl(Bundle persistenceBundle,
      ParsedPersistenceUnit unit,
      ServiceReference providerRef) {
    info = new PersistenceUnitInfoImpl(persistenceBundle, unit, providerRef, useDataSourceFactory);
  }

  public Map<String, Object> getContainerProperties() {
    Map<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceUnitConstants.USE_DATA_SOURCE_FACTORY, useDataSourceFactory.toString());
    return props;
  }

  public PersistenceUnitInfo getPersistenceUnitInfo() {
    return info;
  }

  public void destroy() {
    info.clearUp();
  }

  @Override
  public void registered() {
   //No op, our PersistenceUnitInfoImpl is lazy
  }

  @Override
  public void unregistered() {
    info.unregistered();
  }
}
