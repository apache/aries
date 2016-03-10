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
package org.apache.aries.tx.control.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.inject.Inject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionRolledBackException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class XATransactionTest extends AbstractIntegrationTest {

	@Inject
	@Filter("(osgi.xa.enabled=true)")
	private TransactionControl txControl;
	
	@Inject
	@Filter("(osgi.xa.enabled=true)")
	private JDBCConnectionProviderFactory factory;
	
	protected Connection connection1;
	protected Connection connection2;

	private Server server1;
	private Server server2;

	@Before
	public void setUp() throws Exception {
		Properties jdbc = new Properties();
		
		server1 = Server.createTcpServer("-tcpPort", "0");
		server1.start();

		server2 = Server.createTcpServer("-tcpPort", "0");
		server2.start();
			
		String jdbcUrl1 = "jdbc:h2:tcp://127.0.0.1:" + server1.getPort() + "/" + getRemoteDBPath("db1");
		String jdbcUrl2 = "jdbc:h2:tcp://127.0.0.1:" + server2.getPort() + "/" + getRemoteDBPath("db2");
		
		jdbc.setProperty(DataSourceFactory.JDBC_URL, jdbcUrl1);
		connection1 = programaticConnection(jdbc);
		
		jdbc.setProperty(DataSourceFactory.JDBC_URL, jdbcUrl2);
		connection2 = programaticConnection(jdbc);
		
		txControl.required(() -> {
				Statement s = connection1.createStatement();
				try {
					s.execute("DROP TABLE TEST_TABLE");
				} catch (SQLException sqle) {}
				s.execute("CREATE TABLE TEST_TABLE ( message varchar(255) )");
				return null;
			});
		txControl.required(() -> {
				Statement s = connection2.createStatement();
				try {
					s.execute("DROP TABLE TEST_TABLE");
				} catch (SQLException sqle) {}
				s.execute("CREATE TABLE TEST_TABLE ( idValue varchar(16) PRIMARY KEY )");
				return null;
			});
	}

	private Connection programaticConnection(Properties jdbc) {
		
		JDBCConnectionProviderFactory resourceProviderFactory = context()
				.getService(JDBCConnectionProviderFactory.class, 5000);
		
		DataSourceFactory dsf = context().getService(DataSourceFactory.class, 5000);
		
		return resourceProviderFactory.getProviderFor(dsf, jdbc, null).getResource(txControl);
	}

	@After
	public void tearDown() {

		try {
			txControl.required(() -> connection1.createStatement()
					.execute("DROP TABLE TEST_TABLE"));
		} catch (Exception e) {}
		try {
			txControl.required(() -> connection2.createStatement()
					.execute("DROP TABLE TEST_TABLE"));
		} catch (Exception e) {}
		
		
		if(server1 != null) {
			server1.stop();
		}

		if(server2 != null) {
			server2.stop();
		}

		connection1 = null;
		connection2 = null;
	}

	@Test
	public void testTwoPhaseCommit() {
		txControl.required(() -> {
			connection1.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			
			connection2.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello 1' )");
			
			return null;
		});
		
		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection1.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));

		assertEquals("Hello 1", txControl.notSupported(() -> {
			ResultSet rs = connection2.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));
	}

	@Test
	public void testTwoPhaseRollback() {
		try {
			txControl.required(() -> {
				connection1.createStatement()
					.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
				
				connection2.createStatement()
					.execute("Insert into TEST_TABLE values ( 'Hello 1' )");
				
				txControl.requiresNew(() -> {
						connection2.createStatement()
							.execute("Insert into TEST_TABLE values ( 'Hello 2' )");
						return null;
					});
				
				txControl.getCurrentContext().registerXAResource(new PoisonResource());
				
				return null;
			});
			fail("Should roll back");
		} catch (TransactionRolledBackException trbe) {
		}
		
		assertEquals(0, (int) txControl.notSupported(() -> {
			ResultSet rs = connection1.createStatement()
					.executeQuery("Select count(*) from TEST_TABLE");
			rs.next();
			return rs.getInt(1);
		}));
		
		
		assertEquals("1: Hello 2", txControl.notSupported(() -> {
			Statement s = connection2.createStatement();
			
			ResultSet rs = s.executeQuery("Select count(*) from TEST_TABLE");
			rs.next();
			int count = rs.getInt(1);
			
			rs = s.executeQuery("Select idValue from TEST_TABLE ORDER BY idValue");
			
			rs.next();
			return "" + count + ": " + rs.getString(1);
		}));
	}
	
	
	@Configuration
	public Option[] xaServerH2XATxConfiguration() {
		String localRepo = System.getProperty("maven.repo.local");
		if (localRepo == null) {
			localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
		}
		
		return options(junitBundles(), systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
				when(localRepo != null)
				.useOptions(CoreOptions.vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo)),
				mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.coordinator").versionAsInProject(),
				mavenBundle("org.apache.aries.tx-control", "tx-control-service-xa").versionAsInProject(),
				mavenBundle("com.h2database", "h2").versionAsInProject(),
				mavenBundle("org.apache.aries.tx-control", "tx-control-provider-jdbc-xa").versionAsInProject(),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject()
				
//				,CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
				);
	}

	private String getRemoteDBPath(String name) {
		String fullResourceName = getClass().getName().replace('.', '/') + ".class";
		
		String resourcePath = getClass().getResource(getClass().getSimpleName() + ".class").getPath();
		
		File testClassesDir = new File(resourcePath.substring(0, resourcePath.length() - fullResourceName.length()));
		
		String dbPath = new File(testClassesDir.getParentFile(), "testdb/" + name).getAbsolutePath();
		return dbPath;
	}
	
	private static class PoisonResource implements XAResource {

		@Override
		public void commit(Xid arg0, boolean arg1) throws XAException {
			throw new XAException(XAException.XA_RBOTHER);
		}

		@Override
		public void end(Xid arg0, int arg1) throws XAException {
		}

		@Override
		public void forget(Xid arg0) throws XAException {
		}

		@Override
		public int getTransactionTimeout() throws XAException {
			return 30;
		}

		@Override
		public boolean isSameRM(XAResource arg0) throws XAException {
			return false;
		}

		@Override
		public int prepare(Xid arg0) throws XAException {
			throw new XAException(XAException.XA_RBOTHER);
		}

		@Override
		public Xid[] recover(int arg0) throws XAException {
			return new Xid[0];
		}

		@Override
		public void rollback(Xid arg0) throws XAException {
		}

		@Override
		public boolean setTransactionTimeout(int arg0) throws XAException {
			return false;
		}

		@Override
		public void start(Xid arg0, int arg1) throws XAException {
		}
	}
}
