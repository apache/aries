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
package org.apache.aries.proxy.itests;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.apache.aries.proxy.weavinghook.ProxyWeavingController;
import org.apache.aries.proxy.weavinghook.WeavingHelper;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WovenClass;

@ExamReactorStrategy(PerMethod.class)
public abstract class AbstractWeavingProxyTest extends AbstractProxyTest
{

  /**
   * This test does two things. First of all it checks that we can proxy a final 
   * class. It also validates that the class implements WovenProxy, and that the
   * delegation still works
   */
  @Test
  public void checkProxyFinalClass() throws Exception
  {
    Bundle b = bundleContext.getBundle();
    TestCallable dispatcher = new TestCallable();
    TestCallable template = new TestCallable();
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(TestCallable.class);
    @SuppressWarnings("unchecked")
	Callable<Object> o = (Callable<Object>) mgr.createDelegatingProxy(b, classes, 
        dispatcher, template);
    if(!!!(o instanceof WovenProxy))
      fail("Proxy should be woven!");

    Object inner = new Integer(3);
    dispatcher.setReturn(new TestCallable());
    ((TestCallable)dispatcher.call()).setReturn(inner);

    assertSame("Should return the same object", inner, o.call());
  }

  /**
   * This method checks that we correctly proxy a class with final methods.
   */
  @Test
  public void checkProxyFinalMethods() throws Exception
  {
    Bundle b = bundleContext.getBundle();
    Callable<Object> c = new TestCallable();
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    Runnable r = new Runnable() {
      public final void run() {
      }
    };
    classes.add(r.getClass());
    Object o = mgr.createDelegatingProxy(b, classes, c, r);
    if(!!!(o instanceof WovenProxy))
      fail("Proxy should be woven!");
  }

  @Test(expected = FinalModifierException.class)
  public void checkProxyController() throws Exception
  {

    bundleContext.registerService(ProxyWeavingController.class.getName(), new ProxyWeavingController() {

      public boolean shouldWeave(WovenClass arg0, WeavingHelper arg1)
      {
        return false;
      }
    }, null);

    Bundle b = bundleContext.getBundle();
    Callable<Object> c = new TestCallable();
    Collection<Class<?>> classes = new ArrayList<Class<?>>();
    // Don't use anonymous inner class in this test as IBM and Sun load it at a different time
    // For IBM JDK, the anonymous inner class will be loaded prior to the controller is registered.
    Callable<?> callable = new TestFinalDelegate();
    classes.add(callable.getClass());
    Object o = mgr.createDelegatingProxy(b, classes, c, callable);
    if(o instanceof WovenProxy)
      fail("Proxy should not have been woven!");
  }

  private static class TestFinalDelegate extends AbstractList<String> implements Callable<String> {

    @Override
    public String get(int location)
    {
      return null;
    }

    @Override
    public int size()
    {
      return 0;
    }

    public final String call() throws Exception
    {
      return null;
    }
  }
}
