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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.proxy.ProxyTestClassInnerClasses.ProxyTestClassInner;
import org.apache.aries.blueprint.proxy.ProxyTestClassInnerClasses.ProxyTestClassStaticInner;
import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.impl.SingleInstanceDispatcher;
import org.junit.Test;
import org.osgi.framework.wiring.BundleWiring;

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

  protected abstract Object getProxyInstance(Class<?> proxyClass);
  protected abstract Object getProxyInstance(Class<?> proxyClass, InvocationListener listener);
  protected abstract Class<?> getProxyClass(Class<?> clazz);
  protected abstract Object setDelegate(Object proxy, Callable<Object> dispatcher);
  
  protected Class<?> getTestClass() {
	  return ProxyTestClassGeneral.class;
  }
  
  protected Method getDeclaredMethod(Class<?> testClass, String name,
		Class<?>... classes) throws Exception {
	return getProxyClass(testClass).getDeclaredMethod(name, classes);
  }
  
/**
   * This test uses the ProxySubclassGenerator to generate and load a subclass
   * of the specified getTestClass().
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
    assertNotNull("Generated proxy subclass was null", getProxyClass(getTestClass()));
    assertNotNull("Generated proxy subclass instance was null", getProxyInstance(getProxyClass(getTestClass())));
  }
  /**
   * Test a basic method invocation on the proxy subclass
   */
  @Test
  public void testMethodInvocation() throws Exception {
    Method m = getDeclaredMethod(getTestClass(), "testMethod", String.class,
        int.class, Object.class);
    String x = "x";
    String returned = (String) m.invoke(getProxyInstance(getProxyClass(getTestClass())), x, 1, new Object());
    assertEquals("Object returned from invocation was not correct.", x, returned);
  }
  
  /**
   * Test different argument types on a method invocation
   */
  @Test
  public void testMethodArgs() throws Exception
  {
    Method m = getDeclaredMethod(getTestClass(), "testArgs", double.class,
        short.class, long.class, char.class, byte.class, boolean.class);
    Character xc = Character.valueOf('x');
    String x = xc.toString();
    String returned = (String) m.invoke(getProxyInstance(getProxyClass(getTestClass())), Double.MAX_VALUE, Short.MIN_VALUE, Long.MAX_VALUE, xc
        .charValue(), Byte.MIN_VALUE, false);
    assertEquals("Object returned from invocation was not correct.", x, returned);
  }
  
  /**
   * Test a method that returns void
   */
  @Test
  public void testReturnVoid() throws Exception
  {
    Method m = getDeclaredMethod(getTestClass(), "testReturnVoid");
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    m.invoke(getProxyInstance(getProxyClass(getTestClass())));
  }

  /**
   * Test a method that returns an int
   */
  @Test
  public void testReturnInt() throws Exception
  {
    Method m = getDeclaredMethod(getTestClass(), "testReturnInt");
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    Integer returned = (Integer) m.invoke(getProxyInstance(getProxyClass(getTestClass())));
    assertEquals("Expected object was not returned from invocation", Integer.valueOf(17), returned);
  }

  /**
   * Test a method that returns an Integer
   */
  @Test
  public void testReturnInteger() throws Exception
  {
    Method m = getDeclaredMethod(getTestClass(), "testReturnInteger");
    Integer returned = (Integer) m.invoke(getProxyInstance(getProxyClass(getTestClass())));
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
      m = getDeclaredMethod(getTestClass(), "bMethod");
    } catch (NoSuchMethodException nsme) {
      m = getProxyClass(getTestClass()).getSuperclass().getDeclaredMethod("bMethod");
    }
    m.invoke(getProxyInstance(getProxyClass(getTestClass())));
  }

  /**
   * Test a protected method declared higher up the superclass hierarchy
   */
  @Test
  public void testProtectedHierarchyMethod() throws Exception
  {
    Method m = null;
    try {
      m = getDeclaredMethod(getTestClass(), "bProMethod");
    } catch (NoSuchMethodException nsme) {
      m = getProxyClass(getTestClass()).getSuperclass().getDeclaredMethod("bProMethod");
    }
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    m.invoke(getProxyInstance(getProxyClass(getTestClass())));
  }
  
  /**
   * Test a default method declared higher up the superclass hierarchy
   */
  @Test
  public void testDefaultHierarchyMethod() throws Exception
  {
    Method m = null;
    try {
      m = getDeclaredMethod(getTestClass(), "bDefMethod");
    } catch (NoSuchMethodException nsme) {
      m = getProxyClass(getTestClass()).getSuperclass().getDeclaredMethod("bDefMethod", new Class[] {});
    }
    //for these weaving tests we are loading the woven test classes on a different classloader
    //to this class so we need to set the method accessible
    m.setAccessible(true);
    m.invoke(getProxyInstance(getProxyClass(getTestClass())));
  }

  /**
   * Test a covariant override method
   */
  @Test
  public void testCovariant() throws Exception
  {
    Class<?> proxy = getProxyClass(ProxyTestClassCovariantOverride.class);
    
    Method m = getDeclaredMethod(ProxyTestClassCovariantOverride.class, "getCovariant");
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
    Method m = getDeclaredMethod(ProxyTestClassGeneric.class, "setSomething", String.class);
    m.invoke(o, "aString");
    
    if(getClass() == WovenProxyGeneratorTest.class)
    	m = getDeclaredMethod(ProxyTestClassGeneric.class.getSuperclass(), "getSomething");
    else 
        m = getDeclaredMethod(ProxyTestClassGeneric.class, "getSomething");

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
    Class<?> retrieved = getProxyClass(getTestClass());
    assertNotNull("The new class was null", retrieved);
    assertEquals("The same class was not returned", retrieved, getProxyClass(getTestClass()));

  }
  
  @Test
  public void testEquals() throws Exception {
    Object p1 = getProxyInstance(getProxyClass(getTestClass()));
    Object p2 = getProxyInstance(getProxyClass(getTestClass()));
    
    assertFalse("Should not be equal", p1.equals(p2));
    
    Object p3 = getP3();
    
    p1 = setDelegate(p1, new SingleInstanceDispatcher(p3));
    p2 = setDelegate(p2, new SingleInstanceDispatcher(p3));
    
    assertTrue("Should be equal", p1.equals(p2));
    
    Object p4 = getProxyInstance(getProxyClass(getTestClass()));
    Object p5 = getProxyInstance(getProxyClass(getTestClass()));
    
    p4 = setDelegate(p4, new SingleInstanceDispatcher(p1));
    p5 = setDelegate(p5, new SingleInstanceDispatcher(p2));
    
    assertTrue("Should be equal", p4.equals(p5));
  }
  
  protected abstract Object getP3() throws Exception;
  
  @Test
  public void testInterception() throws Throwable {
    
    TestListener tl = new TestListener();
    Object obj = getProxyInstance(getProxyClass(getTestClass()), tl);
    
    assertCalled(tl, false, false, false);
    
    Method m = getDeclaredMethod(getTestClass(), "testReturnInteger", new Class[] {});
    m.invoke(obj);
    
    assertCalled(tl, true, true, false);
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    m = getDeclaredMethod(getTestClass(), "testException", new Class[] {});
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
    
    m = getDeclaredMethod(getTestClass(), "testInternallyCaughtException", new Class[] {});
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
  @Test
  public void testStaticInner() throws Exception {
    assertNotNull(getProxyInstance(getProxyClass(ProxyTestClassStaticInner.class)));
  }
  @Test
  public void testInner() throws Exception {
    //An inner class has no no-args (the parent gets added as an arg) so we can't
    //get an instance
    assertNotNull(getProxyClass(ProxyTestClassInner.class));
  }
  
  /**
   * Test an abstract class
   */
  @Test
  public void testAbstractClass() throws Exception
  {
    Object ptca = getProxyInstance(getProxyClass(ProxyTestClassAbstract.class));
    ptca = setDelegate(ptca, new Callable<Object>() {

      public Object call() throws Exception {
        //We have to use a proxy instance here because we need it to be a subclass
        //of the one from the weaving loader in the weaving test...
        return getProxyInstance(ProxyTestClassChildOfAbstract.class);
      }
    });
    
    Method m = ptca.getClass().getDeclaredMethod("getMessage");
    assertEquals("Working", m.invoke(ptca));
  }
  
  public static BundleWiring getWiring(ClassLoader loader) throws Exception {
      BundleWiring wiring = mock(BundleWiring.class);
      when(wiring.getClassLoader()).thenReturn(loader);
      return wiring;
  }
}
