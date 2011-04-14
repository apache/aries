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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.impl.SingleInstanceDispatcher;
import org.junit.Test;

public abstract class AbstractProxyTest {

  protected static class TestListener implements InvocationListener {
  
    boolean preInvoke = false;
    boolean postInvoke = false;
    boolean postInvokeExceptionalReturn = false;
    private Method m;
    private Object token;
    private Throwable e;
    
    public Object preInvoke(Object proxy, Method m, Object[] args)
        throws Throwable {
      preInvoke = true;
      token = new Object();
      this.m = m;
      return token;
    }
  
    public void postInvoke(Object token, Object proxy, Method m,
        Object returnValue) throws Throwable {
      postInvoke = this.token == token && this.m == m;
    }
  
    public void postInvokeExceptionalReturn(Object token, Object proxy,
        Method m, Throwable exception) throws Throwable {
      postInvokeExceptionalReturn = this.token == token && this.m == m;
      e = exception;
    }
    
    public void clear() {
      preInvoke = false;
      postInvoke = false;
      postInvokeExceptionalReturn = false;
      token = null;
      m = null;
      e = null;
    }
    
    public Method getLastMethod() {
      return m;
    }
    
    public Throwable getLastThrowable() {
      return e;
    }
  }

  protected static final Class<?> TEST_CLASS = ProxyTestClassGeneral.class;
  
  protected abstract Object getProxyInstance(Class<?> proxyClass);
  protected abstract Object getProxyInstance(Class<?> proxyClass, InvocationListener listener);
  protected abstract Class<?> getProxyClass(Class<?> clazz);
  protected abstract Object setDelegate(Object proxy, Callable<Object> dispatcher);
  
  /**
   * This test uses the ProxySubclassGenerator to generate and load a subclass
   * of the specified TEST_CLASS.
   * 
   * Once the subclass is generated we check that it wasn't null. We check
   * that the InvocationHandler constructor doesn't return a null object
   * either
   * 
   * Test method for
   * {@link org.apache.aries.proxy.impl.ProxySubclassGenerator#generateAndLoadSubclass()}
   * .
   */
  @Test
  public void testGenerateAndLoadProxy() throws Exception
  {
    assertNotNull("Generated proxy subclass was null", getProxyClass(TEST_CLASS));
    assertNotNull("Generated proxy subclass instance was null", getProxyInstance(getProxyClass(TEST_CLASS)));
  }
  /**
   * Test a basic method invocation on the proxy subclass
   */
  @Test
  public void testMethodInvocation() throws Exception {
    Method m = getProxyClass(TEST_CLASS).getDeclaredMethod("testMethod", new Class[] { String.class,
        int.class, Object.class });
    String x = "x";
    String returned = (String) m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)), x, 1, new Object());
    assertEquals("Object returned from invocation was not correct.", x, returned);
  }
  
  /**
   * Test different argument types on a method invocation
   */
  @Test
  public void testMethodArgs() throws Exception
  {
    Method m = getProxyClass(TEST_CLASS).getDeclaredMethod("testArgs", new Class[] { double.class,
        short.class, long.class, char.class, byte.class, boolean.class });
    Character xc = Character.valueOf('x');
    String x = xc.toString();
    String returned = (String) m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)), Double.MAX_VALUE, Short.MIN_VALUE, Long.MAX_VALUE, xc
        .charValue(), Byte.MIN_VALUE, false);
    assertEquals("Object returned from invocation was not correct.", x, returned);
  }
  
  /**
   * Test a method that returns void
   */
  @Test
  public void testReturnVoid() throws Exception
  {
    Method m = getProxyClass(TEST_CLASS).getDeclaredMethod("testReturnVoid", new Class[] {});
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)));
  }

  /**
   * Test a method that returns an int
   */
  @Test
  public void testReturnInt() throws Exception
  {
    Method m = getProxyClass(TEST_CLASS).getDeclaredMethod("testReturnInt", new Class[] {});
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    Integer returned = (Integer) m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)));
    assertEquals("Expected object was not returned from invocation", Integer.valueOf(17), returned);
  }

  /**
   * Test a method that returns an Integer
   */
  @Test
  public void testReturnInteger() throws Exception
  {
    Method m = getProxyClass(TEST_CLASS).getDeclaredMethod("testReturnInteger", new Class[] {});
    Integer returned = (Integer) m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)));
    assertEquals("Expected object was not returned from invocation", Integer.valueOf(1), returned);
  }

  /**
   * Test a public method declared higher up the superclass hierarchy
   */
  @Test
  public void testPublicHierarchyMethod() throws Exception
  {
    Method m = null;
    try {
      m = getProxyClass(TEST_CLASS).getDeclaredMethod("bMethod", new Class[] {});
    } catch (NoSuchMethodException nsme) {
      m = getProxyClass(TEST_CLASS).getSuperclass().getDeclaredMethod("bMethod", new Class[] {});
    }
    m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)));
  }

  /**
   * Test a protected method declared higher up the superclass hierarchy
   */
  @Test
  public void testProtectedHierarchyMethod() throws Exception
  {
    Method m = null;
    try {
      m = getProxyClass(TEST_CLASS).getDeclaredMethod("bProMethod", new Class[] {});
    } catch (NoSuchMethodException nsme) {
      m = getProxyClass(TEST_CLASS).getSuperclass().getDeclaredMethod("bProMethod", new Class[] {});
    }
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)));
  }
  
  /**
   * Test a default method declared higher up the superclass hierarchy
   */
  @Test
  public void testDefaultHierarchyMethod() throws Exception
  {
    Method m = null;
    try {
      m = getProxyClass(TEST_CLASS).getDeclaredMethod("bDefMethod", new Class[] {});
    } catch (NoSuchMethodException nsme) {
      m = getProxyClass(TEST_CLASS).getSuperclass().getDeclaredMethod("bDefMethod", new Class[] {});
    }
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    m.invoke(getProxyInstance(getProxyClass(TEST_CLASS)));
  }

  /**
   * Test a covariant override method
   */
  @Test
  public void testCovariant() throws Exception
  {
    Class<?> proxy = getProxyClass(ProxyTestClassCovariantOverride.class);
    
    Method m = proxy.getDeclaredMethod("getCovariant", new Class[] {});
    Object returned = m.invoke(getProxyInstance(proxy));
    assertTrue("Object was of wrong type: " + returned.getClass().getSimpleName(),
        proxy.isInstance(returned));
  }

  /**
   * Test a method with generics
   */
  @Test
  public void testGenerics() throws Exception
  {
    Class<?> proxy = getProxyClass(ProxyTestClassGeneric.class);
    
    Object o = getProxyInstance(proxy);
    Method m = proxy.getDeclaredMethod("setSomething",
        new Class[] { String.class });
    m.invoke(o, "aString");
    
    try {
      m = proxy.getDeclaredMethod("getSomething", new Class[] {});
    } catch (NoSuchMethodException nsme) {
      m = proxy.getSuperclass().getDeclaredMethod("getSomething", new Class[] {});
    }
    Object returned = m.invoke(o);
    assertTrue("Object was of wrong type", String.class.isInstance(returned));
    assertEquals("String had wrong value", "aString", returned);
  }
  
  /**
   * Test that we don't generate classes twice
   */
  @Test
  public void testRetrieveClass() throws Exception
  {
    Class<?> retrieved = getProxyClass(TEST_CLASS);
    assertNotNull("The new class was null", retrieved);
    assertEquals("The same class was not returned", retrieved, getProxyClass(TEST_CLASS));

  }
  
  @Test
  public void testEquals() throws IllegalAccessException, InstantiationException {
    Object p1 = getProxyInstance(getProxyClass(TEST_CLASS));
    Object p2 = getProxyInstance(getProxyClass(TEST_CLASS));
    
    assertFalse("Should not be equal", p1.equals(p2));
    
    Object p3 = getP3();
    
    p1 = setDelegate(p1, new SingleInstanceDispatcher(p3));
    p2 = setDelegate(p2, new SingleInstanceDispatcher(p3));
    
    assertTrue("Should be equal", p1.equals(p2));
    
    Object p4 = getProxyInstance(getProxyClass(TEST_CLASS));
    Object p5 = getProxyInstance(getProxyClass(TEST_CLASS));
    
    p4 = setDelegate(p4, new SingleInstanceDispatcher(p1));
    p5 = setDelegate(p5, new SingleInstanceDispatcher(p2));
    
    assertTrue("Should be equal", p4.equals(p5));
  }
  
  protected abstract Object getP3();
  
  @Test
  public void testInterception() throws Throwable {
    
    TestListener tl = new TestListener();
    Object obj = getProxyInstance(getProxyClass(TEST_CLASS), tl);
    
    assertCalled(tl, false, false, false);
    
    Method m = getProxyClass(TEST_CLASS).getDeclaredMethod("testReturnInteger", new Class[] {});
    m.invoke(obj);
    
    assertCalled(tl, true, true, false);
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    m = getProxyClass(TEST_CLASS).getDeclaredMethod("testException", new Class[] {});
    try {
      m.invoke(obj);
      fail("Should throw an exception");
    } catch (InvocationTargetException re) {
      if(!!!re.getTargetException().getClass().equals(RuntimeException.class))
        throw re.getTargetException();
      assertCalled(tl, true, false, true);
    }
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    m = getProxyClass(TEST_CLASS).getDeclaredMethod("testInternallyCaughtException", new Class[] {});
    try {
      m.invoke(obj);
    } finally {
      assertCalled(tl, true, true, false);
    }
  }
  
  protected void assertCalled(TestListener listener, boolean pre, boolean post, boolean ex) {
    assertEquals(pre, listener.preInvoke);
    assertEquals(post, listener.postInvoke);
    assertEquals(ex, listener.postInvokeExceptionalReturn);
  }
}
