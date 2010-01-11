package org.apache.aries.jpa.container.unit.impl;

import java.util.Map;

import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;

public class ManagedPersistenceUnitInfoImpl implements
    ManagedPersistenceUnitInfo {

  private final PersistenceUnitInfo info;
  public ManagedPersistenceUnitInfoImpl(Bundle persistenceBundle,
      ParsedPersistenceUnit unit) {
    info = new PersistenceUnitInfoImpl(persistenceBundle, unit);
  }

  public Map<String, Object> getContainerProperties() {
    return null;
  }

  public PersistenceUnitInfo getPersistenceUnitInfo() {
    return info;
  }


}
