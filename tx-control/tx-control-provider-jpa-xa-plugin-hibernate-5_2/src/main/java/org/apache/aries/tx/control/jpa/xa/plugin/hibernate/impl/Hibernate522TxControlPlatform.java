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
package org.apache.aries.tx.control.jpa.xa.plugin.hibernate.impl;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.osgi.service.transaction.control.TransactionControl;

/**
 * This plugin provides the extra feature needed to support Hibernate
 * 5.2.2 and above
 *
 */
public class Hibernate522TxControlPlatform extends Hibernate520TxControlPlatform { 

	private static final long serialVersionUID = 1L;

	public Hibernate522TxControlPlatform(ThreadLocal<TransactionControl> txControlToUse) {
		super(txControlToUse);
	}

	@Override
	public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return new TxControlDdlTransactionIsolator(jdbcContext);
	}

	public static class TxControlDdlTransactionIsolator implements DdlTransactionIsolator {

		private final JdbcContext jdbcContext;

		public TxControlDdlTransactionIsolator(JdbcContext jdbcContext) {
			this.jdbcContext = jdbcContext;
		}

		@Override
		public Connection getIsolatedConnection() {
			try {
				return jdbcContext.getJdbcConnectionAccess().obtainConnection();
			} catch (SQLException e) {
				throw new SchemaManagementException("Unable to access the transactional connection", e );
			}
		}

		@Override
		public JdbcContext getJdbcContext() {
			return jdbcContext;
		}

		@Override
		public void prepare() { }

		@Override
		public void release() { }
	}
}
