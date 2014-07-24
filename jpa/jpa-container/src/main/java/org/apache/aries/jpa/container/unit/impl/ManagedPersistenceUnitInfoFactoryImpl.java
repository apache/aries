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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory;
import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ManagedPersistenceUnitInfoFactoryImpl implements
    ManagedPersistenceUnitInfoFactory {

  private ConcurrentMap<Bundle, Collection<ManagedPersistenceUnitInfoImpl>> persistenceUnits = 
      new ConcurrentHashMap<Bundle, Collection<ManagedPersistenceUnitInfoImpl>>();
  
  public Collection<? extends ManagedPersistenceUnitInfo> createManagedPersistenceUnitMetadata(
      BundleContext containerContext, Bundle persistenceBundle,
      ServiceReference providerReference,
      Collection<ParsedPersistenceUnit> persistenceMetadata) {
    
    Collection<ManagedPersistenceUnitInfoImpl> managedUnits = new ArrayList<ManagedPersistenceUnitInfoImpl>();
    
    for(ParsedPersistenceUnit unit : persistenceMetadata)
      managedUnits.add(new ManagedPersistenceUnitInfoImpl(persistenceBundle, unit, providerReference));
    
    Collection<?> existing = persistenceUnits.putIfAbsent(persistenceBundle, managedUnits);
    if(existing != null)
      throw new IllegalStateException(NLS.MESSAGES.getMessage("previous.pus.have.not.been.destroyed", persistenceBundle.getSymbolicName(), persistenceBundle.getVersion()));
    return Collections.unmodifiableCollection(managedUnits);
  }

  public void destroyPersistenceBundle(BundleContext containerContext, Bundle bundle) {
    Collection<ManagedPersistenceUnitInfoImpl> mpus = persistenceUnits.remove(bundle);
    if(mpus == null)
      return; // already destroyed
    for(ManagedPersistenceUnitInfoImpl impl : mpus) {
      impl.destroy();
    }
  }

  public String getDefaultProviderClassName() {
    return null;
  }
}
