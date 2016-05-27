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
