package org.apache.aries.tx.control.service.common.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public abstract class AbstractTransactionContextImpl implements TransactionContext {

	protected final AtomicReference<Throwable> firstUnexpectedException = new AtomicReference<>();

	protected final List<Throwable> subsequentExceptions = new ArrayList<>();

	protected final List<Runnable> preCompletion = new ArrayList<>();

	protected final List<Consumer<TransactionStatus>> postCompletion = new ArrayList<>();
	
	protected final Map<Object, Object> scopedVariables = new HashMap<>();

	@Override
	public Object getScopedValue(Object key) {
		return scopedVariables.get(key);
	}

	@Override
	public void putScopedValue(Object key, Object value) {
		scopedVariables.put(key, value);
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
