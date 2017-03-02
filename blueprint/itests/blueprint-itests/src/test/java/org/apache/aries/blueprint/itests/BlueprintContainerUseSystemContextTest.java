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

import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;

/**
 * Shows that the blueprint extender uses the system bundle to find user bundles if the respective property is set
 */
public class BlueprintContainerUseSystemContextTest extends AbstractBlueprintIntegrationTest {

	ServiceRegistration eventHook;
	ServiceRegistration findHook;
	@Before
	public void regiserHook() {
		final BundleContext systemContext = context().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
		eventHook = context().registerService(EventHook.class, new EventHook() {
			@Override
			public void event(BundleEvent event,
					Collection contexts) {
				if ("org.apache.aries.blueprint.sample".equals(event.getBundle().getSymbolicName())) {
					// hide sample from everything but the system bundle
					// TODO on R6 we should be able to even try hiding from the system bundle
					// R5 it was not clear if hooks could hide from the system bundle
					// equinox R5 does allow hiding from system bundle
					contexts.retainAll(Collections.singleton(systemContext));
				}
			}
		}, null);
		findHook = context().registerService(FindHook.class, new FindHook(){
			@Override
			public void find(BundleContext context, Collection bundles) {
				if (context.equals(systemContext)) {
					// TODO on R6 we should be able to even try hiding from the system bundle
					// R5 it was not clear if hooks could hide from the system bundle
					// equinox R5 does allow hiding from system bundle
					return;
				}
				for (Iterator iBundles = bundles.iterator(); iBundles.hasNext();) {
					if ("org.apache.aries.blueprint.sample".equals(((Bundle) iBundles.next()).getSymbolicName())) {
						// hide sample from everything
						iBundles.remove();
					}
				}
			}}, null);
	}

	@After 
	public void unregisterHook() {
		eventHook.unregister();
		findHook.unregister();
	}

    @Test
    public void test() throws Exception {
    	applyCommonConfiguration(context());
        Bundle bundle = context().installBundle(sampleBundleOption().getURL());
        assertNotNull(bundle);
        bundle.start();
        
        // do the test
        Helper.testBlueprintContainer(context(), bundle);
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            CoreOptions.systemProperty("org.apache.aries.blueprint.use.system.context").value("true"),
            Helper.blueprintBundles()
        };
    }

}
