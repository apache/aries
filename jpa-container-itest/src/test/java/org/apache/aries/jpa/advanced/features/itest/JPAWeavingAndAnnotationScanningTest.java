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
package org.apache.aries.jpa.advanced.features.itest;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.apache.aries.itest.ExtraOptions.*;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;

public abstract class JPAWeavingAndAnnotationScanningTest extends AbstractIntegrationTest {

  @Test
  public void testAnnotatedClassFound() throws Exception {
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    EntityManager em = emf.createEntityManager();
    
    em.getTransaction().begin();
    
    Car c = new Car();
    
    c.setColour("Blue");
    c.setNumberPlate("AB11CDE");
    c.setNumberOfSeats(7);
    c.setEngineSize(1900);
    
    em.persist(c);
    
    em.getTransaction().commit();
    
    assertEquals(7, em.find(Car.class, "AB11CDE").getNumberOfSeats());
  }
    
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    return testOptions(
        transactionBootDelegation(),
        paxLogging("INFO"),
        
        // Bundles
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("commons-pool", "commons-pool"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
        mavenBundle("org.apache.derby", "derby"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),

        mavenBundle("org.osgi", "org.osgi.compendium"),

//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
        
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.advanced.itest.bundle"),
        
        PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
        equinox().version("3.7.0.v20110613"));
  }
}
