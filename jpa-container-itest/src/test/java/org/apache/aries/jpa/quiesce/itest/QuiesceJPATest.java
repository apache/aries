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
package org.apache.aries.jpa.quiesce.itest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;

import static org.apache.aries.itest.ExtraOptions.*;

import java.util.Collections;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
public class QuiesceJPATest extends AbstractIntegrationTest {
  
  private static class TestQuiesceCallback implements QuiesceCallback{

    private int calls = 0;
    
    public void bundleQuiesced(Bundle... arg0) {
      calls++;
    }

    public boolean bundleClearedUp()
    {
      return calls == 1;
    }
  }
  
  private class MultiQuiesceCallback implements QuiesceCallback{

    private int calls = 0;
    
    private boolean contextFirst = true;
    
    public void bundleQuiesced(Bundle... arg0) {
      if(++calls == 1)
        try {
          context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
              + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
        } catch (Throwable t){
          contextFirst = false;
          if(t instanceof RuntimeException)
            throw (RuntimeException) t;
          else if (t instanceof Error)
            throw (Error) t;
          else
            throw new RuntimeException(t);
        }
            
    }

    public boolean bundleClearedUp()
    {
      return calls == 2 && contextFirst;
    }
  }
  
  
  @After
  public void restartTestBundles() throws BundleException {
    Bundle b = context().getBundleByName("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    b = context().getBundleByName("org.apache.aries.jpa.container");
    b.stop();
    b.start();
    
    b = context().getBundleByName("org.apache.aries.jpa.container.context");
    b.stop();
    b.start();
  }
  
  @Test
  public void testSimpleContextQuiesce() throws Exception {

    //Get a managed context registered
    PersistenceContextProvider provider = context().getService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    
    //Quiesce it
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
    
    //Restart the bundle to check the context gets re-registered
    Bundle b = context().getBundleByName("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
  }

  @Test
  public void testComplexContextQuiesce() throws Exception {
    //This is load bearing. we have to wait to create the EntityManager until the DataSource is available
    context().getService(DataSource.class);
    
    // Get a managed context registered
    PersistenceContextProvider provider = context().getService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    
    //Set up a transaction so we can check the Quiesce waits properly
    UserTransaction tm = context().getService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    tm.commit();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
    
    
    //Restart the bundle to check the context gets re-registered, then ensure it isn't
    //tidied up immediately again!
    Bundle b = context().getBundleByName("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    tm.commit();
    
    Thread.sleep(1000);
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", 100);
    
    //Test again to make sure we don't hold state over
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", 100);
    
    tm.commit();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
    
  }
  
  @Test
  public void testContextRuntimeQuiesce() throws Exception {
    //This is load bearing. we have to wait to create the EntityManager until the DataSource is available
    context().getService(DataSource.class);
    
    PersistenceContextProvider provider = context().getService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    
    UserTransaction tm = context().getService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.container.context")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce not finished", callback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    tm.commit();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
  }
  
  @Test
  public void testSimpleUnitQuiesce() throws Exception {

    context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No unit should exist",refs);
    
    //Restart the bundle to check the unit gets re-registered
    Bundle b = context().getBundleByName("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
  }

  @Test
  public void testComplexUnitQuiesce() throws Exception {
    //This is load bearing. we have to wait to create the EntityManager until the DataSource is available
    context().getService(DataSource.class);
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    EntityManager em = emf.createEntityManager();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    em.close();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No context should exist",refs);
    
    //Restart the bundle to check the unit gets re-registered and is not immediately unregistered
    Bundle b = context().getBundleByName("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    em = emf.createEntityManager();
    em.close();
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", 100);
    
    //Test a second time to make sure state isn't held
    
    em = emf.createEntityManager();
    
    callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    em.close();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No context should exist",refs);
  }
  
  @Test
  public void testContainerRuntimeQuiesce() throws Exception {
    //This is load bearing. we have to wait to create the EntityManager until the DataSource is available
    context().getService(DataSource.class);
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    
    EntityManager em = emf.createEntityManager();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.container")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished early", callback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");

    em.close();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No context should exist",refs);
  }
  
  @Test
  public void testComplexQuiesceInteraction() throws Exception {

    //This is load bearing. we have to wait to create the EntityManager until the DataSource is available
    context().getService(DataSource.class);
    
    // Get a managed context registered
    PersistenceContextProvider provider = context().getService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    
    //Set up a transaction so we can check the Quiesce waits properly
    UserTransaction tm = context().getService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    //Quiesce the Unit, nothing should happen
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback unitCallback = new TestQuiesceCallback();
    
    participant.quiesce(unitCallback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", unitCallback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    
    //Quiesce the context, still nothing
    
    participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback contextCallback = new TestQuiesceCallback();
    
    participant.quiesce(contextCallback, Collections.singletonList(context().getBundleByName(
    "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", unitCallback.bundleClearedUp());
    assertFalse("Quiesce finished", contextCallback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    //Keep the unit alive
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
  
  
    EntityManager em = emf.createEntityManager();
    
    tm.commit();
    
    assertTrue("Quiesce not finished", contextCallback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
    
    //Still a unit
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    em.close();
    
    assertTrue("Quiesce not finished", unitCallback.bundleClearedUp());
    
    refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
  
    assertNull("No unit should exist",refs);
  }
  
  @Test
  public void testComplexQuiesceInteraction2() throws Exception {
    //This is load bearing. we have to wait to create the EntityManager until the DataSource is available
    context().getService(DataSource.class);
    
    // Get a managed context registered
    PersistenceContextProvider provider = context().getService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    
    //Set up a transaction so we can check the Quiesce waits properly
    UserTransaction tm = context().getService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    //Quiesce the Unit, nothing should happen
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    MultiQuiesceCallback callback = new MultiQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    //Quiesce the context, still nothing
    participant = getParticipant("org.apache.aries.jpa.container.context");
    
    participant.quiesce(callback, Collections.singletonList(
            context().getBundleByName("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    emf = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    
    tm.commit();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No context should exist",refs);
    
  }
  
  
  
  private QuiesceParticipant getParticipant(String bundleName) throws InvalidSyntaxException {
    ServiceReference[] refs = bundleContext.getServiceReferences(QuiesceParticipant.class.getName(), null);
    
    if(refs != null) {
      for(ServiceReference ref : refs) {
        if(ref.getBundle().getSymbolicName().equals(bundleName))
          return (QuiesceParticipant) bundleContext.getService(ref);
      }
    }
    
    
    return null;
  }

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    return testOptions(
        transactionBootDelegation(),
        paxLogging("DEBUG"),

        // Bundles
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.cglib"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.api"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.core"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.testds"),
        mavenBundle("org.apache.derby", "derby"),
        mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core"), 
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager"),
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("commons-pool", "commons-pool"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
        mavenBundle("org.apache.openjpa", "openjpa"),

//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
        
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle"),

        // Add in a workaround to get OSGi 4.3 support with the current version of pax-exam
        PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
        equinox().version("3.7.0.v20110613")
    );
      
  }
}
