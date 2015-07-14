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
package org.apache.aries.jpa.container.itest;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public class EMFBuilderTest extends AbstractJPAItest {
    @Inject
    @Filter("(osgi.unit.name=dsf-test-unit)")
    EntityManagerFactoryBuilder emfBuilder;
    
    @Test
    public void testBuilder() throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();
        EntityManagerFactory emf = emfBuilder.createEntityManagerFactory(props);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Car c = new Car();
        c.setColour("Blue");
        c.setNumberPlate("AB11CDE");
        c.setNumberOfSeats(7);
        c.setEngineSize(1900);
        em.persist(c);
        em.getTransaction().commit();
        Car c2 = em.find(Car.class, "AB11CDE");
        Assert.assertEquals(7, c2.getNumberOfSeats());
        em.close();
        emf.close();
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(), //
            ariesJpa20(), //
            hibernate(), //
            derbyDSF(), //
            testBundle() //
        };
    }

}
