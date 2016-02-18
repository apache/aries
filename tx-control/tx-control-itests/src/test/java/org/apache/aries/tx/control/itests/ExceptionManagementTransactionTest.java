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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionRolledBackException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ExceptionManagementTransactionTest extends AbstractIntegrationTest {

	@Inject
	TransactionControl txControl;

	@Inject
	JDBCConnectionProviderFactory resourceProviderFactory;
	
	@Inject
	DataSourceFactory dsf;
	
	Connection connection;

	@Before
	public void setUp() {
		Properties jdbc = new Properties();
		
		jdbc.setProperty(DataSourceFactory.JDBC_URL, "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
		
		connection = resourceProviderFactory.getProviderFor(dsf, jdbc, null).getResource(txControl);
		
		
		txControl.required(() -> {
				Statement s = connection.createStatement();
				try {
					s.execute("DROP TABLE TEST_TABLE");
				} catch (SQLException sqle) {}
					s.execute("CREATE TABLE TEST_TABLE ( message varchar(255) )");
					return null;
			});
	}
	
	@After
	public void tearDown() {

		txControl.required(() -> connection.createStatement()
				.execute("DROP TABLE TEST_TABLE"));

	}
	
	@Test
	public void testRuntimeException() {
		
		RuntimeException toThrow = new RuntimeException("Bang!");
		
		try {
			txControl.required(() -> {
						connection.createStatement()
							.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
						throw toThrow;
					});
			fail("An exception should occur!");
		} catch (TransactionRolledBackException tre) {
			assertSame(toThrow, tre.getCause());
		}

		assertRollback();
	}

	@Test
	public void testCheckedException() {
		URISyntaxException toThrow = new URISyntaxException("yuck", "Bang!");
		
		try {
			txControl.required(() -> {
						connection.createStatement()
							.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
						throw toThrow;
					});
			fail("An exception should occur!");
			// We have to catch Exception as the compiler complains
			// otherwise
		} catch (TransactionRolledBackException tre) {
			assertSame(toThrow, tre.getCause());
		}

		assertRollback();
	}

	//This test currently fails - the local implementation should probably
	//use the coordinator a little differently
	@Test
	@Ignore
	public void testPreCompletionException() {
		RuntimeException toThrow = new RuntimeException("Bang!");
		
		try {
			txControl.required(() -> {
				txControl.getCurrentContext().preCompletion(() -> {
						throw toThrow;
					});
				return connection.createStatement()
					.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			});
			fail("An exception should occur!");
			// We have to catch Exception as the compiler complains
			// otherwise
		} catch (TransactionRolledBackException tre) {
			assertSame(toThrow, tre.getCause());
		}
		
		assertRollback();
	}

	private void assertRollback() {
		assertEquals(Integer.valueOf(0), txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select count(*) from TEST_TABLE");
			rs.next();
			return rs.getInt(1);
		}));
	}

	@Configuration
	public Option[] configuration() {
		String localRepo = System.getProperty("maven.repo.local");
		if (localRepo == null) {
			localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
		}
		return options(junitBundles(), systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
				when(localRepo != null)
						.useOptions(CoreOptions.vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo)),
				mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.coordinator").versionAsInProject(),
				mavenBundle("org.apache.aries.tx-control", "tx-control-service-local").versionAsInProject(),
				mavenBundle("com.h2database", "h2").versionAsInProject(),
				mavenBundle("org.apache.aries.tx-control", "tx-control-provider-jdbc-local").versionAsInProject(),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject()
				
		/*
		 * vmOption
		 * ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
		 * waitForFrameworkStartup(),
		 */
		);
	}

}
