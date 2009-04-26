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
package org.apache.geronimo.blueprint.itests;

import java.net.URLDecoder;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Currency;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.apache.servicemix.kernel.testing.support.Counter;
import org.apache.geronimo.blueprint.sample.Foo;
import org.apache.geronimo.blueprint.sample.Bar;
import org.apache.geronimo.blueprint.sample.InterfaceA;
import org.apache.geronimo.blueprint.sample.CurrencyTypeConverter;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.ServiceUnavailableException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiListenerUtils;

public class TestBlueprintContext extends AbstractIntegrationTest {

    public void test() throws Exception {
        // Create a config to check the property placeholder
        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample");
        Hashtable props = new Hashtable();
        props.put("key.b", "10");
        cf.update(props);

        Resource res = locateBundle(getBundle("org.apache.geronimo", "blueprint-sample"));
        Bundle bundle = installBundle(res);
        assertNotNull(bundle);

        bundle.start();

        BlueprintContext blueprintContext = getBlueprintContextForBundle("blueprint-sample", 5000);
        assertNotNull(blueprintContext);

        Object obj = blueprintContext.getComponent("bar");
        assertNotNull(obj);
        assertEquals(Bar.class, obj.getClass());
        Bar bar = (Bar) obj;
        assertNotNull(bar.getContext());
        assertEquals("Hello FooBar", bar.getValue());
        assertNotNull(bar.getList());
        assertEquals(2, bar.getList().size());
        assertEquals("a list element", bar.getList().get(0));
        assertEquals(Integer.valueOf(5), bar.getList().get(1));
        obj = blueprintContext.getComponent("foo");
        assertNotNull(obj);
        assertEquals(Foo.class, obj.getClass());
        Foo foo = (Foo) obj;
        assertEquals(5, foo.getA());
        assertEquals(10, foo.getB());
        assertSame(bar, foo.getBar());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertEquals(new SimpleDateFormat("yyyy.MM.dd").parse("2009.04.17"), foo.getDate());

        assertTrue(foo.isInitialized());
        assertFalse(foo.isDestroyed());

        obj = getOsgiService(Foo.class, 5000);
        assertNotNull(obj);
        assertSame(foo, obj);

        bundle.stop();

        Thread.sleep(1000);

        try {
            blueprintContext = getBlueprintContextForBundle("blueprint-sample", 1);
            fail("ModuleContext should have been unregistered");
        } catch (Exception e) {
            // Expected, as the module context should have been unregistered
        }

        assertTrue(foo.isInitialized());
        assertTrue(foo.isDestroyed());
    }

    protected BlueprintContext getBlueprintContextForBundle(String symbolicName, long timeout) throws Exception {
        return getOsgiService(BlueprintContext.class, "(osgi.blueprint.context.symbolicname=" + symbolicName + ")", timeout);
    }

    public <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        // translate from seconds to miliseconds
        long time = timeout * 1000;

        // use the counter to make sure the threads block
        final Counter counter = new Counter("waitForOsgiService on bnd=" + type.getName());

        counter.increment();

        final List<T> services = new ArrayList<T>();

        ServiceListener listener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    services.add((T) bundleContext.getService(event.getServiceReference()));
                    counter.decrement();
                }
            }
        };

        String flt = OsgiFilterUtils.unifyFilter(type.getName(), filter);
        OsgiListenerUtils.addServiceListener(bundleContext, listener, flt);

        if (logger.isDebugEnabled())
            logger.debug("start waiting for OSGi service=" + type.getName());

        try {
            if (counter.waitForZero(time)) {
                logger.warn("waiting for OSGi service=" + type.getName() + " timed out");
                throw new RuntimeException("Gave up waiting for OSGi service '" + type.getName() + "' to be created");
            }
            else if (logger.isDebugEnabled()) {
                logger.debug("found OSGi service=" + type.getName());
            }
            return services.get(0);
        }
        finally {
            // inform waiting thread
            bundleContext.removeServiceListener(listener);
        }
    }

    /**
	 * The manifest to use for the "virtual bundle" created
	 * out of the test classes and resources in this project
	 *
	 * This is actually the boilerplate manifest with one additional
	 * import-package added. We should provide a simpler customization
	 * point for such use cases that doesn't require duplication
	 * of the entire manifest...
	 */
	protected String getManifestLocation() {
		return "classpath:org/apache/geronimo/blueprint/MANIFEST.MF";
	}

	/**
	 * The location of the packaged OSGi bundles to be installed
	 * for this test. Values are Spring resource paths. The bundles
	 * we want to use are part of the same multi-project maven
	 * build as this project is. Hence we use the localMavenArtifact
	 * helper method to find the bundles produced by the package
	 * phase of the maven build (these tests will run after the
	 * packaging phase, in the integration-test phase).
	 *
	 * JUnit, commons-logging, spring-core and the spring OSGi
	 * test bundle are automatically included so do not need
	 * to be specified here.
	 */
	protected String[] getTestBundlesNames() {
        return new String[] {
                getBundle("org.apache.geronimo", "blueprint-bundle"),
		};
	}

    private Bundle installBundle(Resource location) throws Exception {
        Assert.notNull(bundleContext);
        Assert.notNull(location);
        if (logger.isDebugEnabled())
            logger.debug("Installing bundle from location " + location.getDescription());

        String bundleLocation;

        try {
            bundleLocation = URLDecoder.decode(location.getURL().toExternalForm(), "UTF-8");
        }
        catch (Exception ex) {
            // the URL cannot be created, fall back to the description
            bundleLocation = location.getDescription();
        }

        return bundleContext.installBundle(bundleLocation, location.getInputStream());
    }

}
