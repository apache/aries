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
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Predicate;

import org.apache.aries.tx.control.itests.entity.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JPALifecycleTest extends AbstractJPATransactionTest {

	private static final long LIFETIME = 30000;
	
	private static final int CONNECTIONS = 17;
	
	protected Option jpaProvider() {
		return CoreOptions.composite(
			// Add JTA 1.1 as a system package because of the link to javax.sql
			// Also set javax.xml.stream to 1.0 due to hibernate's funny packaging
			
			systemProperty(ARIES_EMF_BUILDER_TARGET_FILTER)
				.value("(osgi.unit.provider=org.hibernate.jpa.HibernatePersistenceProvider)"),
			systemPackage("javax.xml.stream;version=1.0"),
			systemPackage("javax.xml.stream.events;version=1.0"),
			systemPackage("javax.xml.stream.util;version=1.0"),
			systemPackage("javax.transaction;version=1.1"),
			systemPackage("javax.transaction.xa;version=1.1"),
			bootClasspathLibrary(mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1")).beforeFramework(),
			
			// Hibernate bundles and their dependencies (JPA API is available from the tx-control)
			mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.antlr", "2.7.7_5"),
			mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.dom4j", "1.6.1_5"),
			mavenBundle("org.javassist", "javassist", "3.18.1-GA"),
			mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"),
			mavenBundle("org.jboss", "jandex", "2.0.0.Final"),
			mavenBundle("org.hibernate.common", "hibernate-commons-annotations", "5.0.1.Final"),
			mavenBundle("org.hibernate", "hibernate-core", "5.0.9.Final"),
			mavenBundle("org.hibernate", "hibernate-osgi", "5.0.9.Final"),
			mavenBundle("org.hibernate", "hibernate-entitymanager", "5.0.9.Final"));
	}
	
	@Override
	protected Dictionary<String, Object> getBaseProperties() {
		// Set a short lifecycle for pooled connections and force a non-standard number
		Dictionary<String, Object> config = new Hashtable<>();
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
	public void testStopOfJPABundle() {
		doBundleStoppingTest(b -> b.getSymbolicName().contains("tx-control-provider-jpa"),
				"There was a problem getting hold of a database connection");
	}

	private void doBundleStoppingTest(Predicate<Bundle> p, String exceptionMessage) {
		Message m = new Message();
		m.message = "Hello World";
		txControl.required(() -> {em.persist(m); return null;});

		assertEquals(m.message, txControl.notSupported(() -> em.find(Message.class, m.id).message));

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
				assertEquals(m.message, txControl.notSupported(() -> em.find(Message.class, m.id).message));
				fail("Should not be accessible " + (Boolean.getBoolean(IS_XA) ? "xa" : "local"));
			} catch (ScopedWorkException swe) {
				assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
				assertEquals(exceptionMessage, swe.getCause().getMessage());
			} catch (TransactionException te) {
				assertEquals(exceptionMessage, te.getMessage());
			}
		} finally {
			toStop.stream().forEach(b -> {
				System.out.println("Restarting " + b.getSymbolicName());
				try {
					b.start();
				} catch (BundleException e) {
				}
			});
			getService(JPAEntityManagerProvider.class, 5000);
		}
	}

	@Test
	public void testDeleteOfConfig() throws Exception {
		
		Message m = new Message();
		m.message = "Hello World";
		txControl.required(() -> {em.persist(m); return null;});

		assertEquals(m.message, txControl.notSupported(() -> em.find(Message.class, m.id).message));

		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);

		Configuration[] configurations = cm
				.listConfigurations("(service.factoryPid=org.apache.aries.tx.control.jpa.*)");

		assertNotNull(configurations);
		assertEquals(1, configurations.length);

		configurations[0].delete();

		Thread.sleep(2000);

		try {
			assertEquals(m.message, txControl.notSupported(() -> em.find(Message.class, m.id).message));
			fail("Should not be accessible " + (Boolean.getBoolean(IS_XA) ? "xa" : "local"));
		} catch (ScopedWorkException swe) {
			assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
			assertEquals("There was a problem getting hold of a database connection", swe.getCause().getMessage());
		}
	}

	@Test
	public void testUpdateOfConfig() throws Exception {
		
		Message m = new Message();
		m.message = "Hello World";
		txControl.required(() -> {em.persist(m); return null;});

		assertEquals(m.message, txControl.notSupported(() -> em.find(Message.class, m.id).message));

		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);

		Configuration[] configurations = cm
				.listConfigurations("(service.factoryPid=org.apache.aries.tx.control.jpa.*)");

		assertNotNull(configurations);
		assertEquals(1, configurations.length);

		configurations[0].update();

		Thread.sleep(2000);

		try {
			assertEquals(m.message, txControl.notSupported(() -> em.find(Message.class, m.id).message));
			fail("Should not be accessible " + (Boolean.getBoolean(IS_XA) ? "xa" : "local"));
		} catch (ScopedWorkException swe) {
			assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
			assertEquals("There was a problem getting hold of a database connection", swe.getCause().getMessage());
		}
	}
//
//	@Test
//	public void testReleaseOfFactoryService() {
//		Assume.assumeFalse("Not a factory test", isConfigured());
//
//		txControl.required(
//				() -> connection.createStatement().execute("Insert into TEST_TABLE values ( 'Hello World!' )"));
//
//		assertEquals("Hello World!", txControl.notSupported(() -> {
//			ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
//			rs.next();
//			return rs.getString(1);
//		}));
//
//		trackers.stream().filter(t -> t.getService() instanceof JDBCConnectionProviderFactory).findFirst().get()
//				.close();
//		;
//
//		try {
//			assertEquals("Hello World!", txControl.notSupported(() -> {
//				ResultSet rs = connection.createStatement().executeQuery("Select * from TEST_TABLE");
//				rs.next();
//				return rs.getString(1);
//			}));
//			fail("Should not be accessible");
//		} catch (ScopedWorkException swe) {
//			assertTrue(swe.getCause().toString(), swe.getCause() instanceof TransactionException);
//			assertEquals("There was a problem getting hold of a database connection", swe.getCause().getMessage());
//		}
//	}
}
