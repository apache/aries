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
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.apache.aries.blueprint.sample.BindingListener;
import org.apache.aries.blueprint.sample.DefaultRunnable;
import org.apache.aries.blueprint.sample.DestroyTest;
import org.apache.aries.blueprint.sample.InterfaceA;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.ServiceUnavailableException;

@RunWith(PaxExam.class)
public class TestReferences extends AbstractBlueprintIntegrationTest {

    @SuppressWarnings("rawtypes")
    @Test
    public void testUnaryReference() throws Exception {
        BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        BindingListener listener = (BindingListener) blueprintContainer.getComponentInstance("bindingListener");
        assertNull(listener.getA());
        assertNull(listener.getReference());

        InterfaceA a = (InterfaceA) blueprintContainer.getComponentInstance("ref2");
        try {
            a.hello("world");
            fail("A ServiceUnavailableException should have been thrown");
        } catch (ServiceUnavailableException e) {
            // Ignore, expected
        }

        ServiceRegistration reg1 = bundleContext.registerService(InterfaceA.class.getName(), new InterfaceA() {
            public String hello(String msg) {
                return "Hello " + msg + "!";
            }
        }, null);
        waitForAsynchronousHandling();

        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Hello world!", a.hello("world"));

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(1));
        ServiceRegistration reg2 = bundleContext.registerService(InterfaceA.class.getName(), new InterfaceA() {
            public String hello(String msg) {
                return "Good morning " + msg + "!";
            }
        }, props);

        waitForAsynchronousHandling();

        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Hello world!", a.hello("world"));

        reg1.unregister();
        waitForAsynchronousHandling();
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Good morning world!", a.hello("world"));

        reg2.unregister();
        waitForAsynchronousHandling();

        assertNull(listener.getA());
        assertNull(listener.getReference());
        try {
            a.hello("world");
            fail("A ServiceUnavailableException should have been thrown");
        } catch (ServiceUnavailableException e) {
            // Ignore, expected
        }
    }

    @Test
    public void testListReferences() throws Exception {
        BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        BindingListener listener = (BindingListener) blueprintContainer.getComponentInstance("listBindingListener");
        assertNull(listener.getA());
        assertNull(listener.getReference());

        List<?> refs = (List<?>) blueprintContainer.getComponentInstance("ref-list");
        assertNotNull(refs);
        assertTrue(refs.isEmpty());

        InterfaceA testService = new InterfaceA() {
            public String hello(String msg) {
                return "Hello " + msg + "!";
            }
        };
        bundleContext.registerService(InterfaceA.class.getName(), testService, null);
    
        waitForAsynchronousHandling();
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals(1, refs.size());
        InterfaceA a = (InterfaceA) refs.get(0);
        assertNotNull(a);
        assertEquals("Hello world!", a.hello("world"));

    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testDefaultReference() throws Exception {
      BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
      assertNotNull(blueprintContainer);

      Runnable refRunnable = (Runnable) blueprintContainer.getComponentInstance("refWithDefault");
      DefaultRunnable defaultRunnable = (DefaultRunnable) blueprintContainer.getComponentInstance("defaultRunnable");
      refRunnable.run();
      waitForAsynchronousHandling();
      Thread.sleep(2000);
      
      assertEquals("The default runnable was not called", 1, defaultRunnable.getCount());
      
      final AtomicBoolean called = new AtomicBoolean(false);
      Runnable mockService = new Runnable() {
        public void run() {
            called.set(true);
        }
      };
      
      ServiceRegistration reg = bundleContext.registerService(Runnable.class.getName(), mockService, null);
      waitForAsynchronousHandling();
      Thread.sleep(2000);

      refRunnable.run();
      
      assertEquals("The default runnable was called when a service was bound", 1, defaultRunnable.getCount());
      
      Assert.assertTrue("Service should have been called", called.get());
      
      reg.unregister();
      waitForAsynchronousHandling();
      Thread.sleep(2000);

      refRunnable.run();
      
      assertEquals("The default runnable was not called", 2, defaultRunnable.getCount());
    }
    
    @Test
    public void testReferencesCallableInDestroy() throws Exception {
      bundleContext.registerService(Runnable.class.getName(), new Thread(), null);
      
      BlueprintContainer blueprintContainer = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
      assertNotNull(blueprintContainer);
      
      DestroyTest dt = (DestroyTest) blueprintContainer.getComponentInstance("destroyCallingReference");
      
      Bundle b = findBundle("org.apache.aries.blueprint.sample");
      assertNotNull(b);
      b.stop();
      
      assertTrue("The destroy method was called", dt.waitForDestruction(1000));
      
      Exception e = dt.getDestroyFailure();
      
      if (e != null) throw e;
    }

    private Bundle findBundle(String bsn)
    {
      for (Bundle b : bundleContext.getBundles()) {
        if (bsn.equals(b.getSymbolicName())) return b;
      }
      
      return null;
    }

    private void waitForAsynchronousHandling() throws InterruptedException {
      // Since service events are handled asynchronously in AbstractServiceReferenceRecipe, pause
       Thread.sleep(200);
      
   }

   @Configuration
    public static Option[] configuration() {
        return new Option[] {
                CoreOptions.junitBundles(),
                Helper.blueprintBundles(),
                CoreOptions.mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample")
        };
    }

}
