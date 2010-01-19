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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.context.impl;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Hashtable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.impl.PersistenceContextManager;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


public class PersistenceContextManagerTest {

  private BundleContext context;
  private PersistenceContextManager mgr;
  
  private EntityManagerFactory emf1;
  private EntityManagerFactory emf2;
  
  private ServiceRegistration reg1;
  private ServiceRegistration reg2;
  
  private Bundle client1;
  private Bundle client2;
  
  @Before
  public void setUp()
  {
    client1 = Skeleton.newMock(Bundle.class);
    client2 = Skeleton.newMock(Bundle.class);
    emf1 = Skeleton.newMock(EntityManagerFactory.class);
    emf2 = Skeleton.newMock(EntityManagerFactory.class);
    context = Skeleton.newMock(new BundleMock("system.bundle", new Hashtable<Object, Object>()), Bundle.class).getBundleContext();
    mgr = new PersistenceContextManager(context, null);
    mgr.open();
  }
  
  @After
  public void tearDown()
  {
    BundleContextMock.clear();
  }
  
  /**
   * A simple test to show we get a service registered when a unit
   * is registered first.
   * 
   * @throws InvalidSyntaxException
   */
  @Test
  public void testUnitThenContext() throws InvalidSyntaxException
  {
    String unitName = "unit";
    
    reg1 = registerUnit(emf1, unitName, TRUE);
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
    
    mgr.registerContext(unitName, client1, new HashMap<String, Object>());
    
    assertContextRegistered(unitName);
  }

  /**
   * A simple test to show we get a service unregistered when a context
   * is removed.
   * 
   * @throws InvalidSyntaxException
   */
  @Test
  public void testUnitThenContextThenRemoveContext() throws InvalidSyntaxException
  {
    testUnitThenContext();
    mgr.unregisterContext("unit", client1);
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
  }
  
  /**
   * A simple test to show we get a service unregistered when a unit
   * is removed.
   * 
   * @throws InvalidSyntaxException
   */
  @Test
  public void testUnitThenContextThenRemoveUnit() throws InvalidSyntaxException
  {
    testUnitThenContext();
    reg1.unregister();
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
  }
  
  /**
   * A simple test to show we get a service registered when a context
   * is registered first.
   * 
   * @throws InvalidSyntaxException
   */
  @Test
  public void testContextThenUnit() throws InvalidSyntaxException
  {
    String unitName = "unit";
    
    mgr.registerContext(unitName, client1, new HashMap<String, Object>());
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
    
    reg1 = registerUnit(emf1, unitName, TRUE);
    
    assertContextRegistered(unitName);
  }

  /**
   * A simple test to show we get a service unregistered when a context
   * is removed.
   * 
   * @throws InvalidSyntaxException
   */
  @Test
  public void testContextThenUnitThenRemoveContext() throws InvalidSyntaxException
  {
    testContextThenUnit();
    mgr.unregisterContext("unit", client1);
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
  }
  
  /**
   * A simple test to show we get a service unregistered when a unit
   * is removed.
   * 
   * @throws InvalidSyntaxException
   */
  @Test
  public void testContextThenUnitThenRemoveUnit() throws InvalidSyntaxException
  {
    testContextThenUnit();
    reg1.unregister();
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
  }
  
  /**
   * Test that we don't register a service when the unit and
   * context don't match
   */
  @Test
  public void testAddDifferentContext()
  {
    reg1 = registerUnit(emf1, "unit", TRUE);
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
    
    mgr.registerContext("context", client1, new HashMap<String, Object>());
    
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
  }
  
  /**
   * Test that we don't unregister a service when a different context is
   * removed
   * @throws InvalidSyntaxException
   */
  @Test
  public void testRemoveDifferentContext() throws InvalidSyntaxException
  {
    testAddDifferentContext();
    
    mgr.registerContext("unit", client1, new HashMap<String, Object>());
    
    assertContextRegistered("unit");
    
    mgr.unregisterContext("context", client1);
    
    assertContextRegistered("unit");
  }
  
  /**
   * Test that we don't unregister a service when a different unit is
   * removed
   * @throws InvalidSyntaxException
   */
  @Test
  public void testRemoveDifferentUnit() throws InvalidSyntaxException
  {
    testAddDifferentContext();
    reg2 = registerUnit(emf2, "context", TRUE);
    assertContextRegistered("context");
    reg1.unregister();
    assertContextRegistered("context");
    reg2.unregister();
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
  }
  
  /**
   * Test that we cope when multiple clients consume the same context
   * @throws InvalidSyntaxException
   */
  @Test
  public void testMultipleClients() throws InvalidSyntaxException
  {
    testContextThenUnit();
    
    mgr.registerContext("unit", client2, new HashMap<String, Object>());
    assertContextRegistered("unit");
    
    mgr.unregisterContext("unit", client1);
    assertContextRegistered("unit");
    
    mgr.unregisterContext("unit", client2);
    BundleContextMock.assertNoServiceExists(EntityManager.class.getName());
  }
  
  
  private ServiceRegistration registerUnit(EntityManagerFactory emf, String name, Boolean managed) {
    
    Hashtable<String, Object> props = new Hashtable<String, Object>();
    
    if(name != null)
      props.put(PersistenceUnitConstants.OSGI_UNIT_NAME, name);
    
    if(managed)
      props.put(PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT, managed);
    
    props.put(PersistenceUnitConstants.OSGI_UNIT_PROVIDER, "some.provider.Name");
    
    return context.registerService(
        EntityManagerFactory.class.getName(), emf, props);
  }
  
  private void assertContextRegistered(String name) throws InvalidSyntaxException {
    BundleContextMock.assertServiceExists(EntityManager.class.getName());
    
    ServiceReference[] refs = context.getServiceReferences(EntityManager.class.getName(), null);
    
    assertEquals("Too many EntityManagers", 1, refs.length);
    
    assertEquals("Wrong unit name", name, refs[0].getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME));
    
    assertEquals("Wrong provider name", "some.provider.Name", refs[0].getProperty(PersistenceUnitConstants.OSGI_UNIT_PROVIDER));
    
    assertEquals("Unit should be managed", Boolean.TRUE, refs[0].getProperty(PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT));
  }
}
