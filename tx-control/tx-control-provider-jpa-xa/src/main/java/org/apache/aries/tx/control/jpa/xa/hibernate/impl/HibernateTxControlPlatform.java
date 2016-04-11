package org.apache.aries.tx.control.jpa.xa.hibernate.impl;

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_ROLLEDBACK;
import static javax.transaction.Status.STATUS_UNKNOWN;
import static org.hibernate.ConnectionAcquisitionMode.AS_NEEDED;
import static org.hibernate.ConnectionReleaseMode.AFTER_STATEMENT;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.TransactionRequiredException;
import javax.transaction.Synchronization;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.SynchronizationRegistry;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinator.TransactionDriver;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionStatus;

public class HibernateTxControlPlatform implements TransactionCoordinatorBuilder {
	
	private static final long serialVersionUID = 1L;

	private final TransactionControl control;
	
	
	public HibernateTxControlPlatform(TransactionControl control) {
		this.control = control;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		return new HibernateTxControlCoordinator(owner, options.shouldAutoJoinTransaction());
	}

	@Override
	public boolean isJta() {
		return true;
	}

	@Override
	public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return AFTER_STATEMENT;
	}

	@Override
	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return AS_NEEDED;
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
				if(!control.activeTransaction()) {
					throw new TransactionRequiredException("There is no transaction active to join");
				}
				
				internalJoin();
			}
		}

		private void internalJoin() {
			TransactionContext currentContext = control.getCurrentContext();
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
			if (autoJoin && !joined && control.activeTransaction()) {
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
			return control.activeTransaction();
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
			return HibernateTxControlPlatform.this;
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
			TransactionContext currentContext = control.getCurrentContext();
			currentContext.preCompletion(synchronization::beforeCompletion);
			currentContext.postCompletion(status -> synchronization.afterCompletion(toIntStatus(status)));
		}

		private void beforeCompletion() {
			try {
				owner.beforeTransactionCompletion();
			}
			catch (RuntimeException re) {
				control.setRollbackOnly();
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
			if(!control.activeTransaction()) {
				throw new IllegalStateException("There is no existing active transaction scope");
			}
		}

		@Override
		public void commit() {
			if(!control.activeTransaction()) {
				throw new IllegalStateException("There is no existing active transaction scope");
			}
		}

		@Override
		public void rollback() {
			if(!control.activeTransaction()) {
				throw new IllegalStateException("There is no existing active transaction scope");
			}
			control.setRollbackOnly();
		}

		@Override
		public org.hibernate.resource.transaction.spi.TransactionStatus getStatus() {
			TransactionStatus status = control.getCurrentContext().getTransactionStatus();
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
			control.setRollbackOnly();
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
			
			if(transacted) {
				return control.requiresNew(c);
			} else {
				return control.notSupported(c);
			}
				
		}
	}
}
