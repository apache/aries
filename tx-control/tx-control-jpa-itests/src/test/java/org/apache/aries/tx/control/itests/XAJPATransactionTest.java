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
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.aries.tx.control.itests.entity.Message;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionRolledBackException;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class XAJPATransactionTest {

	static final String XA_TEST_UNIT_1 = "xa-test-unit-1";
	static final String XA_TEST_UNIT_2 = "xa-test-unit-2";

	protected static final String ARIES_EMF_BUILDER_TARGET_FILTER = "aries.emf.builder.target.filter";
	
	@Inject
	BundleContext context;
	
	@Inject
	@Filter("(osgi.xa.enabled=true)")
	protected TransactionControl txControl;

	protected EntityManager em1;
	protected EntityManager em2;

	private Server server1;
	private Server server2;
	
	private final List<ServiceTracker<?,?>> trackers = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		server1 = Server.createTcpServer("-tcpPort", "0");
		server1.start();

		server2 = Server.createTcpServer("-tcpPort", "0");
		server2.start();
			
		String jdbcUrl1 = "jdbc:h2:tcp://127.0.0.1:" + server1.getPort() + "/" + getRemoteDBPath("db1");
		String jdbcUrl2 = "jdbc:h2:tcp://127.0.0.1:" + server2.getPort() + "/" + getRemoteDBPath("db2");
		
		em1 = configuredEntityManager(jdbcUrl1, XA_TEST_UNIT_1);
		em2 = configuredEntityManager(jdbcUrl2, XA_TEST_UNIT_2);
	}

	private String getRemoteDBPath(String dbName) {
		String fullResourceName = getClass().getName().replace('.', '/') + ".class";
		
		String resourcePath = getClass().getClassLoader().getResource(fullResourceName).getPath();
		
		File testClassesDir = new File(resourcePath.substring(0, resourcePath.length() - fullResourceName.length()));
		
		return new File(testClassesDir.getParentFile(), "testdb/" + dbName).getAbsolutePath();
	}
	
	private EntityManager configuredEntityManager(String jdbcUrl, String unit) throws Exception {
		
		Dictionary<String, Object> props = getBaseProperties();
		
		props.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, "org.h2.Driver");
		props.put(DataSourceFactory.JDBC_URL, jdbcUrl);
		props.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, unit);
		
		String filter = System.getProperty(ARIES_EMF_BUILDER_TARGET_FILTER);
		
		if(filter != null) {
			props.put(ARIES_EMF_BUILDER_TARGET_FILTER, "(&(osgi.unit.name=" + unit + ")" + filter + ")");
		}
		
		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);
		
		org.osgi.service.cm.Configuration config = cm.createFactoryConfiguration(
				"org.apache.aries.tx.control.jpa.xa", null);
		config.update(props);
		
		return getService(JPAEntityManagerProvider.class,
				"(" + EntityManagerFactoryBuilder.JPA_UNIT_NAME + "=" + unit + ")",
				5000).getResource(txControl);
	}

	private <T> T getService(Class<T> clazz, long timeout) {
		try {
			return getService(clazz, null, timeout);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private <T> T getService(Class<T> clazz, String filter, long timeout) throws InvalidSyntaxException {
		org.osgi.framework.Filter f = FrameworkUtil.createFilter(filter == null ? "(|(foo=bar)(!(foo=bar)))" : filter); 
		
		ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(context, clazz, null) {
			@Override
			public T addingService(ServiceReference<T> reference) {
				return f.match(reference) ? super.addingService(reference) : null;
			}
		};

		tracker.open();
		try {
			T t = tracker.waitForService(timeout);
			if(t == null) {
				throw new NoSuchElementException(clazz.getName());
			}
			return t;
		} catch (InterruptedException e) {
			throw new RuntimeException("Error waiting for service " + clazz.getName(), e);
		} finally {
			trackers.add(tracker);
		}
	}
	
	protected Dictionary<String, Object> getBaseProperties() {
		return new Hashtable<>();
	}
	
	@After
	public void tearDown() {

		clearConfiguration();
		
		if(server1 != null) {
			server1.stop();
		}
		if(server2 != null) {
			server2.stop();
		}

		trackers.stream().forEach(ServiceTracker::close);
		
		em1 = null;
		em2 = null;
	}

	private void clearConfiguration() {
		ConfigurationAdmin cm = getService(ConfigurationAdmin.class, 5000);
		org.osgi.service.cm.Configuration[] cfgs = null;
		try {
			cfgs = cm.listConfigurations(null);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(cfgs != null) {
			for(org.osgi.service.cm.Configuration cfg : cfgs) {
				try {
					cfg.delete();
				} catch (Exception e) {}
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
	    return probe;
	}
	
	@Configuration
	public Option[] xaTxConfiguration() {
		String localRepo = System.getProperty("maven.repo.local");
		if (localRepo == null) {
			localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
		}
		
		return options(junitBundles(), systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
				when(localRepo != null)
				.useOptions(CoreOptions.vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo)),
				mavenBundle("org.apache.aries.tx-control", "tx-control-service-xa").versionAsInProject(),
				mavenBundle("com.h2database", "h2").versionAsInProject(),
				mavenBundle("org.apache.aries.tx-control", "tx-control-provider-jpa-xa").versionAsInProject(),
				jpaProvider(),
				mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container", ariesJPAVersion()),
				mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject(),
				
				streamBundle(getTestUnit(XA_TEST_UNIT_1)),
				streamBundle(getTestUnit(XA_TEST_UNIT_2))
				
//				,CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
				);
	}

	private InputStream getTestUnit(String unit) {
		String descriptor = "META-INF/" + unit + ".xml";
		return TinyBundles.bundle()
			.add(Message.class)
			.add(descriptor, getClass().getResource("/" + descriptor))
			.set("Meta-Persistence", descriptor)
			.set("Bundle-SymbolicName", unit)
			.set("Import-Package", "javax.persistence")
			.set("Require-Capability", "osgi.contract;filter:=\"(&(osgi.contract=JavaJPA)(version=2.0))\"")
			// This line is necessary while https://hibernate.atlassian.net/browse/HHH-10742 is unfixed
			.set("DynamicImport-Package", "org.hibernate.proxy,javassist.util.proxy")
			.build();
	}

	protected String ariesJPAVersion() {
		return "2.3.0";
	}
	
	protected abstract Option jpaProvider();
	
	@Test
	public void testTwoPhaseCommit() throws Exception {
		Object m1 = getMessageEntityFrom(XA_TEST_UNIT_1);
		Object m2 = getMessageEntityFrom(XA_TEST_UNIT_2);

		txControl.required(() -> {
			setMessage(m1, "Hello World!");
			
			em1.persist(m1);

			setMessage(m2, "Hello 1!");
			
			em2.persist(m2);
			
			return null;
		});
		
		assertEquals("Hello World!", txControl.notSupported(() -> {
			return getMessage(em1.find(m1.getClass(), getId(m1)));
		}));

		assertEquals("Hello 1!", txControl.notSupported(() -> {
			return getMessage(em2.find(m2.getClass(), getId(m2)));
		}));
	}

	@Test
	public void testTwoPhaseRollback()  throws Exception  {
		Object m1 = getMessageEntityFrom(XA_TEST_UNIT_1);
		Object m2 = getMessageEntityFrom(XA_TEST_UNIT_2);
		Object m3 = getMessageEntityFrom(XA_TEST_UNIT_2);
		try {

			txControl.required(() -> {
				setMessage(m1, "Hello World!");
				
				em1.persist(m1);

				setMessage(m2, "Hello 1!");
				
				em2.persist(m2);
				
				txControl.requiresNew(() -> {
						setMessage(m3, "Hello 2!");
						em2.persist(m3);
						return null;
					});
				
				txControl.getCurrentContext().registerXAResource(new PoisonResource(), null);
				
				return null;
			});
			fail("Should roll back");
		} catch (TransactionRolledBackException trbe) {
		}
		
		assertEquals(0, (int) txControl.notSupported(() -> {
				CriteriaBuilder cb = em1.getCriteriaBuilder();
				CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
				countQuery.select(cb.count(countQuery.from(m1.getClass())));
				
				return em1.createQuery(countQuery).getSingleResult().intValue();
			}));
		
		
		assertEquals(Arrays.asList("Hello 2!"), txControl.notSupported(() -> {
			CriteriaBuilder cb = em2.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery(String.class);
			query.select(query.from(m2.getClass()).get("message"));
			
			return em2.createQuery(query).getResultList();
		}));
	}
	
	Object getMessageEntityFrom(String unit) throws Exception {
		Class<?> clz = Arrays.stream(context.getBundles())
					.filter(b -> unit.equals(b.getSymbolicName()))
					.map(b -> {
							try {
								return b.loadClass("org.apache.aries.tx.control.itests.entity.Message");
							} catch (ClassNotFoundException e) {
								throw new RuntimeException(e);
							}
						})
					.findFirst().orElseThrow(() -> new IllegalArgumentException(unit));
		return clz.newInstance();
	}
	
	void setMessage(Object entity, String message) throws Exception {
		Field f = entity.getClass().getField("message");
		f.set(entity, message);
	}

	String getMessage(Object entity) throws Exception {
		Field f = entity.getClass().getField("message");
		return (String) f.get(entity);
	}

	Integer getId(Object entity) throws Exception {
		Field f = entity.getClass().getField("id");
		return (Integer) f.get(entity);
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
