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
package org.apache.aries.tx.control.jpa.xa.plugin.hibernate.impl;

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_ROLLEDBACK;
import static javax.transaction.Status.STATUS_UNKNOWN;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.TransactionRequiredException;
import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin provides support for Hibernate 5.2.0 and 5.2.1,
 * after this Hibernate added a breaking change...
 *
 */
public class Hibernate520TxControlPlatform implements 
	TransactionCoordinatorBuilder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Hibernate520TxControlPlatform.class);
	
	private static final long serialVersionUID = 1L;

	private final ThreadLocal<TransactionControl> txControlToUse;
	
	public Hibernate520TxControlPlatform(ThreadLocal<TransactionControl> txControlToUse) {
		this.txControlToUse = txControlToUse;
	}
	
	public TransactionControl getTxControl() {
		TransactionControl transactionControl = txControlToUse.get();
		if(transactionControl == null) {
			throw new TransactionException("A No Transaction Context could not be created because there is no associated Transaction Control");
		}
		return transactionControl;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorBuilder.Options options) {
		return new HibernateTxControlCoordinator(owner, options.shouldAutoJoinTransaction());
	}

	@Override
	public boolean isJta() {
		return true;
	}

	@Override
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
	}
	
	public class HibernateTxControlCoordinator implements TransactionCoordinator, 
		SynchronizationRegistry, TransactionDriver, IsolationDelegate {
		
		private static final long serialVersionUID = 1L;

		private final List<TransactionObserver> registeredObservers = new ArrayList<>();

		private final TransactionCoordinatorOwner owner;

		private final boolean autoJoin;
		
		private boolean joined = false;
		
		public HibernateTxControlCoordinator(TransactionCoordinatorOwner owner, boolean autoJoin) {
			this.owner = owner;
			this.autoJoin = autoJoin;
		}

		@Override
		public void explicitJoin() {
			if(!joined) {
				if(!getTxControl().activeTransaction()) {
					throw new TransactionRequiredException("There is no transaction active to join");
				}
				
				internalJoin();
			}
		}

		private void internalJoin() {
			TransactionContext currentContext = getTxControl().getCurrentContext();
			currentContext.preCompletion(this::beforeCompletion);
			currentContext.postCompletion(this::afterCompletion);
			joined = true;
		}

		@Override
		public boolean isJoined() {
			return joined;
		}

		@Override
		public void pulse() {
			if (autoJoin && !joined && getTxControl().activeTransaction()) {
				internalJoin();
			}
		}

		@Override
		public TransactionDriver getTransactionDriverControl() {
			return this;
		}

		@Override
		public SynchronizationRegistry getLocalSynchronizations() {
			return this;
		}

		@Override
		public boolean isActive() {
			return getTxControl().activeTransaction();
		}

		@Override
		public IsolationDelegate createIsolationDelegate() {
			return this;
		}

		@Override
		public void addObserver(TransactionObserver observer) {
			registeredObservers.add(observer);
		}

		@Override
		public void removeObserver(TransactionObserver observer) {
			registeredObservers.remove(observer);
		}

		@Override
		public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
			return Hibernate520TxControlPlatform.this;
		}

		@Override
		public void setTimeOut(int seconds) {
			// TODO How do we support this?
		}

		@Override
		public int getTimeOut() {
			return -1;
		}
	
		@Override
		public void registerSynchronization(Synchronization synchronization) {
			LOGGER.debug("Registering a synchronization with the current transaction");
			TransactionContext currentContext = getTxControl().getCurrentContext();
			currentContext.preCompletion(synchronization::beforeCompletion);
			currentContext.postCompletion(status -> synchronization.afterCompletion(toIntStatus(status)));
		}

		private void beforeCompletion() {
			try {
				owner.beforeTransactionCompletion();
			}
			catch (RuntimeException re) {
				getTxControl().setRollbackOnly();
				throw re;
			}
			finally {
				registeredObservers.forEach(TransactionObserver::beforeCompletion);
			}
		}
		
		private void afterCompletion(TransactionStatus status) {
			if ( owner.isActive() ) {
				toIntStatus(status);
				
				boolean committed = status == COMMITTED;
				owner.afterTransactionCompletion(committed, false);
				
				registeredObservers.forEach(o -> o.afterCompletion(committed, false));
			}
		}

		private int toIntStatus(TransactionStatus status) {
			switch(status) {
				case COMMITTED:
					return STATUS_COMMITTED;
				case ROLLED_BACK:
					return STATUS_ROLLEDBACK;
				default:
					return STATUS_UNKNOWN;
			}
		}

		@Override
		public void begin() {
			if(!getTxControl().activeTransaction()) {
				throw new IllegalStateException("There is no existing active transaction scope");
			}
		}

		@Override
		public void commit() {
			if(!getTxControl().activeTransaction()) {
				throw new IllegalStateException("There is no existing active transaction scope");
			}
		}

		@Override
		public void rollback() {
			if(!getTxControl().activeTransaction()) {
				throw new IllegalStateException("There is no existing active transaction scope");
			}
			getTxControl().setRollbackOnly();
		}

		@Override
		public org.hibernate.resource.transaction.spi.TransactionStatus getStatus() {
			TransactionStatus status = getTxControl().getCurrentContext().getTransactionStatus();
			switch(status) {
				case ACTIVE:
					return org.hibernate.resource.transaction.spi.TransactionStatus.ACTIVE;
				case COMMITTED:
					return org.hibernate.resource.transaction.spi.TransactionStatus.COMMITTED;
				case PREPARING:
				case PREPARED:
				case COMMITTING:
					return org.hibernate.resource.transaction.spi.TransactionStatus.COMMITTING;
				case MARKED_ROLLBACK:
					return org.hibernate.resource.transaction.spi.TransactionStatus.MARKED_ROLLBACK;
				case NO_TRANSACTION:
					return org.hibernate.resource.transaction.spi.TransactionStatus.NOT_ACTIVE;
				case ROLLED_BACK:
					return org.hibernate.resource.transaction.spi.TransactionStatus.ROLLED_BACK;
				case ROLLING_BACK:
					return org.hibernate.resource.transaction.spi.TransactionStatus.ROLLING_BACK;
				default:
					throw new IllegalStateException("The state " + status + " is unknown");
			}
		}

		@Override
		public void markRollbackOnly() {
			getTxControl().setRollbackOnly();
		}

		@Override
		public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted) throws HibernateException {
			Callable<T> c = () -> {
			
				JdbcSessionOwner sessionOwner = owner.getJdbcSessionOwner();
				Connection conn = sessionOwner.getJdbcConnectionAccess().obtainConnection();
				
				try {
					return work.accept(new WorkExecutor<>(), conn);
				} finally {
					sessionOwner.getJdbcConnectionAccess().releaseConnection(conn);
				}
			};
			
			return delegateCallable(c, transacted);
		}

		@Override
		public <T> T delegateCallable(Callable<T> c, boolean transacted) throws HibernateException {
			try {
				if(transacted) {
					LOGGER.debug("Performing a query in a nested transaction");
					return getTxControl().requiresNew(c);
				} else {
					LOGGER.debug("Suspending the current transaction to run a query");
					return getTxControl().notSupported(c);
				}
			} finally {
				LOGGER.debug("The previous transaction has been resumed");
			}
		}
	}
}
