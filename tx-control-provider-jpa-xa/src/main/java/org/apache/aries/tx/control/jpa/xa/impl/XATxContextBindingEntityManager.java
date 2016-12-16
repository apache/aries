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
package org.apache.aries.tx.control.jpa.xa.impl;

import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.apache.aries.tx.control.jpa.common.impl.EntityManagerWrapper;
import org.apache.aries.tx.control.jpa.common.impl.ScopedEntityManagerWrapper;
import org.apache.aries.tx.control.jpa.common.impl.TxEntityManagerWrapper;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

public class XATxContextBindingEntityManager extends EntityManagerWrapper {

	private final TransactionControl				txControl;
	private final UUID								resourceId;
	private final AbstractJPAEntityManagerProvider	provider;
	private final ThreadLocal<TransactionControl>	commonTxStore;
	

	public XATxContextBindingEntityManager(TransactionControl txControl,
			AbstractJPAEntityManagerProvider provider, UUID resourceId, 
			ThreadLocal<TransactionControl> commonTxStore) {
		this.txControl = txControl;
		this.provider = provider;
		this.resourceId = resourceId;
		this.commonTxStore = commonTxStore;
	}

	@Override
	protected final EntityManager getRealEntityManager() {

		TransactionContext txContext = txControl.getCurrentContext();

		if (txContext == null) {
			throw new TransactionException("The resource " + provider
					+ " cannot be accessed outside of an active Transaction Context");
		}

		EntityManager existing = (EntityManager) txContext.getScopedValue(resourceId);

		if (existing != null) {
			return existing;
		}

		TransactionControl previous = commonTxStore.get();
		commonTxStore.set(txControl);
		
		EntityManager toReturn;
		EntityManager toClose;

		try {
			if (txContext.getTransactionStatus() == NO_TRANSACTION) {
				toClose = provider.createEntityManager();
				toReturn = new ScopedEntityManagerWrapper(toClose);
			} else if (txContext.supportsXA()) {
				toClose = provider.createEntityManager();
				toReturn = new TxEntityManagerWrapper(toClose);
				toClose.joinTransaction();
			} else {
				throw new TransactionException(
						"There is a transaction active, but it does not support local participants");
			}
		} catch (Exception sqle) {
			commonTxStore.set(previous);
			throw new TransactionException(
					"There was a problem getting hold of a database connection",
					sqle);
		}

		
		txContext.postCompletion(x -> {
				try {
					toClose.close();
				} catch (PersistenceException sqle) {
					// TODO log this
				}
				commonTxStore.set(previous);
			});
		
		txContext.putScopedValue(resourceId, toReturn);
		
		return toReturn;
	}
}
