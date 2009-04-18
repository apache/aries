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
package org.apache.felix.blueprint.itests;

import java.net.URLDecoder;

import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.apache.geronimo.osgi.example.Foo;
import org.apache.geronimo.osgi.example.Bar;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.context.ModuleContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class Test extends AbstractIntegrationTest {

    public void test() throws Exception {
        Resource res = locateBundle(getBundle("org.apache.felix", "sample"));
        Bundle bundle = installBundle(res);
        assertNotNull(bundle);
        bundle.start();

        ModuleContext moduleContext = getOsgiService(ModuleContext.class, 5000);
        assertNotNull(moduleContext);

        Object obj = moduleContext.getComponent("bar");
        assertNotNull(obj);
        assertEquals(Bar.class, obj.getClass());
        obj = moduleContext.getComponent("foo");
        assertNotNull(obj);
        assertEquals(Foo.class, obj.getClass());

        // TODO: components properties

        Foo foo = getOsgiService(Foo.class, 5000);
        assertNotNull(foo);
        assertSame(foo, obj);

        bundle.stop();
        try {
            moduleContext = getOsgiService(ModuleContext.class, 1);
            fail("ModuleContext should have been unregistered");
        } catch (Exception e) {
            // Expected, as the module context should have been unregistered
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
		return "classpath:org/apache/felix/blueprint/MANIFEST.MF";
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
                getBundle("org.apache.felix", "blueprint-bundle"),
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
