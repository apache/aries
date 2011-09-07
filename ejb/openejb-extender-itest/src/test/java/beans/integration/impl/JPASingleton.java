package beans.integration.impl;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import beans.jpa.Laptop;

@Singleton
public class JPASingleton {
  
  @PersistenceContext(unitName="ejb-test")
  private EntityManager em;
  
  public void editEntity(String serial) {
    Laptop l = em.find(Laptop.class, serial);
    
    l.setHardDiskSize(Integer.MAX_VALUE);
    l.setNumberOfCores(4);
  }

}
