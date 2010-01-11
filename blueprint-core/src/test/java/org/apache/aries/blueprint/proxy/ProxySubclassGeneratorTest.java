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

import static org.junit.Assert.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.apache.aries.blueprint.proxy.FinalModifierException;
import org.apache.aries.blueprint.proxy.ProxySubclassGenerator;
import org.apache.aries.blueprint.proxy.ProxySubclassMethodHashSet;

public class ProxySubclassGeneratorTest
{
  private static final Class<?> TEST_CLASS = ProxyTestClassGeneral.class;
  private static final Class<?> FINAL_METHOD_CLASS = ProxyTestClassFinalMethod.class;
  private static final Class<?> FINAL_CLASS = ProxyTestClassFinal.class;
  private static final Class<?> GENERIC_CLASS = ProxyTestClassGeneric.class;
  private static final Class<?> COVARIANT_CLASS = ProxyTestClassCovariantOverride.class;
  private static ProxySubclassMethodHashSet<String> expectedMethods = new ProxySubclassMethodHashSet<String>(
      12);
  private InvocationHandler ih = null;
  private Class<?> generatedProxySubclass = null;
  private Object o = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception
  {
    ih = new FakeInvocationHandler();
    ((FakeInvocationHandler)ih).setDelegate(TEST_CLASS.newInstance());
    generatedProxySubclass = getGeneratedSubclass();
    o = getSubclassInstance(generatedProxySubclass);
  }

  /**
   * This test uses the ProxySubclassGenerator to generate and load a subclass
   * of the specified TEST_CLASS.
   * 
   * Once the subclass is generated we check that it wasn't null. We check
   * that the InvocationHandler constructor doesn't return a null object
   * either
   * 
   * Test method for
   * {@link com.ibm.osgi.blueprint.internal.proxy.ProxySubclassGenerator#generateAndLoadSubclass()}
   * .
   */
  @Test
  public void testGenerateAndLoadSubclass() throws Exception
  {
    assertNotNull("Generated proxy subclass was null", generatedProxySubclass);
    assertNotNull("Generated proxy subclass instance was null", o);
  }

  /**
   * Test that the methods found declared on the generated proxy subclass are
   * the ones that we expect.
   */
  @Test
  public void testExpectedMethods() throws Exception
  {
    Class<?> superclass = TEST_CLASS;

    do {
      Method[] declaredMethods = superclass.getDeclaredMethods();
      List<Method> listOfDeclaredMethods = new ArrayList<Method>();
      for (Method m : declaredMethods) {
        int i = m.getModifiers();
        if (Modifier.isPrivate(i) || Modifier.isFinal(i)) {
          // private or final don't get added
        } else if (!(Modifier.isPublic(i) || Modifier.isPrivate(i) || Modifier.isProtected(i))) {
          // the method is default visibility, check the package
          if (m.getDeclaringClass().getPackage().equals(TEST_CLASS.getPackage())) {
            // default vis with same package gets added
            listOfDeclaredMethods.add(m);
          }
        } else {
          listOfDeclaredMethods.add(m);
        }
      }

      declaredMethods = listOfDeclaredMethods.toArray(new Method[] {});
      ProxySubclassMethodHashSet<String> foundMethods = new ProxySubclassMethodHashSet<String>(
          declaredMethods.length);
      foundMethods.addMethodArray(declaredMethods);
      // as we are using a set we shouldn't get duplicates
      expectedMethods.addAll(foundMethods);
      superclass = superclass.getSuperclass();
    } while (superclass != null);

    // add the getter and setter for the invocation handler to the expected
    // set
    // and the unwrapObject method
    Method[] ihMethods = new Method[] {
        generatedProxySubclass.getMethod("setInvocationHandler",
            new Class[] { InvocationHandler.class }),
        generatedProxySubclass.getMethod("getInvocationHandler", new Class[] {}) };
    expectedMethods.addMethodArray(ihMethods);

    Method[] generatedProxySubclassMethods = generatedProxySubclass.getDeclaredMethods();
    ProxySubclassMethodHashSet<String> generatedMethods = new ProxySubclassMethodHashSet<String>(
        generatedProxySubclassMethods.length);
    generatedMethods.addMethodArray(generatedProxySubclassMethods);

    // check that all the methods we have generated were expected
    for (String gen : generatedMethods) {
      assertTrue("Unexpected method: " + gen, expectedMethods.contains(gen));
    }
    // check that all the expected methods were generated
    for (String exp : expectedMethods) {
      assertTrue("Method was not generated: " + exp, generatedMethods.contains(exp));
    }
    // check the sets were the same
    assertEquals("Sets were not the same", expectedMethods, generatedMethods);

  }

  /**
   * Test a basic method invocation on the proxy subclass
   */
  @Test
  public void testMethodInvocation() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("testMethod", new Class[] { String.class,
        int.class, Object.class });
    String x = "x";
    String returned = (String) m.invoke(o, x, 1, new Object());
    assertEquals("Object returned from invocation was not correct.", x, returned);
  }

  /**
   * Test different argument types on a method invocation
   */
  @Test
  public void testMethodArgs() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("testArgs", new Class[] { double.class,
        short.class, long.class, char.class, byte.class, boolean.class });
    Character xc = Character.valueOf('x');
    String x = xc.toString();
    String returned = (String) m.invoke(o, Double.MAX_VALUE, Short.MIN_VALUE, Long.MAX_VALUE, xc
        .charValue(), Byte.MIN_VALUE, false);
    assertEquals("Object returned from invocation was not correct.", x, returned);
  }

  /**
   * Test a method that returns void
   */
  @Test
  public void testReturnVoid() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("testReturnVoid", new Class[] {});
    m.invoke(o);
  }

  /**
   * Test a method that returns an int
   */
  @Test
  public void testReturnInt() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("testReturnInt", new Class[] {});
    Integer returned = (Integer) m.invoke(o);
    assertEquals("Expected object was not returned from invocation", Integer.valueOf(17), returned);
  }

  /**
   * Test a method that returns an Integer
   */
  @Test
  public void testReturnInteger() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("testReturnInteger", new Class[] {});
    Integer returned = (Integer) m.invoke(o);
    assertEquals("Expected object was not returned from invocation", Integer.valueOf(1), returned);
  }

  /**
   * Test a public method declared higher up the superclass hierarchy
   */
  @Test
  public void testPublicHierarchyMethod() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("bMethod", new Class[] {});
    m.invoke(o);
  }

  /**
   * Test a protected method declared higher up the superclass hierarchy
   */
  @Test
  public void testProtectedHierarchyMethod() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("bProMethod", new Class[] {});
    m.invoke(o);
  }

  /**
   * Test a default method declared higher up the superclass hierarchy
   */
  @Test
  public void testDefaultHierarchyMethod() throws Exception
  {
    Method m = generatedProxySubclass.getDeclaredMethod("bDefMethod", new Class[] {});
    m.invoke(o);
  }

  /**
   * Test a covariant override method
   */
  @Test
  public void testCovariant() throws Exception
  {
    ((FakeInvocationHandler)ih).setDelegate(COVARIANT_CLASS.newInstance());
    o = ProxySubclassGenerator.newProxySubclassInstance(COVARIANT_CLASS, ih);
    generatedProxySubclass = o.getClass();
    Method m = generatedProxySubclass.getDeclaredMethod("getCovariant", new Class[] {});
    Object returned = m.invoke(o);
    assertTrue("Object was of wrong type: " + returned.getClass().getSimpleName(), COVARIANT_CLASS
        .isInstance(returned));
  }

  /**
   * Test a method with generics
   */
  @Test
  public void testGenerics() throws Exception
  {
    ((FakeInvocationHandler)ih).setDelegate(GENERIC_CLASS.newInstance());
    o = ProxySubclassGenerator.newProxySubclassInstance(GENERIC_CLASS, ih);
    generatedProxySubclass = o.getClass();
    Method m = generatedProxySubclass.getDeclaredMethod("setSomething",
        new Class[] { String.class });
    m.invoke(o, "aString");
    m = generatedProxySubclass.getDeclaredMethod("getSomething", new Class[] {});
    Object returned = m.invoke(o);
    assertTrue("Object was of wrong type", String.class.isInstance(returned));
    assertEquals("String had wrong value", "aString", returned);
  }

  /**
   * Test a method marked final
   */
  @Test
  public void testFinalMethod() throws Exception
  {
    try {
      ProxySubclassGenerator.getProxySubclass(FINAL_METHOD_CLASS);
    } catch (FinalModifierException e) {
      assertFalse("Should have found final method not final class", e.isFinalClass());
    }
  }

  /**
   * Test a class marked final
   */
  @Test
  public void testFinalClass() throws Exception
  {
    try {
      ProxySubclassGenerator.getProxySubclass(FINAL_CLASS);
    } catch (FinalModifierException e) {
      assertTrue("Should have found final class", e.isFinalClass());
    }
  }

  /**
   * Test that we don't generate classes twice
   */
  @Test
  public void testRetrieveClass() throws Exception
  {
    Class<?> retrieved = ProxySubclassGenerator.getProxySubclass(TEST_CLASS);
    assertNotNull("The new class was null", retrieved);
    assertEquals("The same class was not returned", generatedProxySubclass, retrieved);

  }

  /**
   * Test a private constructor
   */
  @Test
  public void testPrivateConstructor() throws Exception
  {
    Object o = ProxySubclassGenerator.newProxySubclassInstance(
        ProxyTestClassPrivateConstructor.class, ih);
    assertNotNull("The new instance was null", o);

  }
  
  /**
   * Test object equality between real and proxy using a Collaborator
   */
  @Test
  public void testObjectEquality() throws Exception
  {
    Object delegate = TEST_CLASS.newInstance();
    InvocationHandler collaborator = new Collaborator(null, null, delegate);
    Object o = ProxySubclassGenerator.newProxySubclassInstance(TEST_CLASS, collaborator);
    //Calling equals on the proxy with an arg of the unwrapped object should be true
    assertTrue("The proxy object should be equal to its delegate",o.equals(delegate));
    InvocationHandler collaborator2 = new Collaborator(null, null, delegate);
    Object o2 = ProxySubclassGenerator.newProxySubclassInstance(TEST_CLASS, collaborator2);
    //The proxy of a delegate should equal another proxy of the same delegate
    assertTrue("The proxy object should be equal to another proxy instance of the same delegate", o2.equals(o));
  }
  

  private Class<?> getGeneratedSubclass() throws Exception
  {
    return ProxySubclassGenerator.getProxySubclass(TEST_CLASS);
  }

  private Object getSubclassInstance(Class<?> clazz) throws Exception
  {
    return clazz.getConstructor(InvocationHandler.class).newInstance(ih);
  }

  private class FakeInvocationHandler implements InvocationHandler
  {
    private Object delegate = null;
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     * java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
      Object result = method.invoke(delegate, args);
      return result;
    }

    void setDelegate(Object delegate){
      this.delegate = delegate;
    }
    
  }
}
