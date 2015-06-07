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

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;

public class JPAContainerDataSourceFactoryTest extends AbstractJPAItest {
    private static final String DSF_TEST_UNIT = "dsf-test-unit";
    private static final String DSF_XA_TEST_UNIT = "dsf-xa-test-unit";
    
    @Inject
    @Filter("(osgi.unit.name=" + DSF_TEST_UNIT + ")")
    EntityManagerFactory emfDSF;
    
    @Inject
    @Filter("(osgi.unit.name=" + DSF_XA_TEST_UNIT + ")")
    EntityManagerFactory emfDSFXA;
    

    @Test
    public void testDataSourceFactoryLifecycle() throws Exception {
        EntityManager em = emfDSF.createEntityManager();
        em.getTransaction().begin();
        Car c = createCar();
        em.persist(c);
        em.getTransaction().commit();
        em.close();

        assertCarFound(emfDSF);

        em = emfDSF.createEntityManager();
        em.getTransaction().begin();
        deleteCar(em, c);
        em.getTransaction().commit();
        em.close();
    }

    @Test
    public void testDataSourceFactoryXALifecycle() throws Exception {
        EntityManager em = emfDSFXA.createEntityManager();

        // Use a JTA transaction to show integration
        UserTransaction ut = context().getService(UserTransaction.class);
        ut.begin();
        em.joinTransaction();
        Car c = createCar();
        em.persist(c);
        ut.commit();
        em.close();

        assertCarFound(emfDSFXA);

        em = emfDSFXA.createEntityManager();
        ut.begin();
        em.joinTransaction();
        deleteCar(em, c);
        ut.commit();
        em.close();
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
        EntityManager em = emf.createEntityManager();
        assertEquals("blue", em.find(Car.class, "123456").getColour());
        em.close();
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(), //
            ariesJpa20(), //
            derbyDSF(), //
            hibernate(), //
            testBundle()
        };
    }

}
