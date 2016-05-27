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
package org.apache.aries.tx.control.service.local.impl;

import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTING;
import static org.osgi.service.transaction.control.TransactionStatus.MARKED_ROLLBACK;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLING_BACK;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public class TransactionContextImpl extends AbstractTransactionContextImpl implements TransactionContext {

	final List<LocalResource> resources = new ArrayList<>();

	private final boolean readOnly;

	private AtomicReference<TransactionStatus> tranStatus = new AtomicReference<>(ACTIVE);

	private Object txId;

	public TransactionContextImpl(Object txId, boolean readOnly) {
		this.txId = txId;
		this.readOnly = readOnly;
	}

	@Override
	public Object getTransactionKey() {
		return txId;
	}

	@Override
	public boolean getRollbackOnly() throws IllegalStateException {
		switch (tranStatus.get()) {
			case MARKED_ROLLBACK:
			case ROLLING_BACK:
			case ROLLED_BACK:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException {
		TransactionStatus status = tranStatus.get();
		switch (status) {
			case ACTIVE:
			case MARKED_ROLLBACK:
				if(!tranStatus.compareAndSet(status, MARKED_ROLLBACK))
					setRollbackOnly();
				break;
			case COMMITTING:
				// TODO something here? If it's the first resource then it might
				// be ok to roll back?
				throw new IllegalStateException("The transaction is already being committed");
			case COMMITTED:
				throw new IllegalStateException("The transaction is already committed");
	
			case ROLLING_BACK:
			case ROLLED_BACK:
				// A no op
				break;
			default:
				throw new IllegalStateException("The transaction is in an unkown state");
		}
	}
	
	@Override
	protected void safeSetRollbackOnly() {
		TransactionStatus status = tranStatus.get();
		switch (status) {
			case ACTIVE:
			case MARKED_ROLLBACK:
				if(!tranStatus.compareAndSet(status, MARKED_ROLLBACK))
					safeSetRollbackOnly();
				break;
			default:
				break;
		}
	}

	@Override
	public TransactionStatus getTransactionStatus() {
		return tranStatus.get();
	}

	@Override
	public void preCompletion(Runnable job) throws IllegalStateException {
		if (tranStatus.get().compareTo(MARKED_ROLLBACK) > 0) {
			throw new IllegalStateException("The current transaction is in state " + tranStatus);
		}

		preCompletion.add(job);
	}

	@Override
	public void postCompletion(Consumer<TransactionStatus> job) throws IllegalStateException {
		TransactionStatus status = tranStatus.get();
		if (status == COMMITTED || status == ROLLED_BACK) {
			throw new IllegalStateException("The current transaction is in state " + tranStatus);
		}

		postCompletion.add(job);
	}

	@Override
	public void registerXAResource(XAResource resource) {
		throw new IllegalStateException("Not an XA manager");
	}

	@Override
	public void registerLocalResource(LocalResource resource) {
		if (tranStatus.get().compareTo(MARKED_ROLLBACK) > 0) {
			throw new IllegalStateException("The current transaction is in state " + tranStatus);
		}
		resources.add(resource);
	}

	@Override
	public boolean supportsXA() {
		return false;
	}

	@Override
	public boolean supportsLocal() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	protected boolean isAlive() {
		TransactionStatus status = tranStatus.get();
		return status != COMMITTED && status != ROLLED_BACK;
	}

	@Override
	public void finish() {
		
		beforeCompletion(() -> setRollbackOnly());

		TransactionStatus status;

		if (getRollbackOnly()) {
			vanillaRollback();
			status = ROLLED_BACK;
		} else {
			tranStatus.set(COMMITTING);

			List<LocalResource> committed = new ArrayList<>(resources.size());
			List<LocalResource> rolledback = new ArrayList<>(0);

			resources.stream().forEach(lr -> {
				try {
					if (getRollbackOnly()) {
						lr.rollback();
						rolledback.add(lr);
					} else {
						lr.commit();
						committed.add(lr);
					}
				} catch (Exception e) {
					firstUnexpectedException.compareAndSet(null, e);
					if (committed.isEmpty()) {
						tranStatus.set(ROLLING_BACK);
					}
					rolledback.add(lr);
				}
			});
			status = tranStatus.updateAndGet(ts -> ts == ROLLING_BACK ? ROLLED_BACK : COMMITTED);
		}
		afterCompletion(status);
	}
	
	private void vanillaRollback() {
		
		tranStatus.set(ROLLING_BACK);
	
		resources.stream().forEach(lr -> {
				try {
					lr.rollback();
				} catch (Exception e) {
					// TODO log this
					recordFailure(e);
				}
			});
		
		tranStatus.set(ROLLED_BACK);
	}
}
