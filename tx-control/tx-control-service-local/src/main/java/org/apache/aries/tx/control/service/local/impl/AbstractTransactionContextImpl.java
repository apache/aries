package org.apache.aries.tx.control.service.local.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public abstract class AbstractTransactionContextImpl
		implements TransactionContext {

	protected static class TransactionVariablesKey {}

	protected final AtomicReference<Exception> firstUnexpectedException = new AtomicReference<>();
	
	protected final List<Exception> subsequentExceptions = new ArrayList<>();
	
	protected final Coordination coordination;
	
	protected final List<Runnable> preCompletion = new ArrayList<>();
	
	protected final List<Consumer<TransactionStatus>> postCompletion = new ArrayList<>();
	
	public AbstractTransactionContextImpl(Coordination coordination) {
		this.coordination = coordination;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getScopedValue(Object key) {
		return ((HashMap<Object,Object>) coordination.getVariables()
				.getOrDefault(TransactionVariablesKey.class, new HashMap<>()))
						.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putScopedValue(Object key, Object value) {
		((HashMap<Object,Object>) coordination.getVariables().computeIfAbsent(
				TransactionVariablesKey.class, k -> new HashMap<>())).put(key, value);
	}
	
	
	protected void beforeCompletion(Runnable onFirstError) {
		preCompletion.stream().forEach(r -> {
			try {
				r.run();
			} catch (Exception e) {
				if(firstUnexpectedException.compareAndSet(null, e)) {
					onFirstError.run();
				} else {
					subsequentExceptions.add(e);
				}
				// TODO log this
			}
		});
	}
}
