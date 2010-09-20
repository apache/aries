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
package org.apache.aries.jpa.blueprint.itest;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

public class JPATestBean {
  
  private EntityManagerFactory persistenceUnit;
  
  private EntityManager persistenceContext;
  
  private EntityManagerFactory constructorEMF;
  
  private EntityManager constructorEM;

  public JPATestBean(EntityManagerFactory constructorEMF,
      EntityManager constructorEM) {
    this.constructorEMF = constructorEMF;
    this.constructorEM = constructorEM;
  }
  
  public JPATestBean() {  }
  
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
  
  /**
   * @return True if we sucessfully called a method on the EMF
   */
  public boolean constructorPUnitAvailable() {
    constructorEMF.isOpen();
    return true;
  }
  
  /**
   * @return True if we sucessfully called a method on the EM
   */
  public boolean constructorPContextAvailable() {
    constructorEM.isOpen();
    return true;
  }
}
