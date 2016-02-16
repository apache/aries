package org.apache.aries.tx.control.service.local.impl;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.transaction.control.TransactionContext;

public abstract class AbstractTransactionContextImpl
		implements TransactionContext {

	protected static class TransactionVariablesKey {}

	AtomicReference<Exception> unexpectedException = new AtomicReference<>();
	
	protected final Coordination coordination;
	
	public AbstractTransactionContextImpl(Coordination coordination) {
		this.coordination = coordination;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getScopedValue(Object key) {
		return ((HashMap<Object,Object>) coordination.getVariables()
				.getOrDefault(AbstractTransactionContextImpl.TransactionVariablesKey.class, new HashMap<>()))
						.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putScopedValue(Object key, Object value) {
		((HashMap<Object,Object>) coordination.getVariables().computeIfAbsent(
				AbstractTransactionContextImpl.TransactionVariablesKey.class, k -> new HashMap<>())).put(key, value);
	}
}
