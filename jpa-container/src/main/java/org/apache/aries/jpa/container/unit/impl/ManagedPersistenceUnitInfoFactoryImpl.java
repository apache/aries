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

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ManagedPersistenceUnitInfoFactoryImpl implements
    ManagedPersistenceUnitInfoFactory {

  public Collection<ManagedPersistenceUnitInfo> createManagedPersistenceUnitMetadata(
      BundleContext containerContext, Bundle persistenceBundle,
      ServiceReference providerReference,
      Collection<ParsedPersistenceUnit> persistenceMetadata) {
    
    Collection<ManagedPersistenceUnitInfo> managedUnits = new ArrayList<ManagedPersistenceUnitInfo>();
    
    for(ParsedPersistenceUnit unit : persistenceMetadata)
      managedUnits.add(new ManagedPersistenceUnitInfoImpl(persistenceBundle, unit));
    
    return managedUnits;
  }

  public void destroyPersistenceBundle(Bundle bundle) {
    // TODO Auto-generated method stub

  }

  public String getDefaultProviderClassName() {
    return null;
  }

}
