package org.apache.aries.jpa.container;

import java.util.Collection;

import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public interface ManagedPersistenceUnitInfoFactory {

  Collection<ManagedPersistenceUnitInfo> createManagedPersistenceUnitMetadata(BundleContext containerContext, Bundle persistenceBundle, ServiceReference providerReference, Collection<ParsedPersistenceUnit> persistenceMetadata);
  
  String getDefaultProviderClassName();

  void destroyPersistenceBundle(Bundle bundle);
}
