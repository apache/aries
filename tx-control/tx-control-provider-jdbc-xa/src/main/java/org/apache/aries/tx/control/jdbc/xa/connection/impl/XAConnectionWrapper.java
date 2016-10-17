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
package org.apache.aries.tx.control.jdbc.xa.connection.impl;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.common.impl.ConnectionWrapper;

public class XAConnectionWrapper extends ConnectionWrapper {

	private final Connection connection;
	
	private final XAResource xaResource;

	private final XAConnection xaConnection;
	
	public XAConnectionWrapper(XAConnection xaConnection) throws SQLException {
		this.xaConnection = xaConnection;
		this.connection = xaConnection.getConnection();
		this.xaResource = xaConnection.getXAResource();
	}

	@Override
	protected Connection getDelegate() {
		return connection;
	}

	public XAResource getXaResource() {
		return xaResource;
	}

	@Override
	public void close() throws SQLException {
		xaConnection.close();
	}
	
	
}
