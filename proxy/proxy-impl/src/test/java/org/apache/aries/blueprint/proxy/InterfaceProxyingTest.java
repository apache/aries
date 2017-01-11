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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.proxy.AbstractProxyTest.TestListener;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.proxy.impl.interfaces.ClassLoaderProxy;
import org.apache.aries.proxy.impl.interfaces.InterfaceProxyGenerator;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWiring;


@SuppressWarnings("unchecked")
public class InterfaceProxyingTest {

  public final static class TestCallable implements Callable<Object> {
    
    private Object list = new Callable<Object>() {


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

  private Bundle testBundle;

  /**
   * Extended BundleMock which handles update() and adapt() methods
   */
  public static class BundleMockEx extends BundleMock {
    private BundleWiring currentWiring = Skeleton.newMock(BundleWiring.class);

    public BundleMockEx(String name, Dictionary<?, ?> properties) {
      super(name, properties);
    }

    public <A> A adapt(Class<A> type) {
      if (type == BundleWiring.class) {
        return (A) currentWiring;
      }
      return null;
    }

    public void update() throws BundleException {
      this.currentWiring = Skeleton.newMock(BundleWiring.class);
    }
  }

  @Before
  public void setup() {
    testBundle = Skeleton.newMock(new BundleMockEx("test",
        new Hashtable<Object, Object>()), Bundle.class);
  }
  
  @Test
  public void testGetProxyInstance1() throws Exception{
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>(Arrays.asList(Closeable.class));
    
    Object o = InterfaceProxyGenerator.getProxyInstance(testBundle, null, classes, constantly(null), null);
    
    assertTrue(o instanceof Closeable);
  }
  
  @Test
  public void testGetProxyInstance2() throws Exception{
    
    Collection<Class<? extends Object>> classes = Arrays.asList(Closeable.class, Iterable.class, Map.class);
    
    Object o = InterfaceProxyGenerator.getProxyInstance(testBundle, null, classes, constantly(null), null);
    
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
    
    Callable<Object> o = (Callable<Object>) InterfaceProxyGenerator.getProxyInstance(testBundle, 
        null, classes, tc, tl);
    
    assertCalled(tl, false, false, false);
    
    assertNull(null, o.call());
    
    assertCalled(tl, true, true, false);
    
    assertEquals(Callable.class.getMethod("call"), 
        tl.getLastMethod());

    tl.clear();
    assertCalled(tl, false, false, false);
    
    tc.setReturn(new Callable<Object>() {

 
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
    
    Object o1 = InterfaceProxyGenerator.getProxyInstance(testBundle, null, classes, constantly(null), null);
    Object o2 = InterfaceProxyGenerator.getProxyInstance(testBundle, null, classes, constantly(null), null);
    
    assertSame(o1.getClass(), o2.getClass());
  }
  
  @Test
  public void testComplexInterface() throws Exception {
    Collection<Class<?>> classes = new ArrayList<Class<?>>(Arrays.asList(ProxyTestInterface.class));
    
    final TestCallable tc = new TestCallable();
    tc.setReturn(5);
    
    Object o = InterfaceProxyGenerator.getProxyInstance(testBundle, null, classes, constantly(tc), null);
    
    assertTrue(o instanceof ProxyTestInterface);
    
    assertTrue(o instanceof Callable);
    
    assertEquals(5, ((Callable<Object>)o).call());
  }
  
  @Test
  public void testHandlesObjectMethods() throws Exception {
      TestListener listener = new TestListener();
      List<String> list = Arrays.asList("one", "two", "three");
      Object proxied = InterfaceProxyGenerator.getProxyInstance(testBundle, null, Arrays.<Class<?>>asList(List.class), constantly(list), listener);
      
      // obeys hashCode and equals, they *are* on the interface (actually they're
      // on several interfaces, we process them in alphabetical order, so Collection
      // comes ahead of List.
      assertTrue(proxied.equals(Arrays.asList("one", "two", "three")));
      assertEquals(Collection.class.getMethod("equals", Object.class), listener.getLastMethod());
      listener.clear();
      assertEquals(Arrays.asList("one", "two", "three").hashCode(), proxied.hashCode());
      assertEquals(Collection.class.getMethod("hashCode"), listener.getLastMethod());
      listener.clear();
      // and toString
      assertEquals(list.toString(), proxied.toString());
      assertEquals(Object.class.getMethod("toString"), listener.getLastMethod());
      listener.clear();
      
      Runnable runnable = new Runnable() {
        public void run() {}
      };
      proxied = InterfaceProxyGenerator.getProxyInstance(testBundle, null, Arrays.<Class<?>>asList(Runnable.class), constantly(runnable), listener);
      
      // obeys hashCode and equals, they *are not* on the interface
      assertTrue(proxied.equals(runnable));
      assertEquals(Object.class.getMethod("equals", Object.class), listener.getLastMethod());
      listener.clear();
      assertEquals(runnable.hashCode(), proxied.hashCode());
      assertEquals(Object.class.getMethod("hashCode"), listener.getLastMethod());
      listener.clear();
  }
  
  private static class TestClassLoader extends ClassLoader {
      public TestClassLoader() throws Exception {
          
          InputStream is = TestClassLoader.class.getClassLoader().getResourceAsStream("org/apache/aries/blueprint/proxy/TestInterface.class");
          ByteArrayOutputStream bout = new ByteArrayOutputStream();

          int b;
          while ((b = is.read()) != -1) {
              bout.write(b);
          }
          
          is.close();
          
          byte[] bytes = bout.toByteArray();
          defineClass("org.apache.aries.blueprint.proxy.TestInterface", bytes, 0, bytes.length);
      }
  }
  
  @Test
  public void testNoStaleProxiesForRefreshedBundle() throws Exception {
      Bundle bundle = (Bundle) Skeleton.newMock(new Class<?>[] { Bundle.class, ClassLoaderProxy.class });      
      Skeleton skel = Skeleton.getSkeleton(bundle);
      
      TestClassLoader loader = new TestClassLoader();
      skel.setReturnValue(new MethodCall(ClassLoaderProxy.class, "getClassLoader"), loader);
      skel.setReturnValue(new MethodCall(Bundle.class, "getLastModified"), 10l);
      skel.setReturnValue(new MethodCall(Bundle.class, "adapt", BundleWiring.class), Skeleton.newMock(BundleWiring.class));

      Class<?> clazz = loader.loadClass("org.apache.aries.blueprint.proxy.TestInterface");
      
      Object proxy = InterfaceProxyGenerator.getProxyInstance(bundle, null, Arrays.<Class<?>>asList(clazz), constantly(null), null);
      assertTrue(clazz.isInstance(proxy));

      ClassLoader parent1 = proxy.getClass().getClassLoader().getParent();
      
      /* Now again but with a changed classloader as if the bundle had refreshed */
      
      TestClassLoader loaderToo = new TestClassLoader();
      skel.setReturnValue(new MethodCall(ClassLoaderProxy.class, "getClassLoader"), loaderToo);
      skel.setReturnValue(new MethodCall(Bundle.class, "getLastModified"), 20l);

      // let's change the returned revision
      skel.setReturnValue(new MethodCall(Bundle.class, "adapt", BundleWiring.class), Skeleton.newMock(BundleWiring.class));
      
      Class<?> clazzToo = loaderToo.loadClass("org.apache.aries.blueprint.proxy.TestInterface");
      
      Object proxyToo = InterfaceProxyGenerator.getProxyInstance(bundle, null, Arrays.<Class<?>>asList(clazzToo), constantly(null), null);
      assertTrue(clazzToo.isInstance(proxyToo));

      ClassLoader parent2= proxyToo.getClass().getClassLoader().getParent();

      // parents should be different, as the are the classloaders of different bundle revisions
      assertTrue(parent1 != parent2);
  }

  protected void assertCalled(TestListener listener, boolean pre, boolean post, boolean ex) {
    assertEquals(pre, listener.preInvoke);
    assertEquals(post, listener.postInvoke);
    assertEquals(ex, listener.postInvokeExceptionalReturn);
  }
  
  private Callable<Object> constantly(final Object result) {
      return new Callable<Object>() {
          public Object call() throws Exception {
              return result;
          }             
        };
  }
}
