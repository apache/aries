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
package org.apache.aries.tx.control.jdbc.xa.impl;

import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.common.impl.AbstractJDBCConnectionProvider;
import org.apache.aries.tx.control.jdbc.common.impl.ConnectionWrapper;
import org.apache.aries.tx.control.jdbc.common.impl.ScopedConnectionWrapper;
import org.apache.aries.tx.control.jdbc.common.impl.TxConnectionWrapper;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XAConnectionWrapper;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

public class XAEnabledTxContextBindingConnection extends ConnectionWrapper {

	private final TransactionControl				txControl;
	private final UUID								resourceId;
	private final AbstractJDBCConnectionProvider	provider;
	private final boolean							xaEnabled;
	private final boolean							localEnabled;
	private final String							recoveryIdentifier;

	public XAEnabledTxContextBindingConnection(TransactionControl txControl,
			AbstractJDBCConnectionProvider provider, UUID resourceId, boolean xaEnabled, boolean localEnabled,
			String recoveryIdentifier) {
		this.txControl = txControl;
		this.provider = provider;
		this.resourceId = resourceId;
		this.xaEnabled = xaEnabled;
		this.localEnabled = localEnabled;
		this.recoveryIdentifier = recoveryIdentifier;
	}

	@Override
	protected final Connection getDelegate() {

		TransactionContext txContext = txControl.getCurrentContext();

		if (txContext == null) {
			throw new TransactionException("The resource " + provider
					+ " cannot be accessed outside of an active Transaction Context");
		}

		Connection existing = (Connection) txContext.getScopedValue(resourceId);

		if (existing != null) {
			return existing;
		}

		Connection toReturn;
		Connection toClose;

		try {
			if (txContext.getTransactionStatus() == NO_TRANSACTION) {
				toClose = provider.getConnection();
				toReturn = new ScopedConnectionWrapper(toClose);
			} else if (txContext.supportsXA() && xaEnabled) {
				toClose = provider.getConnection();
				toReturn = new TxConnectionWrapper(toClose);
				txContext.registerXAResource(getXAResource(toClose), recoveryIdentifier);
			} else if (txContext.supportsLocal() && localEnabled) {
				toClose = provider.getConnection();
				toReturn = new TxConnectionWrapper(toClose);
				txContext.registerLocalResource(getLocalResource(toClose));
			} else {
				throw new TransactionException(
						"There is a transaction active, but it does not support local participants");
			}
		} catch (Exception sqle) {
			throw new TransactionException(
					"There was a problem getting hold of a database connection",
					sqle);
		}

		
		txContext.postCompletion(x -> {
				try {
					toClose.close();
				} catch (SQLException sqle) {
					// TODO log this
				}
			});
		
		txContext.putScopedValue(resourceId, toReturn);
		
		return toReturn;
	}

	
	static XAResource getXAResource(Connection conn) throws SQLException {
		if(conn instanceof XAConnectionWrapper) {
			return ((XAConnectionWrapper)conn).getXaResource();
		} else if(conn.isWrapperFor(XAConnectionWrapper.class)){
			return conn.unwrap(XAConnectionWrapper.class).getXaResource();
		} else {
			throw new IllegalArgumentException("The XAResource for the connection cannot be found");
		}
	}
	
	private LocalResource getLocalResource(Connection conn) {
		return new LocalResource() {
			@Override
			public void commit() throws TransactionException {
				try {
					conn.commit();
				} catch (SQLException e) {
					throw new TransactionException(
							"An error occurred when committing the connection", e);
				}
			}

			@Override
			public void rollback() throws TransactionException {
				try {
					conn.rollback();
				} catch (SQLException e) {
					throw new TransactionException(
							"An error occurred when rolling back the connection", e);
				}
			}

		};
	}
}
