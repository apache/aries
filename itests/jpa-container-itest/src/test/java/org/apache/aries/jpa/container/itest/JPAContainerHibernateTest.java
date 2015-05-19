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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class JPAContainerHibernateTest extends AbstractJPAItest {
    @Test
    public void testCarCreateDelete() throws Exception {
        resolveBundles();
        EntityManagerFactory emf = getEMF(TEST_UNIT);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Car c = new Car();
        c.setNumberPlate("123456");
        c.setColour("blue");
        em.persist(c);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        em.getTransaction().begin();
        deleteCar(em, c);
        em.getTransaction().commit();
        em.close();
    }

    private void deleteCar(EntityManager em, Car c) {
        c = em.merge(c);
        em.remove(c);
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(), //
            ariesJpa20(), //
            testBundle(), //
            transactionWrapper(), //
            testDs(), //
            hibernate()
        };
    }
}
