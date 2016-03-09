package org.apache.aries.tx.control.service.xa.impl;

import static java.util.Optional.ofNullable;
import static javax.transaction.xa.XAException.XA_HEURMIX;
import static javax.transaction.xa.XAException.XA_RBOTHER;
import static javax.transaction.xa.XAException.XA_RBPROTO;
import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTING;
import static org.osgi.service.transaction.control.TransactionStatus.MARKED_ROLLBACK;
import static org.osgi.service.transaction.control.TransactionStatus.PREPARED;
import static org.osgi.service.transaction.control.TransactionStatus.PREPARING;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLING_BACK;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.TransactionStatus;

public class TransactionContextImpl extends AbstractTransactionContextImpl implements TransactionContext {

	final List<LocalResource> resources = new ArrayList<>();

	private final Transaction oldTran;
	
	private final Transaction currentTransaction;
	
	private final AtomicReference<TransactionStatus> completionState = new AtomicReference<>();

	private final GeronimoTransactionManager transactionManager;
	
	private final Object key;

	public TransactionContextImpl(GeronimoTransactionManager transactionManager, Coordination coordination) {
		super(coordination);
		this.transactionManager = transactionManager;
		Transaction tmp = null;
		try {
			tmp = transactionManager.suspend();
			transactionManager.begin();
		} catch (Exception e) {
			if(tmp != null) {
				try {
					transactionManager.resume(tmp);
				} catch (Exception e1) {
					e.addSuppressed(e1);
				}
			}
			throw new TransactionException("There was a serious error creating a transaction");
		}
		oldTran = tmp;
		currentTransaction = transactionManager.getTransaction();
		key = transactionManager.getTransactionKey();
	}

	@Override
	public Object getTransactionKey() {
		return key;
	}

	@Override
	public boolean getRollbackOnly() throws IllegalStateException {
		switch (getTransactionStatus()) {
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
		TransactionStatus status = getTransactionStatus();
		switch (status) {
			case ACTIVE:
			case MARKED_ROLLBACK:
				try {
					currentTransaction.setRollbackOnly();
				} catch (Exception e) {
					throw new TransactionException("Unable to set rollback for the transaction", e);
				}
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
		TransactionStatus status = getTransactionStatus();
		switch (status) {
			case ACTIVE:
			case MARKED_ROLLBACK:
				try {
					currentTransaction.setRollbackOnly();
				} catch (Exception e) {
					throw new TransactionException("Unable to set rollback for the transaction", e);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public TransactionStatus getTransactionStatus() {
		return ofNullable(completionState.get())
			.orElseGet(this::getStatusFromTransaction);
	}

	private TransactionStatus getStatusFromTransaction() {
		int status;
		try {
			status = currentTransaction.getStatus();
		} catch (SystemException e) {
			throw new TransactionException("Unable to determine the state of the transaction.", e);
		}
		
		switch (status) {
			case Status.STATUS_ACTIVE:
				return ACTIVE;
			case Status.STATUS_MARKED_ROLLBACK:
				return MARKED_ROLLBACK;
			case Status.STATUS_PREPARING:
				return PREPARING;
			case Status.STATUS_PREPARED:
				return PREPARED;
			case Status.STATUS_COMMITTING:
				return COMMITTING;
			case Status.STATUS_COMMITTED:
				return COMMITTED;
			case Status.STATUS_ROLLING_BACK:
				return ROLLING_BACK;
			case Status.STATUS_ROLLEDBACK:
				return ROLLED_BACK;
			default:
				throw new TransactionException("Unable to determine the state of the transaction: " + status);
		}
	}

	@Override
	public void preCompletion(Runnable job) throws IllegalStateException {
		TransactionStatus status = getTransactionStatus();
		if (status.compareTo(MARKED_ROLLBACK) > 0) {
			throw new IllegalStateException("The current transaction is in state " + status);
		}

		preCompletion.add(job);
	}

	@Override
	public void postCompletion(Consumer<TransactionStatus> job) throws IllegalStateException {
		TransactionStatus status = getTransactionStatus();
		if (status == COMMITTED || status == ROLLED_BACK) {
			throw new IllegalStateException("The current transaction is in state " + status);
		}

		postCompletion.add(job);
	}

	@Override
	public void registerXAResource(XAResource resource) {
		TransactionStatus status = getTransactionStatus();
		if (status.compareTo(MARKED_ROLLBACK) > 0) {
			throw new IllegalStateException("The current transaction is in state " + status);
		}
		try {
			currentTransaction.enlistResource(resource);
		} catch (Exception e) {
			throw new TransactionException("The transaction was unable to enlist a resource", e);
		}
	}

	@Override
	public void registerLocalResource(LocalResource resource) {
		TransactionStatus status = getTransactionStatus();
		if (status.compareTo(MARKED_ROLLBACK) > 0) {
			throw new IllegalStateException("The current transaction is in state " + status);
		}
		resources.add(resource);
	}

	@Override
	public boolean supportsXA() {
		return true;
	}

	@Override
	public boolean supportsLocal() {
		return true;
	}

	@Override
	protected boolean isAlive() {
		TransactionStatus status = getTransactionStatus();
		return status != COMMITTED && status != ROLLED_BACK;
	}

	@Override
	public void finish() {
		
		if(!resources.isEmpty()) {
			XAResource localResource = new LocalXAResourceImpl();
			try {
				currentTransaction.enlistResource(localResource);
			} catch (Exception e) {
				safeSetRollbackOnly();
				recordFailure(e);
				try {
					localResource.rollback(null);
				} catch (XAException e1) {
					recordFailure(e1);
				}
			}
		}
		
		TxListener listener; 
		boolean manualCallListener;
		if(!preCompletion.isEmpty() || !postCompletion.isEmpty()) {
			listener = new TxListener();
			try {
				transactionManager.registerInterposedSynchronization(listener);
				manualCallListener = false;
			} catch (Exception e) {
				manualCallListener = true;
				recordFailure(e);
				safeSetRollbackOnly();
			}
		} else {
			listener = null;
			manualCallListener = false;
		}
		

		try {
			int status;
			try {
				if (getRollbackOnly()) {
					// GERONIMO-4449 says that we get no beforeCompletion 
					// callback for rollback :(
					if(listener != null) {
						listener.beforeCompletion();
					}
					transactionManager.rollback();
					status = Status.STATUS_ROLLEDBACK;
					completionState.set(ROLLED_BACK);
				} else {
					if(manualCallListener) {
						listener.beforeCompletion();
					}
					transactionManager.commit();
					status = Status.STATUS_COMMITTED;
					completionState.set(COMMITTED);
				}
			} catch (Exception e) {
				recordFailure(e);
				status = Status.STATUS_ROLLEDBACK;
				completionState.set(ROLLED_BACK);
			}
			if(manualCallListener) {
				listener.afterCompletion(status);
			}
		} finally {
			try {
				transactionManager.resume(oldTran);
			} catch (Exception e) {
				recordFailure(e);
			}
		}
	}
	
	private class LocalXAResourceImpl implements XAResource {

		private final AtomicBoolean finished = new AtomicBoolean();
		
		@Override
		public void commit(Xid xid, boolean onePhase) throws XAException {
			if(!finished.compareAndSet(false, true)) {
				return;
			}
			doCommit();
		}

		private void doCommit() throws XAException {
			AtomicBoolean commit = new AtomicBoolean(true);
			
			List<LocalResource> committed = new ArrayList<>(resources.size());
			List<LocalResource> rolledback = new ArrayList<>(0);

			resources.stream().forEach(lr -> {
				try {
					if (commit.get()) {
						lr.commit();
						committed.add(lr);
					} else {
						lr.rollback();
						rolledback.add(lr);
					}
				} catch (Exception e) {
					recordFailure(e);
					if (committed.isEmpty()) {
						commit.set(false);
						// This is needed to override the status from the
						// Transaction, which thinks that we're committing
						// until we throw an XAException from this commit.
						completionState.set(ROLLING_BACK);
					}
					rolledback.add(lr);
				}
			});
			
			if(!rolledback.isEmpty()) {
				if(committed.isEmpty()) {
					throw (XAException) new XAException(XA_RBOTHER)
						.initCause(firstUnexpectedException.get());
				} else {
					throw (XAException) new XAException(XA_HEURMIX)
						.initCause(firstUnexpectedException.get());
				}
			}
		}

		@Override
		public void end(Xid xid, int flags) throws XAException {
			//Nothing to do here
		}

		@Override
		public void forget(Xid xid) throws XAException {
			//Nothing to do here
		}

		@Override
		public int getTransactionTimeout() throws XAException {
			return 3600;
		}

		@Override
		public boolean isSameRM(XAResource xares) throws XAException {
			return this == xares;
		}

		@Override
		public int prepare(Xid xid) throws XAException {
			if(!finished.compareAndSet(false, true)) {
				switch(getTransactionStatus()) {
					case COMMITTING:
						return XA_OK;
					case ROLLING_BACK:
						throw new XAException(XA_RBOTHER);
					default:
						throw new XAException(XA_RBPROTO);
				}
			}
			completionState.set(COMMITTING);
			doCommit();
			return XA_OK;
		}

		@Override
		public Xid[] recover(int flag) throws XAException {
			return new Xid[0];
		}

		@Override
		public void rollback(Xid xid) throws XAException {
			if(!finished.compareAndSet(false, true)) {
				return;
			}
			resources.stream().forEach(lr -> {
				try {
					lr.rollback();
				} catch (Exception e) {
					// TODO log this
					recordFailure(e);
				}
			});
		}

		@Override
		public boolean setTransactionTimeout(int seconds) throws XAException {
			return false;
		}

		@Override
		public void start(Xid xid, int flags) throws XAException {
			// Nothing to do here
		}
		
	}
	
	private class TxListener implements Synchronization {

		@Override
		public void beforeCompletion() {
			TransactionContextImpl.this.beforeCompletion(() -> safeSetRollbackOnly());
		}

		@Override
		public void afterCompletion(int status) {
			TransactionContextImpl.this.afterCompletion(status == Status.STATUS_COMMITTED ? COMMITTED : ROLLED_BACK);
		}
		
	}
}
