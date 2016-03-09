package org.apache.aries.tx.control.service.common.impl;

import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.transaction.xa.XAResource;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public class NoTransactionContextImpl extends AbstractTransactionContextImpl
		implements TransactionContext {

	private final AtomicBoolean finished = new AtomicBoolean(false);

	public NoTransactionContextImpl(Coordination coordination) {
		super(coordination);
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
		if (coordination.isTerminated()) {
			throw new IllegalStateException(
					"The transaction context has finished");
		}
		
		preCompletion.add(job);
	}

	@Override
	public void postCompletion(Consumer<TransactionStatus> job)
			throws IllegalStateException {
		if (coordination.isTerminated()) {
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
