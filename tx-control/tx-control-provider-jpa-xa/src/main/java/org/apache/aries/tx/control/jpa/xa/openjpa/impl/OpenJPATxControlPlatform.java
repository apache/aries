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
package org.apache.aries.tx.control.jpa.xa.openjpa.impl;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_COMMITTING;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static javax.transaction.Status.STATUS_PREPARING;
import static javax.transaction.Status.STATUS_ROLLEDBACK;
import static javax.transaction.Status.STATUS_ROLLING_BACK;
import static javax.transaction.Status.STATUS_UNKNOWN;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.openjpa.ee.ManagedRuntime;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionStatus;

public class OpenJPATxControlPlatform implements ManagedRuntime, TransactionManager, Transaction {

	private final TransactionControl txControl;
	
	public OpenJPATxControlPlatform(TransactionControl txControl) {
		this.txControl = txControl;
	}

	@Override
	public void doNonTransactionalWork(Runnable arg0) throws NotSupportedException {
		txControl.notSupported(() -> {
			arg0.run();
			return null;
		});
	}

	@Override
	public Throwable getRollbackCause() throws Exception {
		return null;
	}

	@Override
	public Object getTransactionKey() throws Exception, SystemException {
		return txControl.getCurrentContext().getTransactionKey();
	}

	@Override
	public TransactionManager getTransactionManager() throws Exception {
		return this;
	}

	@Override
	public void setRollbackOnly(Throwable arg0) throws Exception {
		txControl.setRollbackOnly();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		txControl.setRollbackOnly();
	}

	@Override
	public int getStatus() throws SystemException {
		TransactionContext currentContext = txControl.getCurrentContext();
		if(currentContext != null) {
			return toIntStatus(currentContext.getTransactionStatus());
		}
		return STATUS_NO_TRANSACTION;
	}

	private int toIntStatus(TransactionStatus status) {
		switch(status) {
			case NO_TRANSACTION:
				return STATUS_NO_TRANSACTION;
			case ACTIVE:
				return STATUS_ACTIVE;
			case PREPARING:
				return STATUS_PREPARING;
			case PREPARED:
				return Status.STATUS_PREPARED;
			case COMMITTING:
				return STATUS_COMMITTING;
			case COMMITTED:
				return STATUS_COMMITTED;
			case MARKED_ROLLBACK:
				return STATUS_MARKED_ROLLBACK;
			case ROLLING_BACK:
				return STATUS_ROLLING_BACK;
			case ROLLED_BACK:
				return STATUS_ROLLEDBACK;
			default:
				return STATUS_UNKNOWN;
		}
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		return this;
	}

	@Override
	public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
		return false;
	}

	@Override
	public boolean enlistResource(XAResource xaRes) throws IllegalStateException, RollbackException, SystemException {
		txControl.getCurrentContext().registerXAResource(xaRes, null);
		return true;
	}

	@Override
	public void registerSynchronization(Synchronization synch)
			throws IllegalStateException, RollbackException, SystemException {
		TransactionContext currentContext = txControl.getCurrentContext();
		currentContext.preCompletion(synch::beforeCompletion);
		currentContext.postCompletion(status -> synch.afterCompletion(toIntStatus(status)));
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		throw new NotSupportedException("The Transaction contol service does not support open scoped work");
	}

	@Override
	public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
			RollbackException, SecurityException, SystemException {
		throw new SystemException("The Transaction contol service does not support open scoped work");
	}

	@Override
	public void resume(Transaction tobj) throws IllegalStateException, InvalidTransactionException, SystemException {
		throw new SystemException("The Transaction contol service does not support open scoped work");
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		throw new SystemException("The Transaction contol service does not support open scoped work");
	}

	@Override
	public void setTransactionTimeout(int seconds) throws SystemException {
		throw new SystemException("The Transaction contol service does not support open scoped work");
	}

	@Override
	public Transaction suspend() throws SystemException {
		throw new SystemException("The Transaction contol service does not support open scoped work");
	}

}
