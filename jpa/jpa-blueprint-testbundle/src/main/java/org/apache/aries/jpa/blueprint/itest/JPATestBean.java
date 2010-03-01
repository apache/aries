package org.apache.aries.jpa.blueprint.itest;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

public class JPATestBean {
  
  private EntityManagerFactory persistenceUnit;
  
  private EntityManager persistenceContext;
  
  public void setPersistenceUnit(EntityManagerFactory emf) {
    persistenceUnit = emf;
  }

  public void setPersistenceContext(EntityManager em) {
    persistenceContext = em;
  }
  
  /**
   * @return True if we sucessfully called a method on the EMF
   */
  public boolean pUnitAvailable() {
    persistenceUnit.isOpen();
    return true;
  }
  
  /**
   * @return True if we sucessfully called a method on the EM
   */
  public boolean pContextAvailable() {
    persistenceContext.isOpen();
    return true;
  }
  
}
