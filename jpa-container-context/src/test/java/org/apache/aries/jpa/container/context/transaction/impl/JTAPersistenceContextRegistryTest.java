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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;

public class JTAPersistenceContextRegistryTest {

  private static class TranSyncRegistryMock
  {
    private String key;
    
    private Map<String, List<Synchronization>> syncs = new HashMap<String, List<Synchronization>>();
    
    private Map<String, Map<Object,Object>> resources = new HashMap<String, Map<Object,Object>>();
    
    public void setTransactionKey(String s)
    {
      key = s;
    }
    
    public Object getTransactionKey() {
      return key;
    }

    public void registerInterposedSynchronization(Synchronization arg0) {
      List<Synchronization> list = syncs.get(key);
      if(list == null) {
        list = new ArrayList<Synchronization>();
        syncs.put(key, list);
      }
       list.add(arg0);
    }
    
    public Object getResource(Object o) {
      Object toReturn = null;
      Map<Object, Object> map = resources.get(key);
      if(map != null)
        toReturn = map.get(o);
      return toReturn;
    }
    
    public void putResource(Object resourceKey, Object value) {
      Map<Object, Object> map = resources.get(key);
      if(map == null) {
        map = new HashMap<Object, Object>();
        resources.put(key, map);
      }
      map.put(resourceKey, value);
    }
    
    
    public void afterCompletion(String s)
    {
      for(Synchronization sync : syncs.get(s))
        sync.afterCompletion(Status.STATUS_COMMITTED);
      
      resources.remove(s);
    }
  }
  
  private TranSyncRegistryMock reg;
  
  private EntityManagerFactory emf1;
  private Map<Object,Object> props1;
  private EntityManagerFactory emf2;
  private Map<Object,Object> props2;
  
  private JTAPersistenceContextRegistry contexts;
  
  @Before
  public void setup() 
  {
    reg = new TranSyncRegistryMock();

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

    
    contexts = new JTAPersistenceContextRegistry();
    contexts.setTranRegistry(Skeleton.newMock(reg, TransactionSynchronizationRegistry.class));
  }
  
  @Test
  public void testMultiGetsOneTran()
  {
    reg.setTransactionKey("");
    
    EntityManager em1a = contexts.getCurrentPersistenceContext(emf1, props1);
    EntityManager em1b = contexts.getCurrentPersistenceContext(emf1, props1);
    
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf1).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em1a, em1b);
    
    EntityManager em2a = contexts.getCurrentPersistenceContext(emf2, props1);
    EntityManager em2b = contexts.getCurrentPersistenceContext(emf2, props1);
    
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf2).assertNotCalled(new MethodCall(EntityManagerFactory.class, "createEntityManager"));
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    assertSame("We should get the same delegate!", em2a, em2b);
    
    reg.afterCompletion("");
    
    Skeleton.getSkeleton(em1a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"),1);
    Skeleton.getSkeleton(em2a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"),1);
  }
  
  @Test
  public void testMultiGetsMultiTrans()
  {
    reg.setTransactionKey("a");
    EntityManager em1a = contexts.getCurrentPersistenceContext(emf1, props1);
    reg.setTransactionKey("b");
    EntityManager em1b = contexts.getCurrentPersistenceContext(emf1, props2);
    
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf1).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props2), 1);
   
    assertNotSame("We should not get the same delegate!", em1a, em1b);
    
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em1b).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    
    reg.setTransactionKey("a");
    EntityManager em2a = contexts.getCurrentPersistenceContext(emf2, props1);
    reg.setTransactionKey("b");
    EntityManager em2b = contexts.getCurrentPersistenceContext(emf2, props2);
    
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props1), 1);
    Skeleton.getSkeleton(emf2).assertCalledExactNumberOfTimes(new MethodCall(EntityManagerFactory.class, "createEntityManager", props2), 1);
   
    assertNotSame("We should get the same delegate!", em2a, em2b);
    
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em2b).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    
    reg.setTransactionKey("b");
    reg.afterCompletion("b");
    
    Skeleton.getSkeleton(em1a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em1b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em2a).assertNotCalled(new MethodCall(EntityManager.class, "close"));
    Skeleton.getSkeleton(em2b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    
    reg.setTransactionKey("a");
    reg.afterCompletion("a");
    
    Skeleton.getSkeleton(em1a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em1b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em2a).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    Skeleton.getSkeleton(em2b).assertCalledExactNumberOfTimes(new MethodCall(EntityManager.class, "close"), 1);
    
  }
  
}
