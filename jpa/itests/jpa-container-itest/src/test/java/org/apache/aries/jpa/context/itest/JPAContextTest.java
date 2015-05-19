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

import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.util.Filter;

public abstract class JPAContextTest extends AbstractJPAItest {

    @Inject
    UserTransaction ut;

    @Inject
    @Filter("(osgi.unit.name=test-unit)")
    EmSupplier emSupplier;

    @Before
    public void preCall() {
        emSupplier.preCall();
    }
    
    @After
    public void postCall() {
        emSupplier.postCall();
    }
    
    @Test
    public void testCreateAndChange() throws Exception {
        resolveBundles();
        EntityManager em = emSupplier.get();
        ut.begin();
        em.joinTransaction();
        try {
            deleteCars(em);
            Car car = new Car();
            car.setNumberOfSeats(5);
            car.setEngineSize(1200);
            car.setColour("blue");
            car.setNumberPlate("A1AAA");
            em.persist(car);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ut.commit();
        }
        
        Car c = em.find(Car.class, "A1AAA");
        
        assertEquals(5, c.getNumberOfSeats());
        assertEquals(1200, c.getEngineSize());
        assertEquals("blue", c.getColour());
        
        ut.begin();
        try {
            Car car = em.find(Car.class, "A1AAA");
            car.setNumberOfSeats(2);
            car.setEngineSize(2000);
            car.setColour("red");
        } finally {
            ut.commit();
        }
        
        c = em.find(Car.class, "A1AAA");
        
        assertEquals(2, c.getNumberOfSeats());
        assertEquals(2000, c.getEngineSize());
        assertEquals("red", c.getColour());
    }

    private void deleteCars(EntityManager em) {
        Query q = em.createQuery("DELETE from Car c");
        q.executeUpdate();

        q = em.createQuery("SELECT Count(c) from Car c");
        assertEquals(0l, q.getSingleResult());
    }

    @Test
    public void testQueries() throws Exception {
        final EntityManager em = emSupplier.get();
        try {
            ut.begin();
            em.joinTransaction();
            deleteCars(em);
        } finally {
            ut.commit();
        }

        Query countQuery = em.createQuery("SELECT Count(c) from Car c");
        assertEquals(0l, countQuery.getSingleResult());

        ut.begin();
        em.joinTransaction();
        try {
            Car car = new Car();
            car.setNumberOfSeats(5);
            car.setEngineSize(1200);
            car.setColour("blue");
            car.setNumberPlate("A1AAA");
            em.persist(car);

            car = new Car();
            car.setNumberOfSeats(7);
            car.setEngineSize(1800);
            car.setColour("green");
            car.setNumberPlate("B2BBB");
            em.persist(car);
        } finally {
            ut.commit();
        }

        assertEquals(2l, countQuery.getSingleResult());

        TypedQuery<Car> carQuery = em.createQuery("Select c from Car c ORDER by c.engineSize",
                                                         Car.class);

        List<Car> list = carQuery.getResultList();
        assertEquals(2l, list.size());

        assertEquals(5, list.get(0).getNumberOfSeats());
        assertEquals(1200, list.get(0).getEngineSize());
        assertEquals("blue", list.get(0).getColour());
        assertEquals("A1AAA", list.get(0).getNumberPlate());

        assertEquals(7, list.get(1).getNumberOfSeats());
        assertEquals(1800, list.get(1).getEngineSize());
        assertEquals("green", list.get(1).getColour());
        assertEquals("B2BBB", list.get(1).getNumberPlate());

        ut.begin();
        em.joinTransaction();
        try {
            Car car = em.find(Car.class, "A1AAA");
            car.setNumberOfSeats(2);
            car.setEngineSize(2000);
            car.setColour("red");

            car = em.find(Car.class, "B2BBB");
            em.remove(car);

            car = new Car();
            car.setNumberOfSeats(2);
            car.setEngineSize(800);
            car.setColour("black");
            car.setNumberPlate("C3CCC");
            em.persist(car);

        } finally {
            ut.commit();
        }

        assertEquals(2l, countQuery.getSingleResult());

        list = carQuery.getResultList();
        assertEquals(2l, list.size());

        assertEquals(2, list.get(0).getNumberOfSeats());
        assertEquals(800, list.get(0).getEngineSize());
        assertEquals("black", list.get(0).getColour());
        assertEquals("C3CCC", list.get(0).getNumberPlate());

        assertEquals(2, list.get(1).getNumberOfSeats());
        assertEquals(2000, list.get(1).getEngineSize());
        assertEquals("red", list.get(1).getColour());
        assertEquals("A1AAA", list.get(1).getNumberPlate());
    }

}
