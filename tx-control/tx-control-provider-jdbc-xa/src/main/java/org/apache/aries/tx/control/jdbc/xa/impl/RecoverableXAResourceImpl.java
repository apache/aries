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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.osgi.service.transaction.control.recovery.RecoverableXAResource;

public class RecoverableXAResourceImpl implements RecoverableXAResource {

	private final String id;
	
	private final JDBCConnectionProviderImpl providerImpl;
	
	private final String recoveryUser;
	
	private final String recoveryPw;
	
	public RecoverableXAResourceImpl(String id, JDBCConnectionProviderImpl providerImpl, String recoveryUser,
			String recoveryPw) {
		this.id = id;
		this.providerImpl = providerImpl;
		this.recoveryUser = recoveryUser;
		this.recoveryPw = recoveryPw;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public XAResource getXAResource() throws Exception {
		DataSource rawDataSource = providerImpl.getRawDataSource();

		Connection recoveryConn;
		if(recoveryUser != null) {
			recoveryConn = rawDataSource.getConnection(recoveryUser, recoveryPw);
		} else {
			recoveryConn = rawDataSource.getConnection();
		}
		
		return new CloseableXAResource(recoveryConn);
	}

	@Override
	public void releaseXAResource(XAResource xaRes) {
		if(xaRes instanceof CloseableXAResource) {
			try {
				((CloseableXAResource) xaRes).close();
			} catch (Exception e) {
				// This is fine, the connection has been returned
			}
		} else {
			throw new IllegalArgumentException("The XAResource being returned was not created by this provider implementation");
		}
	}

	private static class CloseableXAResource implements XAResource, AutoCloseable {
		private final Connection conn;
		
		private final XAResource resource;
		
		public CloseableXAResource(Connection conn) throws SQLException {
			conn.isValid(5);
			this.conn = conn;
			this.resource = XAEnabledTxContextBindingConnection.getXAResource(conn);
		}

		@Override
		public void close() throws Exception {
			conn.close();
		}

		public void commit(Xid arg0, boolean arg1) throws XAException {
			resource.commit(arg0, arg1);
		}

		public void end(Xid arg0, int arg1) throws XAException {
			resource.end(arg0, arg1);
		}

		public void forget(Xid arg0) throws XAException {
			resource.forget(arg0);
		}

		public int getTransactionTimeout() throws XAException {
			return resource.getTransactionTimeout();
		}

		public boolean isSameRM(XAResource arg0) throws XAException {
			return resource.isSameRM(arg0);
		}

		public int prepare(Xid arg0) throws XAException {
			return resource.prepare(arg0);
		}

		public Xid[] recover(int arg0) throws XAException {
			return resource.recover(arg0);
		}

		public void rollback(Xid arg0) throws XAException {
			resource.rollback(arg0);
		}

		public boolean setTransactionTimeout(int arg0) throws XAException {
			return resource.setTransactionTimeout(arg0);
		}

		public void start(Xid arg0, int arg1) throws XAException {
			resource.start(arg0, arg1);
		}
	}
}
