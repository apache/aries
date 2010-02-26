package org.apache.aries.jpa.blueprint.itest;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

public class JPATestBean {
  
  private EntityManagerFactory emf;
  
  private EntityManager em;
  
  public void setPUnit(EntityManagerFactory pUnit) {
    emf = pUnit;
  }

  public void setPContext(EntityManager pContext) {
    em = pContext;
  }
  
  /**
   * @return True if we sucessfully called a method on the EMF
   */
  public boolean isPUnit() {
    emf.isOpen();
    return true;
  }
  
  /**
   * @return True if we sucessfully called a method on the EM
   */
  public boolean isPContext() {
    em.isOpen();
    return true;
  }
  
}
