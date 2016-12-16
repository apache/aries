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
package org.apache.aries.tx.control.jpa.local.impl;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

public class JPAEntityManagerProviderImpl extends AbstractJPAEntityManagerProvider {

	private final UUID					uuid	= UUID.randomUUID();

	public JPAEntityManagerProviderImpl(EntityManagerFactory emf, Runnable onClose) {
		super(emf, onClose);
	}

	@Override
	public EntityManager getResource(TransactionControl txControl) throws TransactionException {
		return new TxContextBindingEntityManager(txControl, this, uuid);
	}
}
