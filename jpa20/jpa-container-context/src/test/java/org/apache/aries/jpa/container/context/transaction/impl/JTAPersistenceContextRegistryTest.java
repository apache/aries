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
package org.apache.aries.jpa.container.context.transaction.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TransactionRequiredException;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class JTAPersistenceContextRegistryTest {

  private TranSyncRegistryMock reg;
  
  private EntityManagerFactory emf1;
  private Map<Object,Object> props1;
  private EntityManagerFactory emf2;
  private Map<Object,Object> props2;
  
  private JTAPersistenceContextRegistry contexts;
  private BundleContext ctx;
  private TransactionSynchronizationRegistry tsr;

  private ServiceReference ref;
  
  @Before
  public void setup() 
  {
    reg = new TranSyncRegistryMock();
    tsr = Skeleton.newMock(reg, TransactionSynchronizationRegistry.class);
    ctx = Skeleton.newMock(BundleContext.class);

    props1 = new HashMap<Object, Object>();
    props1.put("prop1", "value1");
    
    props2 = new HashMap<Object, Object>();
    props2.put("prop2", "value2");
    
    emf1 = Skeleton.newMock(EntityManagerFactory.class);
    
    Skeleton.getSkeleton(emf1).setReturnValue(new MethodCall(EntityManagerFactory.class, 
        "createEntityManager", props1), Skeleton.newMock(EntityManager.class));
    Skeleton.getSkeleton(emf1).setReturnValue(new MethodCall(EntityManagerFactory.class, 
        "createEntityManager", props2), Skeleton.newMock(EntityManager.class));
    
    emf2 = Skeleton.newMock(EntityManagerFactory.class);

    Skeleton.getSkeleton(emf2).setReturnValue(new MethodCall(EntityManagerFactory.class, 
        "createEntityManager", props1), Skeleton.newMock(EntityManager.class));
    Skeleton.getSkeleton(emf2).setReturnValue(new MethodCall(EntityManagerFactory.class, 
        "createEntityManager", props2), Skeleton.newMock(EntityManager.class));

    
    contexts = new JTAPersistenceContextRegistry(ctx);
    ref = Skeleton.newMock(ServiceReference.class);
    Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(BundleContext.class,
        "getService", ref), tsr);
    contexts.addingService(ref);
  }
  
  @Test
  public void testIsTranActive()
  {
    reg.setTransactionKey(null);
    
    assertFalse(contexts.isTransactionActive());
    
    reg.setTransactionKey("");
    
    assertTrue(contexts.isTransactionActive());
  }
  
  @Test
  public void testMultiGetsOneTran()
  {
    AtomicLong useCount = new AtomicLong(0);
    DestroyCallback cbk = Skeleton.newMock(DestroyCallback.class);
    reg.setTransactionKey("");
    
    EntityManager em1a = contexts.getCurrentPersistenceContext(emf1, props1, useCount, cbk);
    EntityManager em1b = contexts.getCurrentPersistenceContext(emf1, props1, useCount, cbk);
    
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf1).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em1a, em1b);
    assertEquals("Expected only one creation", 1, useCount.get());
    Skeleton.getSkeleton(cbk).assertSkeletonNotCalled();
    
    EntityManager em2a = contexts.getCurrentPersistenceContext(emf2, props1, useCount, cbk);
    EntityManager em2b = contexts.getCurrentPersistenceContext(emf2, props1, useCount, cbk);
    
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf2).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em2a, em2b);
    assertEquals("Expected a second creation", 2, useCount.get());
    Skeleton.getSkeleton(cbk).assertSkeletonNotCalled();
    
    reg.afterCompletion("");
    
    Skeleton.getSkeleton(em1a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"),1);
    Skeleton.getSkeleton(em2a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"),1);
    assertEquals("Expected creations to be uncounted", 0, useCount.get());
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(DestroyCallback.class,
        "callback"), 2);
  }
  
  @Test
  public void testMultiGetsMultiTrans()
  {
    AtomicLong useCount = new AtomicLong(0);
    DestroyCallback cbk = Skeleton.newMock(DestroyCallback.class);
    
    reg.setTransactionKey("a");
    EntityManager em1a = contexts.getCurrentPersistenceContext(emf1, props1, useCount, cbk);
    reg.setTransactionKey("b");
    EntityManager em1b = contexts.getCurrentPersistenceContext(emf1, props2, useCount, cbk);
    
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props2), 1);
    
    assertNotSame("We should not get the same delegate!", em1a, em1b);
    
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em1b).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    
    assertEquals("Expected two creations", 2, useCount.get());
    Skeleton.getSkeleton(cbk).assertSkeletonNotCalled();
    
    reg.setTransactionKey("a");
    EntityManager em2a = contexts.getCurrentPersistenceContext(emf2, props1, useCount, cbk);
    reg.setTransactionKey("b");
    EntityManager em2b = contexts.getCurrentPersistenceContext(emf2, props2, useCount, cbk);
    
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props2), 1);
   
    assertNotSame("We should get the same delegate!", em2a, em2b);
    
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em2b).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    
    assertEquals("Expected four creations", 4, useCount.get());
    Skeleton.getSkeleton(cbk).assertSkeletonNotCalled();
    
    reg.setTransactionKey("b");
    reg.afterCompletion("b");
    
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em1b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em2b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    
    assertEquals("Expected two uncreations", 2, useCount.get());
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(DestroyCallback.class,
    "callback"), 2);
    
    reg.setTransactionKey("a");
    reg.afterCompletion("a");
    
    Skeleton.getSkeleton(em1a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em1b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em2a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em2b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    assertEquals("Expected no remaining instances", 0, useCount.get());
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(DestroyCallback.class,
    "callback"), 4);
  }
  
  @Test
  public void testNoTranSyncRegistry() {
    JTAPersistenceContextRegistry registry = new JTAPersistenceContextRegistry(ctx);
    
    reg.setTransactionKey(null);
    
    assertFalse(registry.jtaIntegrationAvailable());
    assertFalse(registry.isTransactionActive());
    
    Skeleton.getSkeleton(tsr).assertSkeletonNotCalled();
    
    reg.setTransactionKey("");
    
    assertFalse(registry.jtaIntegrationAvailable());
    assertFalse(registry.isTransactionActive());
    
    reg.setTransactionKey(null);
    contexts.removedService(ref, ref);
    
    assertFalse(contexts.jtaIntegrationAvailable());
    assertFalse(contexts.isTransactionActive());
    
    Skeleton.getSkeleton(tsr).assertSkeletonNotCalled();
    
    reg.setTransactionKey("");
    
    assertFalse(contexts.jtaIntegrationAvailable());
    assertFalse(contexts.isTransactionActive());
    
    
    Skeleton.getSkeleton(tsr).assertSkeletonNotCalled();
  }
  
  @Test(expected=TransactionRequiredException.class)
  public void testGetNoTran() {
    reg.setTransactionKey(null);
    contexts.getCurrentPersistenceContext(emf1, props1, new AtomicLong(), Skeleton.newMock(DestroyCallback.class));
  }
  
  @Test(expected=TransactionRequiredException.class)
  public void testGetNoTranSyncRegistry() {
    reg.setTransactionKey("");
    contexts.removedService(ref, ref);
    contexts.getCurrentPersistenceContext(emf1, props1, new AtomicLong(), Skeleton.newMock(DestroyCallback.class));
  }
  
  @Test(expected=TransactionRequiredException.class)
  public void testGetExistingNoTran() {
    reg.setTransactionKey(null);
    contexts.getExistingPersistenceContext(emf1);
  }
  
  @Test(expected=TransactionRequiredException.class)
  public void testGetExistingNoTranSyncRegistry() {
    reg.setTransactionKey("");
    contexts.removedService(ref, ref);
    contexts.getExistingPersistenceContext(emf1);
  }
  
  @Test
  public void testGetExisting()
  {
    reg.setTransactionKey("");
    
    assertNull(contexts.getExistingPersistenceContext(emf1));
    assertNull(contexts.getExistingPersistenceContext(emf2));
    
    AtomicLong useCount = new AtomicLong(0);
    DestroyCallback cbk = Skeleton.newMock(DestroyCallback.class);
    reg.setTransactionKey("");
    
    EntityManager em1a = contexts.getCurrentPersistenceContext(emf1, props1, useCount, cbk);
    EntityManager em1b = contexts.getExistingPersistenceContext(emf1);
    
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf1).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em1a, em1b);
    assertEquals("Expected only one creation", 1, useCount.get());
    Skeleton.getSkeleton(cbk).assertSkeletonNotCalled();
    
    EntityManager em2a = contexts.getCurrentPersistenceContext(emf2, props1, useCount, cbk);
    EntityManager em2b = contexts.getExistingPersistenceContext(emf2);
    
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf2).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em2a, em2b);
    assertEquals("Expected a second creation", 2, useCount.get());
    Skeleton.getSkeleton(cbk).assertSkeletonNotCalled();
    
    reg.afterCompletion("");
    
    Skeleton.getSkeleton(em1a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"),1);
    Skeleton.getSkeleton(em2a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"),1);
    assertEquals("Expected creations to be uncounted", 0, useCount.get());
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(DestroyCallback.class,
        "callback"), 2);
  }
  
  @Test
  public void testManageExisting()
  {
    reg.setTransactionKey("");
    
    assertNull(contexts.getExistingPersistenceContext(emf1));
    assertNull(contexts.getExistingPersistenceContext(emf2));
    
    EntityManager em1a = emf1.createEntityManager(props1);
    contexts.manageExistingPersistenceContext(emf1, em1a);
    
    Skeleton.getSkeleton(em1a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "joinTransaction"), 1);
    
    EntityManager em1b = contexts.getExistingPersistenceContext(emf1);
    
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf1).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em1a, em1b);
    
    EntityManager em2a = emf2.createEntityManager(props1);
    contexts.manageExistingPersistenceContext(emf2, em2a);
    
    Skeleton.getSkeleton(em2a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "joinTransaction"), 1);
    
    EntityManager em2b = contexts.getExistingPersistenceContext(emf2);
    
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf2).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em2a, em2b);
    
    reg.afterCompletion("");
    
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
  }
  
}
