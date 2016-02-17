package org.apache.aries.tx.control.jdbc.common.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.Executor;

import org.osgi.service.transaction.control.TransactionException;

public class TxConnectionWrapper extends ConnectionWrapper {

	private Connection delegate;

	public TxConnectionWrapper(Connection delegate) {
		this.delegate = delegate;
		try {
			delegate.setAutoCommit(false);
		} catch (SQLException e) {
			throw new TransactionException("Unable to disable autocommit");
		}
	}

	@Override
	protected final Connection getDelegate() {
		return delegate;
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		throw new TransactionException(
				"Auto-commit cannot be changed for a Transactional Connection");
	}

	@Override
	public void commit() throws SQLException {
		throw new TransactionException(
				"Commit cannot be called for a Transactional Connection");
	}

	@Override
	public void rollback() throws SQLException {
		throw new TransactionException(
				"Rollback cannot be called for a Transactional Connection");
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		throw new TransactionException(
				"Savepoints are not available for a Transactional Connection");
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		throw new TransactionException(
				"Savepoints are not available for a Transactional Connection");
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		throw new TransactionException(
				"Rollback cannot be called for a Transactional Connection");
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new TransactionException(
				"Savepoints are not available for a Transactional Connection");
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
