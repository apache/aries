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

import static javax.transaction.xa.XAResource.XA_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.TransactionRolledBackException;
import org.osgi.service.transaction.control.TransactionStatus;
import org.osgi.service.transaction.control.recovery.RecoverableXAResource;

/**
 * The tests in this class look a little odd because we're using an
 * unmanaged resource. This is to avoid creating a dependency on a
 * JDBCResourceProvider just for the tests, and to give explicit
 * control of when things get registered
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionLogTest {

	@Mock
	BundleContext ctx;

	@Mock
	ServiceReference<RecoverableXAResource> serviceRef;
	
	TransactionControlImpl txControl;
	
	JdbcDataSource dataSource;
	
	Server server;
	
	@Before
	public void setUp() throws Exception {
		Map<String, Object> config = new HashMap<>();
		config.put("recovery.log.enabled", true);
		config.put("recovery.log.dir", "target/recovery-test/recoverylog");
		
		txControl = new TransactionControlImpl(ctx, config);
		
		setupServerAndDataSource();
		
		try (Connection conn = dataSource.getConnection()) {
			Statement s = conn.createStatement();
			try {s.execute("DROP TABLE TEST_TABLE");} catch (SQLException sqle) {}
			s.execute("CREATE TABLE TEST_TABLE ( message varchar(255) )");
		}
	}

	private void setupServerAndDataSource() throws SQLException {
		server = Server.createTcpServer("-tcpPort", "0");
		server.start();
		
		File dbPath = new File("target/recovery-test/database");
		
		dataSource = new JdbcDataSource();
		dataSource.setUrl("jdbc:h2:tcp://127.0.0.1:" + server.getPort() + "/" + dbPath.getAbsolutePath());
	}
	
	@After
	public void destroy() {
		txControl.destroy();
		try (Connection conn = dataSource.getConnection()) {
			conn.createStatement().execute("shutdown immediately");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		delete(new File("target/recovery-test"));
	}

	private void delete(File file) {
		if(file.isDirectory()) {
			for(File f : file.listFiles()) {
				delete(f);
			}
		} 
		file.delete();
	}

	@Test
	public void testRequiredNoRecovery() throws Exception {
		XAConnection xaConn = dataSource.getXAConnection();
		try {
			txControl.required(() -> {
	
				txControl.getCurrentContext().registerXAResource(xaConn.getXAResource(), null);
	
				Connection conn = xaConn.getConnection();
				// conn.setAutoCommit(false);
				
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
			assertFalse(rs.next());
		}
	}

	@Test
	public void testRequired2PCNoRecovery() throws Exception {
		XAConnection xaConn = dataSource.getXAConnection();
		XAConnection xaConn2 = dataSource.getXAConnection();
		try {
			txControl.required(() -> {
				
				txControl.getCurrentContext().registerXAResource(xaConn.getXAResource(), null);
				txControl.getCurrentContext().registerXAResource(xaConn2.getXAResource(), null);
				
				Connection conn = xaConn.getConnection();
				// conn.setAutoCommit(false);
				Connection conn2 = xaConn2.getConnection();
				conn2.setAutoCommit(false);
				
				conn.createStatement()
						.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
				return conn2.createStatement()
						.execute("Insert into TEST_TABLE values ( 'Hello World 2!' )");
			});	
		} finally {
			xaConn.close();
		}
		
		try (Connection conn = dataSource.getConnection()) {
			ResultSet rs = conn.createStatement()
					.executeQuery("Select * from TEST_TABLE order by message DESC");
			rs.next();
			assertEquals("Hello World!", rs.getString(1));
			rs.next();
			assertEquals("Hello World 2!", rs.getString(1));
			assertFalse(rs.next());
		}
	}

	@Test
	public void testRequiredRecoverable() throws Exception {
		XAConnection xaConn = dataSource.getXAConnection();
		try {
			txControl.required(() -> {
				
				txControl.getCurrentContext().registerXAResource(xaConn.getXAResource(), "foo");
				
				Connection conn = xaConn.getConnection();
				// conn.setAutoCommit(false);
				
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
	public void testRequiredRecoveryRequiredPrePrepare() throws Exception {
		doRecoveryRequired((good, poison) -> {
				txControl.getCurrentContext().registerXAResource(poison, null);
				txControl.getCurrentContext().registerXAResource(good, "foo");
			}, TransactionStatus.ROLLED_BACK);
		
		boolean success = false;
		XAConnection conn = dataSource.getXAConnection();
		for(int i=0; i < 5; i++) {
			if(conn.getXAResource().recover(XAResource.TMSTARTRSCAN).length == 0) {
				success = true;
				break;
			} else {
				// Wait for recovery to happen!
				Thread.sleep(500);
			}
		}
		
		assertTrue("No recovery in time", success);
	}
	
	@Test
	public void testRequiredRecoveryRequiredPostPrepare() throws Exception {
		doRecoveryRequired((good, poison) -> {
				txControl.getCurrentContext().registerXAResource(good, "foo");
				txControl.getCurrentContext().registerXAResource(poison, null);
			}, TransactionStatus.COMMITTED);
		
		boolean success = false;
		for(int i=0; i < 5; i++) {
			try (Connection conn = dataSource.getConnection()) {
				ResultSet rs = conn.createStatement()
						.executeQuery("Select * from TEST_TABLE");
				if(rs.next()) {
					assertEquals("Hello World!", rs.getString(1));
					success = true;
					break;
				} else {
					// Wait for recovery to happen!
					Thread.sleep(500);
				}
			}
		}
		
		assertTrue("No recovery in time", success);
	}
	
	public void doRecoveryRequired(BiConsumer<XAResource, XAResource> ordering, 
			TransactionStatus expectedFinalState) throws Exception {
		
		//Register the recoverable resource
		ArgumentCaptor<ServiceListener> captor = ArgumentCaptor.forClass(ServiceListener.class);
		Mockito.verify(ctx).addServiceListener(captor.capture(), Mockito.anyString());
		Mockito.when(ctx.getService(serviceRef)).thenReturn(new TestRecoverableResource("foo", dataSource));
		
		captor.getValue().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, serviceRef));
		
		XAConnection xaConn = dataSource.getXAConnection();
		AtomicReference<TransactionStatus> ref = new AtomicReference<TransactionStatus>();
		try {
			txControl.required(() -> {
				
				txControl.getCurrentContext().postCompletion(ref::set);
				
				Connection conn = xaConn.getConnection();
				// conn.setAutoCommit(false);
				
				XAResource dsResource = xaConn.getXAResource();
				
				XAResource poison = Mockito.mock(XAResource.class);
				Mockito.when(poison.prepare(Mockito.any())).thenAnswer(i -> {
					// Now kill the db server before it commits!
					conn.createStatement().execute("shutdown immediately");
					Thread.sleep(1000);
					return XA_OK;	
				});

				ordering.accept(dsResource, poison);
				
				return conn.createStatement()
						.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			});	
		} catch (TransactionException te) {
			assertEquals(expectedFinalState, ref.get());
			assertEquals(expectedFinalState == ROLLED_BACK, te instanceof TransactionRolledBackException);
		} finally {
			try {
				xaConn.close();
			} catch (SQLException sqle) {}
		}
		
		setupServerAndDataSource();
		
	}

	static class TestRecoverableResource implements RecoverableXAResource {

		private final String id;
		
		private final JdbcDataSource dataSource;
		
		public TestRecoverableResource(String id, JdbcDataSource dataSource) {
			this.id = id;
			this.dataSource = dataSource;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public XAResource getXAResource() throws Exception {
			XAConnection xaConnection = dataSource.getXAConnection();
			if(xaConnection.getConnection().isValid(2)) {
				return xaConnection.getXAResource();
			} else {
				return null;
			}
		}

		@Override
		public void releaseXAResource(XAResource xaRes) {
			// This is valid for H2;
			try {
				((XAConnection) xaRes).close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	@Test
	public void testRequiredWithRollback() throws Exception {
		XAConnection xaConn = dataSource.getXAConnection();
		try {
			txControl.required(() -> {
				
				txControl.getCurrentContext().registerXAResource(xaConn.getXAResource(), null);
				
				Connection conn = xaConn.getConnection();
				// conn.setAutoCommit(false);
				
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
