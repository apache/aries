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
package org.apache.aries.blueprint.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.proxy.AbstractProxyTest.TestListener;
import org.apache.aries.proxy.impl.weaving.InterfaceCombiningClassAdapter;
import org.junit.Test;

public class InterfaceProxyingTest {

  public final static class TestCallable implements Callable<Object> {
    
    private Object list = new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        return null;
      }
    };
    
    public Object call() throws Exception {
      return list;
    }
    
    public void setReturn(Object o) {
      list = o;
    }
  }
  
  @Test
  public void testGetProxyInstance1() throws Exception{
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>(Arrays.asList(Closeable.class));
    
    Object o = InterfaceCombiningClassAdapter.getProxyInstance(classes, 
        new Callable<Object>() {

          @Override
          public Object call() throws Exception {
            return null;
          }
    }, null);
    
    assertTrue(o instanceof Closeable);
  }
  
  @Test
  public void testGetProxyInstance2() throws Exception{
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>(Arrays.asList(Closeable.class,
        Iterable.class, Map.class));
    
    Object o = InterfaceCombiningClassAdapter.getProxyInstance(classes, 
        new Callable<Object>() {

          @Override
          public Object call() throws Exception {
            return null;
          }
    }, null);
    
    assertTrue(o instanceof Closeable);
    assertTrue(o instanceof Iterable);
    assertTrue(o instanceof Map);
    
  }

  /**
   * Test a class whose super couldn't be woven
   */
  @Test
  public void testDelegationAndInterception() throws Exception
  {
    Collection<Class<?>> classes = new ArrayList<Class<?>>(Arrays.asList(Callable.class));
    TestListener tl = new TestListener();
    TestCallable tc = new TestCallable();
    
    Callable o = (Callable) InterfaceCombiningClassAdapter.getProxyInstance(classes, tc, tl);
    
    assertCalled(tl, false, false, false);
    
    assertNull(null, o.call());
    
    assertCalled(tl, true, true, false);
    
    assertEquals(Callable.class.getMethod("call"), 
        tl.getLastMethod());

    tl.clear();
    assertCalled(tl, false, false, false);
    
    tc.setReturn(new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        throw new RuntimeException();
      }
    });
    try {
      o.call();
      fail("Should throw an exception");
    } catch (RuntimeException re) {
      assertCalled(tl, true, false, true);
      assertSame(re, tl.getLastThrowable());
    }
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    tc.setReturn(new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        try {
          throw new RuntimeException();
        } catch (RuntimeException re) {
          return new Object();
        }
      }
    });
    
    
    try {
      assertNotNull(o.call());
    } finally {
      assertCalled(tl, true, true, false);
    }
  }
  
  @Test
  public void testCaching() throws Exception {
    Collection<Class<?>> classes = new ArrayList<Class<?>>(Arrays.asList(Closeable.class));
    
    Object o1 = InterfaceCombiningClassAdapter.getProxyInstance(classes, 
        new Callable<Object>() {

          @Override
          public Object call() throws Exception {
            return null;
          }
    }, null);
    
    Object o2 = InterfaceCombiningClassAdapter.getProxyInstance(classes, 
        new Callable<Object>() {

          @Override
          public Object call() throws Exception {
            return null;
          }
    }, null);
    
    assertSame(o1.getClass(), o2.getClass());
  }
  
  protected void assertCalled(TestListener listener, boolean pre, boolean post, boolean ex) {
    assertEquals(pre, listener.preInvoke);
    assertEquals(post, listener.postInvoke);
    assertEquals(ex, listener.postInvokeExceptionalReturn);
  }
}
