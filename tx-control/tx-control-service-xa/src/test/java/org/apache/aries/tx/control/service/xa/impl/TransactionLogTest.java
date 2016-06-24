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
package org.apache.aries.tx.control.service.xa.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.XAConnection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransactionLogTest {

	TransactionControlImpl txControl;
	
	JdbcDataSource dataSource;

	@Before
	public void setUp() throws Exception {
		Map<String, Object> config = new HashMap<>();
		config.put("recovery.enabled", true);
		config.put("recovery.log.dir", "target/generated/recoverylog");
		
		txControl = new TransactionControlImpl(null, config);
		
		dataSource = new JdbcDataSource();
		dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
		
		try (Connection conn = dataSource.getConnection()) {
			Statement s = conn.createStatement();
			try {s.execute("DROP TABLE TEST_TABLE");} catch (SQLException sqle) {}
			s.execute("CREATE TABLE TEST_TABLE ( message varchar(255) )");
		}
	}
	
	@After
	public void destroy() {
		txControl.destroy();
	}

	@Test
	public void testRequired() throws Exception {
		XAConnection xaConn = dataSource.getXAConnection();
		try {
			txControl.required(() -> {
	
				txControl.getCurrentContext().registerXAResource(xaConn.getXAResource());
	
				Connection conn = xaConn.getConnection();
				
				return conn.createStatement()
					.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			});	
		} finally {
			xaConn.close();
		}

		try (Connection conn = dataSource.getConnection()) {
			ResultSet rs = conn.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			assertEquals("Hello World!", rs.getString(1));
		}
	}

	@Test
	public void testRequiredWithRollback() throws Exception {
		XAConnection xaConn = dataSource.getXAConnection();
		try {
			txControl.required(() -> {
				
				txControl.getCurrentContext().registerXAResource(xaConn.getXAResource());
				
				Connection conn = xaConn.getConnection();
				
				conn.createStatement()
						.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
				
				txControl.setRollbackOnly();
				return null;
			});	
		} finally {
			xaConn.close();
		}
		
		try (Connection conn = dataSource.getConnection()) {
			ResultSet rs = conn.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			assertFalse(rs.next());
		}
	}

}
