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
package org.apache.aries.tx.control.jpa.common.impl;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.osgi.service.transaction.control.TransactionException;

public class TxEntityManagerWrapper extends EntityManagerWrapper {

	private final EntityManager entityManager;
	
	public TxEntityManagerWrapper(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	protected EntityManager getRealEntityManager() {
		return entityManager;
	}

	@Override
	public void close() {
		// A no-op
	}

	@Override
	public void joinTransaction() {
		// A no-op
	}

	@Override
	public boolean isJoinedToTransaction() {
		return true;
	}

	@Override
	public EntityTransaction getTransaction() {
		throw new TransactionException("Programmatic transaction management is not supported for a Transactional EntityManager");
	}
}
