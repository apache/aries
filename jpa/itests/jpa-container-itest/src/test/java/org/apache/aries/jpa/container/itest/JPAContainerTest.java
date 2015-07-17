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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractCarJPAITest;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.junit.Test;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public abstract class JPAContainerTest extends AbstractCarJPAITest {

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
        EmSupplier emSupplier = getService(EmSupplier.class, "(osgi.unit.name=xa-test-unit)");
        emSupplier.preCall();
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
        emSupplier.postCall();
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

}
