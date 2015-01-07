/**
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
package org.apache.aries.blueprint.itests.cm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.apache.aries.blueprint.itests.AbstractBlueprintIntegrationTest;
import org.apache.aries.blueprint.itests.Helper;
import org.apache.aries.blueprint.itests.cm.service.Foo;
import org.apache.aries.blueprint.itests.cm.service.FooFactory;
import org.apache.aries.blueprint.itests.cm.service.FooInterface;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ManagedServiceFactoryUseSystemBundleTest extends AbstractBlueprintIntegrationTest {
	private static final String CM_BUNDLE = "org.apache.aries.blueprint.cm";
	private static final String TEST_BUNDLE = "org.apache.aries.blueprint.cm.test.b1";
	@Inject
	ConfigurationAdmin ca;
	
	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		probe.setHeader(Constants.EXPORT_PACKAGE, Foo.class.getPackage().getName());
    	probe.setHeader(Constants.IMPORT_PACKAGE, Foo.class.getPackage().getName());
		return probe;
	}

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
    	InputStream testBundle = TinyBundles.bundle()
    		.add(FooInterface.class)
    		.add(Foo.class)
    		.add(FooFactory.class)
    		.add("OSGI-INF/blueprint/context.xml", 
    				getResource("ManagedServiceFactoryTest.xml"))
    		.set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE)
    		.set(Constants.EXPORT_PACKAGE, Foo.class.getPackage().getName())
    		.set(Constants.IMPORT_PACKAGE, Foo.class.getPackage().getName())
    		.build(TinyBundles.withBnd());
    	return new Option[] {
    			baseOptions(),
                CoreOptions.systemProperty("org.apache.aries.blueprint.use.system.context").value("true"),
    			Helper.blueprintBundles(),
    			CoreOptions.keepCaches(),
    			CoreOptions.streamBundle(testBundle)
    	};
    }

	ServiceRegistration eventHook;
	ServiceRegistration findHook;
	@Before
	public void regiserHook() throws BundleException {
		context().getBundleByName(CM_BUNDLE).stop();
		final BundleContext systemContext = context().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
		eventHook = context().registerService(EventListenerHook.class, new EventListenerHook() {

			@Override
			public void event(ServiceEvent event,
					Map contexts) {
				if (CM_BUNDLE.equals(event.getServiceReference().getBundle().getSymbolicName())) {
					// hide from everything but the system bundle
					// TODO on R6 we should be able to even try hiding from the system bundle
					// R5 it was not clear if hooks could hide from the system bundle
					// equinox R5 does allow hiding from system bundle
					contexts.keySet().retainAll(Collections.singleton(systemContext));
				}
			}

		}, null);
		findHook = context().registerService(FindHook.class, new FindHook(){
			@Override
			public void find(BundleContext context, String arg1, String arg2,
					boolean arg3, Collection references) {
				// hide from everything but the system bundle
				// TODO on R6 we should be able to even try hiding from the system bundle
				// R5 it was not clear if hooks could hide from the system bundle
				// equinox R5 does allow hiding from system bundle
				if (!context.equals(systemContext)) {
					for (Iterator<ServiceReference> iReferences = references.iterator(); iReferences.hasNext();) {
						if (CM_BUNDLE.equals(iReferences.next().getBundle().getSymbolicName())) {
							iReferences.remove();
						}
					}
				}
			}
			
		}, null);
		context().getBundleByName(CM_BUNDLE).start();
	}

	@After
	public void unregisterHook() {
		eventHook.unregister();
		findHook.unregister();
	}

    @Test
    public void test1() throws Exception {
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);
        
		ServiceReference sr = getServiceRef(Foo.class, "(key=foo1)");
        Foo foo = (Foo)context().getService(sr);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());
        assertEquals("5", sr.getProperty("a"));
        assertNull(sr.getProperty("b"));

        props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("b", "foo");
        cf.update(props);
        Thread.sleep(500);

        // No update of bean after creation
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());

        // Only initial update of service properties
        assertEquals("5", sr.getProperty("a"));
        assertNull(sr.getProperty("b"));
    }

    @Test
    public void test2() throws Exception {
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory2", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);

		ServiceReference sr = getServiceRef(Foo.class, "(key=foo2)");
		Foo foo = (Foo)context().getService(sr);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());
        assertNull(sr.getProperty("a"));
        assertNull(sr.getProperty("b"));

        props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("b", "foo");
        cf.update(props);

        // Update after creation
        Thread.sleep(500);
        assertEquals(5, foo.getA());
        assertEquals("foo", foo.getB());

        // No update of service properties
        assertNull(sr.getProperty("a"));
        assertNull(sr.getProperty("b"));
    }



    @Test
    public void test3() throws Exception {
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory3", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);

		ServiceReference sr = getServiceRef(Foo.class, "(&(key=foo3)(a=5))");
        assertNotNull(sr);
        Foo foo = (Foo) context().getService(sr);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());
        assertEquals("5", sr.getProperty("a"));
        assertNull(sr.getProperty("b"));

        props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("b", "foo");
        cf.update(props);

        // Update after creation
        Thread.sleep(500);
        assertEquals(5, foo.getA());
        assertEquals("foo", foo.getB());

        // Update of service properties
        assertEquals("5", sr.getProperty("a"));
        assertEquals("foo", sr.getProperty("b"));
        cf.delete();
    }

	@Test
    public void testCreateAndUpdate() throws Exception {
        Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory3", null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("a", "5");
        cf.update(props);

        Configuration cf2 = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory3", null);
        Hashtable<String,String> props2 = new Hashtable<String,String>();
        props2.put("a", "7");
        cf2.update(props2);

        ServiceReference sr = getServiceRef(Foo.class, "(&(key=foo3)(a=5))");
        ServiceReference sr2 = getServiceRef(Foo.class, "(&(key=foo3)(a=7))");

        Foo foo = (Foo) context().getService(sr);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals("default", foo.getB());
        assertEquals("5", sr.getProperty("a"));
        assertNull(sr.getProperty("b"));

        Foo foo2 = (Foo) context().getService(sr2);
        assertNotNull(foo2);
        assertEquals(7, foo2.getA());
        assertEquals("default", foo2.getB());
        assertEquals("7", sr2.getProperty("a"));
        assertNull(sr2.getProperty("b"));

        props = new Hashtable<String,String>();
        props.put("a", "5");
        props.put("b", "foo");
        cf.update(props);

        props2 = new Hashtable<String,String>();
        props2.put("a", "7");
        props2.put("b", "foo2");
        cf2.update(props2);

        // Update after creation
        Thread.sleep(500);
        assertEquals(5, foo.getA());
        assertEquals("foo", foo.getB());

        // Update of service properties
        assertEquals("5", sr.getProperty("a"));
        assertEquals("foo", sr.getProperty("b"));

        // 2a Update after creation
        assertEquals(7, foo2.getA());
        assertEquals("foo2", foo2.getB());

        // 2b Update of service properties
        assertEquals("7", sr2.getProperty("a"));
        assertEquals("foo2", sr2.getProperty("b"));
        cf.delete();
        cf2.delete();
    }

  @Test
  public void testCreateAndUpdateUsingUpdateMethod() throws Exception {
    Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory4", null);
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put("a", "5");
    cf.update(props);

    Configuration cf2 = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory4", null);
    Hashtable<String, String> props2 = new Hashtable<String, String>();
    props2.put("a", "7");
    cf2.update(props2);

    ServiceReference sr = getServiceRef(Foo.class, "(&(key=foo4)(a=5))");
    ServiceReference sr2 = getServiceRef(Foo.class, "(&(key=foo4)(a=7))");

    Foo foo = (Foo) context().getService(sr);
    assertNotNull(foo);
    assertEquals(5, foo.getA());
    assertEquals("default", foo.getB());
    assertEquals("5", sr.getProperty("a"));
    assertNull(sr.getProperty("b"));

    Foo foo2 = (Foo) context().getService(sr2);
    assertNotNull(foo2);
    assertEquals(7, foo2.getA());
    assertEquals("default", foo2.getB());
    assertEquals("7", sr2.getProperty("a"));
    assertNull(sr2.getProperty("b"));

    props = new Hashtable<String, String>();
    props.put("a", "5");
    props.put("b", "foo");
    cf.update(props);

    props2 = new Hashtable<String, String>();
    props2.put("a", "7");
    props2.put("b", "foo2");
    cf2.update(props2);

    // Update after creation
    Thread.sleep(500);
    assertEquals(5, foo.getA());
    assertEquals("foo", foo.getB());

    // Update of service properties
    assertEquals("5", sr.getProperty("a"));
    assertEquals("foo", sr.getProperty("b"));

    // 2a Update after creation
    assertEquals(7, foo2.getA());
    assertEquals("foo2", foo2.getB());

    // 2b Update of service properties
    assertEquals("7", sr2.getProperty("a"));
    assertEquals("foo2", sr2.getProperty("b"));
  }
  
  @Test
  public void testFactoryCreation() throws Exception {
    Configuration cf = ca.createFactoryConfiguration("blueprint-sample-managed-service-factory5", null);
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put("a", "5");
    cf.update(props);

	ServiceReference sr = getServiceRef(Foo.class, "(key=foo5)");
    Foo foo = (Foo) context().getService(sr);
    assertNotNull(foo);
    assertEquals(5, foo.getA());
    assertEquals("default", foo.getB());
    assertEquals("5", sr.getProperty("a"));
    assertNull(sr.getProperty("b"));

    props = new Hashtable<String, String>();
    props.put("a", "5");
    props.put("b", "foo");
    cf.update(props);
    Thread.sleep(500);

    // No update of bean after creation
    assertEquals(5, foo.getA());
    assertEquals("default", foo.getB());

    // Only initial update of service properties
    assertEquals("5", sr.getProperty("a"));
    assertNull(sr.getProperty("b"));
  }
  
	private ServiceReference getServiceRef(Class serviceInterface, String filter) throws InvalidSyntaxException {
		int tries = 0;
		do {
			 ServiceReference[] srAr = bundleContext.getServiceReferences(serviceInterface.getName(), filter);
			 if (srAr != null && srAr.length > 0) {
				 return (ServiceReference) srAr[0];
			 }
			 tries ++;
			 try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Ignore
			}
		}  while (tries < 100);
      throw new RuntimeException("Could not find service " + serviceInterface.getName() + ", " + filter);
	}
}
