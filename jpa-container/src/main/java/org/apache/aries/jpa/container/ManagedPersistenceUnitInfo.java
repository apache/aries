package org.apache.aries.jpa.container;

import java.util.Map;

import javax.persistence.spi.PersistenceUnitInfo;

public interface ManagedPersistenceUnitInfo {

  PersistenceUnitInfo getPersistenceUnitInfo();
  
  Map<String, Object> getContainerProperties();
  
}
