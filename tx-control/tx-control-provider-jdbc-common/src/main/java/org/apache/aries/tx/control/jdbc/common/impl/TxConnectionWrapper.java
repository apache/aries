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
			throw new TransactionException("Unable to disable autocommit", e);
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
