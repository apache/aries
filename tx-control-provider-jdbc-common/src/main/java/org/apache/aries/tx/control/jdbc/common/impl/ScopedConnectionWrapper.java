package org.apache.aries.tx.control.jdbc.common.impl;

import java.sql.Connection;
import java.util.concurrent.Executor;

public class ScopedConnectionWrapper extends ConnectionWrapper {

	private Connection delegate;

	public ScopedConnectionWrapper(Connection delegate) {
		this.delegate = delegate;
	}

	@Override
	protected final Connection getDelegate() {
		return delegate;
	}
	
	@Override
	public void close() {
		// A no-op
	}

	@Override
	public void abort(Executor e) {
		// A no-op
	}
}
