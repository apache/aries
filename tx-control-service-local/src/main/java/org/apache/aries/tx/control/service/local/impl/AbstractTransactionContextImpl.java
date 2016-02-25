package org.apache.aries.tx.control.service.local.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public abstract class AbstractTransactionContextImpl implements TransactionContext {

	protected static class TransactionVariablesKey {
	}

	protected final AtomicReference<Throwable> firstUnexpectedException = new AtomicReference<>();

	protected final List<Throwable> subsequentExceptions = new ArrayList<>();

	protected final Coordination coordination;

	protected final List<Runnable> preCompletion = new ArrayList<>();

	protected final List<Consumer<TransactionStatus>> postCompletion = new ArrayList<>();

	public AbstractTransactionContextImpl(Coordination coordination) {
		this.coordination = coordination;

		coordination.addParticipant(new Participant() {

			@Override
			public void failed(Coordination coordination) throws Exception {
				Throwable failure = coordination.getFailure();
				recordFailure(failure);
				safeSetRollbackOnly();
			}

			@Override
			public void ended(Coordination coordination) throws Exception {
				if (isAlive()) {
					// TODO log this
					recordFailure(new IllegalStateException(
							"The surrounding coordination ended before the transaction completed"));
					safeSetRollbackOnly();

				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getScopedValue(Object key) {
		return ((HashMap<Object, Object>) coordination.getVariables().getOrDefault(TransactionVariablesKey.class,
				new HashMap<>())).get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putScopedValue(Object key, Object value) {
		((HashMap<Object, Object>) coordination.getVariables().computeIfAbsent(TransactionVariablesKey.class,
				k -> new HashMap<>())).put(key, value);
	}

	protected void beforeCompletion(Runnable onFirstError) {
		preCompletion.stream().forEach(r -> {
			try {
				r.run();
			} catch (Exception e) {
				if (firstUnexpectedException.compareAndSet(null, e)) {
					onFirstError.run();
				} else {
					subsequentExceptions.add(e);
				}
				// TODO log this
			}
		});
	}

	protected void afterCompletion(TransactionStatus status) {
		postCompletion.stream().forEach(c -> {
			try {
				c.accept(status);
			} catch (Exception e) {
				recordFailure(e);
				// TODO log this
			}
		});
	}

	protected abstract boolean isAlive();

	protected void recordFailure(Throwable failure) {
		if (!firstUnexpectedException.compareAndSet(null, failure)) {
			subsequentExceptions.add(failure);
		}
	}

	protected abstract void safeSetRollbackOnly();

	public abstract void finish();
}
