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
package org.apache.aries.jpa.context.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

public abstract class JPAContextTest extends AbstractJPAItest {
  
  @Inject
  UserTransaction ut;
 
  @Test
  public void findEntityManagerFactory() throws Exception {
    getEMF(TEST_UNIT);
  }
  
  @Test
  public void findManagedContextFactory() throws Exception {
    try{
      getProxyEMF(TEST_UNIT);
      fail("No context should exist");
    } catch (RuntimeException re) {
      //Expected
    }
    
    registerClient(TEST_UNIT);
    getProxyEMF(TEST_UNIT);    
  }
  
  @SuppressWarnings("rawtypes")
  @Test
  public void testTranRequired() throws Exception {
    registerClient(BP_TEST_UNIT);
    
    EntityManagerFactory emf = getProxyEMF(BP_TEST_UNIT);
    
    final EntityManager managedEm = emf.createEntityManager();
    
    ensureTREBehaviour(false, managedEm, "contains", new Object());
    ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi");
    ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi");
    ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", Object.class);
    ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", "hi");
    ensureTREBehaviour(false, managedEm, "createQuery", "hi");
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object());
    ensureTREBehaviour(true, managedEm, "flush");
    ensureTREBehaviour(false, managedEm, "getDelegate");
    ensureTREBehaviour(false, managedEm, "getFlushMode");
    ensureTREBehaviour(false, managedEm, "getReference", Object.class, new Object());
    ensureTREBehaviour(true, managedEm, "lock", new Object(), LockModeType.NONE);
    ensureTREBehaviour(true, managedEm, "merge", new Object());
    ensureTREBehaviour(true, managedEm, "persist", new Object());
    ensureTREBehaviour(true, managedEm, "refresh", new Object());
    ensureTREBehaviour(true, managedEm, "remove", new Object());
    ensureTREBehaviour(false, managedEm, "setFlushMode", FlushModeType.AUTO);
    ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi", Object.class);
    ensureTREBehaviour(false, managedEm, "createQuery", Proxy.newProxyInstance(this.getClass().getClassLoader(),
       new Class[] {CriteriaQuery.class}, new InvocationHandler() {
      
      public Object invoke(Object proxy, Method method, Object[] args)
          throws Throwable {
        return null;
      }
    }));
    ensureTREBehaviour(false, managedEm, "createQuery", "hi", Object.class);
    ensureTREBehaviour(false, managedEm, "detach", new Object());
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), new HashMap());
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE);
    ensureTREBehaviour(true, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC);
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE, 
        new HashMap());
    ensureTREBehaviour(true, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC, 
        new HashMap());
    ensureTREBehaviour(false, managedEm, "getCriteriaBuilder");
    ensureTREBehaviour(true, managedEm, "getLockMode", new Object());
    ensureTREBehaviour(false, managedEm, "getMetamodel");
    ensureTREBehaviour(false, managedEm, "getProperties");
    ensureTREBehaviour(true, managedEm, "lock", new Object(), LockModeType.NONE, new HashMap());
    ensureTREBehaviour(true, managedEm, "refresh", new Object(), new HashMap());
    ensureTREBehaviour(true, managedEm, "refresh", new Object(), LockModeType.NONE);
    ensureTREBehaviour(true, managedEm, "refresh", new Object(), LockModeType.NONE, new HashMap());
    ensureTREBehaviour(false, managedEm, "setProperty", "hi", new Object());
    ensureTREBehaviour(false, managedEm, "unwrap", Object.class);

    // now test that with a transaction actually active we don't get *any* TransactionRequiredExceptions
    ut.begin();
    try{
      ensureTREBehaviour(false, managedEm, "contains", new Object());
      ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi");
      ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi");
      ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", Object.class);
      ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", "hi");
      ensureTREBehaviour(false, managedEm, "createQuery", "hi");
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object());
      ensureTREBehaviour(false, managedEm, "flush");
      ensureTREBehaviour(false, managedEm, "getDelegate");
      ensureTREBehaviour(false, managedEm, "getFlushMode");
      ensureTREBehaviour(false, managedEm, "getReference", Object.class, new Object());
      ensureTREBehaviour(false, managedEm, "lock", new Object(), LockModeType.NONE);
      ensureTREBehaviour(false, managedEm, "merge", new Object());
      ensureTREBehaviour(false, managedEm, "persist", new Object());
      ensureTREBehaviour(false, managedEm, "refresh", new Object());
      ensureTREBehaviour(false, managedEm, "remove", new Object());
      ensureTREBehaviour(false, managedEm, "setFlushMode", FlushModeType.AUTO);
      ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi", Object.class);
      ensureTREBehaviour(false, managedEm, "createQuery", Proxy.newProxyInstance(this.getClass().getClassLoader(),
         new Class[] {CriteriaQuery.class}, new InvocationHandler() {
          
          public Object invoke(Object proxy, Method method, Object[] args)
              throws Throwable {
            return null;
          }
        }));
      ensureTREBehaviour(false, managedEm, "createQuery", "hi", Object.class);
      ensureTREBehaviour(false, managedEm, "detach", new Object());
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), new HashMap());
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE);
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC);
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE, 
          new HashMap());
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC, 
          new HashMap());
      ensureTREBehaviour(false, managedEm, "getCriteriaBuilder");
      ensureTREBehaviour(false, managedEm, "getLockMode", new Object());
      ensureTREBehaviour(false, managedEm, "getMetamodel");
      ensureTREBehaviour(false, managedEm, "getProperties");
      ensureTREBehaviour(false, managedEm, "lock", new Object(), LockModeType.NONE, new HashMap());      
      ensureTREBehaviour(false, managedEm, "refresh", new Object(), new HashMap());
      ensureTREBehaviour(false, managedEm, "refresh", new Object(), LockModeType.NONE);
      ensureTREBehaviour(false, managedEm, "refresh", new Object(), LockModeType.NONE, new HashMap());
      ensureTREBehaviour(false, managedEm, "setProperty", "hi", new Object());
      ensureTREBehaviour(false, managedEm, "unwrap", Object.class);
    } finally {
      ut.rollback();
    }
  }
  
  @Test
  public void testNonTxEmIsCleared() throws Exception {
    registerClient(BP_TEST_UNIT);
    EntityManagerFactory emf = getProxyEMF(BP_TEST_UNIT);
    doNonTxEmIsCleared(emf);
  }

  private void doNonTxEmIsCleared(EntityManagerFactory emf)
      throws NotSupportedException, SystemException, HeuristicMixedException,
      HeuristicRollbackException, RollbackException {
    final EntityManager managedEm = emf.createEntityManager();
    
    ut.begin();
    try {
      
      Query q = managedEm.createQuery("DELETE from Car c");
      q.executeUpdate();
      
      q = managedEm.createQuery("SELECT Count(c) from Car c");
      assertEquals(0l, q.getSingleResult());
      
      Car car = new Car();
      car.setNumberOfSeats(5);
      car.setEngineSize(1200);
      car.setColour("blue");
      car.setNumberPlate("A1AAA");
      managedEm.persist(car);
    } catch (Exception e) {
      e.printStackTrace();
    }finally {
      ut.commit();
    }
    
    Car c = managedEm.find(Car.class, "A1AAA");
    
    assertEquals(5, c.getNumberOfSeats());
    assertEquals(1200, c.getEngineSize());
    assertEquals("blue", c.getColour());
    
    ut.begin();
    try {
      Car car = managedEm.find(Car.class, "A1AAA");
      car.setNumberOfSeats(2);
      car.setEngineSize(2000);
      car.setColour("red");
    } finally {
      ut.commit();
    }
    
    c = managedEm.find(Car.class, "A1AAA");
    
    assertEquals(2, c.getNumberOfSeats());
    assertEquals(2000, c.getEngineSize());
    assertEquals("red", c.getColour());
  }
  
  @Test
  public void testNonTxEmIsClearedUsingXADataSourceWrapper() throws Exception {
    registerClient(BP_XA_TEST_UNIT);
    EntityManagerFactory emf = getProxyEMF(BP_XA_TEST_UNIT);
    doNonTxEmIsCleared(emf);
  }

  @Test
  public void testNonTxQueries() throws Exception {
    registerClient(BP_TEST_UNIT);
    EntityManagerFactory emf = getProxyEMF(BP_TEST_UNIT);
    final EntityManager managedEm = emf.createEntityManager();
    ut.begin();
    try {
      
      Query q = managedEm.createQuery("DELETE from Car c");
      q.executeUpdate();
      
      q = managedEm.createQuery("SELECT Count(c) from Car c");
      assertEquals(0l, q.getSingleResult());
    } finally {
      ut.commit();
    }
    
    Query countQuery = managedEm.createQuery("SELECT Count(c) from Car c");
    assertEquals(0l, countQuery.getSingleResult());
    
    ut.begin();
    try {
      Car car = new Car();
      car.setNumberOfSeats(5);
      car.setEngineSize(1200);
      car.setColour("blue");
      car.setNumberPlate("A1AAA");
      managedEm.persist(car);
      
      car = new Car();
      car.setNumberOfSeats(7);
      car.setEngineSize(1800);
      car.setColour("green");
      car.setNumberPlate("B2BBB");
      managedEm.persist(car);
    } finally {
      ut.commit();
    }
    
    assertEquals(2l, countQuery.getSingleResult());
    
    TypedQuery<Car> carQuery = managedEm.
             createQuery("Select c from Car c ORDER by c.engineSize", Car.class);
    
    List<Car> list = carQuery.getResultList();
    assertEquals(2l, list.size());
    
    assertEquals(5, list.get(0).getNumberOfSeats());
    assertEquals(1200, list.get(0).getEngineSize());
    assertEquals("blue", list.get(0).getColour());
    assertEquals("A1AAA", list.get(0).getNumberPlate());
    
    assertEquals(7, list.get(1).getNumberOfSeats());
    assertEquals(1800, list.get(1).getEngineSize());
    assertEquals("green", list.get(1).getColour());
    assertEquals("B2BBB", list.get(1).getNumberPlate());
    
    ut.begin();
    try {
      Car car = managedEm.find(Car.class, "A1AAA");
      car.setNumberOfSeats(2);
      car.setEngineSize(2000);
      car.setColour("red");
      
      car = managedEm.find(Car.class, "B2BBB");
      managedEm.remove(car);
      
      car = new Car();
      car.setNumberOfSeats(2);
      car.setEngineSize(800);
      car.setColour("black");
      car.setNumberPlate("C3CCC");
      managedEm.persist(car);
      
    } finally {
      ut.commit();
    }
    
    assertEquals(2l, countQuery.getSingleResult());
    
    list = carQuery.getResultList();
    assertEquals(2l, list.size());
    
    assertEquals(2, list.get(0).getNumberOfSeats());
    assertEquals(800, list.get(0).getEngineSize());
    assertEquals("black", list.get(0).getColour());
    assertEquals("C3CCC", list.get(0).getNumberPlate());
    
    assertEquals(5, list.get(1).getNumberOfSeats());
    assertEquals(1200, list.get(1).getEngineSize());
    assertEquals("blue", list.get(1).getColour());
    assertEquals("A1AAA", list.get(1).getNumberPlate());
  }

  private void ensureTREBehaviour(boolean expectedToFail, EntityManager em, String methodName, Object... args) throws Exception {
    List<Class<?>> argTypes = new ArrayList<Class<?>>();
    for(Object o : args) {
      if(o instanceof Map)
        argTypes.add(Map.class);
      else if (o instanceof CriteriaQuery)
        argTypes.add(CriteriaQuery.class);
      else
        argTypes.add(o.getClass());
    }
    
    Method m = EntityManager.class.getMethod(methodName, 
        argTypes.toArray(new Class[args.length]));
    
    try {
      m.invoke(em, args);
      if(expectedToFail)
        fail("A transaction is required");
    } catch (InvocationTargetException ite) {
      if(expectedToFail && 
          !!!(ite.getCause() instanceof TransactionRequiredException))
        fail("We got the wrong failure. Expected a TransactionRequiredException" +
        		", got a " + ite.toString());
      else if (!!!expectedToFail && 
          ite.getCause() instanceof TransactionRequiredException)
        fail("We got the wrong failure. Expected not to get a TransactionRequiredException" +
            ", but we got one anyway!");
    }
  }
  

}
