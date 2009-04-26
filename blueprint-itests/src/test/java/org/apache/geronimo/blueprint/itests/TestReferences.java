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
import java.util.Hashtable;
import java.util.List;

import org.apache.geronimo.blueprint.sample.BindingListener;
import org.apache.geronimo.blueprint.sample.InterfaceA;
import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.ServiceUnavailableException;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class TestReferences extends AbstractIntegrationTest {

    public void testUnaryReference() throws Exception {
        BlueprintContext blueprintContext = getBlueprintContextForBundle("blueprint-sample");
        assertNotNull(blueprintContext);

        BindingListener listener = (BindingListener) blueprintContext.getComponent("bindingListener");
        assertNull(listener.getA());
        assertNull(listener.getReference());

        InterfaceA a = (InterfaceA) blueprintContext.getComponent("ref2");
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
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Hello world!", a.hello("world"));

        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(1));
        ServiceRegistration reg2 = bundleContext.registerService(InterfaceA.class.getName(), new InterfaceA() {
            public String hello(String msg) {
                return "Good morning " + msg + "!";
            }
        }, props);
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Good morning world!", a.hello("world"));

        reg2.unregister();
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Hello world!", a.hello("world"));

        reg1.unregister();
        assertNull(listener.getA());
        assertNull(listener.getReference());
        try {
            a.hello("world");
            fail("A ServiceUnavailableException should have been thrown");
        } catch (ServiceUnavailableException e) {
            // Ignore, expected
        }
    }

    public void testListReferences() throws Exception {
        BlueprintContext blueprintContext = getBlueprintContextForBundle("blueprint-sample");
        assertNotNull(blueprintContext);

        BindingListener listener = (BindingListener) blueprintContext.getComponent("listBindingListener");
        assertNull(listener.getA());
        assertNull(listener.getReference());

        List refs = (List) blueprintContext.getComponent("ref-list");
        assertNotNull(refs);
        assertTrue(refs.isEmpty());

        ServiceRegistration reg1 = bundleContext.registerService(InterfaceA.class.getName(), new InterfaceA() {
            public String hello(String msg) {
                return "Hello " + msg + "!";
            }
        }, null);
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals(1, refs.size());
        InterfaceA a = (InterfaceA) refs.get(0);
        assertNotNull(a);
        assertEquals("Hello world!", a.hello("world"));

    }

    protected BlueprintContext getBlueprintContextForBundle(String symbolicName) throws Exception {
        String filter = "(&(" + Constants.OBJECTCLASS + "=" + BlueprintContext.class.getName() + ")(osgi.blueprint.context.symbolicname=" + symbolicName + "))";
        ServiceTracker tracker = new ServiceTracker(bundleContext, org.osgi.framework.FrameworkUtil.createFilter(filter), null);
        tracker.open();
        return (BlueprintContext) tracker.waitForService(5000);
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
                getBundle("org.apache.geronimo", "blueprint-sample"),
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