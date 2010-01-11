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
