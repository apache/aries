package org.apache.aries.jpa.container.util;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory;
import org.apache.aries.jpa.container.unit.impl.ManagedPersistenceUnitInfoFactoryImpl;

public class FakeManagedPersistenceUnitFactory extends
    ManagedPersistenceUnitInfoFactoryImpl implements
    ManagedPersistenceUnitInfoFactory {

  @Override
  public String getDefaultProviderClassName() {
    // TODO Auto-generated method stub
    return "use.this.Provider";
  }
  
  

}
