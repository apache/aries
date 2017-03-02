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

import static javax.persistence.spi.PersistenceUnitTransactionType.JTA;
import static javax.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static org.junit.Assert.assertEquals;
import static org.osgi.service.jdbc.DataSourceFactory.OSGI_JDBC_DRIVER_CLASS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractCarJPAITest;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.junit.Test;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public abstract class JPAContainerTest extends AbstractCarJPAITest {
    @Inject
    Coordinator coordinator;

    @Test
    public void testCarEMFBuilder() throws Exception {
        EntityManagerFactoryBuilder emfBuilder = getService(EntityManagerFactoryBuilder.class,
                                                            "(osgi.unit.name=" + DSF_TEST_UNIT + ")");
        Map<String, Object> props = new HashMap<String, Object>();
        EntityManagerFactory emf = emfBuilder.createEntityManagerFactory(props);
        carLifecycleRL(emf.createEntityManager());
    }

    @Test
    public void testCarEMF() throws Exception {
        carLifecycleRL(getEMF(TEST_UNIT).createEntityManager());
    }
    
    @Test
    public void testEMFXA() throws Exception {
        EntityManager em = getEMF(XA_TEST_UNIT).createEntityManager();
        carLifecycleXA(ut, em);
        em.close();
    }

    @Test
    public void testDataSourceFactoryLifecycle() throws Exception {
        carLifecycleRL(getEMF(DSF_TEST_UNIT).createEntityManager());
    }

    @Test
    public void testDataSourceFactoryXALifecycle() throws Exception {
        EntityManager em = getEMF(DSF_XA_TEST_UNIT).createEntityManager();
        carLifecycleXA(ut, em);
        em.close();
    }
    

    @Test
    public void testEmSupplier() throws Exception {
        EmSupplier emSupplier = getService(EmSupplier.class, "(osgi.unit.name=" + XA_TEST_UNIT + ")");
        Coordination coordination = coordinator.begin("test", 0);
        try {
            EntityManager em = emSupplier.get();
            carLifecycleXA(ut, em);

            Query countQuery = em.createQuery("SELECT Count(c) from Car c");
            assertEquals(0l, countQuery.getSingleResult());

            ut.begin();
            em.joinTransaction();
            em.persist(createBlueCar());
            em.persist(createGreenCar());
            ut.commit();

            assertEquals(2l, countQuery.getSingleResult());

            TypedQuery<Car> carQuery = em.createQuery("Select c from Car c ORDER by c.engineSize", Car.class);
            List<Car> list = carQuery.getResultList();
            assertEquals(2, list.size());

            assertBlueCar(list.get(0));
            assertGreenCar(list.get(1));

            ut.begin();
            em.joinTransaction();
            changeToRed(em.find(Car.class, BLUE_CAR_PLATE));
            em.remove(em.find(Car.class, GREEN_CAR_PLATE));
            em.persist(createBlackCar());
            ut.commit();

            assertEquals(2l, countQuery.getSingleResult());
            list = carQuery.getResultList();
            assertEquals(2, list.size());

            assertBlackCar(list.get(0));
            assertChangedBlueCar(list.get(1));

            cleanup(em);
        } finally {
            coordination.end();
        }
    }

    private void changeToRed(Car car) {
        car.setNumberOfSeats(2);
        car.setEngineSize(2000);
        car.setColour("red");
    }

    private void cleanup(EntityManager em) throws Exception {
        ut.begin();
        em.joinTransaction();
        delete(em, BLACK_CAR_PLATE);
        delete(em, BLUE_CAR_PLATE);
        ut.commit();
    }

    private void assertChangedBlueCar(Car car) {
        assertEquals(2, car.getNumberOfSeats());
        assertEquals(2000, car.getEngineSize());
        assertEquals("red", car.getColour());
        assertEquals(BLUE_CAR_PLATE, car.getNumberPlate());
    }

    @Test
    public void testCarEMFBuilderExternalDS() throws Exception {
    	DataSourceFactory dsf = getService(DataSourceFactory.class, 
    			"(" + OSGI_JDBC_DRIVER_CLASS + "=org.apache.derby.jdbc.EmbeddedDriver)");
       
    	EntityManagerFactoryBuilder emfBuilder = getService(EntityManagerFactoryBuilder.class,
    			"(osgi.unit.name=" + EXTERNAL_TEST_UNIT + ")");
    	
    	Properties jdbcProps = new Properties();
    	jdbcProps.setProperty("url", "jdbc:derby:memory:DSFTEST;create=true");
    	
    	Map<String, Object> props = new HashMap<String, Object>();
    	props.put("javax.persistence.nonJtaDataSource", dsf.createDataSource(jdbcProps));
    	props.put("javax.persistence.transactionType", RESOURCE_LOCAL.name());
    	
    	EntityManagerFactory emf = emfBuilder.createEntityManagerFactory(props);
    	carLifecycleRL(emf.createEntityManager());
    }

    @Test
    public void testCarEMFBuilderExternalDSXA() throws Exception {
    	DataSource ds = getService(DataSource.class, 
    			"(" + OSGI_JDBC_DRIVER_CLASS + "=org.apache.derby.jdbc.EmbeddedDriver-pool-xa)");
    	
    	EntityManagerFactoryBuilder emfBuilder = getService(EntityManagerFactoryBuilder.class,
    			"(osgi.unit.name=" + EXTERNAL_TEST_UNIT + ")");
    	
    	
    	Map<String, Object> props = new HashMap<String, Object>();
    	props.put("javax.persistence.jtaDataSource", ds);
    	props.put("javax.persistence.transactionType", JTA.name());
    	
    	//EclipseLink also needs a non-jta-datasource
    	DataSourceFactory dsf = getService(DataSourceFactory.class, 
    			"(" + OSGI_JDBC_DRIVER_CLASS + "=org.apache.derby.jdbc.EmbeddedDriver)");
    	Properties jdbcProps = new Properties();
    	jdbcProps.setProperty("url", "jdbc:derby:memory:TEST1;create=true");
    	props.put("javax.persistence.nonJtaDataSource", dsf.createDataSource(jdbcProps));

    	
    	EntityManagerFactory emf = emfBuilder.createEntityManagerFactory(props);
    	carLifecycleXA(ut, emf.createEntityManager());
    }
    
    @Test
    public void testCarEMFBuilderNoNonJTADataSource() throws Exception {
        EntityManagerFactoryBuilder emfBuilder = getService(EntityManagerFactoryBuilder.class,
                        "(osgi.unit.name=" + EXTERNAL_TEST_UNIT + ")");
        
        
        Map<String, Object> props = new HashMap<String, Object>();
        //EclipseLink also needs a non-jta-datasource
        DataSourceFactory dsf = getService(DataSourceFactory.class, 
                        "(" + OSGI_JDBC_DRIVER_CLASS + "=org.apache.derby.jdbc.EmbeddedDriver)");
        Properties jdbcProps = new Properties();
        jdbcProps.setProperty("url", "jdbc:derby:memory:TESTNOJTA;create=true");
        props.put("javax.persistence.dataSource", dsf.createDataSource(jdbcProps));

        
        EntityManagerFactory emf = emfBuilder.createEntityManagerFactory(props);
        carLifecycleRL(emf.createEntityManager());
    }

}
