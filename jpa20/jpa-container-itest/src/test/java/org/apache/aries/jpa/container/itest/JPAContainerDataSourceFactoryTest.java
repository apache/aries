/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.container.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;

public class JPAContainerDataSourceFactoryTest extends AbstractJPAItest {
	private static final String DSF_TEST_UNIT = "dsf-test-unit";
	private static final String DSF_XA_TEST_UNIT = "dsf-xa-test-unit";
	
	@SuppressWarnings("rawtypes")
	private ServiceRegistration reg;

	@Before
	public void waitStartup() throws InvalidSyntaxException {
		getEMF(TEST_UNIT);
		assertNull(getEMFRefs(DSF_TEST_UNIT));
		assertNull(getEMFRefs(DSF_XA_TEST_UNIT));
		reg = registerDataSourceFactory();
	}
	
	@After
	public void shutDown() throws InvalidSyntaxException {
		reg.unregister();
		assertNull(getEMFRefs(DSF_TEST_UNIT));
		assertNull(getEMFRefs(DSF_XA_TEST_UNIT));
	}

	@Test
	public void testDataSourceFactoryLifecycle() throws Exception {
		EntityManagerFactory emf = getEMF(DSF_TEST_UNIT);

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Car c = createCar();
		em.persist(c);
		em.getTransaction().commit();
		em.close();
		
		assertCarFound(emf);

		em = emf.createEntityManager();
		em.getTransaction().begin();
		deleteCar(em, c);
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testDataSourceFactoryXALifecycle() throws Exception {
		EntityManagerFactory emf = getEMF(DSF_XA_TEST_UNIT);
		EntityManager em = emf.createEntityManager();

		// Use a JTA transaction to show integration
		UserTransaction ut = context().getService(UserTransaction.class);
		ut.begin();
		em.joinTransaction();
		Car c = createCar();
		em.persist(c);
		ut.commit();
		em.close();

		assertCarFound(emf);
		
		em = emf.createEntityManager();
		ut.begin();
		em.joinTransaction();
		deleteCar(em, c);
		ut.commit();
		em.close();
	}

	private static class DerbyDataSourceFactory implements DataSourceFactory {

		public DataSource createDataSource(Properties props)
				throws SQLException {
			EmbeddedDataSource ds = new EmbeddedDataSource();
			ds.setDatabaseName("memory:TEST");
			ds.setCreateDatabase("create");
			return ds;
		}

		public ConnectionPoolDataSource createConnectionPoolDataSource(
				Properties props) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public XADataSource createXADataSource(Properties props)
				throws SQLException {
			EmbeddedXADataSource ds = new EmbeddedXADataSource();
			ds.setDatabaseName("memory:TEST");
			ds.setCreateDatabase("create");
			return ds;
		}

		public Driver createDriver(Properties props) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ServiceRegistration registerDataSourceFactory() {
		Hashtable<String, Object> props = new Hashtable();
		props.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS,	"org.apache.derby.jdbc.EmbeddedDriver");
		return context().registerService(DataSourceFactory.class.getName(), new DerbyDataSourceFactory(), props);
	}

	private Car createCar() {
		Car c = new Car();
		c.setNumberPlate("123456");
		c.setColour("blue");
		return c;
	}
	

	private void deleteCar(EntityManager em, Car c) {
		c = em.merge(c);
		em.remove(c);
	}

	private void assertCarFound(EntityManagerFactory emf) {
		EntityManager em;
		em = emf.createEntityManager();
		assertEquals("blue", em.find(Car.class, "123456").getColour());
	}

	@Configuration
	public Option[] configuration() {
		return options(
				baseOptions(),
				ariesJpa(),
				transactionWrapper(),
				openJpa(),
				testBundle()
				);
	}

}
