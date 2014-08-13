/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.context;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TransactionRequiredException;


public interface JTAPersistenceContextManager {

  /**
   * Get the existing Transaction scoped PersistenceContext for this transaction and persistence unit
   * @param emf The PersistenceUnit to get a PersistenceContext for
   * @return An existing transaction scoped PersistenceContext, or null if none already exists 
   * @throws TransactionRequiredException if there is no current transaction
   */
  public EntityManager getExistingPersistenceContext(EntityManagerFactory emf) throws TransactionRequiredException;
 
  /**
   * Make the supplied {@link EntityManager} the current persistence context for this transaction.
   * The {@link JTAPersistenceContextManager} will call {@link EntityManager#joinTransaction()} 
   * but will not close the EntityManager when the transaction commits.
   * After a successful call to this method, {@link #getExistingPersistenceContext(EntityManagerFactory)}
   * will return the {@link EntityManager} em
   * @param emf The persistence unit
   * @param em The persistence context that will be the "managed" persistence context for the current
   * transaction
   * @throws TransactionRequiredException if there is no active transaction
   * @throws IllegalStateException if there is already a persistence context associated with this transaction
   */
  public void manageExistingPersistenceContext(EntityManagerFactory emf, EntityManager em) 
  throws TransactionRequiredException, IllegalStateException;
  
  /**
   * Determine whether there is an active transaction
   */
  public boolean isTransactionActive();
}
