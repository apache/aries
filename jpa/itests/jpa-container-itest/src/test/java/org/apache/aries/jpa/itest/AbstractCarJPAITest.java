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
package org.apache.aries.jpa.itest;

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.junit.Assert;
import org.osgi.framework.BundleException;

public abstract class AbstractCarJPAITest extends AbstractJPAItest {
    protected static final String BLUE_CAR_PLATE = "A1AAA";
    protected static final String GREEN_CAR_PLATE = "B2BBB";
    protected static final String BLACK_CAR_PLATE = "C3CCC";

    protected Car createBlueCar() {
        Car car = new Car();
        car.setNumberOfSeats(5);
        car.setEngineSize(1200);
        car.setColour("blue");
        car.setNumberPlate(BLUE_CAR_PLATE);
        return car;
    }

    protected Car createGreenCar() {
        Car car;
        car = new Car();
        car.setNumberOfSeats(7);
        car.setEngineSize(1800);
        car.setColour("green");
        car.setNumberPlate(GREEN_CAR_PLATE);
        return car;
    }
    

    protected Car createBlackCar() {
        Car car;
        car = new Car();
        car.setNumberOfSeats(2);
        car.setEngineSize(800);
        car.setColour("black");
        car.setNumberPlate(BLACK_CAR_PLATE);
        return car;
    }

    protected void assertBlueCar(Car car) {
        Assert.assertNotNull("Blue car not found (null)", car);
        assertEquals(5, car.getNumberOfSeats());
        assertEquals(1200, car.getEngineSize());
        assertEquals("blue", car.getColour());
        assertEquals(BLUE_CAR_PLATE, car.getNumberPlate());
    }

    protected void assertGreenCar(Car car) {
        assertEquals(7, car.getNumberOfSeats());
        assertEquals(1800, car.getEngineSize());
        assertEquals("green", car.getColour());
        assertEquals(GREEN_CAR_PLATE, car.getNumberPlate());
    }
    
    protected void assertBlackCar(Car car) {
        assertEquals(2, car.getNumberOfSeats());
        assertEquals(800, car.getEngineSize());
        assertEquals("black", car.getColour());
        assertEquals(BLACK_CAR_PLATE, car.getNumberPlate());
    }

    /**
     * Create, find and delete car using resource local transactions
     * @param emf
     * @throws BundleException
     */
    protected void carLifecycleRL(EntityManager em) throws BundleException {
        em.getTransaction().begin();
        Car car = createBlueCar();
        em.persist(car);
        em.getTransaction().commit();

        Car car2 = em.find(Car.class, BLUE_CAR_PLATE);
        assertBlueCar(car2);
        em.getTransaction().begin();
        em.remove(car2);
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Create, find and delete car using XA Transactions
     * @param ut
     * @param em
     * @throws Exception
     */
    protected void carLifecycleXA(UserTransaction ut, EntityManager em) throws Exception {
        ut.begin();
        em.joinTransaction();
        delete(em, BLUE_CAR_PLATE);
        em.persist(createBlueCar());
        ut.commit();

        Car c = em.find(Car.class, BLUE_CAR_PLATE);
        assertBlueCar(c);

        ut.begin();
        em.joinTransaction();
        delete(em, BLUE_CAR_PLATE);
        ut.commit();
    }

    protected void delete(EntityManager em, String plateId) {
        Car car = em.find(Car.class, plateId);
        if (car != null) {
            em.remove(car);
        }
    }
}
