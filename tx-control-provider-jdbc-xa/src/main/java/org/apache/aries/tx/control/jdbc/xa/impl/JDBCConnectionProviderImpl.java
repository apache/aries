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
import java.util.UUID;

import javax.sql.DataSource;

import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;

public class JDBCConnectionProviderImpl implements JDBCConnectionProvider {

	private final UUID			uuid	= UUID.randomUUID();

	private final DataSource dataSource;
	
	private final boolean xaEnabled;
	
	private final boolean localEnabled;
	
	public JDBCConnectionProviderImpl(DataSource dataSource, boolean xaEnabled,
			boolean localEnabled) {
		this.dataSource = dataSource;
		this.xaEnabled = xaEnabled;
		this.localEnabled = localEnabled;
	}

	@Override
	public Connection getResource(TransactionControl txControl)
			throws TransactionException {
		return new XAEnabledTxContextBindingConnection(txControl, dataSource , uuid,
				xaEnabled, localEnabled);
	}

}
