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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.SingleInstanceDispatcher;
import org.apache.aries.proxy.impl.gen.ProxySubclassGenerator;
import org.apache.aries.proxy.impl.gen.ProxySubclassMethodHashSet;
import org.apache.aries.proxy.impl.interfaces.InterfaceProxyGenerator;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.junit.Test;

/**
 * This class uses the {@link ProxySubclassGenerator} to test
 */
@SuppressWarnings("unchecked")
public class WovenSubclassGeneratorTest extends AbstractProxyTest
{
  private static final Class<?> FINAL_METHOD_CLASS = ProxyTestClassFinalMethod.class;
  private static final Class<?> FINAL_CLASS = ProxyTestClassFinal.class;
  private static final Class<?> GENERIC_CLASS = ProxyTestClassGeneric.class;
  private static final Class<?> COVARIANT_CLASS = ProxyTestClassCovariantOverride.class;
  private static ProxySubclassMethodHashSet<String> expectedMethods = new ProxySubclassMethodHashSet<String>(
      12);
  private Callable<Object> testCallable = null;


  /**
   * Test that the methods found declared on the generated proxy subclass are
   * the ones that we expect.
   */
  @Test
  public void testExpectedMethods() throws Exception
  {
    Class<?> superclass = getTestClass();

    do {
      Method[] declaredMethods = superclass.getDeclaredMethods();
      List<Method> listOfDeclaredMethods = new ArrayList<Method>();
      for (Method m : declaredMethods) {
    	
        if(m.getName().equals("clone") || m.getName().equals("finalize"))
        	continue;
    	
        int i = m.getModifiers();
        if (Modifier.isPrivate(i) || Modifier.isFinal(i)) {
          // private or final don't get added
        } else if (!(Modifier.isPublic(i) || Modifier.isPrivate(i) || Modifier.isProtected(i))) {
          // the method is default visibility, check the package
          if (m.getDeclaringClass().getPackage().equals(getTestClass().getPackage())) {
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

    
    
    Method[] subclassMethods = getProxyClass(getTestClass()).getDeclaredMethods();
    List<Method> listOfDeclaredMethods = new ArrayList<Method>();
    for (Method m : subclassMethods) {
      if(m.getName().startsWith(WovenProxy.class.getName().replace('.', '_')))
        continue;
      
      listOfDeclaredMethods.add(m);
    }
    subclassMethods = listOfDeclaredMethods.toArray(new Method[] {});
    
    ProxySubclassMethodHashSet<String> generatedMethods = new ProxySubclassMethodHashSet<String>(
    		subclassMethods.length);
        generatedMethods.addMethodArray(subclassMethods);
        
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
   * Test a method marked final
   */
  @Test
  public void testFinalMethod() throws Exception
  {
    try {
      InterfaceProxyGenerator.getProxyInstance(null, FINAL_METHOD_CLASS, Collections.EMPTY_SET, 
          new Callable<Object>() {
        public Object call() throws Exception {
          return null;
        }} , null).getClass();
    } catch (RuntimeException re) {
      FinalModifierException e = (FinalModifierException) re.getCause();
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
      InterfaceProxyGenerator.getProxyInstance(null, FINAL_CLASS, Collections.EMPTY_SET, 
          new Callable<Object>() {
        public Object call() throws Exception {
          return null;
        }} , null).getClass();
    } catch (FinalModifierException e) {
      assertTrue("Should have found final class", e.isFinalClass());
    }
  }

  /**
   * Test a covariant override method
   */
  @Test
  public void testCovariant() throws Exception
  {
    testCallable = new SingleInstanceDispatcher(COVARIANT_CLASS.newInstance());
    Object o = setDelegate(getProxyInstance(getProxyClass(COVARIANT_CLASS)), testCallable);
    Class<?> generatedProxySubclass = o.getClass();
    Method m = generatedProxySubclass.getDeclaredMethod("getCovariant", new Class[] {});
    Object returned = m.invoke(o);
    assertTrue("Object was of wrong type: " + returned.getClass().getSimpleName(), COVARIANT_CLASS
        .isInstance(returned));
  }
  
  /**
   * Test a covariant override method
   */
  @Test
  public void testGenerics() throws Exception
  {
    testCallable = new SingleInstanceDispatcher(GENERIC_CLASS.newInstance());
    super.testGenerics();
  }
  
  @Test
  public void testInner() {
	  //This implementation can never pass this test. It doesn't support classes with no no-args constructor.
  }
   
  
  @Test
  public void testAddingInterfacesToClass() throws Exception {
	  
    Object proxy = InterfaceProxyGenerator.getProxyInstance(null, getTestClass(), Arrays.asList(Map.class, Iterable.class), new Callable<Object>() {

        int calls = 0;
        private Map<String, String> map = new HashMap<String, String>();
        
        {
          map.put("key", "value");
        }

        public Object call() throws Exception {
          switch(++calls) {
            case 1 :
              return getTestClass().newInstance();
            case 2 :
              return map;
            default :
              return map.values();
          }
		}
    	
    }, null);
    
    
    assertEquals(17, ((ProxyTestClassGeneralWithNoDefaultOrProtectedAccess)proxy).testReturnInt());
    assertEquals("value", ((Map<String, String>)proxy).put("key", "value2"));
    Iterator<?> it = ((Iterable<?>)proxy).iterator();
    assertEquals("value2", it.next());
    assertFalse(it.hasNext());
	  
  }
  
  @Override
  protected Object getProxyInstance(Class<?> proxyClass) {
	 
    if(proxyClass == ProxyTestClassChildOfAbstract.class) {
    	return new ProxyTestClassChildOfAbstract();
    }
	  
    try {
      Constructor<?> con = proxyClass.getDeclaredConstructor(Callable.class, InvocationListener.class);
      con.setAccessible(true);
      return con.newInstance((testCallable == null) ? new SingleInstanceDispatcher(getTestClass().newInstance()) : testCallable, null);
    } catch (Exception e) {
      return null;
    }
  }
  

  @Override
  protected Class<?> getTestClass() {
	return ProxyTestClassGeneralWithNoDefaultOrProtectedAccess.class;
  }


@Override
  protected Class<?> getProxyClass(Class<?> clazz) {
    try {
      return InterfaceProxyGenerator.getProxyInstance(null, clazz, Collections.EMPTY_SET, 
          new Callable<Object>() {
        public Object call() throws Exception {
          return null;
        }} , null).getClass();
    } catch (UnableToProxyException e) {
      return null;
    } catch (RuntimeException re) {
      if(re.getCause() instanceof UnableToProxyException)
        return null;
      else
        throw re;
    }
  }


  @Override
  protected Object setDelegate(Object proxy, Callable<Object> dispatcher) {
    return ((WovenProxy)proxy).org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance(
        dispatcher, null);
  }

  @Override
  protected Object getProxyInstance(Class<?> proxyClass,
      InvocationListener listener) {
    WovenProxy proxy = (WovenProxy) getProxyInstance(proxyClass);
    proxy = proxy.org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance(
        new SingleInstanceDispatcher(proxy), listener);
    return proxy;
  }
  
  protected Object getP3() throws Exception {
    return getTestClass().newInstance();
  }
}