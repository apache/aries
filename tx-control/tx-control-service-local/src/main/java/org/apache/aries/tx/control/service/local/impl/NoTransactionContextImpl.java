package org.apache.aries.tx.control.service.local.impl;

import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.function.Consumer;

import javax.transaction.xa.XAResource;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public class NoTransactionContextImpl extends AbstractTransactionContextImpl
		implements TransactionContext {

	volatile boolean						finished			= false;

	public NoTransactionContextImpl(Coordination coordination) {
		super(coordination);

		coordination.addParticipant(new Participant() {

			@Override
			public void failed(Coordination coordination) throws Exception {
				finished();
			}

			@Override
			public void ended(Coordination coordination) throws Exception {
				finished();
			}

			private void finished() {
				beforeCompletion(() -> {});
				afterCompletion();
			}

			private void afterCompletion() {
				postCompletion.stream().forEach(c -> {
					try {
						c.accept(NO_TRANSACTION);
					} catch (Exception e) {
						firstUnexpectedException.compareAndSet(null, e);
						// TODO log this
					}
				});
			}
		});
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
}
