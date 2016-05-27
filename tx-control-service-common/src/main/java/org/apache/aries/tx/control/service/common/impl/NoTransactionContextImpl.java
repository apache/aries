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
package org.apache.aries.tx.control.service.common.impl;

import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.transaction.xa.XAResource;

import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public class NoTransactionContextImpl extends AbstractTransactionContextImpl
		implements TransactionContext {

	private final AtomicBoolean finished = new AtomicBoolean(false);

	public NoTransactionContextImpl() {
		super();
	}

	@Override
	public Object getTransactionKey() {
		return null;
	}

	@Override
	public boolean getRollbackOnly() throws IllegalStateException {
		throw new IllegalStateException("No transaction is active");
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException {
		throw new IllegalStateException("No transaction is active");
	}

	@Override
	public TransactionStatus getTransactionStatus() {
		return NO_TRANSACTION;
	}

	@Override
	public void preCompletion(Runnable job) throws IllegalStateException {
		if (finished.get()) {
			throw new IllegalStateException(
					"The transaction context has finished");
		}
		
		preCompletion.add(job);
	}

	@Override
	public void postCompletion(Consumer<TransactionStatus> job)
			throws IllegalStateException {
		if (finished.get()) {
			throw new IllegalStateException(
					"The transaction context has finished");
		}

		postCompletion.add(job);
	}

	@Override
	public void registerXAResource(XAResource resource) {
		throw new IllegalStateException("No transaction is active");
	}

	@Override
	public void registerLocalResource(LocalResource resource) {
		throw new IllegalStateException("No transaction is active");
	}

	@Override
	public boolean supportsXA() {
		return false;
	}

	@Override
	public boolean supportsLocal() {
		return false;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	protected boolean isAlive() {
		return !finished.get();
	}
	
	@Override
	public void finish() {
		if(finished.compareAndSet(false, true)) {
			beforeCompletion(() -> {});
			afterCompletion(NO_TRANSACTION);
		}
	}

	@Override
	protected void safeSetRollbackOnly() {
	}
}
