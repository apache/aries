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
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.apache.aries.itest.ExtraOptions.*;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

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

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;

@RunWith(JUnit4TestRunner.class)
public class JPAContainerDataSourceFactoryTest extends AbstractIntegrationTest {

  @Test
  public void testDataSourceFactoryLifecycle() throws Exception {
    //Wait for startup
    context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    //Now go
    ServiceReference[] refs = context().getServiceReferences(
        EntityManagerFactory.class.getName(), "(&(osgi.unit.name=dsf-test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull(refs);
    
    Hashtable<String, Object> props = new Hashtable();
    props.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, "org.apache.derby.jdbc.EmbeddedDriver");
    
    ServiceRegistration reg = context().registerService(DataSourceFactory.class.getName(), 
        new DerbyDataSourceFactory(), props);
    
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, 
        "(&(osgi.unit.name=dsf-test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    
    EntityManager em = emf.createEntityManager();
    
    em.getTransaction().begin();
    
    Car c = new Car();
    c.setNumberPlate("123456");
    c.setColour("blue");
    em.persist(c);
    
    em.getTransaction().commit();
    
    em.close();
    
    em = emf.createEntityManager();
    
    assertEquals("blue", em.find(Car.class, "123456").getColour());
    
    reg.unregister();
    
    refs = context().getServiceReferences(
        EntityManagerFactory.class.getName(), "(&(osgi.unit.name=dsf-test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull(refs);
  }
  
  @Test
  public void testDataSourceFactoryXALifecycle() throws Exception {
    //Wait for startup
    context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    //Now go
    ServiceReference[] refs = context().getServiceReferences(
        EntityManagerFactory.class.getName(), "(&(osgi.unit.name=dsf-xa-test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull(refs);
    
    Hashtable<String, Object> props = new Hashtable();
    props.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, "org.apache.derby.jdbc.EmbeddedDriver");
    
    ServiceRegistration reg = context().registerService(DataSourceFactory.class.getName(), 
        new DerbyDataSourceFactory(), props);
    
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, 
        "(&(osgi.unit.name=dsf-xa-test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    
    EntityManager em = emf.createEntityManager();
    
    //Use a JTA tran to show integration
    UserTransaction ut = context().getService(UserTransaction.class);
    
    ut.begin();
    em.joinTransaction();
    Car c = new Car();
    c.setNumberPlate("123456");
    c.setColour("blue");
    em.persist(c);
    
    ut.commit();
      
    em.close();
    
    em = emf.createEntityManager();
    
    assertEquals("blue", em.find(Car.class, "123456").getColour());
    
    reg.unregister();
    
    refs = context().getServiceReferences(
        EntityManagerFactory.class.getName(), "(&(osgi.unit.name=dsf-xa-test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull(refs);
  }
  
  
  
  private static class DerbyDataSourceFactory implements DataSourceFactory {

    public DataSource createDataSource(Properties props) throws SQLException {
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
  
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    return testOptions(
        transactionBootDelegation(),
        paxLogging("DEBUG"),

        // Bundles
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("commons-pool", "commons-pool"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core"),
        mavenBundle("org.ow2.asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.api"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.core"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager" ),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.wrappers" ),
        mavenBundle("org.apache.derby", "derby"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
        mavenBundle("org.apache.openjpa", "openjpa"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.osgi", "org.osgi.enterprise"),
//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
        //waitForFrameworkStartup(),
        
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle"),
        
        PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
        equinox().version("3.7.0.v20110613"));
  }

}
