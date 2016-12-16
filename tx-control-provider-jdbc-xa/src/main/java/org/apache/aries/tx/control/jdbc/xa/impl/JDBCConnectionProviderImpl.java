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

import org.apache.aries.tx.control.jdbc.common.impl.AbstractJDBCConnectionProvider;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

public class JDBCConnectionProviderImpl extends AbstractJDBCConnectionProvider {

	private final UUID			uuid	= UUID.randomUUID();

	private final boolean xaEnabled;
	
	private final boolean localEnabled;
	
	private final String recoveryIdentifier;
	
	public JDBCConnectionProviderImpl(DataSource dataSource, boolean xaEnabled,
			boolean localEnabled, String recoveryIdentifier) {
		super(dataSource);
		this.xaEnabled = xaEnabled;
		this.localEnabled = localEnabled;
		this.recoveryIdentifier = recoveryIdentifier;
	}

	@Override
	public Connection getResource(TransactionControl txControl)
			throws TransactionException {
		return new XAEnabledTxContextBindingConnection(txControl, this, uuid,
				xaEnabled, localEnabled, recoveryIdentifier);
	}
}
