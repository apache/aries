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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

	private static final long LIFETIME = 30000;
	
	private static final int CONNECTIONS = 17;
	
	@Override
	protected Map<String, Object> resourceProviderConfig() {
		// Set a short lifecycle for pooled connections and force a non-standard number
		Map<String, Object> config = new HashMap<>();
		config.put(JDBCConnectionProviderFactory.IDLE_TIMEOUT, LIFETIME/2);
		config.put(JDBCConnectionProviderFactory.CONNECTION_LIFETIME, LIFETIME);
		config.put(JDBCConnectionProviderFactory.MAX_CONNECTIONS, CONNECTIONS);
		config.put(JDBCConnectionProviderFactory.MIN_CONNECTIONS, CONNECTIONS);
		
		return config;
	}

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
		txControl.required(
				() -> connection.createStatement().execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));

		List<Bundle> toStop = Arrays.stream(context.getBundles()).filter(p).collect(toList());

		System.out.println(toStop);

		try {
			toStop.stream().forEach(b -> {
				System.out.println("Stopping " + b.getSymbolicName());
				try {
					b.stop();
				} catch (BundleException e) {
				}
			});

			try {
				assertEquals("Hello World!", txControl.notSupported(() -> {
					ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
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
			toStop.stream().forEach(b -> {
				try {
					b.start();
				} catch (BundleException e) {
				}
			});
		}
	}

	@Test
	public void testDeleteOfConfig() throws Exception {
		Assume.assumeTrue("Not a configuration test", isConfigured());

		txControl.required(
				() -> connection.createStatement().execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));

		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);

		Configuration[] configurations = cm
				.listConfigurations("(service.factoryPid=org.apache.aries.tx.control.jdbc.*)");

		assertNotNull(configurations);
		assertEquals(1, configurations.length);

		configurations[0].delete();

		Thread.sleep(2000);

		try {
			assertEquals("Hello World!", txControl.notSupported(() -> {
				ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
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

		txControl.required(
				() -> connection.createStatement().execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));

		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);

		Configuration[] configurations = cm
				.listConfigurations("(service.factoryPid=org.apache.aries.tx.control.jdbc.*)");

		assertNotNull(configurations);
		assertEquals(1, configurations.length);

		configurations[0].update();

		Thread.sleep(2000);

		try {
			assertEquals("Hello World!", txControl.notSupported(() -> {
				ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
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

		txControl.required(
				() -> connection.createStatement().execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));

		trackers.stream().filter(t -> t.getService() instanceof JDBCConnectionProviderFactory).findFirst().get()
				.close();
		;

		try {
			assertEquals("Hello World!", txControl.notSupported(() -> {
				ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
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
	public void testReleaseOfFactoryCreatedService() {
		Assume.assumeFalse("Not a factory test", isConfigured());
		
		txControl.required(
				() -> connection.createStatement().execute("Insert into TEST_TABLE values ( 'Hello World!' )"));
		
		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));
		
		JDBCConnectionProviderFactory factory = (JDBCConnectionProviderFactory) trackers.stream()
				.filter(t -> t.getService() instanceof JDBCConnectionProviderFactory)
				.findFirst()
				.get().getService();

		factory.releaseProvider(provider);
		
		try {
			assertEquals("Hello World!", txControl.notSupported(() -> {
				ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
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
	public void testPoolLifecycle() throws Exception {
		Set<String> allIds = new TreeSet<>();

		for(int i = 0; i < 100; i++) {
			Set<String> ids = txControl.notSupported(() -> {
				Set<String> sessionIds = new HashSet<>();
				
				ResultSet rs = connection.createStatement()
						.executeQuery("Select ID, SESSION_START from INFORMATION_SCHEMA.SESSIONS");
				while(rs.next()) {
					String connectionId = rs.getString(1);
					if(connectionId.length() == 1) {
						connectionId = "0" + connectionId;
					}
					sessionIds.add(connectionId + "-"
							+ rs.getString(2));
				}
				return sessionIds;
			});
			
			Set<String> newIds = ids.stream()
						.filter(id -> !allIds.contains(id))
						.collect(Collectors.toSet());
			
			allIds.addAll(ids);
			System.out.println("Currently there are " + ids.size() + " connections");
			System.out.println("In total there have been " + allIds.size() + " connections");
			
			int currentConnections = ids.size();
			
			if(currentConnections > CONNECTIONS) {
				if((currentConnections - newIds.size()) <= CONNECTIONS) {
					System.out.println("The number of connections is too high at " + currentConnections +
							", but " + newIds.size() + " new connections have just been added. The previous connections may be in the process of being closed and so this loop will not fail.");
				} else {
					fail("Too many sessions " + currentConnections);
				}
			}
				
			Thread.sleep(500);
		}
		
		int size = allIds.size();
		if(size <= CONNECTIONS + 1) {
			assertEquals("Expected 34 sessions, but found " + size + " " + allIds, 34, size);
		} else if(size <= (2 * CONNECTIONS)) {
			System.out.println("We really should have 34 sessions, but " + size  + 
					" is probably enough ");
		} else {
			fail("There should not need to be more than " + (2 * CONNECTIONS) + " connections");
		}
	}
}
