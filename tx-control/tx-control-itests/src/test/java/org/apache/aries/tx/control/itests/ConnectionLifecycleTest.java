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

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ConnectionLifecycleTest extends AbstractTransactionTest {

	@Test
	public void testStopOfTxControlBundle() {
		doBundleStoppingTest(b -> b.getSymbolicName().contains("tx-control-service"),
				"The transaction control service is closed");
	}

	@Test
	public void testStopOfJDBCBundle() {
		doBundleStoppingTest(b -> b.getSymbolicName().contains("tx-control-provider-jdbc"), 
				"There was a problem getting hold of a database connection");
	}

	private void doBundleStoppingTest(Predicate<Bundle> p, String exceptionMessage) {
		txControl.required(() -> connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));
		
		
		List<Bundle> toStop = Arrays.stream(context.getBundles())
			.filter(p)
			.collect(toList());
		
		System.out.println(toStop);
		
		try {
			toStop.stream()
				.forEach(b -> {
					System.out.println("Stopping " + b.getSymbolicName());
					try {
						b.stop();
					} catch (BundleException e) {}
				});
		
			try {
				assertEquals("Hello World!", txControl.notSupported(() -> {
					ResultSet rs = connection.createStatement()
							.executeQuery("Select * from TEST_TABLE");
					rs.next();
					return rs.getString(1);
				}));
				fail("Should not be accessible");
			} catch (ScopedWorkException swe) {
				assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
				assertEquals(exceptionMessage, swe.getCause().getMessage());
			} catch (TransactionException te) {
				assertEquals(exceptionMessage, te.getMessage());
			}
		} finally {
			toStop.stream()
				.forEach(b -> {
					try {
						b.start();
					} catch (BundleException e) {}
				});
		}
	}

	@Test
	public void testDeleteOfConfig() throws Exception {
		Assume.assumeTrue("Not a configuration test", isConfigured());
		
		
		txControl.required(() -> connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));
		
		
		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);
		
		Configuration[] configurations = cm.listConfigurations(
				"(service.factoryPid=org.apache.aries.tx.control.jdbc.*)");
		
		assertNotNull(configurations);
		assertEquals(1, configurations.length);
		
		configurations[0].delete();
		
		Thread.sleep(2000);
		
		try {
			assertEquals("Hello World!", txControl.notSupported(() -> {
				ResultSet rs = connection.createStatement()
						.executeQuery("Select * from TEST_TABLE");
				rs.next();
				return rs.getString(1);
			}));
			fail("Should not be accessible");
		} catch (ScopedWorkException swe) {
			assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
			assertEquals("There was a problem getting hold of a database connection", swe.getCause().getMessage());
		}
	}

	@Test
	public void testUpdateOfConfig() throws Exception {
		Assume.assumeTrue("Not a configuration test", isConfigured());
		
		
		txControl.required(() -> connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));
		
		
		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);
		
		Configuration[] configurations = cm.listConfigurations(
				"(service.factoryPid=org.apache.aries.tx.control.jdbc.*)");
		
		assertNotNull(configurations);
		assertEquals(1, configurations.length);
		
		configurations[0].update();
		
		Thread.sleep(2000);
		
		try {
			assertEquals("Hello World!", txControl.notSupported(() -> {
				ResultSet rs = connection.createStatement()
						.executeQuery("Select * from TEST_TABLE");
				rs.next();
				return rs.getString(1);
			}));
			fail("Should not be accessible");
		} catch (ScopedWorkException swe) {
			assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
			assertEquals("There was a problem getting hold of a database connection", swe.getCause().getMessage());
		}
	}
	
	@Test
	public void testReleaseOfFactoryService() {
		Assume.assumeFalse("Not a factory test", isConfigured());
		
		txControl.required(() -> connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));
		
		
		trackers.stream()
			.filter(t -> t.getService() instanceof JDBCConnectionProviderFactory)
			.findFirst()
			.get().close();;
		
		
		try {
			assertEquals("Hello World!", txControl.notSupported(() -> {
				ResultSet rs = connection.createStatement()
						.executeQuery("Select * from TEST_TABLE");
				rs.next();
				return rs.getString(1);
			}));
			fail("Should not be accessible");
		} catch (ScopedWorkException swe) {
			assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
			assertEquals("There was a problem getting hold of a database connection", swe.getCause().getMessage());
		}
	}
}
