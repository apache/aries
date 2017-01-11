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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.proxy.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.ProxyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractProxyTest {
  @Inject
  BundleContext bundleContext;
  
  @Inject
  ProxyManager mgr;

  public final static class TestCallable implements Callable<Object> {
    private Object list = new ArrayList<Object>();

    public Object call() throws Exception {
      return list;
    }
    
    public void setReturn(Object o) {
      list = o;
    }
  }
  
  public static class TestDelegate extends AbstractList<String> implements Callable<String> {
    
    private final String message;
 
    /**
     * On HotSpot VMs newer than 1.6 u33, we can only generate subclass proxies for classes
     * with a no-args constructor.
     */
    protected TestDelegate() {
        super();
        this.message = null;
      }

    public TestDelegate(String message) {
      super();
      this.message = message;
    }
    
    public String call() throws Exception {
      return message;
    }
    
    public boolean equals(Object o) {
      if(o instanceof TestDelegate){
        return message.equals(((TestDelegate)o).message);
      }
      return false;
    }
    
    public void throwException() {
      throw new RuntimeException();
    }
    
    public void testInternallyCaughtException() {
      try {
        throw new RuntimeException();
      } catch (RuntimeException re) {
        // no op
      }
    }

    @Override
    public String get(int location) {
      return null;
    }

    @Override
    public int size() {
      return 0;
    }
  }
  
  private class TestListener implements InvocationListener {

    boolean preInvoke = false;
    boolean postInvoke = false;
    boolean postInvokeExceptionalReturn = false;
    Object token;
    
    public Object preInvoke(Object proxy, Method m, Object[] args)
        throws Throwable {
      preInvoke = true;
      token = new Object();
      return token;
    }

    public void postInvoke(Object token, Object proxy, Method m,
        Object returnValue) throws Throwable {
      postInvoke = this.token == token;
    }

    public void postInvokeExceptionalReturn(Object token, Object proxy,
        Method m, Throwable exception) throws Throwable {
      postInvokeExceptionalReturn = this.token == token;
    }
    
    public void clear() {
      preInvoke = false;
      postInvoke = false;
      postInvokeExceptionalReturn = false;
      token = null;
    }
  }

  @Test
  public void testEquals() throws Exception {
    Bundle b = bundleContext.getBundle();
    
    TestCallable c = new TestCallable();
    c.setReturn(new TestDelegate("One"));
    
    TestCallable c2 = new TestCallable();
    c.setReturn(new TestDelegate("Two"));
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(List.class);
    Object proxy = mgr.createDelegatingProxy(b, classes, c, new TestDelegate("Three"));
    Object otherProxy = mgr.createDelegatingProxy(b, classes, c, new TestDelegate("Four"));
    Object totallyOtherProxy = mgr.createDelegatingProxy(b, classes, c2, new TestDelegate("Five"));
    assertTrue("The object is not equal to itself", proxy.equals(proxy));
    assertTrue("The object is not equal to another proxy of itself", proxy.equals(otherProxy));
    assertFalse("The object is equal to proxy to another object", proxy.equals(totallyOtherProxy));
  }

  @Test
  public void testDelegation() throws Exception {
    Bundle b = bundleContext.getBundle();
    
    TestCallable c = new TestCallable();
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(TestDelegate.class);
    
    TestDelegate proxy = (TestDelegate) mgr.createDelegatingProxy(b, classes, c, new TestDelegate(""));
    
    c.setReturn(new TestDelegate("Hello"));
    
    assertEquals("Wrong message", "Hello", proxy.call());
    
    c.setReturn(new TestDelegate("Hello again"));
    assertEquals("Wrong message", "Hello again", proxy.call());
  }
  
  @Test
  public void testInterception() throws Exception {
    Bundle b = bundleContext.getBundle();
    
    TestDelegate td = new TestDelegate("Hello");
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(TestDelegate.class);
    
    TestListener tl = new TestListener();
    
    TestDelegate proxy = (TestDelegate) mgr.createInterceptingProxy(b, classes, td, tl);
    
    //We need to call clear here, because the object will have had its toString() called
    tl.clear();
    assertCalled(tl, false, false, false);
    
    assertEquals("Wrong message", "Hello", proxy.call());
    assertCalled(tl, true, true, false);
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    try {
      proxy.throwException();
      fail("Should throw an exception");
    } catch (RuntimeException re) {
      assertCalled(tl, true, false, true);
    }
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    try {
      proxy.testInternallyCaughtException();
    } finally {
      assertCalled(tl, true, true, false);
    }
  }
  
  @Test
  public void testDelegationAndInterception() throws Exception {
    Bundle b = bundleContext.getBundle();
    
    TestCallable c = new TestCallable();
    
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(TestDelegate.class);
    
    TestListener tl = new TestListener();
    
    TestDelegate proxy = (TestDelegate) mgr.createDelegatingInterceptingProxy(b, classes, c, new TestDelegate(""), tl);
    
    c.setReturn(new TestDelegate("Hello"));
    
    //We need to call clear here, because the object will have had its toString() called
    tl.clear();
    assertCalled(tl, false, false, false);
    
    assertEquals("Wrong message", "Hello", proxy.call());
    assertCalled(tl, true, true, false);
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    c.setReturn(new TestDelegate("Hello again"));
    
    assertEquals("Wrong message", "Hello again", proxy.call());
    assertCalled(tl, true, true, false);
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    try {
      proxy.throwException();
      fail("Should throw an exception");
    } catch (RuntimeException re) {
      assertCalled(tl, true, false, true);
    }
    
    tl.clear();
    assertCalled(tl, false, false, false);
    
    try {
      proxy.testInternallyCaughtException();
    } finally {
      assertCalled(tl, true, true, false);
    }
  }
  
  private void assertCalled(TestListener listener, boolean pre, boolean post, boolean ex) {
    assertEquals(pre, listener.preInvoke);
    assertEquals(post, listener.postInvoke);
    assertEquals(ex, listener.postInvokeExceptionalReturn);
  }
  
  protected Option generalOptions() {
	  String localRepo = System.getProperty("maven.repo.local");
      if (localRepo == null) {
          localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
      }
     return composite(
              CoreOptions.junitBundles(),
              systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
              when(localRepo != null).useOptions(CoreOptions.vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo)),
              mavenBundle("org.ow2.asm", "asm-debug-all").versionAsInProject(),
              mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
              mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject()
              /* vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
              waitForFrameworkStartup(),*/
     );
  }

  protected Option[] proxyBundles()
  {
    return options(
      generalOptions(),
      mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api").versionAsInProject(),
      mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl").versionAsInProject()
    );
  }

    protected Option[] proxyUberBundle() {
      return options(
        generalOptions(),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject()
      );
    }

}
