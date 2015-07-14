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
package org.apache.aries.jpa.context.itest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class EmSupplierTest extends AbstractJPAItest {

    @Inject
    private UserTransaction ut;
    
    private EmSupplier emSupplier;

    @Before
    public void preCall() {
        if (emSupplier == null) {
            emSupplier = getService(EmSupplier.class, "(osgi.unit.name=xa-test-unit)");
        }
        emSupplier.preCall();
    }
    
    @After
    public void postCall() {
        emSupplier.postCall();
    }

    @Test
    public void testCreateAndChange() throws Exception {
        EntityManager em = emSupplier.get();
        ut.begin();
        em.joinTransaction();
        deleteCars(em);
        em.persist(createBlueCar());
        ut.commit();
        
        Car c = em.find(Car.class, BLUE_CAR_PLATE);
        assertBlueCar(c);
        
        ut.begin();
        Car car = em.find(Car.class, BLUE_CAR_PLATE);
        car.setNumberOfSeats(2);
        car.setEngineSize(2000);
        car.setColour("red");
        ut.commit();
        
        c = em.find(Car.class, BLUE_CAR_PLATE);
        assertEquals(2, c.getNumberOfSeats());
        assertEquals(2000, c.getEngineSize());
        assertEquals("red", c.getColour());
    }

    @Test
    public void testQueries() throws Exception {
        final EntityManager em = emSupplier.get();

        ut.begin();
        em.joinTransaction();
        deleteCars(em);
        ut.commit();

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
        Car car = em.find(Car.class, "A1AAA");
        car.setNumberOfSeats(2);
        car.setEngineSize(2000);
        car.setColour("red");
        em.remove(em.find(Car.class, "B2BBB"));
        em.persist(createBlackCar());
        ut.commit();

        assertEquals(2l, countQuery.getSingleResult());

        list = carQuery.getResultList();
        assertEquals(2, list.size());

        assertEquals(2, list.get(0).getNumberOfSeats());
        assertEquals(800, list.get(0).getEngineSize());
        assertEquals("black", list.get(0).getColour());
        assertEquals("C3CCC", list.get(0).getNumberPlate());

        assertEquals(2, list.get(1).getNumberOfSeats());
        assertEquals(2000, list.get(1).getEngineSize());
        assertEquals("red", list.get(1).getColour());
        assertEquals("A1AAA", list.get(1).getNumberPlate());
    }

    private Car createBlackCar() {
        Car car;
        car = new Car();
        car.setNumberOfSeats(2);
        car.setEngineSize(800);
        car.setColour("black");
        car.setNumberPlate("C3CCC");
        return car;
    }
    
    private void deleteCars(EntityManager em) {
        Query q = em.createQuery("DELETE from Car c");
        q.executeUpdate();

        q = em.createQuery("SELECT Count(c) from Car c");
        assertEquals(0l, q.getSingleResult());
    }
}
