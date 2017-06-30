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

import static java.util.stream.Collectors.toList;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionBuilder;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.TransactionRolledBackException;

public abstract class AbstractTransactionControlImpl implements TransactionControl {

	private final class TransactionBuilderImpl extends TransactionBuilder {

		private boolean readOnly = false;
		
		@Override
		public TransactionBuilder readOnly() {
			readOnly = true;
			return this;
		}

		private void checkValid() {
			
			if(closed.get()) {
				throw new TransactionException("The transaction control service is closed");
			}
			
			List<Class<? extends Throwable>> duplicates = rollbackFor.stream()
					.filter(noRollbackFor::contains)
					.collect(toList());
			if(!duplicates.isEmpty()) {
				throw new TransactionException("The transaction declares that the Exceptions " + 
						duplicates + " must both trigger and not trigger rollback");
			}
		}

		@Override
		public <T> T required(Callable<T> work)
				throws TransactionException, TransactionRolledBackException {
			checkValid();
			
			boolean endTransaction = false;

			AbstractTransactionContextImpl existingTran = existingTx.get();
			AbstractTransactionContextImpl currentTran;
			try {
				if (existingTran == null || existingTran.getTransactionStatus() == NO_TRANSACTION) {
					currentTran = startTransaction(readOnly);
					endTransaction = true;
					existingTx.set(currentTran);
				} else if (existingTran.isReadOnly() && !readOnly){
					throw new TransactionException("A read only transaction is currently active, and cannot be upgraded to a writeable transaction");
				} else {
					currentTran = existingTran;
				}
				return doWork(work, currentTran, endTransaction);
			} finally {
				existingTx.set(existingTran);
			}
			
		}

		@Override
		public <T> T requiresNew(Callable<T> work)
				throws TransactionException, TransactionRolledBackException {
			checkValid();
			
			AbstractTransactionContextImpl existingTran = existingTx.get();
			try {
				AbstractTransactionContextImpl currentTran = startTransaction(readOnly);
				existingTx.set(currentTran);
				return doWork(work, currentTran, true);
			} finally {
				existingTx.set(existingTran);
			}

		}

		@Override
		public <T> T supports(Callable<T> work) throws TransactionException {
			checkValid();
			
			boolean endTransaction = false;

			AbstractTransactionContextImpl existingTran = existingTx.get();
			AbstractTransactionContextImpl currentTran;
			try {
				if (existingTran == null) {
					currentTran = new NoTransactionContextImpl();
					endTransaction = true;
					existingTx.set(currentTran);
				} else {
					currentTran = existingTran;
				}
				return doWork(work, currentTran, endTransaction);
			} finally {
				existingTx.set(existingTran);
			}

		}

		@Override
		public <T> T notSupported(Callable<T> work)
				throws TransactionException {
			checkValid();
			
			boolean endTransaction = false;

			AbstractTransactionContextImpl existingTran = existingTx.get();
			AbstractTransactionContextImpl currentTran;
			
			try {
				if (existingTran == null || existingTran.getTransactionStatus() != NO_TRANSACTION) {
					// We must create a new coordination to scope our new
					// transaction
					currentTran = new NoTransactionContextImpl();
					endTransaction = true;
					existingTx.set(currentTran);
				} else {
					currentTran = existingTran;
				}
				return doWork(work, currentTran, endTransaction);
			} finally {
				existingTx.set(existingTran);
			}
		}

		private <R> R doWork(Callable<R> transactionalWork,
				AbstractTransactionContextImpl currentTran, 
				boolean endTransaction) {
			R result;
			try {
				result = transactionalWork.call();

			} catch (Throwable t) {
				//TODO handle noRollbackFor
				if(requiresRollback(t)) {
					currentTran.safeSetRollbackOnly();
				}
				if(endTransaction) {
					try {
						currentTran.finish();
					} catch (Exception e) {
						currentTran.recordFailure(e);
					}
				}
				
				TransactionContext toPropagate = endTransaction ? null : currentTran;
				
				ScopedWorkException workException;
				
				if(t instanceof ScopedWorkException) {
					workException = new ScopedWorkException("A nested piece of scoped work threw an exception", 
							t.getCause(), toPropagate);
					workException.addSuppressed(t);
				} else {
					workException = new ScopedWorkException("The scoped work threw an exception", 
							t, toPropagate);
				}
				
				Throwable throwable = currentTran.firstUnexpectedException.get();
				if(throwable != null) {
					workException.addSuppressed(throwable);
				}
				currentTran.subsequentExceptions.stream().forEach(workException::addSuppressed);
				
				throw workException;
			}
			
			if(endTransaction) {
				try {
					currentTran.finish();
				} catch (Exception e) {
					currentTran.recordFailure(e);
				}
			}
			
			Throwable throwable = currentTran.firstUnexpectedException.get();
			if(throwable != null) {
				TransactionException te = currentTran.getTransactionStatus() == ROLLED_BACK ?
						new TransactionRolledBackException("The transaction rolled back due to a failure", throwable) :
						new TransactionException("There was an error in the Transaction completion.", throwable);
				
				currentTran.subsequentExceptions.stream().forEach(te::addSuppressed);
				
				throw te;
			}
			
			return result;
		}

		private boolean requiresRollback(Throwable t) {
			return mostSpecificMatch(noRollbackFor, t)
				.map(noRollbackType -> mostSpecificMatch(rollbackFor, t)
						.map(rollbackType -> noRollbackType.isAssignableFrom(rollbackType))
						.orElse(false))
				.orElse(true);
		}
		
		private Optional<Class<? extends Throwable>> mostSpecificMatch(Collection<Class<? extends Throwable>> types, Throwable t) {
			return types.stream()
					.filter(c -> c.isInstance(t))
					.max((c1, c2) -> {
							if(c1 == c2) return 0;
							
							return c1.isAssignableFrom(c2) ? 1 : c2.isAssignableFrom(c1) ? -1 : 0;
						});
		}
	}

	private final ThreadLocal<AbstractTransactionContextImpl> existingTx = new ThreadLocal<>();
	
	private final AtomicBoolean closed = new AtomicBoolean();

	protected abstract AbstractTransactionContextImpl startTransaction(boolean readOnly);

	@Override
	public TransactionBuilder build() {
		return new TransactionBuilderImpl();
	}

	@Override
	public boolean getRollbackOnly() throws IllegalStateException {
		return getCurrentTranContextChecked().getRollbackOnly();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException {
		getCurrentTranContextChecked().setRollbackOnly();
	}

	@Override
	public <T> T required(Callable<T> work)
			throws TransactionException, TransactionRolledBackException {
		return build().required(work);
	}

	@Override
	public <T> T requiresNew(Callable<T> work)
			throws TransactionException, TransactionRolledBackException {
		return build().requiresNew(work);
	}

	@Override
	public <T> T notSupported(Callable<T> work) throws TransactionException {
		return build().notSupported(work);
	}

	@Override
	public <T> T supports(Callable<T> work) throws TransactionException {
		return build().supports(work);
	}

	@Override
	public boolean activeTransaction() {
		TransactionContext context = getCurrentContext();
		return context != null
				&& context.getTransactionStatus() != NO_TRANSACTION;
	}

	@Override
	public boolean activeScope() {
		return getCurrentContext() != null;
	}

	private TransactionContext getCurrentTranContextChecked() {
		TransactionContext toUse = getCurrentContext();
		if (toUse == null) {
			throw new IllegalStateException(
					"There is no applicable transaction context");
		}
		return toUse;
	}

	@Override
	public TransactionContext getCurrentContext() {
		return existingTx.get();
	}

	@Override
	public void ignoreException(Throwable t) throws IllegalStateException {
		// TODO Auto-generated method stub

	}

	public void close() {
		closed.set(true);
	}
}
